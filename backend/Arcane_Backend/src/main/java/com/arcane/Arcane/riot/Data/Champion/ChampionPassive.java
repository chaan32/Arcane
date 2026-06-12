package com.arcane.Arcane.riot.Data.Champion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChampionPassive {

    @Column(name = "passive_name") // 컬럼명 충돌 방지
    private String name;

    @Column(name = "passive_description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "passive_image")
    private String imageFull; // "Xerath_Passive1.png"
}
