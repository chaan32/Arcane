package com.arcane.Arcane.riot.analysis.domain;
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
        name = "champion_position_stat",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_champion_position_snapshot",
                        columnNames = {"snapshot_id", "champion_id", "queue_id", "team_position"}
                )
        },
        indexes = {
                @Index(name = "idx_champion_position_champion", columnList = "champion_id"),
                @Index(name = "idx_champion_position_position", columnList = "team_position"),
                @Index(name = "idx_champion_position_win_rate", columnList = "win_rate"),
                @Index(name = "idx_champion_position_tier", columnList = "tier"),
                @Index(name = "idx_champion_position_tier_score", columnList = "tier_score")
        }
)
public class ChampionPositionStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id", nullable = false, length = 80)
    private String snapshotId;

    @Column(name = "champion_id", nullable = false)
    private Long championId;

    @Column(name = "queue_id", nullable = false)
    private Integer queueId;

    @Column(name = "team_position", nullable = false, length = 20)
    private String teamPosition;

    @Column(nullable = false)
    private Integer games;

    @Column(nullable = false)
    private Integer wins;

    @Column(nullable = false)
    private Integer losses;

    @Column(name = "win_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal winRate;

    @Column(name = "pick_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal pickRate;

    @Column(name = "adjusted_win_rate", precision = 5, scale = 2)
    private BigDecimal adjustedWinRate;

    @Column(name = "recency_score", precision = 5, scale = 2)
    private BigDecimal recencyScore;

    @Column(name = "frequency_score", precision = 5, scale = 2)
    private BigDecimal frequencyScore;

    @Column(name = "performance_score", precision = 5, scale = 2)
    private BigDecimal performanceScore;

    @Column(name = "tier_score", precision = 5, scale = 2)
    private BigDecimal tierScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 20)
    private ChampionTier tier;

    @Column(name = "avg_kills", nullable = false, precision = 6, scale = 2)
    private BigDecimal avgKills;

    @Column(name = "avg_deaths", nullable = false, precision = 6, scale = 2)
    private BigDecimal avgDeaths;

    @Column(name = "avg_assists", nullable = false, precision = 6, scale = 2)
    private BigDecimal avgAssists;

    @Column(name = "avg_kda", nullable = false, precision = 6, scale = 2)
    private BigDecimal avgKda;

    @Column(name = "avg_cs", nullable = false, precision = 8, scale = 2)
    private BigDecimal avgCs;

    @Column(name = "avg_damage_dealt", nullable = false, precision = 10, scale = 2)
    private BigDecimal avgDamageDealt;

    @Column(name = "avg_damage_taken", nullable = false, precision = 10, scale = 2)
    private BigDecimal avgDamageTaken;

    @Column(name = "avg_vision_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal avgVisionScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
