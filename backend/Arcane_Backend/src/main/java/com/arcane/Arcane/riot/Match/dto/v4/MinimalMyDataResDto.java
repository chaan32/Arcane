package com.arcane.Arcane.riot.Match.dto.v4;

import com.arcane.Arcane.riot.Data.Rune.dto.MatchRuneResDto;
import com.arcane.Arcane.riot.Match.dto.v3.MyDataResDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MinimalMyDataResDto {
    private String puuid;
    private Boolean win;
    private Long championId;
    private String championNameEn;
    private String championNameKo;
    private Integer champLevel;
    private String teamPosition;
    private Long item0, item1, item2, item3, item4, item5, item6;

    // 킬뎃 정보
    private Float kda;
    private Integer kills;
    private Integer deaths;
    private Integer assists;

    // 스펠, 스킬 정보
    private Integer spell1Casts;
    private Integer spell2Casts;
    private Integer spell3Casts;
    private Integer spell4Casts;
    private Integer summoner1Id;
    private Integer summoner1Casts;
    private Integer summoner2Id;
    private Integer summoner2Casts;

    // 추가사항
    private MatchRuneResDto rune;

    public static MinimalMyDataResDto of (MyDataResDto myDataResDto){
        return MinimalMyDataResDto.builder()
                .puuid(myDataResDto.getPuuid())
                .win(myDataResDto.getWin())
                .championId(myDataResDto.getChampionId())
                .championNameEn(myDataResDto.getChampionNameEn())
                .championNameKo(myDataResDto.getChampionNameKo())
                .champLevel(myDataResDto.getChampLevel())
                .teamPosition(myDataResDto.getTeamPosition())
                .item0(myDataResDto.getItem0())
                .item1(myDataResDto.getItem1())
                .item2(myDataResDto.getItem2())
                .item3(myDataResDto.getItem3())
                .item4(myDataResDto.getItem4())
                .item5(myDataResDto.getItem5())
                .item6(myDataResDto.getItem6())
                .kda(myDataResDto.getKda())
                .kills(myDataResDto.getKills())
                .deaths(myDataResDto.getDeaths())
                .assists(myDataResDto.getAssists())
                .summoner1Id(myDataResDto.getSummoner1Id())
                .summoner2Id(myDataResDto.getSummoner2Id())
                .rune(myDataResDto.getRune())
                .build();
    }
}
