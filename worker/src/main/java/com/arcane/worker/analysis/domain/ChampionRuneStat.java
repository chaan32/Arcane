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
        name = "champion_rune_stat",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_champion_rune_stat",
                        columnNames = {
                                "snapshot_id",
                                "champion_id",
                                "queue_id",
                                "team_position",
                                "rune_key"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_champion_rune_champion", columnList = "champion_id, team_position")
        }
)
public class ChampionRuneStat {

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

    @Column(name = "rune_key", nullable = false, length = 180)
    private String runeKey;

    @Column(name = "primary_style")
    private Integer primaryStyle;

    @Column(name = "sub_style")
    private Integer subStyle;

    @Column(name = "primary_perk_1")
    private Integer primaryPerk1;

    @Column(name = "primary_perk_2")
    private Integer primaryPerk2;

    @Column(name = "primary_perk_3")
    private Integer primaryPerk3;

    @Column(name = "primary_perk_4")
    private Integer primaryPerk4;

    @Column(name = "sub_perk_1")
    private Integer subPerk1;

    @Column(name = "sub_perk_2")
    private Integer subPerk2;

    @Column
    private Integer offense;

    @Column
    private Integer flex;

    @Column
    private Integer defense;

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
