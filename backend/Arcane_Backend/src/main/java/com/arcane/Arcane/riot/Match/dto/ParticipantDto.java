package com.arcane.Arcane.riot.Match.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.arcane.Arcane.riot.Match.dto.perks.PerksDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantDto {
    // 승리 여부
    @JsonProperty("win")
    private Boolean win;

    // 소환사 정보
    @JsonProperty("riotIdGameName")
    private String riotIdGameName;

    @JsonProperty("riotIdTagline")
    private String riotIdTagline;

    @JsonProperty("puuid")
    private String puuid;

    @JsonProperty("summonerId")
    private String summonerId;

    // 인게임 챔피언 정보
    @JsonProperty("championId")
    private Long championId;

    @JsonProperty("champLevel")
    private Integer champLevel;

    @JsonProperty("role")
    private String role;

    @JsonProperty("teamPosition")
    private String teamPosition;
    // KDA 정보
    private Float kda;

    @JsonProperty("kills")
    private Integer kills;

    @JsonProperty("deaths")
    private Integer deaths;

    @JsonProperty("assists")
    private Integer assists;

    // 아이템 정보
    @JsonProperty("item0")
    private Long item0;

    @JsonProperty("item1")
    private Long item1;

    @JsonProperty("item2")
    private Long item2;

    @JsonProperty("item3")
    private Long item3;

    @JsonProperty("item4")
    private Long item4;

    @JsonProperty("item5")
    private Long item5;

    @JsonProperty("item6")
    private Long item6;

    // 미니언 정보 두개 합치면 됨
    @JsonProperty("totalMinionsKilled")
    private Integer totalMinionsKilled;

    @JsonProperty("neutralMinionsKilled")
    private Integer neutralMinionsKilled;

    // 받은 피해량 및 딜량
    @JsonProperty("totalDamageDealtToChampions")
    private Long totalDamageDealtToChampions;

    @JsonProperty("totalDamageTaken")
    private Long totalDamageTaken;


    // 시야 점수 정보
    @JsonProperty("wardsKilled")
    private Integer wardKilled;

    @JsonProperty("wardsPlaced")
    private Integer wardPlaced;

    @JsonProperty("visionWardsBoughtInGame")
    private Integer visionWardsBoughtInGame;

    @JsonProperty("visionScore")
    private Integer visionScore;

    // 연속킬 정보
    @JsonProperty("doubleKills")
    private Integer doubleKills;

    @JsonProperty("tripleKills")
    private Integer tripleKills;

    @JsonProperty("quadraKills")
    private Integer quadraKills;

    @JsonProperty("pentaKills")
    private Integer pentaKills;

    @JsonProperty("perks")
    private PerksDto perks;

    @JsonProperty("summoner1Id")
    private Integer summoner1Id;

    @JsonProperty("summoner2Id")
    private Integer summoner2Id;

    @JsonProperty("summoner2Casts")
    private Integer summoner2Casts;

    @JsonProperty("summoner1Casts")
    private Integer summoner1Casts;

    @JsonProperty("spell1Casts")
    private Integer spell1Casts;
    @JsonProperty("spell2Casts")
    private Integer spell2Casts;
    @JsonProperty("spell3Casts")
    private Integer spell3Casts;
    @JsonProperty("spell4Casts")
    private Integer spell4Casts;

    // 지금은 분석 툴이 없으니 100~20 사이의 랜덤 점수 부여
    private Integer ourScore;
    private Integer teamScore;

    // challenges 필드를 Map으로 받아서 kda 값을 직접 꺼내는 방식
    @JsonProperty("challenges")
    private void unpackNested(java.util.Map<String,Object> challenges) {
        Object kdaValue = challenges.get("kda");
        if (kdaValue instanceof Number) {
            this.kda = ((Number) kdaValue).floatValue();
        }
    }
}
