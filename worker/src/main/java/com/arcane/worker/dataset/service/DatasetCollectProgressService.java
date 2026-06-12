package com.arcane.worker.dataset.service;

import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.kafka.event.dto.DatasetCollectRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetCollectProgressService {
    private static final String JOB_KEY_PREFIX = "dataset:collect:job:";
    private static final String RUNNING_LOCK_KEY = "dataset:collect:running";
    private static final Duration RUNNING_LOCK_TTL = Duration.ofHours(8);

    private final RedisTemplate<String, String> redisTemplate;

    public boolean acquireRunningLock(String jobId) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(RUNNING_LOCK_KEY, jobId, RUNNING_LOCK_TTL);
        if (Boolean.TRUE.equals(acquired)) {
            return true;
        }

        String runningJobId = redisTemplate.opsForValue().get(RUNNING_LOCK_KEY);
        if (isTerminalStatus(runningJobId)) {
            releaseRunningLock(runningJobId);
            return Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(RUNNING_LOCK_KEY, jobId, RUNNING_LOCK_TTL));
        }

        return false;
    }

    public void releaseRunningLock(String jobId) {
        try {
            String currentJobId = redisTemplate.opsForValue().get(RUNNING_LOCK_KEY);
            if (jobId.equals(currentJobId)) {
                redisTemplate.delete(RUNNING_LOCK_KEY);
            }
        } catch (Exception e) {
            log.warn(logMessage(
                    "DatasetCollectProgressService.releaseRunningLock",
                    "락 해제 실패",
                    "jobId=" + jobId + " | reason=" + e.getMessage()
            ));
        }
    }

    public boolean isTerminalStatus(String jobId) {
        String status = statusOf(jobId);
        return "COMPLETED".equals(status) || "FAILED".equals(status) || "PUBLISH_FAILED".equals(status);
    }

    public String statusOf(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return null;
        }
        Object status = redisTemplate.opsForHash().get(jobKey(jobId), "status");
        return status == null ? null : status.toString();
    }

    public void markRunning(DatasetCollectRequestedEvent event, int totalPuuids) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("status", "RUNNING");
        values.put("phase", "RANKER_MATCH_ID_FETCH");
        values.put("startedAt", LocalDateTime.now().toString());
        values.put("updatedAt", LocalDateTime.now().toString());
        values.put("message", "랭커 PUUID 기반 매치 ID 수집 시작");
        values.put("totalPuuids", String.valueOf(totalPuuids));
        values.put("processedPuuids", "0");
        values.put("candidateMatchIds", "0");
        values.put("uniqueMatchIds", "0");
        values.put("processedMatches", "0");
        values.put("savedMysqlMatches", "0");
        values.put("savedMongoParticipants", "0");
        values.put("skippedDuplicateMatches", "0");
        values.put("failedMatches", "0");
        values.put("retryCount", "0");
        redisTemplate.opsForHash().putAll(jobKey(event.jobId()), values);

        log.info(logMessage(
                "DatasetCollectProgressService.markRunning",
                "진행 시작",
                "jobId=" + event.jobId() + " | totalPuuids=" + totalPuuids
        ));
    }

    public void updateProgress(String jobId, String phase, String message, Map<String, ?> counters) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("status", "RUNNING");
        values.put("phase", phase);
        values.put("message", message);
        values.put("updatedAt", LocalDateTime.now().toString());

        counters.forEach((key, value) -> values.put(key, stringify(value)));
        redisTemplate.opsForHash().putAll(jobKey(jobId), values);
    }

    public void markCompleted(String jobId, String message) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("status", "COMPLETED");
        values.put("phase", "DONE");
        values.put("message", message);
        values.put("completedAt", LocalDateTime.now().toString());
        values.put("updatedAt", LocalDateTime.now().toString());
        redisTemplate.opsForHash().putAll(jobKey(jobId), values);
    }

    public void markFailed(String jobId, String message) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("status", "FAILED");
        values.put("phase", "FAILED");
        values.put("message", message);
        values.put("failedAt", LocalDateTime.now().toString());
        values.put("updatedAt", LocalDateTime.now().toString());
        redisTemplate.opsForHash().putAll(jobKey(jobId), values);
    }

    private String jobKey(String jobId) {
        return JOB_KEY_PREFIX + jobId;
    }

    private String stringify(Object value) {
        return value == null ? "" : value.toString();
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("데이터 수집 진행률", method, status, detail);
    }
}
