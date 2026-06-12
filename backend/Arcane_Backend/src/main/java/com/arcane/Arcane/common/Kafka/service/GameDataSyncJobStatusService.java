package com.arcane.Arcane.common.Kafka.service;

import com.arcane.Arcane.common.Kafka.dto.GameDataSyncCompletedEvent;
import com.arcane.Arcane.common.Kafka.dto.GameDataSyncFailedEvent;
import com.arcane.Arcane.common.Kafka.dto.GameDataSyncRequestedEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameDataSyncJobStatusService {
    private static final int MAX_RECENT_JOBS = 20;

    private final Map<String, GameDataSyncJobStatus> jobs = new ConcurrentHashMap<>();

    public void markPublished(GameDataSyncRequestedEvent event) {
        jobs.put(event.jobId(), new GameDataSyncJobStatus(
                event.jobId(),
                "PUBLISHED",
                event.requestedBy(),
                event.requestedAt(),
                null,
                null,
                "Kafka 게임 데이터 동기화 요청 발행 완료",
                event.traceId(),
                null,
                0,
                0,
                0,
                0
        ));
    }

    public void markPublishFailed(String jobId, String message) {
        jobs.compute(jobId, (key, current) -> new GameDataSyncJobStatus(
                jobId,
                "PUBLISH_FAILED",
                current == null ? null : current.requestedBy(),
                current == null ? LocalDateTime.now() : current.requestedAt(),
                null,
                LocalDateTime.now(),
                message == null ? "Kafka 게임 데이터 동기화 요청 발행 실패" : message,
                current == null ? null : current.traceId(),
                current == null ? null : current.version(),
                current == null ? 0 : current.championCount(),
                current == null ? 0 : current.itemCount(),
                current == null ? 0 : current.summonerSpellCount(),
                current == null ? 0 : current.runeCount()
        ));
    }

    public void markCompleted(GameDataSyncCompletedEvent event) {
        jobs.compute(event.jobId(), (key, current) -> new GameDataSyncJobStatus(
                event.jobId(),
                "COMPLETED",
                current == null ? null : current.requestedBy(),
                current == null ? null : current.requestedAt(),
                LocalDateTime.now(),
                null,
                event.message() == null ? "게임 데이터 동기화 완료" : event.message(),
                event.traceId(),
                event.version(),
                safe(event.championCount()),
                safe(event.itemCount()),
                safe(event.summonerSpellCount()),
                safe(event.runeCount())
        ));
    }

    public void markFailed(GameDataSyncFailedEvent event) {
        jobs.compute(event.jobId(), (key, current) -> new GameDataSyncJobStatus(
                event.jobId(),
                "FAILED",
                current == null ? null : current.requestedBy(),
                current == null ? null : current.requestedAt(),
                null,
                LocalDateTime.now(),
                event.reason(),
                event.traceId(),
                current == null ? null : current.version(),
                current == null ? 0 : current.championCount(),
                current == null ? 0 : current.itemCount(),
                current == null ? 0 : current.summonerSpellCount(),
                current == null ? 0 : current.runeCount()
        ));
    }

    public List<GameDataSyncJobStatus> recentJobs() {
        return jobs.values().stream()
                .sorted(Comparator.comparing(
                        GameDataSyncJobStatus::lastChangedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(MAX_RECENT_JOBS)
                .toList();
    }

    public GameDataSyncJobStatus latestJob() {
        return recentJobs().stream().findFirst().orElse(null);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    public record GameDataSyncJobStatus(
            String jobId,
            String status,
            Long requestedBy,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            LocalDateTime failedAt,
            String message,
            String traceId,
            String version,
            Integer championCount,
            Integer itemCount,
            Integer summonerSpellCount,
            Integer runeCount
    ) {
        public LocalDateTime lastChangedAt() {
            if (completedAt != null) {
                return completedAt;
            }
            if (failedAt != null) {
                return failedAt;
            }
            return requestedAt;
        }
    }
}
