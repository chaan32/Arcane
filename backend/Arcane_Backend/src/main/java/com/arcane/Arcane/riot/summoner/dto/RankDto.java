package com.arcane.Arcane.riot.summoner.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RankDto {
    private String rankTier;
    private Integer rankLP;
    private Integer rankWin;
    private Integer rankDefeat;

    public static RankDto of(String rankTier, Integer rankLP, Integer rankWin, Integer rankDefeat) {
        return RankDto.builder()
                .rankDefeat(rankDefeat)
                .rankWin(rankWin)
                .rankTier(rankTier)
                .rankLP(rankLP)
                .build();
    }
}
