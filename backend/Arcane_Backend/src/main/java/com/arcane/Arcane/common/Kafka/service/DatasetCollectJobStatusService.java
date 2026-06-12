package com.arcane.Arcane.common.Kafka.service;

import com.arcane.Arcane.common.Kafka.dto.DatasetCollectCompletedEvent;
import com.arcane.Arcane.common.Kafka.dto.DatasetCollectFailEvent;
import com.arcane.Arcane.common.Kafka.dto.DatasetCollectRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DatasetCollectJobStatusService {
    private static final String JOB_KEY_PREFIX = "dataset:collect:job:";
    private static final String RECENT_JOBS_KEY = "dataset:collect:jobs:recent";
    private static final int MAX_RECENT_JOBS = 20;

    private final RedisTemplate<String, String> redisTemplate;

    public void markPublished(DatasetCollectRequestedEvent event) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("jobId", event.jobId());
        values.put("status", "PUBLISHED");
        values.put("phase", "REQUESTED");
        values.put("requestedBy", stringify(event.requestedBy()));
        values.put("requestedAt", stringify(event.requestedAt()));
        values.put("completedAt", "");
        values.put("failedAt", "");
        values.put("message", "Kafka 데이터 수집 요청 발행 완료");
        values.put("traceId", event.traceId());
        values.put("rankingKey", event.payload().rankingKey());
        values.put("rankerLimit", stringify(event.payload().rankerLimit()));
        values.put("matchCount", stringify(event.payload().matchCount()));
        values.put("queueId", stringify(event.payload().queueId()));
        values.put("processedPuuids", "0");
        values.put("totalPuuids", "0");
        values.put("candidateMatchIds", "0");
        values.put("uniqueMatchIds", "0");
        values.put("processedMatches", "0");
        values.put("savedMysqlMatches", "0");
        values.put("savedMongoParticipants", "0");
        values.put("skippedDuplicateMatches", "0");
        values.put("failedMatches", "0");
        values.put("retryCount", "0");
        values.put("updatedAt", stringify(LocalDateTime.now()));

        redisTemplate.opsForHash().putAll(jobKey(event.jobId()), values);
        redisTemplate.opsForList().remove(RECENT_JOBS_KEY, 0, event.jobId());
        redisTemplate.opsForList().leftPush(RECENT_JOBS_KEY, event.jobId());
        redisTemplate.opsForList().trim(RECENT_JOBS_KEY, 0, MAX_RECENT_JOBS - 1);
    }

    public void markPublishFailed(String jobId, String message) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("status", "PUBLISH_FAILED");
        values.put("phase", "PUBLISH");
        values.put("failedAt", stringify(LocalDateTime.now()));
        values.put("message", message == null ? "Kafka 데이터 수집 요청 발행 실패" : message);
        values.put("updatedAt", stringify(LocalDateTime.now()));
        redisTemplate.opsForHash().putAll(jobKey(jobId), values);
    }

    public void markCompleted(DatasetCollectCompletedEvent event) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("status", "COMPLETED");
        values.put("phase", "DONE");
        values.put("completedAt", stringify(event.completedAt()));
        values.put("message", event.message());
        values.put("updatedAt", stringify(LocalDateTime.now()));
        redisTemplate.opsForHash().putAll(jobKey(event.jobId()), values);
    }

    public void markFailed(DatasetCollectFailEvent event) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("status", "FAILED");
        values.put("phase", "FAILED");
        values.put("failedAt", stringify(event.failedAt()));
        values.put("message", event.message());
        values.put("updatedAt", stringify(LocalDateTime.now()));
        redisTemplate.opsForHash().putAll(jobKey(event.jobId()), values);
    }

    public DatasetCollectJobStatus findJob(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return null;
        }

        Map<Object, Object> values = redisTemplate.opsForHash().entries(jobKey(jobId));
        if (values == null || values.isEmpty()) {
            return null;
        }

        return toStatus(values);
    }

    public DatasetCollectJobStatus latestJob() {
        return recentJobs().stream().findFirst().orElse(null);
    }

    public List<DatasetCollectJobStatus> recentJobs() {
        List<String> jobIds = Optional
                .ofNullable(redisTemplate.opsForList().range(RECENT_JOBS_KEY, 0, MAX_RECENT_JOBS - 1))
                .orElse(List.of());

        return jobIds.stream()
                .map(this::findJob)
                .filter(status -> status != null)
                .toList();
    }

    private DatasetCollectJobStatus toStatus(Map<Object, Object> values) {
        return new DatasetCollectJobStatus(
                value(values, "jobId"),
                value(values, "status"),
                value(values, "phase"),
                value(values, "message"),
                value(values, "traceId"),
                value(values, "rankingKey"),
                intValue(values, "rankerLimit"),
                intValue(values, "matchCount"),
                intValue(values, "queueId"),
                intValue(values, "totalPuuids"),
                intValue(values, "processedPuuids"),
                intValue(values, "candidateMatchIds"),
                intValue(values, "uniqueMatchIds"),
                intValue(values, "processedMatches"),
                intValue(values, "savedMysqlMatches"),
                intValue(values, "savedMongoParticipants"),
                intValue(values, "skippedDuplicateMatches"),
                intValue(values, "failedMatches"),
                intValue(values, "retryCount"),
                value(values, "requestedAt"),
                value(values, "startedAt"),
                value(values, "completedAt"),
                value(values, "failedAt"),
                value(values, "updatedAt")
        );
    }

    private String jobKey(String jobId) {
        return JOB_KEY_PREFIX + jobId;
    }

    private String value(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : value.toString();
    }

    private Integer intValue(Map<Object, Object> values, String key) {
        String value = value(values, key);
        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String stringify(Object value) {
        return value == null ? "" : value.toString();
    }

    public record DatasetCollectJobStatus(
            String jobId,
            String status,
            String phase,
            String message,
            String traceId,
            String rankingKey,
            Integer rankerLimit,
            Integer matchCount,
            Integer queueId,
            Integer totalPuuids,
            Integer processedPuuids,
            Integer candidateMatchIds,
            Integer uniqueMatchIds,
            Integer processedMatches,
            Integer savedMysqlMatches,
            Integer savedMongoParticipants,
            Integer skippedDuplicateMatches,
            Integer failedMatches,
            Integer retryCount,
            String requestedAt,
            String startedAt,
            String completedAt,
            String failedAt,
            String updatedAt
    ) {
    }
}
