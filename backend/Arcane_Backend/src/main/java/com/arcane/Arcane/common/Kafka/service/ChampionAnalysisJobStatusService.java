package com.arcane.Arcane.common.Kafka.service;

import com.arcane.Arcane.common.Kafka.dto.ChampionAnalysisCompletedEvent;
import com.arcane.Arcane.common.Kafka.dto.ChampionAnalysisFailedEvent;
import com.arcane.Arcane.common.Kafka.dto.ChampionAnalysisProgressEvent;
import com.arcane.Arcane.common.Kafka.dto.ChampionAnalysisRequestedEvent;
import com.arcane.Arcane.common.Logging.TraceIds;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChampionAnalysisJobStatusService {
    private static final int MAX_RECENT_JOBS = 20;

    private final Map<String, ChampionAnalysisJobStatus> jobs = new ConcurrentHashMap<>();

    public void markPublished(ChampionAnalysisRequestedEvent event) {
        LocalDateTime now = LocalDateTime.now();

        jobs.put(event.jobId(), new ChampionAnalysisJobStatus(
                event.jobId(),
                "PUBLISHED",
                event.requestedBy(),
                now,
                null,
                null,
                "Kafka 챔피언 분석 요청 발행 완료",
                TraceIds.normalize(event.traceId()),
                null,
                0,
                "REQUESTED",
                0,
                0,
                0,
                null,
                event.sourceCollection(),
                event.queueId()
        ));
    }

    public void markPublishFailed(String jobId, String message) {
        jobs.compute(jobId, (key, current) -> {
            LocalDateTime now = LocalDateTime.now();

            return new ChampionAnalysisJobStatus(
                    jobId,
                    "PUBLISH_FAILED",
                    current == null ? null : current.requestedBy(),
                    current == null ? now : current.requestedAt(),
                    null,
                    now,
                    message == null ? "Kafka 챔피언 분석 요청 발행 실패" : message,
                    current == null ? null : current.traceId(),
                    current == null ? null : current.snapshotId(),
                    current == null ? 0 : current.progressPercent(),
                    "PUBLISH_FAILED",
                    current == null ? 0 : current.totalParticipants(),
                    current == null ? 0 : current.totalMatches(),
                    current == null ? 0 : current.totalChampions(),
                    current == null ? null : current.patchVersion(),
                    current == null ? null : current.sourceCollection(),
                    current == null ? null : current.queueId()
            );
        });
    }

    public void markProgress(ChampionAnalysisProgressEvent event) {
        jobs.compute(event.jobId(), (key, current) -> new ChampionAnalysisJobStatus(
                event.jobId(),
                safeText(event.status(), "RUNNING"),
                current == null ? null : current.requestedBy(),
                current == null ? null : current.requestedAt(),
                null,
                null,
                safeText(event.message(), "챔피언 분석 진행 중"),
                TraceIds.normalize(event.traceId()),
                event.snapshotId(),
                clampProgress(event.progressPercent()),
                safeText(event.phase(), "RUNNING"),
                safeInt(event.totalParticipants()),
                safeInt(event.totalMatches()),
                safeInt(event.totalChampions()),
                event.patchVersion(),
                current == null ? null : current.sourceCollection(),
                current == null ? null : current.queueId()
        ));
    }

    public void markCompleted(ChampionAnalysisCompletedEvent event) {
        jobs.compute(event.jobId(), (key, current) -> new ChampionAnalysisJobStatus(
                event.jobId(),
                "COMPLETED",
                current == null ? null : current.requestedBy(),
                current == null ? null : current.requestedAt(),
                LocalDateTime.now(),
                null,
                "챔피언 분석 완료",
                TraceIds.normalize(event.traceId()),
                event.snapshotId(),
                100,
                "DONE",
                safeInt(event.totalParticipants()),
                safeInt(event.totalMatches()),
                safeInt(event.totalChampions()),
                event.patchVersion(),
                current == null ? null : current.sourceCollection(),
                current == null ? null : current.queueId()
        ));
    }

    public void markFailed(ChampionAnalysisFailedEvent event) {
        jobs.compute(event.jobId(), (key, current) -> new ChampionAnalysisJobStatus(
                event.jobId(),
                "FAILED",
                current == null ? null : current.requestedBy(),
                current == null ? null : current.requestedAt(),
                null,
                LocalDateTime.now(),
                event.reason(),
                TraceIds.normalize(event.traceId()),
                current == null ? null : current.snapshotId(),
                current == null ? 0 : current.progressPercent(),
                "FAILED",
                current == null ? 0 : current.totalParticipants(),
                current == null ? 0 : current.totalMatches(),
                current == null ? 0 : current.totalChampions(),
                current == null ? null : current.patchVersion(),
                current == null ? null : current.sourceCollection(),
                current == null ? null : current.queueId()
        ));
    }

    public List<ChampionAnalysisJobStatus> recentJobs() {
        return jobs.values().stream()
                .sorted(Comparator.comparing(
                        ChampionAnalysisJobStatus::lastChangedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(MAX_RECENT_JOBS)
                .toList();
    }

    public ChampionAnalysisJobStatus latestJob() {
        return recentJobs().stream().findFirst().orElse(null);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int clampProgress(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record ChampionAnalysisJobStatus(
            String jobId,
            String status,
            Long requestedBy,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            LocalDateTime failedAt,
            String message,
            String traceId,
            String snapshotId,
            Integer progressPercent,
            String phase,
            Integer totalParticipants,
            Integer totalMatches,
            Integer totalChampions,
            String patchVersion,
            String sourceCollection,
            Integer queueId
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
