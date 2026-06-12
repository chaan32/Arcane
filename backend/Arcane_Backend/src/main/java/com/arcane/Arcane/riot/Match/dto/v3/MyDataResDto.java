package com.arcane.Arcane.riot.Match.dto.v3;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Data.Rune.dto.MatchRuneResDto;
import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import com.arcane.Arcane.riot.Match.dto.perks.StyleDto;
import com.arcane.Arcane.riot.summoner.domain.Summoner;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MyDataResDto {

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

    private Long totalDamageDealtToChampions;
    private Long totalDamageTaken;

    // 미니언 처치 정보
    private Integer totalMinionKills;

    // 연속킬 정보
    private Integer doubleKills;
    private Integer tripleKills;
    private Integer quadraKills;
    private Integer pentaKills;
    private Integer teamLuckScore;
    private Integer ourScore;

    private Integer primaryStyle;
    private Integer subStyle;

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

    // 시야 점수 정보
    private Integer wardKilled;
    private Integer wardPlaced;
    private Integer visionWardsBoughtInGame;
    private Integer visionScore;


    public static MyDataResDto of(MatchParticipant participants, Champion champion) {
        Summoner summoner = participants.getSummoner();
        return MyDataResDto.builder()
                .puuid(summoner.getPuuid())
                .win(participants.getWin())
                .championId(participants.getChampionId())
                .champLevel(participants.getChampLevel())
                .championNameEn(champion.getNameEn())
                .championNameKo(champion.getNameKo())
                .teamPosition(participants.getTeamPosition())
                .item0(participants.getItem0()).item1(participants.getItem1()).item2(participants.getItem2())
                .item3(participants.getItem3()).item4(participants.getItem4()).item5(participants.getItem5())
                .item6(participants.getItem6())

                .kda(participants.getKda())
                .kills(participants.getKills())
                .deaths(participants.getDeaths())
                .assists(participants.getAssists())
                .totalDamageDealtToChampions(participants.getTotalDamageDealtToChampions())
                .totalDamageTaken(participants.getTotalDamageTaken())
                .totalMinionKills(participants.getTotalMinionKills())
                .doubleKills(participants.getDoubleKills())
                .tripleKills(participants.getTripleKills())
                .quadraKills(participants.getQuadraKills())
                .pentaKills(participants.getPentaKills())
                .teamLuckScore(participants.getTeamLuckScore())
                .ourScore(participants.getOurScore())
                .summoner1Casts(participants.getSummoner1Casts())
                .summoner2Casts(participants.getSummoner2Casts())
                .summoner1Id(participants.getSummoner1Id())
                .summoner2Id(participants.getSummoner2Id())
                .spell1Casts(participants.getSpell1Casts())
                .spell2Casts(participants.getSpell2Casts())
                .spell3Casts(participants.getSpell3Casts())
                .spell4Casts(participants.getSpell4Casts())
                .rune(MatchRuneResDto.of(participants.getPerks()))
                .wardKilled(participants.getWardKilled())
                .wardPlaced(participants.getWardPlaced())
                .visionWardsBoughtInGame(participants.getVisionWardsBoughtInGame())
                .visionScore(participants.getVisionScore())
                .build();
    }

    public void setPerks(MatchParticipant participants) {
        Summoner summoner = participants.getSummoner();
        List<StyleDto> styles = participants.getPerks().getStyles();
        for (StyleDto style : styles) {
            if (style.getDescription().equals("primaryStyle")) {
                this.primaryStyle = style.getStyle();
            }
            else if (style.getDescription().equals("subStyle")) {
                this.subStyle = style.getStyle();
            }
        }
    }
}
