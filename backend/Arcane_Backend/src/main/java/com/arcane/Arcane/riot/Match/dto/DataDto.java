package com.arcane.Arcane.riot.Match.dto;

import com.arcane.Arcane.riot.Match.dto.perks.PerksDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataDto {

    private String puuid;

    private Boolean win;
    private Long championId;
    private Integer champLevel;
    private String teamPosition;
    private Long item0;
    private Long item1;
    private Long item2;
    private Long item3;
    private Long item4;
    private Long item5;
    private Long item6;

    private Float kda;

    private Integer kills;
    private Integer deaths;
    private Integer assists;

    private Integer totalMinionKills;
    private Integer leaguePoints;

    private Integer doubleKills;
    private Integer tripleKills;
    private Integer quadraKills;
    private Integer pentaKills;

    // 지금은 분석 툴이 없으니 100~20 사이의 랜덤 점수 부여 (자체 분석 점수, 팀운 점수)
    private Integer ourScore;
    private Integer teamScore;

    private PerksDto perks;

    public static DataDto setDataDto(ParticipantDto participantDto) {
        return DataDto.builder()
                .puuid(participantDto.getPuuid())
                .win(participantDto.getWin())
                .championId(participantDto.getChampionId())
                .champLevel(participantDto.getChampLevel())
                .teamPosition(participantDto.getTeamPosition())
                .item0(participantDto.getItem0())
                .item1(participantDto.getItem1())
                .item2(participantDto.getItem2())
                .item3(participantDto.getItem3())
                .item4(participantDto.getItem4())
                .item5(participantDto.getItem5())
                .item6(participantDto.getItem6())
                .kda(participantDto.getKda())
                .kills(participantDto.getKills())
                .deaths(participantDto.getDeaths())
                .assists(participantDto.getAssists())
                .totalMinionKills(participantDto.getTotalMinionsKilled() + participantDto.getNeutralMinionsKilled())
                .doubleKills(participantDto.getDoubleKills())
                .tripleKills(participantDto.getTripleKills())
                .quadraKills(participantDto.getQuadraKills())
                .pentaKills(participantDto.getPentaKills())
                .perks(participantDto.getPerks())
                .ourScore(participantDto.getOurScore())
                .build();
    }
}
