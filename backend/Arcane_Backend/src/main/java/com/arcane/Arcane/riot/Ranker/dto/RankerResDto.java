package com.arcane.Arcane.riot.Ranker.dto;

import com.arcane.Arcane.riot.summoner.domain.Summoner;
import lombok.Builder;
import lombok.Getter;

// 챌, 그마, 마 모두 포괄적으로 사용할 수 있는 ResDTO
@Getter
@Builder
public class RankerResDto {
    private String puuid;
    private String gameName;
    private String tagLine;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
    private String rank;
    private Double winRate;

    // 이게 뭔지 정확하지는 않음
    private Boolean veteran;
    private Boolean inactive;
    private Boolean freshBlood;
    private Boolean hotStreak;

    private Integer profileIconId;
    private Integer summonerLevel;


    public static RankerResDto of (RiotRankerDto riotRankerDto, Summoner summoner) {
        return RankerResDto.builder()
                .veteran(riotRankerDto.getVeteran())
                .inactive(riotRankerDto.getInactive())
                .freshBlood(riotRankerDto.getFreshBlood())
                .hotStreak(riotRankerDto.getHotStreak())
                .puuid(summoner.getPuuid())
                .gameName(summoner.getGameName())
                .tagLine(summoner.getTagLine())
                .leaguePoints(summoner.getSoloRankLP())
                .wins(summoner.getSoloRankWin())
                .losses(summoner.getSoloRankDefeat())
                .rank(summoner.getSoloRankTier())
                .winRate(((double) summoner.getSoloRankWin() / (summoner.getSoloRankDefeat() + summoner.getSoloRankWin())) * 100)
                .build();
    }
    public static RankerResDto from (RedisRankerDto redisRankerDto){
        return RankerResDto.builder()
                .puuid(redisRankerDto.getPuuid())
                .gameName(redisRankerDto.getGameName())
                .tagLine(redisRankerDto.getTagLine())
                .leaguePoints(redisRankerDto.getLp())
                .wins(redisRankerDto.getWins())
                .losses(redisRankerDto.getLosses())
                .rank(redisRankerDto.getTier())
                .winRate(((double) redisRankerDto.getWins() / (redisRankerDto.getLosses() + redisRankerDto.getWins()) * 100))
                .profileIconId(redisRankerDto.getIcon())
                .summonerLevel(redisRankerDto.getLevel())
                .build();
    }
}
