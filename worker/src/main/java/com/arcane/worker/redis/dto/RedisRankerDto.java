package com.arcane.worker.redis.dto;

import com.arcane.worker.summoner.entity.SummonerEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisRankerDto {
    private String puuid;
    private String gameName;
    private String tagLine;
    private String tier;
    private Integer lp;
    private Integer wins;
    private Integer losses;

    //  레벨이랑 아이콘은 잠정 중단
    private Integer level;
    private Integer icon;



    public static RedisRankerDto of(SummonerEntity summoner) {
        return RedisRankerDto.builder()
                .puuid(summoner.getPuuid())
                .gameName(summoner.getGameName())
                .tagLine(summoner.getTagLine())
                .tier(summoner.getSoloRankTier())
                .lp(summoner.getSoloRankLP())
                .wins(summoner.getSoloRankWin())
                .losses(summoner.getSoloRankDefeat())
                .icon(summoner.getIconId())
                .level(summoner.getLevel())
                .build();
    }
}
