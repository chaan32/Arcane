package com.arcane.Arcane.web.Statistics.dto.detail;

import com.arcane.Arcane.web.Statistics.domain.Champion.ChampionStatsByPosition;
import lombok.Data;

@Data
public class ChampionDetailChampInfo {

    private String championName;
    private String championNameEn;

    // 티어 -> 몇 티어인지?
    private Integer tier;
    // 승률
    private Float winRate;
    // 픽률
    private Float pickRate;
    // 벤률
    private Float banRate;
    // 게임 수
    private Integer gameCount;
    // 비율
    private Float percent;


    public ChampionDetailChampInfo(ChampionStatsByPosition stats) {
        this.tier = stats.getTier();
        this.winRate = stats.getWinRate();
        this.pickRate = stats.getPickRate();
        this.banRate = stats.getBanRate();
        this.gameCount = stats.getTotalGamesPlayed();
        this.championNameEn = stats.getChampion().getNameEn();
        this.championName = stats.getChampion().getNameKo();
    }
}
