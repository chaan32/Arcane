package com.arcane.Arcane.riot.summoner.controller.v1;

import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.riot.Match.dto.v3.MatchInfoResDto;
import com.arcane.Arcane.riot.Match.service.MatchService;
import com.arcane.Arcane.riot.RiotInform.dto.MasteryDto;
import com.arcane.Arcane.riot.RiotInform.dto.ProfileResDto;
import com.arcane.Arcane.riot.summoner.dto.SummonerKeywordResDto;
import com.arcane.Arcane.riot.summoner.dto.SummonerSearchDto;
import com.arcane.Arcane.riot.summoner.dto.SummonerTierResDto;
import com.arcane.Arcane.riot.summoner.service.SummonerSearchService;
import com.arcane.Arcane.riot.summoner.service.SummonerService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Queue;

@RestController
@RequestMapping("/api/v1/summoner")
@Slf4j
@RequiredArgsConstructor
public class SummonerController {
    private final SummonerService summonerService;
    private final SummonerSearchService summonerSearchService;
    private final MatchService matchService;

    /** gameName과 tagLine을 통해서 티어 정보를 검색하는 API이다.
     *
     * @param gameName
     * @param tagLine
     * @return Summoner
     * @throws CannotFoundSummoner
     */
    @Operation(summary = "소환사 티어 정보 조회 (진짜 씀)", description = "게임 닉네임과 태그라인을 통해 해당 소환사의 현재 티어 정보를 조회합니다.")
    @GetMapping("/tier")
    public ResponseEntity<SummonerTierResDto> searchSummonerTierByGameNameAndTagLine(@RequestParam String gameName, @RequestParam String tagLine, @RequestParam Boolean refresh) throws CannotFoundSummoner {
        SummonerTierResDto summonerTierInfo = summonerService.getSummonerTierInfo(gameName, tagLine, refresh);
        return ResponseEntity.ok(summonerTierInfo);
    }



    /** gameName과 tagLine을 통해서 숙련도 정보를 검색하는 API이다. -> 리신 숙련도 15만점 등
     *
     * @param gameName
     * @param tagLine
     * @return
     * @throws CannotFoundSummoner
     */
    @Operation(summary = "소환사 챔피언 숙련도 조회 (진짜 씀)", description = "소환사가 보유한 챔피언들의 숙련도 점수 및 레벨 정보를 조회합니다.")
    @GetMapping("/mastery")
    public ResponseEntity<List<MasteryDto>> summonerMasteryInfo(@RequestParam String gameName, @RequestParam String tagLine) throws CannotFoundSummoner {
        List<MasteryDto> masteryInfo = summonerService.getSummonerMasteryInfo(gameName, tagLine);
        return ResponseEntity.ok(masteryInfo);
    }

    /** gameName과 tagLine을 통해서 전반적인 최근 20게임의 전적을 검색된 사람 기준으로 보내주는 API이다. (전적 갱신 반영 ㅇ)
     *
     * @param gameName
     * @param tagLine
     * @return
     * @throws CannotFoundSummoner
     */
    @Operation(summary = "최근 매치 요약 리스트 조회 (진짜 씀)", description = "해당 소환사의 최근 20게임 매치 요약 정보를 조회합니다.")
    @GetMapping("/matches/{page}")
    public ResponseEntity<Queue<MatchInfoResDto>> summonerMatchesFinal(@RequestParam String gameName, @RequestParam String tagLine,
                                                                       @RequestParam Boolean refresh, @PathVariable int page) throws CannotFoundSummoner {
        Queue<MatchInfoResDto> matchDtos = summonerService.getSummonerMatches(gameName, tagLine, refresh, page);
        return ResponseEntity.ok(matchDtos);
    }



    /** gameName과 tagLine을 통해서 프로필 정보 (레벨, 아이콘 정보 조회)  (전적 갱신 반영 ㅇ)
     *
     * @param gameName
     * @param tagLine
     * @param refresh
     * @return
     * @throws CannotFoundSummoner
     */
    @Operation(summary = "소환사 프로필 조회 (진짜 씀)", description = "소환사의 기본 프로필(레벨, 아이콘 등) 정보를 조회합니다. (전적 갱신 ㅇ)")
    @GetMapping("/profile")
    public ResponseEntity<ProfileResDto> getProfile(@RequestParam String gameName, @RequestParam String tagLine, @RequestParam Boolean refresh) throws CannotFoundSummoner {
        ProfileResDto profileInfo = summonerService.getProfile(gameName, tagLine, refresh);
        return ResponseEntity.ok(profileInfo);
    }


    /** keyword를 통해서 실시간으로 DB에 저장되어 있는 소환사 리스트를 가져옴 (ElasticSearch를 써도 될 거 같긴 함) 👊
     *
     * @param keyword
     * @return
     */
    @Operation(summary = "키워드로 소환사 실시간 검색", description = "키워드(게임네임과 태그라인이 포함될 수도 있음)가 포함된 소환사 리스트를 조회합니다.")
    @GetMapping("/contain/{keyword}")
    public ResponseEntity<List<SummonerKeywordResDto>> getSummonersByKeyWordV2(@PathVariable String keyword){
        List<SummonerKeywordResDto> summonerByKeyword = summonerSearchService.search(keyword, 20);
        return ResponseEntity.ok(summonerByKeyword);
    }

    @Operation(summary = "소환사 DB LIKE 검색 성능 측정용", description = "기존 MySQL LIKE 방식으로 소환사를 검색합니다.")
    @GetMapping("/search/database")
    public ResponseEntity<SummonerSearchDto.SummonerSearchResponse> searchSummonerWithDatabase(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(summonerSearchService.searchWithDatabase(keyword, limit));
    }

    @Operation(summary = "소환사 Elasticsearch 검색", description = "Elasticsearch 인덱스를 통해 소환사를 검색합니다.")
    @GetMapping("/search/elasticsearch")
    public ResponseEntity<SummonerSearchDto.SummonerSearchResponse> searchSummonerWithElasticsearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(summonerSearchService.searchWithElasticsearch(keyword, limit));
    }

    @Operation(summary = "소환사 Elasticsearch 인덱스 재생성", description = "DB에 저장된 소환사를 Elasticsearch에 다시 색인합니다.")
    @PostMapping("/search/elasticsearch/reindex")
    public ResponseEntity<SummonerSearchDto.SummonerSearchReindexResponse> reindexSummonerSearch() {
        return ResponseEntity.ok(summonerSearchService.reindexAllSummoners());
    }

    @Operation(summary = "소환사 검색 DB vs Elasticsearch 벤치마크", description = "같은 키워드로 기존 DB LIKE 검색과 Elasticsearch 검색 시간을 비교합니다.")
    @GetMapping("/search/benchmark")
    public ResponseEntity<SummonerSearchDto.SummonerSearchBenchmarkResponse> benchmarkSummonerSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "10") int iterations
    ) {
        return ResponseEntity.ok(summonerSearchService.benchmark(keyword, limit, iterations));
    }





    @Operation(summary = "매치 참가자별 timeline 얻기", description = "한 matchId의 타임라인을 한 번만 조회해서 PUUID별 이벤트로 반환합니다.")
    @GetMapping("/match/timeline/all")
    public ResponseEntity<Map<String, List<JsonNode>>> getMatchTimelineByParticipants(@RequestParam String matchId) {
        Map<String, List<JsonNode>> eventsByPuuid = matchService.findEventsByMatchId(matchId);
        return ResponseEntity.ok(eventsByPuuid);
    }


    // 정리 대상 method
    /** matchId를 받아서 해당 게임의 상세한 정보를 보내주는 API이다.
     *
     * @param matchId 게임 정보의 식별 id 값
     * @return

     @Operation(summary = "매치 상세 정보 조회", description = "특정 매치 ID를 통해 해당 게임의 모든 플레이어 기록 등 상세 데이터를 조회합니다.")
     @GetMapping("/matchInfo")
     public ResponseEntity<MatchDto> summonerMatchInfo(@RequestParam String matchId) {
     MatchDto matchInfo = matchService.getMatchInfoById(matchId);
     return ResponseEntity.ok(matchInfo);
     }
     */
    /** 🚨미완성 🚨 전적을 검색한 소환사의 이번 시즌에 가장 많이 챔피언을 보내주는 API이다.
     *
     * @param gameName
     * @param tagLine
     * @return
     * @throws CannotFoundSummoner

     @Operation(summary = "소환사 모스트 챔피언 통계", description = "소환사가 이번 시즌에 가장 많이 플레이한 챔피언 통계 정보를 조회합니다.")
     @GetMapping("/most")
     public ResponseEntity<List<ChampionSeasonStatisticsDto>> summonerMostInfo(@RequestParam String gameName, @RequestParam String tagLine) throws CannotFoundSummoner{
     String puuid = summonerService.findPuuid(gameName, tagLine, summonerService.findSummoner(gameName, tagLine));
     List<ChampionSeasonStatisticsDto> sortedStats = matchService.getStatisticsOfMostChampion(puuid);
     return ResponseEntity.ok(sortedStats);
     }
     */
}
