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
        name = "champion_option_stat",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_champion_option_stat",
                        columnNames = {
                                "snapshot_id",
                                "champion_id",
                                "queue_id",
                                "team_position",
                                "option_type",
                                "option_key"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_champion_option_champion",
                        columnList = "champion_id, team_position, option_type"
                ),
                @Index(name = "idx_champion_option_item", columnList = "item_id"),
                @Index(name = "idx_champion_option_spell", columnList = "spell1_id, spell2_id")
        }
)
/*
어떤 챔피언이 어떤 라인에서 어떤 스펠을 들었는가?
 */
public class ChampionOptionStat {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 20)
    private ChampionOptionType optionType;

    @Column(name = "option_key", nullable = false, length = 80)
    private String optionKey;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "spell1_id")
    private Integer spell1Id;

    @Column(name = "spell2_id")
    private Integer spell2Id;

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

