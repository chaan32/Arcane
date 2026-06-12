package com.arcane.worker.analysis.service;

import com.arcane.worker.analysis.domain.ChampionAnalysisSnapshot;
import com.arcane.worker.analysis.domain.ChampionMatchUpStat;
import com.arcane.worker.analysis.domain.ChampionOptionStat;
import com.arcane.worker.analysis.domain.ChampionOptionType;
import com.arcane.worker.analysis.domain.ChampionPositionStat;
import com.arcane.worker.analysis.domain.ChampionRuneStat;
import com.arcane.worker.analysis.domain.ChampionTier;
import com.arcane.worker.common.logging.TraceIds;
import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.analysis.repository.ChampionAnalysisSnapshotRepository;
import com.arcane.worker.analysis.repository.ChampionMatchUpStatRepository;
import com.arcane.worker.analysis.repository.ChampionOptionStatRepository;
import com.arcane.worker.analysis.repository.ChampionPositionStatRepository;
import com.arcane.worker.analysis.repository.ChampionRuneStatRepository;
import com.arcane.worker.kafka.event.dto.ChampionAnalysisRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChampionAnalysisService {

    private static final String DEFAULT_SOURCE_COLLECTION = "riot_match_participant_raw";
    private static final int DEFAULT_QUEUE_ID = 420;
    private static final int WIN_RATE_SMOOTHING_GAMES = 30;
    private static final long RECENT_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(7);
    private static final Set<String> VALID_POSITIONS = Set.of("TOP", "JUNGLE", "MIDDLE", "BOTTOM", "UTILITY");

    private final MongoTemplate mongoTemplate;
    private final ChampionAnalysisSnapshotRepository championAnalysisSnapshotRepository;
    private final ChampionPositionStatRepository championPositionStatRepository;
    private final ChampionOptionStatRepository championOptionStatRepository;
    private final ChampionMatchUpStatRepository championMatchUpStatRepository;
    private final ChampionRuneStatRepository championRuneStatRepository;

    @Transactional
    public AnalysisResult analyze(
            ChampionAnalysisRequestedEvent event,
            Consumer<AnalysisProgress> progressSink
    ) {
        String snapshotId = "champion-analysis-" + UUID.randomUUID();
        int queueId = event.queueId() == null ? DEFAULT_QUEUE_ID : event.queueId();
        String sourceCollection = resolveSourceCollection(event.sourceCollection());

        ChampionAnalysisSnapshot snapshot = ChampionAnalysisSnapshot.running(
                snapshotId,
                queueId,
                sourceCollection,
                event.requestedBy()
        );

        championAnalysisSnapshotRepository.saveAndFlush(snapshot);
        reportProgress(
                event,
                progressSink,
                snapshotId,
                5,
                "SNAPSHOT_CREATED",
                "분석 스냅샷 생성 완료",
                0,
                0,
                0,
                "UNKNOWN"
        );

        try {
            Query query = new Query(Criteria.where("queueId").is(queueId));
            List<Document> participants = mongoTemplate.find(query, Document.class, sourceCollection)
                    .stream()
                    .filter(this::isValidParticipant)
                    .toList();

            int totalParticipants = participants.size();

            int totalMatches = distinctMatchCount(participants);
            int totalChampions = distinctChampionCount(participants);
            String patchVersion = resolvePatchVersion(participants);
            snapshot.updatePatchVersion(patchVersion);
            reportProgress(
                    event,
                    progressSink,
                    snapshotId,
                    20,
                    "RAW_DATA_LOADED",
                    "MongoDB 참가자 raw 데이터 로드 완료",
                    totalParticipants,
                    totalMatches,
                    totalChampions,
                    patchVersion
            );

            List<ChampionPositionStat> positionStats = buildPositionStats(
                    participants,
                    snapshotId,
                    queueId
            );
            reportProgress(
                    event,
                    progressSink,
                    snapshotId,
                    40,
                    "POSITION_STATS_BUILT",
                    "포지션별 챔피언 통계 계산 완료",
                    totalParticipants,
                    totalMatches,
                    totalChampions,
                    patchVersion
            );

            List<ChampionOptionStat> optionStats = buildOptionStats(
                    participants,
                    snapshotId,
                    queueId
            );
            reportProgress(
                    event,
                    progressSink,
                    snapshotId,
                    60,
                    "OPTION_STATS_BUILT",
                    "아이템/소환사 주문 통계 계산 완료",
                    totalParticipants,
                    totalMatches,
                    totalChampions,
                    patchVersion
            );

            List<ChampionMatchUpStat> matchUpStats = buildMatchUpStats(
                    participants,
                    snapshotId,
                    queueId
            );
            reportProgress(
                    event,
                    progressSink,
                    snapshotId,
                    75,
                    "MATCHUP_STATS_BUILT",
                    "상대 챔피언 통계 계산 완료",
                    totalParticipants,
                    totalMatches,
                    totalChampions,
                    patchVersion
            );

            List<ChampionRuneStat> runeStats = buildRuneStats(
                    participants,
                    snapshotId,
                    queueId
            );
            reportProgress(
                    event,
                    progressSink,
                    snapshotId,
                    88,
                    "RUNE_STATS_BUILT",
                    "룬 빌드 통계 계산 완료",
                    totalParticipants,
                    totalMatches,
                    totalChampions,
                    patchVersion
            );

            reportProgress(
                    event,
                    progressSink,
                    snapshotId,
                    94,
                    "PERSISTING",
                    "분석 결과 MySQL 저장 시작",
                    totalParticipants,
                    totalMatches,
                    totalChampions,
                    patchVersion
            );

            championPositionStatRepository.saveAll(positionStats);
            championOptionStatRepository.saveAll(optionStats);
            championMatchUpStatRepository.saveAll(matchUpStats);
            championRuneStatRepository.saveAll(runeStats);

            snapshot.setComplete(totalParticipants, totalMatches, totalChampions);

            championAnalysisSnapshotRepository.findByActiveTrue()
                    .filter(activeSnapshot -> !activeSnapshot.getId().equals(snapshot.getId()))
                    .ifPresent(ChampionAnalysisSnapshot::deactivate);

            snapshot.activate();
            championAnalysisSnapshotRepository.save(snapshot);
            reportProgress(
                    event,
                    progressSink,
                    snapshotId,
                    100,
                    "DONE",
                    "챔피언 분석 완료",
                    totalParticipants,
                    totalMatches,
                    totalChampions,
                    patchVersion
            );

            log.info(logMessage(
                    "ChampionAnalysisService.analyze",
                    "완료",
                    "jobId=" + event.jobId()
                            + " | traceId=" + TraceIds.normalize(event.traceId())
                            + " | snapshotId=" + snapshotId
                            + " | queueId=" + queueId
                            + " | patchVersion=" + patchVersion
                            + " | participants=" + totalParticipants
                            + " | matches=" + totalMatches
                            + " | champions=" + totalChampions
                            + " | positionStats=" + positionStats.size()
                            + " | optionStats=" + optionStats.size()
                            + " | matchUpStats=" + matchUpStats.size()
                            + " | runeStats=" + runeStats.size()
            ));

            return new AnalysisResult(
                    snapshotId,
                    totalParticipants,
                    totalMatches,
                    totalChampions,
                    patchVersion
            );

        } catch (Exception exception) {
            snapshot.fail(exception.getMessage());
            championAnalysisSnapshotRepository.save(snapshot);

            reportProgress(
                    event,
                    progressSink,
                    snapshotId,
                    0,
                    "FAILED",
                    "챔피언 분석 실패: " + exception.getMessage(),
                    0,
                    0,
                    0,
                    "UNKNOWN"
            );

            log.error(logMessage(
                            "ChampionAnalysisService.analyze",
                            "실패",
                            "jobId=" + event.jobId()
                                    + " | traceId=" + TraceIds.normalize(event.traceId())
                                    + " | snapshotId=" + snapshotId
                                    + " | reason=" + exception.getMessage()
                    ),
                    exception
            );

            throw new IllegalStateException("Champion analysis failed.", exception);
        }
    }

    private String resolveSourceCollection(String sourceCollection) {
        if (sourceCollection == null || sourceCollection.isBlank()) {
            return DEFAULT_SOURCE_COLLECTION;
        }
        return sourceCollection;
    }

    private void reportProgress(
            ChampionAnalysisRequestedEvent event,
            Consumer<AnalysisProgress> progressSink,
            String snapshotId,
            int progressPercent,
            String phase,
            String message,
            int totalParticipants,
            int totalMatches,
            int totalChampions,
            String patchVersion
    ) {
        AnalysisProgress progress = new AnalysisProgress(
                snapshotId,
                progressPercent,
                phase,
                message,
                totalParticipants,
                totalMatches,
                totalChampions,
                patchVersion
        );

        log.info(logMessage(
                "ChampionAnalysisService.analyze",
                "진행률 갱신",
                "jobId=" + event.jobId()
                        + " | progress=" + progressPercent + "%"
                        + " | phase=" + phase
                        + " | message=" + message
        ));

        if (progressSink == null) {
            return;
        }

        try {
            progressSink.accept(progress);
        } catch (Exception exception) {
            log.warn(logMessage(
                    "ChampionAnalysisService.reportProgress",
                    "진행률 발행 실패",
                    "jobId=" + event.jobId()
                            + " | progress=" + progressPercent + "%"
                            + " | reason=" + exception.getMessage()
            ));
        }
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("챔피언 분석", method, status, detail);
    }

    private List<ChampionPositionStat> buildPositionStats(
            List<Document> participants,
            String snapshotId,
            int queueId
    ) {
        Map<String, Integer> totalGamesByPosition = new HashMap<>();
        Map<ChampionPositionKey, ChampionAggregate> aggregateMap = new HashMap<>();
        long latestGameEndTimestamp = participants.stream()
                .mapToLong(participant -> longValue(participant, "gameEndTimestamp"))
                .max()
                .orElse(0L);
        long recentCutoff = Math.max(0L, latestGameEndTimestamp - RECENT_WINDOW_MILLIS);

        for (Document participant : participants) {
            Long championId = longObject(participant, "championId");
            String position = stringValue(participant, "teamPosition");
            if (championId == null || position == null) {
                continue;
            }

            totalGamesByPosition.merge(position, 1, Integer::sum);
            ChampionPositionKey key = new ChampionPositionKey(championId, position);
            aggregateMap.computeIfAbsent(key, ignored -> new ChampionAggregate(championId, position))
                    .accept(participant, recentCutoff);
        }

        List<ChampionTierCandidate> candidates = aggregateMap.values().stream()
                .map(aggregate -> aggregate.toCandidate(totalGamesByPosition.getOrDefault(aggregate.position, 0)))
                .toList();

        Map<String, PositionMaxValues> maxValuesByPosition = findMaxValuesByPosition(candidates);

        for (ChampionTierCandidate candidate : candidates) {
            PositionMaxValues maxValues = maxValuesByPosition.get(candidate.position);
            candidate.calculateRfmScore(maxValues);
        }

        assignTierByPosition(candidates);

        LocalDateTime now = LocalDateTime.now();
        return candidates.stream()
                .map(candidate -> candidate.toEntity(snapshotId, queueId, now))
                .toList();
    }

    private List<ChampionOptionStat> buildOptionStats(
            List<Document> participants,
            String snapshotId,
            int queueId
    ) {
        Map<ChampionPositionKey, Integer> totalGamesByChampionPosition = totalGamesByChampionPosition(participants);
        Map<OptionKey, OptionAggregate> aggregateMap = new HashMap<>();

        for (Document participant : participants) {
            Long championId = longObject(participant, "championId");
            String position = stringValue(participant, "teamPosition");
            if (championId == null || position == null) {
                continue;
            }

            boolean win = booleanValue(participant, "win");
            Set<Long> itemIds = new HashSet<>();
            for (int index = 0; index <= 5; index++) {
                Long itemId = longObject(participant, "item" + index);
                if (itemId != null && itemId > 0) {
                    itemIds.add(itemId);
                }
            }

            for (Long itemId : itemIds) {
                OptionKey key = new OptionKey(
                        championId,
                        position,
                        ChampionOptionType.ITEM,
                        "item:" + itemId
                );
                aggregateMap.computeIfAbsent(
                                key,
                                ignored -> new OptionAggregate(
                                        championId,
                                        position,
                                        ChampionOptionType.ITEM,
                                        "item:" + itemId,
                                        itemId,
                                        null,
                                        null
                                )
                        )
                        .accept(win);
            }

            int spell1Id = intValue(participant, "summoner1Id");
            int spell2Id = intValue(participant, "summoner2Id");
            if (spell1Id > 0 && spell2Id > 0) {
                int firstSpellId = Math.min(spell1Id, spell2Id);
                int secondSpellId = Math.max(spell1Id, spell2Id);
                OptionKey key = new OptionKey(
                        championId,
                        position,
                        ChampionOptionType.SPELL,
                        "spell:" + firstSpellId + ":" + secondSpellId
                );
                aggregateMap.computeIfAbsent(
                                key,
                                ignored -> new OptionAggregate(
                                        championId,
                                        position,
                                        ChampionOptionType.SPELL,
                                        "spell:" + firstSpellId + ":" + secondSpellId,
                                        null,
                                        firstSpellId,
                                        secondSpellId
                                )
                        )
                        .accept(win);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        return aggregateMap.values().stream()
                .map(aggregate -> aggregate.toOptionEntity(
                        snapshotId,
                        queueId,
                        totalGamesByChampionPosition.getOrDefault(
                                new ChampionPositionKey(aggregate.championId, aggregate.position),
                                0
                        ),
                        now
                ))
                .toList();
    }

    private List<ChampionMatchUpStat> buildMatchUpStats(
            List<Document> participants,
            String snapshotId,
            int queueId
    ) {
        Map<String, List<Document>> participantsByMatchId = new HashMap<>();
        for (Document participant : participants) {
            String matchId = stringValue(participant, "matchId");
            if (matchId == null || matchId.isBlank()) {
                continue;
            }
            participantsByMatchId.computeIfAbsent(matchId, ignored -> new ArrayList<>())
                    .add(participant);
        }

        Map<MatchUpKey, MatchUpAggregate> aggregateMap = new HashMap<>();
        for (List<Document> matchParticipants : participantsByMatchId.values()) {
            for (Document participant : matchParticipants) {
                Long championId = longObject(participant, "championId");
                String position = stringValue(participant, "teamPosition");
                Integer teamId = integerObject(participant, "teamId");
                if (championId == null || position == null || teamId == null) {
                    continue;
                }

                for (Document opponent : matchParticipants) {
                    Long opponentChampionId = longObject(opponent, "championId");
                    String opponentPosition = stringValue(opponent, "teamPosition");
                    Integer opponentTeamId = integerObject(opponent, "teamId");
                    if (opponentChampionId == null
                            || opponentPosition == null
                            || opponentTeamId == null
                            || !position.equals(opponentPosition)
                            || teamId.equals(opponentTeamId)) {
                        continue;
                    }

                    MatchUpKey key = new MatchUpKey(championId, opponentChampionId, position);
                    aggregateMap.computeIfAbsent(
                                    key,
                                    ignored -> new MatchUpAggregate(championId, opponentChampionId, position)
                            )
                            .accept(participant);
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        return aggregateMap.values().stream()
                .map(aggregate -> aggregate.toEntity(snapshotId, queueId, now))
                .toList();
    }

    private List<ChampionRuneStat> buildRuneStats(
            List<Document> participants,
            String snapshotId,
            int queueId
    ) {
        Map<ChampionPositionKey, Integer> totalGamesByChampionPosition = totalGamesByChampionPosition(participants);
        Map<RuneKey, RuneAggregate> aggregateMap = new HashMap<>();

        for (Document participant : participants) {
            Long championId = longObject(participant, "championId");
            String position = stringValue(participant, "teamPosition");
            RuneBuild runeBuild = extractRuneBuild(participant);
            if (championId == null || position == null || runeBuild == null) {
                continue;
            }

            RuneKey key = new RuneKey(championId, position, runeBuild.runeKey());
            aggregateMap.computeIfAbsent(
                            key,
                            ignored -> new RuneAggregate(championId, position, runeBuild)
                    )
                    .accept(booleanValue(participant, "win"));
        }

        LocalDateTime now = LocalDateTime.now();
        return aggregateMap.values().stream()
                .map(aggregate -> aggregate.toEntity(
                        snapshotId,
                        queueId,
                        totalGamesByChampionPosition.getOrDefault(
                                new ChampionPositionKey(aggregate.championId, aggregate.position),
                                0
                        ),
                        now
                ))
                .toList();
    }

    private Map<ChampionPositionKey, Integer> totalGamesByChampionPosition(List<Document> participants) {
        Map<ChampionPositionKey, Integer> result = new HashMap<>();
        for (Document participant : participants) {
            Long championId = longObject(participant, "championId");
            String position = stringValue(participant, "teamPosition");
            if (championId == null || position == null) {
                continue;
            }
            result.merge(new ChampionPositionKey(championId, position), 1, Integer::sum);
        }
        return result;
    }

    private Map<String, PositionMaxValues> findMaxValuesByPosition(List<ChampionTierCandidate> candidates) {
        Map<String, PositionMaxValues> result = new HashMap<>();
        for (ChampionTierCandidate candidate : candidates) {
            result.computeIfAbsent(candidate.position, ignored -> new PositionMaxValues())
                    .accept(candidate);
        }
        return result;
    }

    private void assignTierByPosition(List<ChampionTierCandidate> candidates) {
        Map<String, List<ChampionTierCandidate>> candidatesByPosition = new HashMap<>();
        for (ChampionTierCandidate candidate : candidates) {
            candidatesByPosition.computeIfAbsent(candidate.position, ignored -> new ArrayList<>())
                    .add(candidate);
        }

        for (List<ChampionTierCandidate> positionCandidates : candidatesByPosition.values()) {
            positionCandidates.sort(Comparator.comparingDouble(ChampionTierCandidate::tierScore).reversed());
            int size = positionCandidates.size();
            for (int index = 0; index < size; index++) {
                double rankPercent = (index + 1) / (double) size;
                positionCandidates.get(index).tier = tierByRank(index, rankPercent);
            }
        }
    }

    private ChampionTier tierByRank(int index, double rankPercent) {
        if (index == 0 || rankPercent <= 0.03) {
            return ChampionTier.OP;
        }
        if (rankPercent <= 0.15) {
            return ChampionTier.TIER_1;
        }
        if (rankPercent <= 0.35) {
            return ChampionTier.TIER_2;
        }
        if (rankPercent <= 0.65) {
            return ChampionTier.TIER_3;
        }
        return ChampionTier.TIER_4;
    }

    private int distinctMatchCount(List<Document> participants) {
        Set<String> matchIds = new HashSet<>();
        for (Document participant : participants) {
            String matchId = stringValue(participant, "matchId");
            if (matchId != null && !matchId.isBlank()) {
                matchIds.add(matchId);
            }
        }
        return matchIds.size();
    }

    private int distinctChampionCount(List<Document> participants) {
        Set<Long> championIds = new HashSet<>();
        for (Document participant : participants) {
            Long championId = longObject(participant, "championId");
            if (championId != null && championId > 0) {
                championIds.add(championId);
            }
        }
        return championIds.size();
    }

    private String resolvePatchVersion(List<Document> participants) {
        Map<String, Integer> versionCounts = new HashMap<>();
        for (Document participant : participants) {
            String gameVersion = stringValue(participant, "gameVersion");
            if (gameVersion != null && !gameVersion.isBlank()) {
                versionCounts.merge(gameVersion, 1, Integer::sum);
            }
        }

        return versionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
    }

    private boolean isValidParticipant(Document participant) {
        Long championId = longObject(participant, "championId");
        String position = stringValue(participant, "teamPosition");
        return championId != null
                && championId > 0
                && position != null
                && VALID_POSITIONS.contains(position);
    }

    private BigDecimal decimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private double percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return numerator * 100.0 / denominator;
    }

    private double normalized(double value, double maxValue) {
        if (maxValue <= 0.0) {
            return 0.0;
        }
        return Math.min(100.0, value * 100.0 / maxValue);
    }

    private Object rawValue(Document document, String key) {
        Object value = document.get(key);
        if (value != null) {
            return value;
        }

        Object payload = document.get("participantPayload");
        return mapValue(payload, key);
    }

    private Object mapValue(Object source, String key) {
        if (source instanceof Document document) {
            return document.get(key);
        }
        if (source instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }

    private String stringValue(Document document, String key) {
        Object value = rawValue(document, key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private int intValue(Document document, String key) {
        return integerFrom(rawValue(document, key));
    }

    private Integer integerObject(Document document, String key) {
        Object value = rawValue(document, key);
        if (value == null) {
            return null;
        }
        return integerFrom(value);
    }

    private int integerFrom(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private long longValue(Document document, String key) {
        Long value = longObject(document, key);
        return value == null ? 0L : value;
    }

    private Long longObject(Document document, String key) {
        return longFrom(rawValue(document, key));
    }

    private Long longFrom(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private double doubleValue(Document document, String key) {
        return doubleFrom(rawValue(document, key));
    }

    private double doubleFrom(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private boolean booleanValue(Document document, String key) {
        return booleanFrom(rawValue(document, key));
    }

    private boolean booleanFrom(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private RuneBuild extractRuneBuild(Document participant) {
        Object perks = rawValue(participant, "perks");
        Object statPerks = mapValue(perks, "statPerks");
        Object styles = mapValue(perks, "styles");

        if (!(styles instanceof List<?> styleList) || styleList.size() < 2) {
            return null;
        }

        Object primaryStyle = styleList.get(0);
        Object subStyle = styleList.get(1);

        int primaryStyleId = integerFrom(mapValue(primaryStyle, "style"));
        int subStyleId = integerFrom(mapValue(subStyle, "style"));
        List<Integer> primaryPerks = selectedPerks(primaryStyle, 4);
        List<Integer> subPerks = selectedPerks(subStyle, 2);

        if (primaryStyleId <= 0 || subStyleId <= 0 || primaryPerks.isEmpty()) {
            return null;
        }

        int primaryPerk1 = perkAt(primaryPerks, 0);
        int primaryPerk2 = perkAt(primaryPerks, 1);
        int primaryPerk3 = perkAt(primaryPerks, 2);
        int primaryPerk4 = perkAt(primaryPerks, 3);
        int subPerk1 = perkAt(subPerks, 0);
        int subPerk2 = perkAt(subPerks, 1);
        int offense = integerFrom(mapValue(statPerks, "offense"));
        int flex = integerFrom(mapValue(statPerks, "flex"));
        int defense = integerFrom(mapValue(statPerks, "defense"));

        String runeKey = primaryStyleId + ":"
                + subStyleId + ":"
                + primaryPerk1 + ":"
                + primaryPerk2 + ":"
                + primaryPerk3 + ":"
                + primaryPerk4 + ":"
                + subPerk1 + ":"
                + subPerk2 + ":"
                + offense + ":"
                + flex + ":"
                + defense;

        return new RuneBuild(
                runeKey,
                primaryStyleId,
                subStyleId,
                primaryPerk1,
                primaryPerk2,
                primaryPerk3,
                primaryPerk4,
                subPerk1,
                subPerk2,
                offense,
                flex,
                defense
        );
    }

    private List<Integer> selectedPerks(Object style, int limit) {
        Object selections = mapValue(style, "selections");
        if (!(selections instanceof List<?> selectionList)) {
            return List.of();
        }

        return selectionList.stream()
                .map(selection -> integerFrom(mapValue(selection, "perk")))
                .filter(perk -> perk > 0)
                .limit(limit)
                .toList();
    }

    private int perkAt(List<Integer> perks, int index) {
        if (index >= perks.size()) {
            return 0;
        }
        return perks.get(index);
    }

    public record AnalysisResult(
            String snapshotId,
            Integer totalParticipants,
            Integer totalMatches,
            Integer totalChampions,
            String patchVersion
    ) {
    }

    public record AnalysisProgress(
            String snapshotId,
            Integer progressPercent,
            String phase,
            String message,
            Integer totalParticipants,
            Integer totalMatches,
            Integer totalChampions,
            String patchVersion
    ) {
    }

    private record ChampionPositionKey(Long championId, String position) {
    }

    private record OptionKey(
            Long championId,
            String position,
            ChampionOptionType optionType,
            String optionKey
    ) {
    }

    private record MatchUpKey(Long championId, Long opponentChampionId, String position) {
    }

    private record RuneKey(Long championId, String position, String runeKey) {
    }

    private record RuneBuild(
            String runeKey,
            Integer primaryStyle,
            Integer subStyle,
            Integer primaryPerk1,
            Integer primaryPerk2,
            Integer primaryPerk3,
            Integer primaryPerk4,
            Integer subPerk1,
            Integer subPerk2,
            Integer offense,
            Integer flex,
            Integer defense
    ) {
    }

    private class OptionAggregate {
        private final Long championId;
        private final String position;
        private final ChampionOptionType optionType;
        private final String optionKey;
        private final Long itemId;
        private final Integer spell1Id;
        private final Integer spell2Id;
        private int games;
        private int wins;

        private OptionAggregate(
                Long championId,
                String position,
                ChampionOptionType optionType,
                String optionKey,
                Long itemId,
                Integer spell1Id,
                Integer spell2Id
        ) {
            this.championId = championId;
            this.position = position;
            this.optionType = optionType;
            this.optionKey = optionKey;
            this.itemId = itemId;
            this.spell1Id = spell1Id;
            this.spell2Id = spell2Id;
        }

        private void accept(boolean win) {
            games++;
            if (win) {
                wins++;
            }
        }

        private ChampionOptionStat toOptionEntity(
                String snapshotId,
                int queueId,
                int totalChampionPositionGames,
                LocalDateTime now
        ) {
            int losses = games - wins;
            return ChampionOptionStat.builder()
                    .snapshotId(snapshotId)
                    .championId(championId)
                    .queueId(queueId)
                    .teamPosition(position)
                    .optionType(optionType)
                    .optionKey(optionKey)
                    .itemId(itemId)
                    .spell1Id(spell1Id)
                    .spell2Id(spell2Id)
                    .games(games)
                    .wins(wins)
                    .losses(losses)
                    .winRate(decimal(percent(wins, games)))
                    .pickRate(decimal(percent(games, totalChampionPositionGames)))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }
    }

    private class MatchUpAggregate {
        private final Long championId;
        private final Long opponentChampionId;
        private final String position;
        private int games;
        private int wins;
        private double kda;
        private double damageDealt;
        private double cs;

        private MatchUpAggregate(Long championId, Long opponentChampionId, String position) {
            this.championId = championId;
            this.opponentChampionId = opponentChampionId;
            this.position = position;
        }

        private void accept(Document participant) {
            games++;
            if (booleanValue(participant, "win")) {
                wins++;
            }
            kda += doubleValue(participant, "kda");
            damageDealt += longValue(participant, "totalDamageDealtToChampions");
            cs += intValue(participant, "totalMinionKills");
        }

        private ChampionMatchUpStat toEntity(String snapshotId, int queueId, LocalDateTime now) {
            int losses = games - wins;
            return ChampionMatchUpStat.builder()
                    .snapshotId(snapshotId)
                    .championId(championId)
                    .opponentChampionId(opponentChampionId)
                    .queueId(queueId)
                    .teamPosition(position)
                    .games(games)
                    .wins(wins)
                    .losses(losses)
                    .winRate(decimal(percent(wins, games)))
                    .avgKda(decimal(kda / games))
                    .avgDamageDealt(decimal(damageDealt / games))
                    .avgCs(decimal(cs / games))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }
    }

    private class RuneAggregate {
        private final Long championId;
        private final String position;
        private final RuneBuild runeBuild;
        private int games;
        private int wins;

        private RuneAggregate(Long championId, String position, RuneBuild runeBuild) {
            this.championId = championId;
            this.position = position;
            this.runeBuild = runeBuild;
        }

        private void accept(boolean win) {
            games++;
            if (win) {
                wins++;
            }
        }

        private ChampionRuneStat toEntity(
                String snapshotId,
                int queueId,
                int totalChampionPositionGames,
                LocalDateTime now
        ) {
            int losses = games - wins;
            return ChampionRuneStat.builder()
                    .snapshotId(snapshotId)
                    .championId(championId)
                    .queueId(queueId)
                    .teamPosition(position)
                    .runeKey(runeBuild.runeKey())
                    .primaryStyle(runeBuild.primaryStyle())
                    .subStyle(runeBuild.subStyle())
                    .primaryPerk1(runeBuild.primaryPerk1())
                    .primaryPerk2(runeBuild.primaryPerk2())
                    .primaryPerk3(runeBuild.primaryPerk3())
                    .primaryPerk4(runeBuild.primaryPerk4())
                    .subPerk1(runeBuild.subPerk1())
                    .subPerk2(runeBuild.subPerk2())
                    .offense(runeBuild.offense())
                    .flex(runeBuild.flex())
                    .defense(runeBuild.defense())
                    .games(games)
                    .wins(wins)
                    .losses(losses)
                    .winRate(decimal(percent(wins, games)))
                    .pickRate(decimal(percent(games, totalChampionPositionGames)))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }
    }

    private class ChampionAggregate {
        private final Long championId;
        private final String position;
        private int games;
        private int wins;
        private int recentGames;
        private double kills;
        private double deaths;
        private double assists;
        private double kda;
        private double cs;
        private double damageDealt;
        private double damageTaken;
        private double visionScore;

        private ChampionAggregate(Long championId, String position) {
            this.championId = championId;
            this.position = position;
        }

        private void accept(Document participant, long recentCutoff) {
            games++;
            if (booleanValue(participant, "win")) {
                wins++;
            }
            if (longValue(participant, "gameEndTimestamp") >= recentCutoff) {
                recentGames++;
            }
            kills += intValue(participant, "kills");
            deaths += intValue(participant, "deaths");
            assists += intValue(participant, "assists");
            kda += doubleValue(participant, "kda");
            cs += intValue(participant, "totalMinionKills");
            damageDealt += longValue(participant, "totalDamageDealtToChampions");
            damageTaken += longValue(participant, "totalDamageTaken");
            visionScore += intValue(participant, "visionScore");
        }

        private ChampionTierCandidate toCandidate(int totalPositionGames) {
            int losses = games - wins;
            double winRate = percent(wins, games);
            double adjustedWinRate = (wins + 0.5 * WIN_RATE_SMOOTHING_GAMES)
                    * 100.0
                    / (games + WIN_RATE_SMOOTHING_GAMES);
            double pickRate = percent(games, totalPositionGames);
            double recencyScore = percent(recentGames, games);

            return new ChampionTierCandidate(
                    championId,
                    position,
                    games,
                    wins,
                    losses,
                    winRate,
                    adjustedWinRate,
                    pickRate,
                    recencyScore,
                    kills / games,
                    deaths / games,
                    assists / games,
                    kda / games,
                    cs / games,
                    damageDealt / games,
                    damageTaken / games,
                    visionScore / games
            );
        }
    }

    private class PositionMaxValues {
        private double pickRate;
        private double avgKda;
        private double avgCs;
        private double avgDamageDealt;
        private double avgVisionScore;

        private void accept(ChampionTierCandidate candidate) {
            pickRate = Math.max(pickRate, candidate.pickRate);
            avgKda = Math.max(avgKda, candidate.avgKda);
            avgCs = Math.max(avgCs, candidate.avgCs);
            avgDamageDealt = Math.max(avgDamageDealt, candidate.avgDamageDealt);
            avgVisionScore = Math.max(avgVisionScore, candidate.avgVisionScore);
        }
    }

    private class ChampionTierCandidate {
        private final Long championId;
        private final String position;
        private final int games;
        private final int wins;
        private final int losses;
        private final double winRate;
        private final double adjustedWinRate;
        private final double pickRate;
        private final double recencyScore;
        private final double avgKills;
        private final double avgDeaths;
        private final double avgAssists;
        private final double avgKda;
        private final double avgCs;
        private final double avgDamageDealt;
        private final double avgDamageTaken;
        private final double avgVisionScore;
        private double frequencyScore;
        private double performanceScore;
        private double tierScore;
        private ChampionTier tier = ChampionTier.TIER_4;

        private ChampionTierCandidate(
                Long championId,
                String position,
                int games,
                int wins,
                int losses,
                double winRate,
                double adjustedWinRate,
                double pickRate,
                double recencyScore,
                double avgKills,
                double avgDeaths,
                double avgAssists,
                double avgKda,
                double avgCs,
                double avgDamageDealt,
                double avgDamageTaken,
                double avgVisionScore
        ) {
            this.championId = championId;
            this.position = position;
            this.games = games;
            this.wins = wins;
            this.losses = losses;
            this.winRate = winRate;
            this.adjustedWinRate = adjustedWinRate;
            this.pickRate = pickRate;
            this.recencyScore = recencyScore;
            this.avgKills = avgKills;
            this.avgDeaths = avgDeaths;
            this.avgAssists = avgAssists;
            this.avgKda = avgKda;
            this.avgCs = avgCs;
            this.avgDamageDealt = avgDamageDealt;
            this.avgDamageTaken = avgDamageTaken;
            this.avgVisionScore = avgVisionScore;
        }

        private void calculateRfmScore(PositionMaxValues maxValues) {
            frequencyScore = normalized(pickRate, maxValues.pickRate);

            double kdaScore = normalized(avgKda, maxValues.avgKda);
            double damageScore = normalized(avgDamageDealt, maxValues.avgDamageDealt);
            double csScore = normalized(avgCs, maxValues.avgCs);
            double visionScore = normalized(avgVisionScore, maxValues.avgVisionScore);
            double utilityScore = (csScore + visionScore) / 2.0;
            performanceScore = adjustedWinRate * 0.55
                    + kdaScore * 0.20
                    + damageScore * 0.15
                    + utilityScore * 0.10;

            double rawTierScore = recencyScore * 0.20
                    + frequencyScore * 0.25
                    + performanceScore * 0.55;

            double sampleConfidence = Math.min(1.0, games / (double) WIN_RATE_SMOOTHING_GAMES);
            tierScore = rawTierScore * (0.75 + sampleConfidence * 0.25);
        }

        private double tierScore() {
            return tierScore;
        }

        private ChampionPositionStat toEntity(String snapshotId, int queueId, LocalDateTime now) {
            return ChampionPositionStat.builder()
                    .snapshotId(snapshotId)
                    .championId(championId)
                    .queueId(queueId)
                    .teamPosition(position)
                    .games(games)
                    .wins(wins)
                    .losses(losses)
                    .winRate(decimal(winRate))
                    .pickRate(decimal(pickRate))
                    .adjustedWinRate(decimal(adjustedWinRate))
                    .recencyScore(decimal(recencyScore))
                    .frequencyScore(decimal(frequencyScore))
                    .performanceScore(decimal(performanceScore))
                    .tierScore(decimal(tierScore))
                    .tier(tier)
                    .avgKills(decimal(avgKills))
                    .avgDeaths(decimal(avgDeaths))
                    .avgAssists(decimal(avgAssists))
                    .avgKda(decimal(avgKda))
                    .avgCs(decimal(avgCs))
                    .avgDamageDealt(decimal(avgDamageDealt))
                    .avgDamageTaken(decimal(avgDamageTaken))
                    .avgVisionScore(decimal(avgVisionScore))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }
    }
}
