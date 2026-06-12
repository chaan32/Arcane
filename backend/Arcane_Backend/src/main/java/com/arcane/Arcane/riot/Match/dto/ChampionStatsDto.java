package com.arcane.Arcane.riot.Match.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChampionStatsDto {
    private long championId;
    private String championName;
    private int gamesPlayed = 0;
    private int wins = 0;
    private int kills = 0;
    private int deaths = 0;
    private int assists = 0;

    public ChampionStatsDto(long championId) {
        this.championId = championId;
    }

    public void incrementGamesPlayed() { this.gamesPlayed++; }
    public void incrementWins() { this.wins++; }
    public void addKda(int kills, int deaths, int assists) {
        this.kills += kills;
        this.deaths += deaths;
        this.assists += assists;
    }

    // 최종 승률 계산
    public double getWinRate() {
        if (gamesPlayed == 0) return 0.0;
        return ((double) wins / gamesPlayed) * 100.0;
    }

    // 최종 KDA 계산
    public double getKda() {
        if (deaths == 0) return (double) (kills + assists); // Perfect KDA
        return (double) (kills + assists) / deaths;
    }
}
