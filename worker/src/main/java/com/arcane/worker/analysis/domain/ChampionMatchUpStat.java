package com.arcane.worker.analysis.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "champion_matchup_stat",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_champion_matchup_snapshot",
                        columnNames = {
                                "snapshot_id",
                                "champion_id",
                                "opponent_champion_id",
                                "queue_id",
                                "team_position"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_champion_matchup_champion", columnList = "champion_id"),
                @Index(name = "idx_champion_matchup_opponent", columnList = "opponent_champion_id")
        }
)
public class ChampionMatchUpStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id", nullable = false, length = 80)
    private String snapshotId;

    // 챔피언 아이디
    @Column(name = "champion_id", nullable = false)
    private Long championId;

    // 상대 챔피언 아이디
    @Column(name = "opponent_champion_id", nullable = false)
    private Long opponentChampionId;

    @Column(name = "queue_id", nullable = false)
    private Integer queueId;

    // 어느 포지션인지?
    @Column(name = "team_position", nullable = false, length = 20)
    private String teamPosition;

    // 몇개 했는가?
    @Column(nullable = false)
    private Integer games;

    // 몇승
    @Column(nullable = false)
    private Integer wins;

    // 몇패?
    @Column(nullable = false)
    private Integer losses;

    // 승률
    @Column(name = "win_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal winRate;

    // 평균 KDA
    @Column(name = "avg_kda", nullable = false, precision = 6, scale = 2)
    private BigDecimal avgKda;

    @Column(name = "avg_damage_dealt", nullable = false, precision = 10, scale = 2)
    private BigDecimal avgDamageDealt;

    @Column(name = "avg_cs", nullable = false, precision = 8, scale = 2)
    private BigDecimal avgCs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
