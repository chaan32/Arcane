package com.arcane.Arcane.riot.Data;


import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.riot.AdditionalData.service.AdditionalDataService;
import com.arcane.Arcane.riot.Data.Champion.ChampionService;
import com.arcane.Arcane.riot.Data.Rune.RuneService;
import com.arcane.Arcane.riot.Data.SummonerSpell.SummonerSpellService;
import com.arcane.Arcane.riot.Ranker.Sheduler.RankerScheduler;
import com.arcane.Arcane.riot.Ranker.service.RankerService;
import com.arcane.Arcane.web.Statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequiredArgsConstructor
@RequestMapping("/import")
public class DataController {
    private final ChampionService championService;
    private final RuneService runeService;
    private final SummonerSpellService summonerSpellService;
    private final AdditionalDataService additionalService;
    private final RankerService rankerService;
    private final RankerScheduler rankerScheduler;
    private final StatisticsService statisticsService;

    @Operation(summary = "챔피언 데이터 임포트", description = "Riot API로부터 최신 챔피언 데이터를 가져와 데이터베이스에 저장합니다.")
    @GetMapping("/champion")
    public ResponseEntity<String> importChampions() {
        try {
            championService.importChampions();
            return ResponseEntity.ok("챔피언 데이터 저장 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("오류 발생: " + e.getMessage());
        }
    }

    @Operation(summary = "룬 데이터 임포트", description = "Riot API로부터 최신 룬 데이터를 가져와 데이터베이스에 저장합니다.")
    @GetMapping("/rune")
    public ResponseEntity<String> importRunes() {
        try {
            runeService.importRunes();
            return ResponseEntity.ok("룬 데이터 저장 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("룬 데이터 저장 실패: " + e.getMessage());
        }
    }
    @Operation(summary = "소환사 주문 데이터 임포트", description = "Riot API로부터 최신 소환사 주문(스펠) 데이터를 가져와 데이터베이스에 저장합니다.")
    @GetMapping("/spell")
    public ResponseEntity<String> importSpells(){
        try{
            summonerSpellService.importSummonerSpells();
            return ResponseEntity.ok("소환사 주문 데이터 저장 완료");
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("소환사 주문 데이터 저장 실패: " + e.getMessage());
        }
    }
    @Operation(summary = "랭커 프로필 다운로드", description = "상위 랭커들의 아이콘 및 레벨 정보를 업데이트합니다.")
    @GetMapping("/iconAndLevel")
    public String divisionRequest() throws CannotFoundSummoner, InterruptedException {
        additionalService.downloadRankersProfile();
        return "good";
    }

    @Operation(summary = "챌린저 랭커 데이터 저장", description = "챌린저 티어 플레이어들의 정보를 수집하여 DB에 저장합니다.")
    @GetMapping("/store-rankers/challenger")
    public ResponseEntity<String> storeRankers() throws InterruptedException {
        // 단순 디비에 저장하는 용도
        rankerService.saveChallenger();

        return ResponseEntity.ok("저장 완료");
    }
    @Operation(summary = "그랜드마스터 랭커 데이터 저장", description = "그랜드마스터 티어 플레이어들의 정보를 수집하여 DB에 저장합니다.")
    @GetMapping("/store-rankers/grandmaster")
    public ResponseEntity<String> storeRankersG() throws InterruptedException {
        // 단순 디비에 저장하는 용도
        rankerService.saveGrandMasters();

        return ResponseEntity.ok("저장 완료");
    }
    @Operation(summary = "마스터 랭커 데이터 저장", description = "마스터 티어 플레이어들의 정보를 수집하여 DB에 저장합니다.")
    @GetMapping("/store-rankers/master")
    public ResponseEntity<String> storeRankersM() throws InterruptedException {
        // 단순 디비에 저장하는 용도
        rankerService.saveMasters();

        return ResponseEntity.ok("저장 완료");
    }

    @Operation(summary = "랭킹을 직접 업데이트", description = "PUUID를 통해 직접 랭킹 정보를 최신화합니다.")
    @GetMapping("/update-ranker/directly")
    public String updateRakingDirect(){
        String jobId = rankerScheduler.runRankingUpdateNow();
        return jobId == null ? "ranking update request skipped" : "ranking update requested. jobId=" + jobId;
    }

    @Operation(summary = "통계 데이터 생성 (Mock)", description = "분석을 위한 전 포지션별 챔피언 통계 데이터를 생성합니다.")
    @GetMapping("/set-statistics/mock-data")
    public String setMock(){
        statisticsService.createAllPositionStatistics();
        return "success";
    }

    @Operation(summary = "직접 패치 버전 가져오기", description = "실시간으로 현재 패치 버전을 가져옵니다.")
    @GetMapping("/get/current-patchVersion/direct")
    public String getPatchVersion(){
        return additionalService.getCurrentPatchVersion();
    }

    @Operation(summary = "패치 버전 가져오기", description = "레디스에 저장된 패치 버전을 가져옵니다.")
    @GetMapping("/get/current-patchVersion")
    public String getPatchVersionFromRedis(){
        return additionalService.getCurrentPatchVersionFromRedis();
    }
}
