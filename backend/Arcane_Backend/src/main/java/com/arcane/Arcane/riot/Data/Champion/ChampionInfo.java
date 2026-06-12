package com.arcane.Arcane.riot.Data.Champion;

import jakarta.persistence.Embeddable; // 엔티티 안에 포함될 경우
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class ChampionInfo {

    private Byte attack;
    private Byte defense;
    private Byte magic;
    private Byte difficulty;


}