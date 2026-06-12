package com.arcane.Arcane.riot.summoner.dto;

import com.arcane.Arcane.riot.summoner.domain.Summoner;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummonerKeywordResDto {
    private Long id;
    private String puuid;
    private String gameName;
    private String tagLine;
    private int icon;
    private int level;
    private String soloRank;
    private Integer soloRankLp;

    public static SummonerKeywordResDto of(Summoner summoner){
        int iconData = 29;
        if (summoner.getIconId() != null){
            iconData = summoner.getIconId();
        }
        int levelData = 0;
        if (summoner.getLevel() != null){
            levelData = summoner.getLevel();
        }
        return SummonerKeywordResDto.builder()
                .id(summoner.getId())
                .puuid(summoner.getPuuid())
                .gameName(summoner.getGameName())
                .tagLine(summoner.getTagLine())
                .soloRank(summoner.getSoloRankTier())
                .soloRankLp(summoner.getSoloRankLP())
                .icon(iconData)
                .level(levelData)
                .build();
    }
}
