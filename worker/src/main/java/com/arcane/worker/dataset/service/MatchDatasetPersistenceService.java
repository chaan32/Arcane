package com.arcane.worker.dataset.service;

import com.arcane.worker.common.logging.WorkerLogSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchDatasetPersistenceService {
    private static final String RAW_COLLECTION = "riot_match_participant_raw";

    private final JdbcTemplate jdbcTemplate;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private static final int EXPECTED_PARTICIPANT_COUNT = 10;

    @Transactional
    public PersistResult persist(String matchId, JsonNode match, boolean saveMysql, boolean saveMongo) {
        if (match == null || match.isMissingNode() || match.isNull()) {
            return PersistResult.empty();
        }

        JsonNode info = match.path("info");
        JsonNode participants = info.path("participants");
        if (!hasCompleteParticipants(participants)) {
            log.warn(logMessage(
                    "MatchDatasetPersistenceService.persist",
                    "저장 스킵",
                    "matchId=" + matchId
                            + " | reason=incomplete_participants"
                            + " | participantCount=" + participants.size()
            ));

            throw new IllegalStateException(
                    "매치 참가자가 10명이 아니거나 PUUID가 올바르지 않습니다. matchId="
                            + matchId
            );
        }

        int savedMysqlMatches = saveMysql ? saveMatchInfo(matchId, info) : 0;
        Long matchInfoId = saveMysql ? findMatchInfoId(matchId) : null;

        int savedMysqlParticipants = 0;
        int savedMongoParticipants = 0;
        int skippedDuplicateParticipants = 0;

        for (int index = 0; index < participants.size(); index++) {
            JsonNode participant = participants.get(index);
            String puuid = text(participant, "puuid");
            if (puuid == null || puuid.isBlank()) {
                continue;
            }

            if (saveMongo) {
                boolean inserted = saveMongoParticipant(matchId, info, participant, index);
                if (inserted) {
                    savedMongoParticipants++;
                } else {
                    skippedDuplicateParticipants++;
                }
            }

            if (saveMysql && matchInfoId != null) {
                boolean inserted = saveMysqlParticipant(matchInfoId, participant);
                if (inserted) {
                    savedMysqlParticipants++;
                } else {
                    skippedDuplicateParticipants++;
                }
            }
        }

        if (saveMysql && matchInfoId != null) {
            int totalParticipantCount = countMysqlParticipants(matchInfoId);

            if (totalParticipantCount != EXPECTED_PARTICIPANT_COUNT) {
                throw new IllegalStateException(
                        "MySQL 참가자 저장 결과가 10명이 아닙니다."
                                + " matchId=" + matchId
                                + ", actual=" + totalParticipantCount
                );
            }
        }

        log.info(logMessage(
                "MatchDatasetPersistenceService.persist",
                "저장 완료",
                "matchId=" + matchId
                        + " | mysqlMatch=" + savedMysqlMatches
                        + " | mysqlParticipants=" + savedMysqlParticipants
                        + " | mongoParticipants=" + savedMongoParticipants
                        + " | duplicateSkip=" + skippedDuplicateParticipants
        ));
        return new PersistResult(
                savedMysqlMatches,
                savedMysqlParticipants,
                savedMongoParticipants,
                skippedDuplicateParticipants
        );
    }

    private int saveMatchInfo(String matchId, JsonNode info) {
        return jdbcTemplate.update("""
                        INSERT IGNORE INTO match_info (
                            game_creation,
                            game_duration,
                            game_end_timestamp,
                            game_mode,
                            game_version,
                            match_id,
                            queue_id
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                longValue(info, "gameCreation"),
                longValue(info, "gameDuration"),
                longValue(info, "gameEndTimestamp"),
                text(info, "gameMode"),
                text(info, "gameVersion"),
                matchId,
                intValue(info, "queueId")
        );
    }

    private Long findMatchInfoId(String matchId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM match_info WHERE match_id = ?",
                Long.class,
                matchId
        );
    }

    private boolean saveMysqlParticipant(Long matchInfoId, JsonNode participant) {
        Long summonerId = findOrCreateSummoner(participant);
        int inserted = jdbcTemplate.update("""
                        INSERT IGNORE INTO match_participant (
                            assists,
                            champ_level,
                            champion_id,
                            deaths,
                            double_kills,
                            item0,
                            item1,
                            item2,
                            item3,
                            item4,
                            item5,
                            item6,
                            kda,
                            kills,
                            match_id,
                            our_score,
                            penta_kills,
                            perks,
                            quadra_kills,
                            spell1casts,
                            spell2casts,
                            spell3casts,
                            spell4casts,
                            summoner_id,
                            summoner1casts,
                            summoner1id,
                            summoner2casts,
                            summoner2id,
                            team_luck_score,
                            team_position,
                            total_damage_dealt_to_champions,
                            total_damage_taken,
                            total_minion_kills,
                            triple_kills,
                            vision_score,
                            vision_wards_bought_in_game,
                            ward_killed,
                            ward_placed,
                            win
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                intValue(participant, "assists"),
                intValue(participant, "champLevel"),
                longValue(participant, "championId"),
                intValue(participant, "deaths"),
                intValue(participant, "doubleKills"),
                longValue(participant, "item0"),
                longValue(participant, "item1"),
                longValue(participant, "item2"),
                longValue(participant, "item3"),
                longValue(participant, "item4"),
                longValue(participant, "item5"),
                longValue(participant, "item6"),
                kda(participant),
                intValue(participant, "kills"),
                matchInfoId,
                0,
                intValue(participant, "pentaKills"),
                participant.path("perks").toString(),
                intValue(participant, "quadraKills"),
                intValue(participant, "spell1Casts"),
                intValue(participant, "spell2Casts"),
                intValue(participant, "spell3Casts"),
                intValue(participant, "spell4Casts"),
                summonerId,
                intValue(participant, "summoner1Casts"),
                intValue(participant, "summoner1Id"),
                intValue(participant, "summoner2Casts"),
                intValue(participant, "summoner2Id"),
                0,
                text(participant, "teamPosition"),
                longValue(participant, "totalDamageDealtToChampions"),
                longValue(participant, "totalDamageTaken"),
                totalMinionKills(participant),
                intValue(participant, "tripleKills"),
                intValue(participant, "visionScore"),
                intValue(participant, "visionWardsBoughtInGame"),
                intValue(participant, "wardsKilled"),
                intValue(participant, "wardsPlaced"),
                boolValue(participant, "win")
        );
        return inserted == 1;
    }

    private Long findOrCreateSummoner(JsonNode participant) {
        String puuid = text(participant, "puuid");
        String gameName = firstText(participant, "riotIdGameName", "summonerName");
        String tagLine = text(participant, "riotIdTagline");
        String trimmedGameName = gameName == null ? null : gameName.replace(" ", "");

        jdbcTemplate.update("""
                        INSERT IGNORE INTO summoner (
                            puuid,
                            game_name,
                            tag_line,
                            trimmed_game_name,
                            create_at,
                            update_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                puuid,
                gameName,
                tagLine,
                trimmedGameName,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        return jdbcTemplate.queryForObject(
                "SELECT id FROM summoner WHERE puuid = ?",
                Long.class,
                puuid
        );
    }

    private boolean saveMongoParticipant(String matchId, JsonNode info, JsonNode participant, int index) {
        String puuid = text(participant, "puuid");
        String documentId = matchId + ":" + puuid;

        org.springframework.data.mongodb.core.query.Query query =
                new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria.where("_id").is(documentId)
                );
        if (mongoTemplate.exists(query, RAW_COLLECTION)) {
            return false;
        }

        try {
            Document document = new Document();
            document.put("_id", documentId);
            document.put("matchId", matchId);
            document.put("puuid", puuid);
            document.put("participantIndex", index);
            document.put("riotIdGameName", firstText(participant, "riotIdGameName", "summonerName"));
            document.put("riotIdTagline", text(participant, "riotIdTagline"));
            document.put("championId", longValue(participant, "championId"));
            document.put("queueId", intValue(info, "queueId"));
            document.put("gameMode", text(info, "gameMode"));
            document.put("gameVersion", text(info, "gameVersion"));
            document.put("gameEndTimestamp", longValue(info, "gameEndTimestamp"));
            document.put("gameDuration", longValue(info, "gameDuration"));
            document.put("win", boolValue(participant, "win"));
            document.put("teamPosition", text(participant, "teamPosition"));
            document.put("kills", intValue(participant, "kills"));
            document.put("deaths", intValue(participant, "deaths"));
            document.put("assists", intValue(participant, "assists"));
            document.put("kda", kda(participant));
            document.put("champLevel", intValue(participant, "champLevel"));
            document.put("totalMinionKills", totalMinionKills(participant));
            document.put("totalDamageDealtToChampions", longValue(participant, "totalDamageDealtToChampions"));
            document.put("totalDamageTaken", longValue(participant, "totalDamageTaken"));
            document.put("visionScore", intValue(participant, "visionScore"));
            document.put("wardsKilled", intValue(participant, "wardsKilled"));
            document.put("wardsPlaced", intValue(participant, "wardsPlaced"));
            document.put("visionWardsBoughtInGame", intValue(participant, "visionWardsBoughtInGame"));
            document.put("participantPayload", objectMapper.convertValue(participant, new TypeReference<Map<String, Object>>() {}));
            document.put("createdAt", LocalDateTime.now());
            document.put("updatedAt", LocalDateTime.now());

            mongoTemplate.insert(document, RAW_COLLECTION);
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private int intValue(JsonNode node, String field) {
        return node.path(field).asInt(0);
    }

    private long longValue(JsonNode node, String field) {
        return node.path(field).asLong(0L);
    }

    private boolean boolValue(JsonNode node, String field) {
        return node.path(field).asBoolean(false);
    }

    private int totalMinionKills(JsonNode participant) {
        return intValue(participant, "totalMinionsKilled")
                + intValue(participant, "neutralMinionsKilled");
    }

    private float kda(JsonNode participant) {
        JsonNode challengeKda = participant.path("challenges").path("kda");
        if (!challengeKda.isMissingNode() && !challengeKda.isNull()) {
            return challengeKda.floatValue();
        }

        int deaths = intValue(participant, "deaths");
        int kills = intValue(participant, "kills");
        int assists = intValue(participant, "assists");
        if (deaths == 0) {
            return kills + assists;
        }
        return (kills + assists) / (float) deaths;
    }

    private int countMysqlParticipants(Long matchInfoId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM match_participant
                WHERE match_id = ?
                """,
                Integer.class,
                matchInfoId
        );

        return count == null ? 0 : count;
    }


    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("매치 데이터 저장", method, status, detail);
    }
    private boolean hasCompleteParticipants(JsonNode participants) {
        if (!participants.isArray()
                || participants.size() != EXPECTED_PARTICIPANT_COUNT) {
            return false;
        }

        Set<String> uniquePuuids = new HashSet<>();

        for (JsonNode participant : participants) {
            String puuid = text(participant, "puuid");

            if (puuid == null
                    || puuid.isBlank()
                    || !uniquePuuids.add(puuid)) {
                return false;
            }
        }

        return true;
    }

    public record PersistResult(
            int savedMysqlMatches,
            int savedMysqlParticipants,
            int savedMongoParticipants,
            int skippedDuplicateParticipants
    ) {
        public static PersistResult empty() {
            return new PersistResult(0, 0, 0, 0);
        }

        public PersistResult plus(PersistResult other) {
            return new PersistResult(
                    savedMysqlMatches + other.savedMysqlMatches,
                    savedMysqlParticipants + other.savedMysqlParticipants,
                    savedMongoParticipants + other.savedMongoParticipants,
                    skippedDuplicateParticipants + other.skippedDuplicateParticipants
            );
        }
    }

}
