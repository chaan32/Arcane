package com.arcane.worker.analysis.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "champion_analysis_snapshot",
        indexes = {
                @Index(name = "idx_champion_analysis_snapshot_active", columnList = "is_active"),
                @Index(name = "idx_champion_analysis_snapshot_status", columnList = "status")
        }
)
/**
 *  분석 작업이 시작 됐는지
 *  1) 완료되었는지
 *  2) 실패했는지
 *  3) 이번 분석 결과가 현재 사용 중인 최신 결과인지
 *
 * [필요 이유]
 * 계산 중인 데이터와 완료된 데이터를 구분하기 어려움
 * 실패한 분석 결과가 프론트에 노출될 수 있음
 * 관리자 페이지에서 진행 상태를 보여주기 어려움
 * 이전 분석 결과와 새 분석 결과를 비교하기 어려움
 */
public class ChampionAnalysisSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 고유 식별 값
    @Column(name = "snapshot_id", nullable = false, unique = true, length = 80)
    private String snapshotId;


    // 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChampionAnalysisStatus status;

    @Column(name = "queue_id", nullable = false)
    private Integer queueId;

    @Column(name = "source_collection", nullable = false, length = 100)
    private String sourceCollection;

    @Column(name = "patch_version", length = 30)
    private String patchVersion;

    // 분석에 쓴 총 participant
    @Column(name = "total_participants", nullable = false)
    private Integer totalParticipants;

    // 분석에 쓴 match 값
    @Column(name = "total_matches", nullable = false)
    private Integer totalMatches;

    // 분석에 쓴 총 챔피언 수
    @Column(name = "total_champions", nullable = false)
    private Integer totalChampions;

    @Column(name = "requested_by")
    private Long requestedBy;

    // 실패 이유
    @Column(length = 500)
    private String message;

    // 써도 되는지 체크하는거
    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ChampionAnalysisSnapshot running(
            String snapshotId,
            Integer queueId,
            String sourceCollection,
            Long requestedBy
    ) {
        LocalDateTime now = LocalDateTime.now();

        return ChampionAnalysisSnapshot.builder()
                .snapshotId(snapshotId)
                .status(ChampionAnalysisStatus.RUNNING)
                .queueId(queueId)
                .sourceCollection(sourceCollection)
                .patchVersion("UNKNOWN")
                .totalParticipants(0)
                .totalMatches(0)
                .totalChampions(0)
                .requestedBy(requestedBy)
                .active(false)
                .startedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void setComplete(int totalParticipants, int totalMatches, int totalChampions) {
        // status completed로
        this.status = ChampionAnalysisStatus.COMPLETED;

        this.totalParticipants = totalParticipants;
        this.totalMatches = totalMatches;
        this.totalChampions = totalChampions;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePatchVersion(String patchVersion) {
        if (patchVersion != null && !patchVersion.isBlank()) {
            this.patchVersion = patchVersion;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void fail(String message) {
        this.message = message;
        this.status = ChampionAnalysisStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}
