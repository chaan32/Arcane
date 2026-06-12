package com.arcane.Arcane.riot.Data.Rune.dto;

import com.arcane.Arcane.riot.Match.dto.perks.StatPerksDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchStatRuneResDto {
    private int defense;
    private int flex;
    private int offense;

    public static MatchStatRuneResDto of (StatPerksDto statPerks){
        return MatchStatRuneResDto.builder()
                .flex(statPerks.getFlex())
                .offense(statPerks.getOffense())
                .defense(statPerks.getDefense())
                .build();
    }
}
