package com.arcane.Arcane.riot.Match.dto.v2;

import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import com.arcane.Arcane.riot.Match.dto.perks.PerksDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayerDataDto {

    // 유저 정보
    private String puuid;
    private String gameName;

    // 인게임 정보
    private Boolean win;
    private Long championId;
    private Integer champLevel;
    private String teamPosition;
    private Long item0, item1, item2, item3, item4, item5, item6;

    // 킬뎃 정보
    private Float kda;
    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Integer totalMinionKills;


    // 연속킬 정보
    private Integer doubleKills;
    private Integer tripleKills;
    private Integer quadraKills;
    private Integer pentaKills;

    private Integer ourScore;
    private Integer teamLuckScore;

    private PerksDto perks;


    public static PlayerDataDto of(MatchParticipant matchParticipant) {
        return PlayerDataDto.builder()
                .puuid(matchParticipant.getSummoner().getPuuid())
                .gameName(matchParticipant.getSummoner().getTrimmedGameName())
                .win(matchParticipant.getWin())
                .championId(matchParticipant.getChampionId())
                .champLevel(matchParticipant.getChampLevel())
                .teamPosition(matchParticipant.getTeamPosition())
                .item0(matchParticipant.getItem0())
                .item1(matchParticipant.getItem1())
                .item2(matchParticipant.getItem2())
                .item3(matchParticipant.getItem3())
                .item4(matchParticipant.getItem4())
                .item5(matchParticipant.getItem5())
                .item6(matchParticipant.getItem6())
                .kda(matchParticipant.getKda())
                .kills(matchParticipant.getKills())
                .deaths(matchParticipant.getDeaths())
                .assists(matchParticipant.getAssists())
                .totalMinionKills(matchParticipant.getTotalMinionKills())
                .doubleKills(matchParticipant.getDoubleKills())
                .tripleKills(matchParticipant.getTripleKills())
                .quadraKills(matchParticipant.getQuadraKills())
                .pentaKills(matchParticipant.getPentaKills())
                .ourScore(matchParticipant.getOurScore())
                .teamLuckScore(matchParticipant.getTeamLuckScore())
                .perks(matchParticipant.getPerks())
                .build();
    }

}
