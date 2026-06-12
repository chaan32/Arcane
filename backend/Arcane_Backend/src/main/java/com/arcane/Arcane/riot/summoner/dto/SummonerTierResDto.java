package com.arcane.Arcane.riot.summoner.dto;

import com.arcane.Arcane.riot.summoner.domain.Summoner;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class SummonerTierResDto {
    private Long id;
    private String gameName;
    private String tagLine;
    private String puuid;

    private RankDto soloRank;
    private RankDto flexRank;
    private LocalDateTime updateAt;

    public static SummonerTierResDto of(Summoner summoner){
        return SummonerTierResDto.builder()
                .id(summoner.getId())
                .gameName(summoner.getGameName())
                .tagLine(summoner.getTagLine())
                .puuid(summoner.getPuuid())
                .soloRank(RankDto.of(
                        summoner.getSoloRankTier(),
                        summoner.getSoloRankLP(),
                        summoner.getSoloRankWin(),
                        summoner.getSoloRankDefeat()))
                .flexRank(RankDto.of(
                        summoner.getFlexRankTier(),
                        summoner.getFlexRankLP(),
                        summoner.getFlexRankWin(),
                        summoner.getFlexRankDefeat()))
                .updateAt(summoner.getUpdatedAt())
                .build();
    }
}
