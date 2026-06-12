package com.arcane.Arcane.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OurParticipantDto {
    // 승리 여부
    @JsonProperty("win")
    private Boolean win;

    // 소환사 정보
    @JsonProperty("riotIdGameName")
    private String riotIdGameName; // 라이엇 게임 닉네임

    @JsonProperty("riotIdTagline")
    private String riotIdTagline; // 라이엇 태그

    @JsonProperty("puuid")
    private String puuid; // 고유 유저 ID

    @JsonProperty("summonerId")
    private String summonerId; // 소환사 ID (암호화)

    // 인게임 챔피언 정보
    @JsonProperty("championId")
    private Long championId; // 챔피언 ID

    @JsonProperty("champLevel")
    private Integer champLevel; // 최종 챔피언 레벨

    @JsonProperty("role")
    private String role; // 시스템 판정 역할군

    @JsonProperty("teamPosition")
    private String teamPosition; // 배정 포지션

    // KDA 정보
    private Float kda; // 계산된 KDA 수치

    @JsonProperty("kills")
    private Integer kills; // 총 킬

    @JsonProperty("deaths")
    private Integer deaths; // 총 데스

    @JsonProperty("assists")
    private Integer assists; // 총 어시스트

    @JsonProperty("firstBloodKill")
    private Boolean firstBloodKill; // 퍼스트 블러드 직접 달성 여부

    @JsonProperty("firstBloodAssist")
    private Boolean firstBloodAssist; // 퍼스트 블러드 관여 여부

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
    private Long item6; // 장신구 슬롯

    // 미니언 및 정글 정보
    @JsonProperty("totalMinionsKilled")
    private Integer totalMinionsKilled; // 처치한 라인 미니언 (CS)

    @JsonProperty("neutralMinionsKilled")
    private Integer neutralMinionsKilled; // 처치한 중립 몬스터 (정글 CS)

    @JsonProperty("totalEnemyJungleMinionsKilled")
    private Integer totalEnemyJungleMinionsKilled; // 카운터 정글 미니언 처치 수

    // 피해량 및 방어 정보
    @JsonProperty("totalDamageDealt")
    private Long totalDamageDealt; // 가한 총 피해량 (오브젝트 포함)

    @JsonProperty("totalDamageDealtToChampions")
    private Long totalDamageDealtToChampions; // 챔피언에게 가한 총 피해량

    @JsonProperty("totalDamageTaken")
    private Long totalDamageTaken; // 받은 총 피해량

    @JsonProperty("totalDamageShieldedOnTeammates")
    private Long totalDamageShieldedOnTeammates; // 아군에게 부여한 보호막 총량

    @JsonProperty("damageSelfMitigated")
    private Long damageSelfMitigated; // 스스로 경감시킨 피해량

    // 골드 및 시간 지표
    @JsonProperty("goldEarned")
    private Integer goldEarned; // 총 획득 골드

    @JsonProperty("longestTimeSpentLiving")
    private Integer longestTimeSpentLiving; // 최장 생존 시간 (초)

    @JsonProperty("totalTimeSpentDead")
    private Integer totalTimeSpentDead; // 사망 대기 총 시간 (초)

    // 오브젝트 및 기타 지표
    @JsonProperty("damageDealtToObjectives")
    private Long damageDealtToObjectives; // 오브젝트 가한 피해량

    @JsonProperty("dragonKills")
    private Integer dragonKills; // 드래곤 직접 처치 수 (막타)

    @JsonProperty("firstTowerKill")
    private Boolean firstTowerKill; // 첫 포탑 직접 파괴 여부

    @JsonProperty("objectivesStolen")
    private Integer objectivesStolen; // 오브젝트 스틸 수

    @JsonProperty("objectivesStolenAssists")
    private Integer objectivesStolenAssists; // 오브젝트 스틸 도움 수

    @JsonProperty("timeCCingOthers")
    private Integer timeCCingOthers; // 적에게 가한 군중 제어(CC) 시간

    // 시야 관련 정보
    @JsonProperty("wardsKilled")
    private Integer wardKilled; // 와드 제거 수

    @JsonProperty("wardsPlaced")
    private Integer wardPlaced; // 와드 설치 수

    @JsonProperty("detectorWardsPlaced")
    private Integer detectorWardsPlaced; // 제어 와드 설치 수

    @JsonProperty("visionWardsBoughtInGame")
    private Integer visionWardsBoughtInGame; // 상점에서 구매한 제어 와드 수

    @JsonProperty("visionScore")
    private Integer visionScore; // 총 시야 점수

    // 멀티킬 기록
    @JsonProperty("doubleKills")
    private Integer doubleKills;
    @JsonProperty("tripleKills")
    private Integer tripleKills;
    @JsonProperty("quadraKills")
    private Integer quadraKills;
    @JsonProperty("pentaKills")
    private Integer pentaKills;

    // 룬 및 스펠 정보
//    @JsonProperty("perks")
//    private PerksDto perks; // 장착한 룬(특성) 정보

    @JsonProperty("summoner1Id")
    private Integer summoner1Id; // 첫 번째 스펠 ID
    @JsonProperty("summoner2Id")
    private Integer summoner2Id; // 두 번째 스펠 ID

    @JsonProperty("summoner1Casts")
    private Integer summoner1Casts; // 첫 번째 스펠 사용 횟수
    @JsonProperty("summoner2Casts")
    private Integer summoner2Casts; // 두 번째 스펠 사용 횟수

    // 스킬 사용 횟수 (Q, W, E, R)
    @JsonProperty("spell1Casts")
    private Integer spell1Casts;
    @JsonProperty("spell2Casts")
    private Integer spell2Casts;
    @JsonProperty("spell3Casts")
    private Integer spell3Casts;
    @JsonProperty("spell4Casts")
    private Integer spell4Casts;

    // 상세 챌린지 지표
    @JsonProperty("challenges")
    private ChallengesDto challenges;

    // 정답과 같은 점수
    private Integer performanceScore;
}