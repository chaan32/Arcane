package com.arcane.Arcane.model.service;

import com.arcane.Arcane.common.Kafka.service.AiScoreKafkaGateway;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.model.dto.AiScoreBenchmarkResponse;
import com.arcane.Arcane.riot.Match.dto.InfoDto;
import com.arcane.Arcane.riot.Match.dto.ParticipantDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonService {
    @Value("${modeling.python-url}")
    private String fastApiUrl;
    @Value("${modeling.transport:http}")
    private String transport;

    private final RestTemplate restTemplate;
    private final AiScoreKafkaGateway aiScoreKafkaGateway;



    public Integer predictScore(InfoDto infoDto, ParticipantDto participantDto) {
        return predictScores(infoDto, List.of(participantDto)).getFirst();
    }

    public List<Integer> predictScores(InfoDto infoDto, List<ParticipantDto> participantDtos) {
        if (participantDtos == null || participantDtos.isEmpty()) {
            return List.of();
        }

        long startedAt = System.currentTimeMillis();
        List<Map<String, Object>> features = participantDtos.stream()
                .map(participantDto -> toScoreFeatures(infoDto, participantDto))
                .toList();

        if (isKafkaTransport()) {
            try {
                List<Double> scores = aiScoreKafkaGateway.requestScores(features);
                log.info(ApiLogSupport.api(
                        "AI 점수 계산",
                        "PythonService.predictScores",
                        "Kafka 완료",
                        "count=" + participantDtos.size()
                                + " | elapsedMs=" + (System.currentTimeMillis() - startedAt)
                ));
                return toIntegerScores(scores, participantDtos);
            } catch (RuntimeException exception) {
                log.warn(ApiLogSupport.api(
                        "AI 점수 계산",
                        "PythonService.predictScores",
                        "Kafka 실패",
                        "count=" + participantDtos.size()
                                + " | fallback=heuristic"
                                + " | reason=" + exception.getMessage()
                ));
                return participantDtos.stream()
                        .map(this::fallbackScore)
                        .toList();
            }
        }

        List<Integer> scores = predictScoresViaHttpSequential(features, participantDtos);
        log.info(ApiLogSupport.api(
                "AI 점수 계산",
                "PythonService.predictScores",
                "HTTP 완료",
                "count=" + participantDtos.size()
                        + " | elapsedMs=" + (System.currentTimeMillis() - startedAt)
        ));
        return scores;
    }

    public AiScoreBenchmarkResponse benchmark(int requestedCount) {
        int count = Math.max(1, Math.min(requestedCount, 200));
        List<Map<String, Object>> featuresList = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            featuresList.add(sampleFeatures(index));
        }

        long httpStartedAt = System.currentTimeMillis();
        for (Map<String, Object> features : featuresList) {
            requestHttpScore(features);
        }
        long httpElapsedMs = System.currentTimeMillis() - httpStartedAt;

        Long kafkaElapsedMs = null;
        String kafkaError = null;
        try {
            long kafkaStartedAt = System.currentTimeMillis();
            aiScoreKafkaGateway.requestScores(featuresList);
            kafkaElapsedMs = System.currentTimeMillis() - kafkaStartedAt;
        } catch (RuntimeException exception) {
            kafkaError = exception.getMessage();
        }

        Double improvementPercent = null;
        if (kafkaElapsedMs != null && httpElapsedMs > 0) {
            improvementPercent = Math.round(((httpElapsedMs - kafkaElapsedMs) * 10000.0 / httpElapsedMs)) / 100.0;
        }

        return new AiScoreBenchmarkResponse(
                count,
                httpElapsedMs,
                kafkaElapsedMs,
                improvementPercent,
                kafkaError
        );
    }

    private List<Integer> predictScoresViaHttpSequential(
            List<Map<String, Object>> featuresList,
            List<ParticipantDto> participantDtos
    ) {
        List<Integer> scores = new ArrayList<>();
        for (int index = 0; index < featuresList.size(); index++) {
            ParticipantDto participantDto = participantDtos.get(index);
            try {
                Double predictedScore = requestHttpScore(featuresList.get(index));
                scores.add(toIntegerScore(predictedScore));
            } catch (RestClientException exception) {
                log.warn(ApiLogSupport.api(
                        "AI 점수 계산 요청",
                        "PythonService.predictScoresViaHttpSequential",
                        "요청 실패",
                        "championId=" + participantDto.getChampionId()
                                + " | puuid=" + participantDto.getPuuid()
                                + " | fallback=heuristic"
                                + " | message=" + exception.getMessage()
                ));
                scores.add(fallbackScore(participantDto));
            }
        }
        return scores;
    }

    private Double requestHttpScore(Map<String, Object> features) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("features", features);
        return restTemplate.postForObject(fastApiUrl + "/predict", request, Double.class);
    }

    private boolean isKafkaTransport() {
        return "kafka".equalsIgnoreCase(transport);
    }

    private List<Integer> toIntegerScores(List<Double> predictedScores, List<ParticipantDto> participantDtos) {
        if (predictedScores == null || predictedScores.size() != participantDtos.size()) {
            return participantDtos.stream()
                    .map(this::fallbackScore)
                    .toList();
        }

        List<Integer> scores = new ArrayList<>();
        for (Double predictedScore : predictedScores) {
            scores.add(toIntegerScore(predictedScore));
        }
        return scores;
    }

    private Map<String, Object> toScoreFeatures(InfoDto infoDto, ParticipantDto participantDto) {
        Map<String, Object> features = new LinkedHashMap<>();

        features.put("game_duration", infoDto.getGameDuration());
        features.put("game_end_timestamp", infoDto.getGameEndTimestamp());
        features.put("queue_id", value(infoDto.getQueueId()));
        features.put("game_mode", text(infoDto.getGameMode()));

        features.put("champion_id", value(participantDto.getChampionId()));
        features.put("win", Boolean.TRUE.equals(participantDto.getWin()));
        features.put("champ_level", value(participantDto.getChampLevel()));
        features.put("team_position", text(participantDto.getTeamPosition()));

        features.put("item0", value(participantDto.getItem0()));
        features.put("item1", value(participantDto.getItem1()));
        features.put("item2", value(participantDto.getItem2()));
        features.put("item3", value(participantDto.getItem3()));
        features.put("item4", value(participantDto.getItem4()));
        features.put("item5", value(participantDto.getItem5()));
        features.put("item6", value(participantDto.getItem6()));

        features.put("kda", resolveKda(participantDto));
        features.put("kills", value(participantDto.getKills()));
        features.put("deaths", value(participantDto.getDeaths()));
        features.put("assists", value(participantDto.getAssists()));

        features.put("total_damage_dealt_to_champions", value(participantDto.getTotalDamageDealtToChampions()));
        features.put("total_damage_taken", value(participantDto.getTotalDamageTaken()));
        features.put("total_minion_kills", totalMinionKills(participantDto));

        features.put("double_kills", value(participantDto.getDoubleKills()));
        features.put("triple_kills", value(participantDto.getTripleKills()));
        features.put("quadra_kills", value(participantDto.getQuadraKills()));
        features.put("penta_kills", value(participantDto.getPentaKills()));

        features.put("spell1casts", value(participantDto.getSpell1Casts()));
        features.put("spell2casts", value(participantDto.getSpell2Casts()));
        features.put("spell3casts", value(participantDto.getSpell3Casts()));
        features.put("spell4casts", value(participantDto.getSpell4Casts()));

        features.put("summoner1id", value(participantDto.getSummoner1Id()));
        features.put("summoner1casts", value(participantDto.getSummoner1Casts()));
        features.put("summoner2id", value(participantDto.getSummoner2Id()));
        features.put("summoner2casts", value(participantDto.getSummoner2Casts()));

        features.put("ward_killed", value(participantDto.getWardKilled()));
        features.put("ward_placed", value(participantDto.getWardPlaced()));
        features.put("vision_wards_bought_in_game", value(participantDto.getVisionWardsBoughtInGame()));
        features.put("vision_score", value(participantDto.getVisionScore()));

        return features;
    }

    private Map<String, Object> sampleFeatures(int index) {
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("game_duration", 1800 + index);
        features.put("game_end_timestamp", 1780000000000L + index);
        features.put("queue_id", 420);
        features.put("game_mode", "CLASSIC");
        features.put("champion_id", 86 + (index % 20));
        features.put("win", index % 2 == 0);
        features.put("champ_level", 13 + (index % 5));
        features.put("team_position", switch (index % 5) {
            case 0 -> "TOP";
            case 1 -> "JUNGLE";
            case 2 -> "MIDDLE";
            case 3 -> "BOTTOM";
            default -> "UTILITY";
        });
        features.put("item0", 1055);
        features.put("item1", 3006);
        features.put("item2", 3031);
        features.put("item3", 6672);
        features.put("item4", 3094);
        features.put("item5", 3036);
        features.put("item6", 3340);
        features.put("kda", 2.0 + (index % 8) * 0.3);
        features.put("kills", 3 + (index % 12));
        features.put("deaths", 1 + (index % 8));
        features.put("assists", 4 + (index % 16));
        features.put("total_damage_dealt_to_champions", 12000 + index * 211);
        features.put("total_damage_taken", 9000 + index * 173);
        features.put("total_minion_kills", 120 + index);
        features.put("double_kills", index % 3);
        features.put("triple_kills", index % 2);
        features.put("quadra_kills", 0);
        features.put("penta_kills", 0);
        features.put("spell1casts", 40 + index);
        features.put("spell2casts", 30 + index);
        features.put("spell3casts", 20 + index);
        features.put("spell4casts", 8 + index);
        features.put("summoner1id", 4);
        features.put("summoner1casts", 5);
        features.put("summoner2id", 14);
        features.put("summoner2casts", 3);
        features.put("ward_killed", index % 8);
        features.put("ward_placed", 8 + (index % 20));
        features.put("vision_wards_bought_in_game", index % 5);
        features.put("vision_score", 12 + index);
        return features;
    }

    private Integer fallbackScore(ParticipantDto participantDto) {
        double kills = value(participantDto.getKills());
        double deaths = Math.max(1.0, value(participantDto.getDeaths()));
        double assists = value(participantDto.getAssists());
        double damage = value(participantDto.getTotalDamageDealtToChampions());
        double cs = totalMinionKills(participantDto);
        double vision = value(participantDto.getVisionScore());
        double winBonus = Boolean.TRUE.equals(participantDto.getWin()) ? 8.0 : -4.0;

        double kdaScore = Math.min(28.0, ((kills + assists) / deaths) * 6.0);
        double damageScore = Math.min(22.0, damage / 1500.0);
        double csScore = Math.min(18.0, cs / 12.0);
        double visionScore = Math.min(12.0, vision / 3.0);
        return clampToScore(20.0 + kdaScore + damageScore + csScore + visionScore + winBonus);
    }

    private Integer toIntegerScore(Double predictedScore) {
        if (predictedScore == null || predictedScore.isNaN() || predictedScore.isInfinite()) {
            return 50;
        }
        return clampToScore(predictedScore);
    }

    private Integer clampToScore(double score) {
        return (int) Math.round(Math.max(0.0, Math.min(100.0, score)));
    }

    private double resolveKda(ParticipantDto participantDto) {
        if (participantDto.getKda() != null) {
            return participantDto.getKda();
        }

        int deaths = Math.max(1, value(participantDto.getDeaths()).intValue());
        return ((double) value(participantDto.getKills()) + value(participantDto.getAssists())) / deaths;
    }

    private Integer totalMinionKills(ParticipantDto participantDto) {
        return value(participantDto.getTotalMinionsKilled()) + value(participantDto.getNeutralMinionsKilled());
    }

    private Integer value(Integer value) {
        return value == null ? 0 : value;
    }

    private Long value(Long value) {
        return value == null ? 0L : value;
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }
}
