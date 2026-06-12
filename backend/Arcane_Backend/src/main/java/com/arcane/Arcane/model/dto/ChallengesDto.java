package com.arcane.Arcane.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChallengesDto {

    // 환경 및 소모품 회복
    @JsonProperty("HealFromMapSources")
    private Double healFromMapSources; // 꿀열매를 통한 회복량

    // 시야 및 와드 컨트롤
    @JsonProperty("controlWardTimeCoverageInRiverOrEnemyHalf")
    private Double controlWardTimeCoverageInRiverOrEnemyHalf; // 강가/적진 제어 와드 유지 시간 비중

    @JsonProperty("controlWardsPlaced")
    private Integer controlWardsPlaced; // 제어 와드 설치 수

    @JsonProperty("visionScoreAdvantageLaneOpponent")
    private Double visionScoreAdvantageLaneOpponent; // 맞라인 상대 대비 시야 점수 우위 비율

    @JsonProperty("visionScorePerMinute")
    private Double visionScorePerMinute; // 분당 시야 점수

    @JsonProperty("wardTakedowns")
    private Integer wardTakedowns; // 와드 제거 횟수

    @JsonProperty("wardTakedownsBefore20M")
    private Integer wardTakedownsBefore20M; // 20분 전 와드 제거 횟수

    // 팀 내 기여도 (피해 비중)
    @JsonProperty("damagePerMinute")
    private Double damagePerMinute; // 분당 가한 피해량

    @JsonProperty("damageTakenOnTeamPercentage")
    private Double damageTakenOnTeamPercentage; // 팀 내 받은 피해 비중

    @JsonProperty("teamDamagePercentage")
    private Double teamDamagePercentage; // 팀 내 가한 피해 비중

    // 라인전 및 킬 관련 지표
    @JsonProperty("maxCsAdvantageOnLaneOpponent")
    private Double maxCsAdvantageOnLaneOpponent; // 상대 라이너 대비 최대 CS 차이

    @JsonProperty("maxKillDeficit")
    private Integer maxKillDeficit; // 경기 중 발생한 최대 킬 열세 차이

    @JsonProperty("mejaisFullStackInTime")
    private Integer mejaisFullStackInTime; // 20분 전 메자이 풀스택 달성 여부(또는 시간)

    @JsonProperty("multikillsAfterAggressiveFlash")
    private Integer multikillsAfterAggressiveFlash; // 앞점멸 사용 후 멀티킬 횟수

    @JsonProperty("quickSoloKills")
    private Integer quickSoloKills; // 암살 솔로킬 수

    @JsonProperty("soloKills")
    private Integer soloKills; // 총 솔로킬 횟수

    @JsonProperty("takedownsAfterGainingLevelAdvantage")
    private Integer takedownsAfterGainingLevelAdvantage; // 레벨 우위 점한 직후 기록한 킬 관여 수

    @JsonProperty("takedownsFirstXMinutes")
    private Integer takedownsFirstXMinutes; // 10분 이내 기록한 킬 관여 수

    // 생존 및 피지컬 지표
    @JsonProperty("quickCleanse")
    private Integer quickCleanse; // CC기 걸린 후 빠른 정화 사용 횟수

    @JsonProperty("skillshotsDodged")
    private Integer skillshotsDodged; // 논타겟 스킬 피한 횟수

    @JsonProperty("skillshotsHit")
    private Integer skillshotsHit; // 논타겟 스킬 적중 횟수

    @JsonProperty("saveAllyFromDeath")
    private Integer saveAllyFromDeath; // 죽을 위기의 아군 구출 횟수

    @JsonProperty("survivedThreeImmobilizesInFight")
    private Integer survivedThreeImmobilizesInFight; // 한 교전에서 CC 3번 맞고 생존한 횟수

    // 타워 및 오브젝트 운영
    @JsonProperty("perfectDragonSoulsTaken")
    private Integer perfectDragonSoulsTaken; // 상대에게 용을 하나도 안 주고 영혼 획득한 횟수

    @JsonProperty("quickFirstTurret")
    private Integer quickFirstTurret; // 매우 이른 시간의 첫 포탑 파괴 기여

    @JsonProperty("soloTurretsLategame")
    private Integer soloTurretsLategame; // 게임 후반 혼자 포탑을 파괴한 수

    @JsonProperty("takedownOnFirstTurret")
    private Integer takedownOnFirstTurret; // 첫 포탑 파괴 관여

    @JsonProperty("teamBaronKills")
    private Integer teamBaronKills; // 팀 전체 바론 처치 수

    @JsonProperty("teamElderDragonKills")
    private Integer teamElderDragonKills; // 팀 전체 장로 드래곤 처치 수

    @JsonProperty("teamRiftHeraldKills")
    private Integer teamRiftHeraldKills; // 팀 전체 전령 처치 수

    @JsonProperty("turretPlatesTaken")
    private Integer turretPlatesTaken; // 획득한 포탑 방패 수

    @JsonProperty("turretTakedowns")
    private Integer turretTakedowns; // 포탑 파괴 관여 총 횟수

    @JsonProperty("turretsTakenWithRiftHerald")
    private Integer turretsTakenWithRiftHerald; // 전령을 활용해 파괴한 포탑 수

    // 정글 및 몬스터 컨트롤
    @JsonProperty("moreEnemyJungleThanOpponent")
    private Double moreEnemyJungleThanOpponent; // 상대 정글러보다 카운터 정글을 더 많이 한 비율

    @JsonProperty("scuttleCrabKills")
    private Integer scuttleCrabKills; // 바위게 처치 횟수

    @JsonProperty("soloBaronKills")
    private Integer soloBaronKills; // 바론 솔로 처치 횟수

    @JsonProperty("voidMonsterKill")
    private Integer voidMonsterKill; // 공허 유충 처치 수
}