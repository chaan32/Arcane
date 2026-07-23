package com.arcane.Arcane.common.Logging;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReadableActivityLogTest {

    @Test
    void parsesWorkerRankingCompletionAsReadableActivity() {
        String line = """
                2026-07-23 10:11:12.123 INFO  [Arcane-Worker] trace=trace-123 thread=ranking-worker logger=com.arcane.worker.ranker.service.RankingService | [WORKER][랭킹 업데이트][RankingService.updateRanking][작업 완료] jobId=ranking-1 | traceId=trace-123
                """.trim();

        Optional<Map<String, Object>> result = ReadableActivityLog.parse(line, "worker");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow())
                .containsEntry("occurredAt", "2026-07-23 10:11:12.123")
                .containsEntry("level", "INFO")
                .containsEntry("category", "랭킹 업데이트")
                .containsEntry("message", "랭킹 업데이트 작업을 완료했습니다.")
                .containsEntry("detail", "작업 ID=ranking-1")
                .containsEntry("traceId", "trace-123")
                .containsEntry("source", "worker");
    }

    @Test
    void parsesWorkerRateLimitWaitAsWarningActivity() {
        String line = """
                2026-07-23 10:12:00.321 WARN  [Arcane-Worker] trace=trace-429 thread=ranking-worker logger=com.arcane.worker.ranker.service.RankingService | [WORKER][랭킹 업데이트][RankingService.requestLeagueByTier][429 대기] tier=CHALLENGER | retryAfterSeconds=125 | attempt=1/3
                """.trim();

        Optional<Map<String, Object>> result = ReadableActivityLog.parse(line, "worker");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow())
                .containsEntry("level", "WARN")
                .containsEntry("category", "랭킹 업데이트")
                .containsEntry("message", "랭킹 업데이트가 Riot API 요청 제한으로 대기 중입니다.")
                .containsEntry("detail", "티어=CHALLENGER · 재시도=1/3 · 대기 시간=125초")
                .containsEntry("traceId", "trace-429");
    }

    @Test
    void ignoresNoisyPerRecordWorkerSuccessLog() {
        String line = """
                2026-07-23 10:13:00.456 INFO  [Arcane-Worker] trace=trace-match thread=dataset-worker logger=com.arcane.worker.dataset.service.MatchDatasetPersistenceService | [WORKER][매치 데이터 저장][MatchDatasetPersistenceService.saveMatch][저장 완료] matchId=KR_123 | participantCount=10
                """.trim();

        Optional<Map<String, Object>> result = ReadableActivityLog.parse(line, "worker");

        assertThat(result).isEmpty();
    }

    @Test
    void ignoresFrequentWorkerProgressLog() {
        String line = """
                2026-07-23 10:14:00.789 INFO  [Arcane-Worker] trace=trace-progress thread=analysis-worker logger=com.arcane.worker.analysis.service.ChampionAnalysisService | [WORKER][챔피언 분석][ChampionAnalysisService.analyze][진행률 갱신] jobId=analysis-1 | progress=42% | phase=AGGREGATING
                """.trim();

        Optional<Map<String, Object>> result = ReadableActivityLog.parse(line, "worker");

        assertThat(result).isEmpty();
    }
}
