package com.arcane.Arcane.riot.RiotInform.dto.Ranker;

import com.arcane.Arcane.riot.summoner.domain.Summoner;
import lombok.Builder;
import lombok.Data;

@Data
public class ChallengersResDto {
    private String puuid;
    private String gameName;
    private String tagLine;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
    private String rank;
    private Double winRate;

    @Builder
    public ChallengersResDto(ChallengerDto dto, Summoner summoner) {
        this.gameName = summoner.getGameName();
        this.tagLine = summoner.getTagLine();
        this.puuid = summoner.getPuuid();

        this.rank = dto.getRank();
        this.wins = dto.getWins();
        this.losses = dto.getLosses();
        this.leaguePoints = dto.getLeaguePoints();

        this.winRate = ((double) wins / (losses + wins)) * 100;
    }
}
