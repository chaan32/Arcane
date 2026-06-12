package com.arcane.Arcane.web.Statistics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Json으로 변환 시 null인 필드는 제외하는 어노테이션 (깔끔한 응답을 위함)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChampionSeasonStatisticsDto {
    private long championId;
    private String championName;
    private int gamesPlayed = 0;
    private int wins = 0;
    private int losses = 0;
    private int kills = 0;
    private int deaths = 0;
    private int assists = 0;
    private long totalDamageDealtToChampions = 0;
    private long totalGameDurationInSeconds = 0;

    public ChampionSeasonStatisticsDto(long championId) {
        this.championId = championId;
    }

    public void addMatchResult(boolean isWin, int kills, int deaths, int assists, long damage, long duration) {
        this.gamesPlayed++;
        if (isWin) {
            this.wins++;
        } else {
            this.losses++;
        }
        this.kills += kills;
        this.deaths += deaths;
        this.assists += assists;
        this.totalDamageDealtToChampions += damage;
        this.totalGameDurationInSeconds += duration;
    }

    // --- 최종 통계를 위한 계산 필드 ---

    public double getWinRate() {
        if (gamesPlayed == 0) return 0.0;
        return ((double) wins / gamesPlayed) * 100.0;
    }

    public double getKda() {
        if (deaths == 0) return (double) (kills + assists);
        return (double) (kills + assists) / deaths;
    }

    public double getAverageDpm() {
        if (totalGameDurationInSeconds == 0) return 0.0;
        return totalDamageDealtToChampions / (totalGameDurationInSeconds / 60.0);
    }
}
