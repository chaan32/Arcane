package com.arcane.Arcane.riot.Data.Champion;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "champion_spell")
public class ChampionSpell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB용 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_key") // FK
    private Champion champion;

    private String spellId;      // "XerathArcanopulseChargeUp"
    private String name;         // "비전 파동"

    @Column(columnDefinition = "TEXT")
    private String description;

    private String spellKey;     // Q, W, E, R, P (서비스에서 넣어줘야 함)
    private String imageFull;    // 이미지 파일명
    private String cooldownBurn; // 쿨타임
    private String costBurn;     // 소모값
    private String version;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
