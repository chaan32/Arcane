package com.arcane.Arcane.web.Statistics.dto;

import com.arcane.Arcane.web.Statistics.domain.Champion.ChampionStatsByPosition;
import lombok.Data;

import java.util.List;

@Data
public class TierResponseDto {
    private String championName;
    private String championNameEn;
    private Integer tier;
    private Integer score;
    private Float pickRate;
    private Float winRate;

    private List<CounterChampionResDto> counterChampions;

    public TierResponseDto(ChampionStatsByPosition stats, List<CounterChampionResDto> counterChampions) {
        this.championName = stats.getChampion().getNameKo();
        this.championNameEn = stats.getChampion().getNameEn();
        this.tier = stats.getTier();
        this.score = stats.getScore();
        this.pickRate = stats.getPickRate();
        this.winRate = stats.getWinRate();
        this.counterChampions = counterChampions;
    }
}
