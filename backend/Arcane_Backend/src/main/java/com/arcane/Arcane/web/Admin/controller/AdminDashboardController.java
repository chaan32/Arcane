package com.arcane.Arcane.web.Admin.controller;

import com.arcane.Arcane.common.Kafka.dto.RankingUpdateRequestResponse;
import com.arcane.Arcane.common.Kafka.dto.DatasetCollectRequestResponse;
import com.arcane.Arcane.common.Kafka.dto.ChampionAnalysisRequestResponse;
import com.arcane.Arcane.common.Kafka.dto.GameDataSyncRequestResponse;
import com.arcane.Arcane.web.Admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return adminDashboardService.dashboard();
    }

    @GetMapping("/ranking-scheduler")
    public Map<String, Object> rankingSchedulerStatus() {
        return adminDashboardService.rankingSchedulerStatus();
    }

    @PatchMapping("/ranking-scheduler")
    public Map<String, Object> updateRankingScheduler(
            @RequestBody RankingSchedulerToggleRequest request
    ) {
        return adminDashboardService.updateRankingScheduler(request.enabled());
    }

    @GetMapping("/server-status")
    public Map<String, Object> serverStatus() {
        return adminDashboardService.serverStatus();
    }

    @GetMapping("/logs")
    public Map<String, Object> logs(
            @RequestParam(defaultValue = "200") int lines,
            @RequestParam(required = false) String fileName
    ) {
        return adminDashboardService.recentLogs(lines, fileName);
    }

    public record RankingSchedulerToggleRequest(boolean enabled) {
    }

    @PostMapping("/ranking-update")
    public RankingUpdateRequestResponse requestRankingUpdate(Authentication authentication) {
        String jobId = adminDashboardService.requestRankingUpdate(authentication.getName());

        return new RankingUpdateRequestResponse(
                jobId, "랭킹 업데이트 요청을 Kafka로 전송했습니다."
        );
    }

    @PostMapping("/dataset-collection")
    public DatasetCollectRequestResponse requestDatasetCollection(
            Authentication authentication,
            @RequestBody(required = false) DatasetCollectionRequest request
    ) {
        String jobId = adminDashboardService.requestDatasetCollect(
                authentication.getName(),
                request == null ? null : request.rankingKey(),
                request == null ? null : request.rankerLimit(),
                request == null ? null : request.matchCount(),
                request == null ? null : request.queueId()
        );

        return new DatasetCollectRequestResponse(
                jobId, "상위 랭커 매치 데이터 수집 요청을 Kafka로 전송했습니다."
        );
    }

    public record DatasetCollectionRequest(
            String rankingKey,
            Integer rankerLimit,
            Integer matchCount,
            Integer queueId
    ) {
    }

    @PostMapping("/champion-analysis")
    public ChampionAnalysisRequestResponse requestChampionAnalysis(Authentication authentication) {
        String jobId = adminDashboardService.requestChampionAnalysis(authentication.getName());

        return new ChampionAnalysisRequestResponse(
                jobId, "챔피언 분석 요청을 Kafka로 전송했습니다."
        );
    }

    @PostMapping("/game-data-sync")
    public GameDataSyncRequestResponse requestGameDataSync(Authentication authentication) {
        String jobId = adminDashboardService.requestGameDataSync(authentication.getName());

        return new GameDataSyncRequestResponse(
                jobId, "게임 데이터 동기화 요청을 Kafka로 전송했습니다."
        );
    }

    @GetMapping("/dataset-collection")
    public Map<String, Object> datasetCollectionStatus() {
        return adminDashboardService.datasetCollectionStatus();
    }

    @GetMapping("/champion-analysis")
    public Map<String, Object> championAnalysisStatus() {
        return adminDashboardService.championAnalysisStatus();
    }

    @GetMapping("/game-data-sync")
    public Map<String, Object> gameDataSyncStatus() {
        return adminDashboardService.gameDataSyncStatus();
    }

    @GetMapping("/dataset-collection/{jobId}")
    public Object datasetCollectionJobStatus(@PathVariable String jobId) {
        return adminDashboardService.datasetCollectionJobStatus(jobId);
    }
}
