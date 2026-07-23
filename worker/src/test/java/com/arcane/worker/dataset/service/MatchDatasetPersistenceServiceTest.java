package com.arcane.worker.dataset.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchDatasetPersistenceServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private MongoTemplate mongoTemplate;

    @Test
    void validatesMysqlParticipantCountAfterAllParticipantsAreSaved() {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicInteger participantInsertCount = new AtomicInteger();
        AtomicLong summonerIdSequence = new AtomicLong(1L);

        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql.contains("match_participant")) {
                        participantInsertCount.incrementAndGet();
                    }
                    return 1;
                });

        when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    Class<?> resultType = invocation.getArgument(1);

                    if (resultType == Long.class && sql.contains("match_info")) {
                        return 100L;
                    }
                    if (resultType == Long.class && sql.contains("summoner")) {
                        return summonerIdSequence.getAndIncrement();
                    }
                    if (resultType == Integer.class && sql.contains("COUNT(*)")) {
                        return participantInsertCount.get();
                    }
                    throw new IllegalArgumentException("Unexpected query: " + sql);
                });

        MatchDatasetPersistenceService service = new MatchDatasetPersistenceService(
                jdbcTemplate,
                mongoTemplate,
                objectMapper
        );

        MatchDatasetPersistenceService.PersistResult result = service.persist(
                "KR_TEST_MATCH",
                completeMatch(objectMapper),
                true,
                false
        );

        assertThat(result.savedMysqlMatches()).isEqualTo(1);
        assertThat(result.savedMysqlParticipants()).isEqualTo(10);
        assertThat(participantInsertCount).hasValue(10);
    }

    private JsonNode completeMatch(ObjectMapper objectMapper) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode info = root.putObject("info");
        info.put("gameCreation", 1L);
        info.put("gameDuration", 1800L);
        info.put("gameEndTimestamp", 2L);
        info.put("gameMode", "CLASSIC");
        info.put("gameVersion", "16.14.1");
        info.put("queueId", 420);

        ArrayNode participants = info.putArray("participants");
        for (int index = 0; index < 10; index++) {
            ObjectNode participant = participants.addObject();
            participant.put("puuid", "puuid-" + index);
            participant.put("riotIdGameName", "player-" + index);
            participant.put("riotIdTagline", "KR1");
            participant.put("championId", index + 1L);
        }
        return root;
    }
}
