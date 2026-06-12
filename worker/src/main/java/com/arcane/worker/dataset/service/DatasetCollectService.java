package com.arcane.worker.dataset.service;

import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.exception.fail.RiotApiFatalException;
import com.arcane.worker.exception.fail.RiotApiRateLimitException;
import com.arcane.worker.exception.fail.RiotApiRetryableException;
import com.arcane.worker.kafka.event.dto.DatasetCollectPayload;
import com.arcane.worker.kafka.event.dto.DatasetCollectRequestedEvent;
import com.arcane.worker.redis.RedisService;
import com.arcane.worker.riot.service.RiotMatchApiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetCollectService {
    private static final int DEFAULT_RANKER_LIMIT = 500;
    private static final int DEFAULT_MATCH_COUNT = 15;
    private static final int DEFAULT_QUEUE_ID = 420;
    private static final int MAX_RETRY_ATTEMPTS = 8;
    private static final int MATCH_DETAIL_WORKER_COUNT = 4;

    private final RedisService redisService;
    private final RiotMatchApiService riotMatchApiService;
    private final MatchDatasetPersistenceService persistenceService;
    private final DatasetCollectProgressService progressService;

    public CollectResult collect(DatasetCollectRequestedEvent event) {
        DatasetCollectPayload payload = event.payload();
        String rankingKey = stringOrDefault(payload == null ? null : payload.rankingKey(), "ranking:all");
        int rankerLimit = intOrDefault(payload == null ? null : payload.rankerLimit(), DEFAULT_RANKER_LIMIT);
        int matchCount = intOrDefault(payload == null ? null : payload.matchCount(), DEFAULT_MATCH_COUNT);
        int queueId = intOrDefault(payload == null ? null : payload.queueId(), DEFAULT_QUEUE_ID);
        boolean saveMysql = boolOrDefault(payload == null ? null : payload.saveMysql(), true);
        boolean saveMongo = boolOrDefault(payload == null ? null : payload.saveMongo(), true);

        if (!progressService.acquireRunningLock(event.jobId())) {
            throw new IllegalStateException("이미 다른 데이터 수집 작업이 진행 중입니다.");
        }

        AtomicInteger retryCount = new AtomicInteger(0);
        try {
            List<String> puuids = redisService.getRankersPuuid(rankingKey, rankerLimit);
            progressService.markRunning(event, puuids.size());

            log.info(logMessage(
                    "DatasetCollectService.collect",
                    "수집 시작",
                    "jobId=" + event.jobId()
                            + " | rankingKey=" + rankingKey
                            + " | rankerLimit=" + rankerLimit
                            + " | puuidCount=" + puuids.size()
                            + " | matchCount=" + matchCount
                            + " | queueId=" + queueId
            ));

            MatchIdCollectResult matchIdCollectResult = collectMatchIds(
                    event.jobId(),
                    puuids,
                    matchCount,
                    queueId,
                    retryCount
            );

            MatchPersistCollectResult persistResult = collectMatchDetails(
                    event.jobId(),
                    new ArrayList<>(matchIdCollectResult.uniqueMatchIds()),
                    queueId,
                    saveMysql,
                    saveMongo,
                    retryCount,
                    matchIdCollectResult.processedPuuids(),
                    matchIdCollectResult.candidateMatchIds()
            );

            CollectResult result = new CollectResult(
                    puuids.size(),
                    matchIdCollectResult.processedPuuids(),
                    matchIdCollectResult.candidateMatchIds(),
                    matchIdCollectResult.uniqueMatchIds().size(),
                    persistResult.processedMatches(),
                    persistResult.savedMysqlMatches(),
                    persistResult.savedMongoParticipants(),
                    persistResult.skippedDuplicateMatches(),
                    persistResult.failedMatches(),
                    retryCount.get()
            );

            progressService.markCompleted(event.jobId(), "챌린저 포함 게임 데이터 수집 완료");
            log.info(logMessage(
                    "DatasetCollectService.collect",
                    "수집 완료",
                    "jobId=" + event.jobId()
                            + " | uniqueMatchIds=" + result.uniqueMatchIds()
                            + " | processedMatches=" + result.processedMatches()
                            + " | savedMongoParticipants=" + result.savedMongoParticipants()
                            + " | retryCount=" + result.retryCount()
            ));
            return result;
        } finally {
            progressService.releaseRunningLock(event.jobId());
        }
    }

    private MatchIdCollectResult collectMatchIds(
            String jobId,
            List<String> puuids,
            int matchCount,
            int queueId,
            AtomicInteger retryCount
    ) {
        Set<String> uniqueMatchIds = new LinkedHashSet<>();
        int candidateMatchIds = 0;
        int processedPuuids = 0;

        for (String puuid : puuids) {
            try {
                List<String> matchIds = withRetry(
                        jobId,
                        "matchIds:" + puuid,
                        () -> riotMatchApiService.getMatchIdsByPuuid(puuid, matchCount, queueId),
                        retryCount
                );
                candidateMatchIds += matchIds.size();
                uniqueMatchIds.addAll(matchIds);
            } catch (RiotApiRetryableException e) {
                log.warn(logMessage(
                        "DatasetCollectService.collectMatchIds",
                        "PUUID 스킵",
                        "jobId=" + jobId + " | puuid=" + puuid + " | reason=" + e.getMessage()
                ));
            }

            processedPuuids++;
            progressService.updateProgress(jobId, "RANKER_MATCH_ID_FETCH", "랭커별 최근 매치 ID 수집 중", Map.of(
                    "processedPuuids", processedPuuids,
                    "candidateMatchIds", candidateMatchIds,
                    "uniqueMatchIds", uniqueMatchIds.size(),
                    "retryCount", retryCount.get()
            ));
        }

        return new MatchIdCollectResult(processedPuuids, candidateMatchIds, uniqueMatchIds);
    }

    private MatchPersistCollectResult collectMatchDetails(
            String jobId,
            List<String> uniqueMatchIds,
            int queueId,
            boolean saveMysql,
            boolean saveMongo,
            AtomicInteger retryCount,
            int processedPuuids,
            int candidateMatchIds
    ) {
        AtomicInteger processedMatches = new AtomicInteger(0);
        AtomicInteger savedMysqlMatches = new AtomicInteger(0);
        AtomicInteger savedMongoParticipants = new AtomicInteger(0);
        AtomicInteger skippedDuplicateMatches = new AtomicInteger(0);
        AtomicInteger failedMatches = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(MATCH_DETAIL_WORKER_COUNT);
        ExecutorCompletionService<MatchDatasetPersistenceService.PersistResult> completionService =
                new ExecutorCompletionService<>(executor);
        List<Future<MatchDatasetPersistenceService.PersistResult>> futures = new ArrayList<>();

        try {
            for (String matchId : uniqueMatchIds) {
                futures.add(completionService.submit(() -> {
                    JsonNode match = withRetry(
                            jobId,
                            "matchDetail:" + matchId,
                            () -> riotMatchApiService.getMatchDetail(matchId),
                            retryCount
                    );
                    if (match == null || match.path("info").path("queueId").asInt(0) != queueId) {
                        return MatchDatasetPersistenceService.PersistResult.empty();
                    }
                    return persistenceService.persist(matchId, match, saveMysql, saveMongo);
                }));
            }

            for (int i = 0; i < futures.size(); i++) {
                try {
                    MatchDatasetPersistenceService.PersistResult result = completionService.take().get();
                    processedMatches.incrementAndGet();
                    savedMysqlMatches.addAndGet(result.savedMysqlMatches());
                    savedMongoParticipants.addAndGet(result.savedMongoParticipants());
                    skippedDuplicateMatches.addAndGet(result.skippedDuplicateParticipants());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RiotApiRetryableException("데이터 수집 worker interrupted.", e);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RiotApiFatalException fatalException) {
                        throw fatalException;
                    }

                    failedMatches.incrementAndGet();
                    log.warn(logMessage(
                            "DatasetCollectService.collectMatchDetails",
                            "매치 스킵",
                            "jobId=" + jobId + " | reason=" + cause.getMessage()
                    ));
                }

                progressService.updateProgress(jobId, "MATCH_DETAIL_FETCH", "매치 상세 조회 및 저장 중", Map.of(
                        "processedPuuids", processedPuuids,
                        "candidateMatchIds", candidateMatchIds,
                        "uniqueMatchIds", uniqueMatchIds.size(),
                        "processedMatches", processedMatches.get(),
                        "savedMysqlMatches", savedMysqlMatches.get(),
                        "savedMongoParticipants", savedMongoParticipants.get(),
                        "skippedDuplicateMatches", skippedDuplicateMatches.get(),
                        "failedMatches", failedMatches.get(),
                        "retryCount", retryCount.get()
                ));
            }
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return new MatchPersistCollectResult(
                processedMatches.get(),
                savedMysqlMatches.get(),
                savedMongoParticipants.get(),
                skippedDuplicateMatches.get(),
                failedMatches.get()
        );
    }

    private <T> T withRetry(String jobId, String operation, Supplier<T> supplier, AtomicInteger retryCount) {
        int attempt = 0;
        while (true) {
            try {
                return supplier.get();
            } catch (RiotApiRateLimitException e) {
                attempt++;
                retryCount.incrementAndGet();
                long waitSeconds = Math.max(1L, e.getRetryAfterSeconds()) + 1L;
                progressService.updateProgress(jobId, "RATE_LIMIT_WAIT", "Riot API 제한으로 대기 중", Map.of(
                        "retryCount", retryCount.get()
                ));
                sleep(waitSeconds, operation);
            } catch (RiotApiRetryableException e) {
                attempt++;
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    throw e;
                }

                retryCount.incrementAndGet();
                long waitSeconds = Math.min(120L, 10L * attempt);
                progressService.updateProgress(jobId, "RETRY_WAIT", "Riot API 일시 오류로 재시도 대기 중", Map.of(
                        "retryCount", retryCount.get()
                ));
                sleep(waitSeconds, operation);
            }
        }
    }

    private void sleep(long waitSeconds, String operation) {
        try {
            log.warn(logMessage(
                    "DatasetCollectService.sleep",
                    "재시도 대기",
                    "operation=" + operation + " | waitSeconds=" + waitSeconds
            ));
            Thread.sleep(TimeUnit.SECONDS.toMillis(waitSeconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RiotApiRetryableException("Riot API retry sleep interrupted.", e);
        }
    }

    private String stringOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int intOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private boolean boolOrDefault(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("챌린저 데이터 수집", method, status, detail);
    }

    public record CollectResult(
            int totalPuuids,
            int processedPuuids,
            int candidateMatchIds,
            int uniqueMatchIds,
            int processedMatches,
            int savedMysqlMatches,
            int savedMongoParticipants,
            int skippedDuplicateMatches,
            int failedMatches,
            int retryCount
    ) {
    }

    private record MatchIdCollectResult(
            int processedPuuids,
            int candidateMatchIds,
            Set<String> uniqueMatchIds
    ) {
    }

    private record MatchPersistCollectResult(
            int processedMatches,
            int savedMysqlMatches,
            int savedMongoParticipants,
            int skippedDuplicateMatches,
            int failedMatches
    ) {
    }
}
