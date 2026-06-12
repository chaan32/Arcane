package com.arcane.Arcane.riot.Data.SummonerSpell;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmmrSpell {

    @Id
    private Long spellId; // 4 = 점멸

    private String name;  // 점멸

    @Column(length = 1000)
    private String description;

    private String imageFull; // SummonerFlash.png
    private String version;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
