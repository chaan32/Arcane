package com.arcane.Arcane.riot.RiotInform.service;

import com.arcane.Arcane.common.Exception.Fail.*;
import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.riot.Match.dto.MatchDto;
import com.arcane.Arcane.riot.Match.dto.minimal.MinimalMatchDto;
import com.arcane.Arcane.riot.Ranker.domain.Tier;
import com.arcane.Arcane.riot.Ranker.dto.FromRiotRankerResDto;
import com.arcane.Arcane.riot.RiotInform.dto.*;
import com.arcane.Arcane.riot.RiotInform.dto.Ranker.ChallengerLeagueDto;
import com.arcane.Arcane.riot.summoner.dto.SummonerDto;
import com.arcane.Arcane.model.dto.MatchModelDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiotApiService {
    @Value("${riot.api-key}")
    private String apiKey;

    private String baseUrlAsia = "https://asia.api.riotgames.com";
    private String baseUrlKR = "https://kr.api.riotgames.com";
    private static final String KR_BASE_URL = "https://kr.api.riotgames.com";

    private final RestTemplate restTemplate = new RestTemplate();


    // puuid 캐시 관련
    private static final long ACCOUNT_CACHE_TTL_MILLIS = 60_000L;
    private final ConcurrentMap<String, CachedAccount> accountCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> accountLocks = new ConcurrentHashMap<>();

    /** 캐시 키 값 생성
     * Hide On Bush#KR1 -> hideonbush#kr1
     * @param gameName
     * @param tagLine
     * @return
     */
    private String makeCacheKey(String gameName, String tagLine) {
        return (gameName + "#" + tagLine).replace(" ", "").toLowerCase(Locale.ROOT);
    }
    // 캐시 메모리에 저장될 DTO
    private record CachedAccount(RiotAccountDto account, long cachedAt) {
        // 생성자
        private CachedAccount(RiotAccountDto account) {
            this(account, System.currentTimeMillis());
        }

        // 만료 되었는지?
        private boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > ACCOUNT_CACHE_TTL_MILLIS;
        }
    }

    // ------------------------------------ 헬퍼 메소드 ------------------------------------ 👊
    /** Header 파일 만들어서 객체 만들어서 리턴하기
     *
     * @return
     */
    private HttpEntity<?> makeEntity(){
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);
        return new HttpEntity<>(headers);
    }
    /** 솔랭, 자랭 티어 설정하는 메소드
     *
     * @param dto
     * @param list
     * @return
     */
    private SummonerDto setSummonerDtoV2(SummonerDto dto, List<TierInfoDto> list) {
        if (list.size() == 2) {
            TierInfoDto solo, flex;
            if (list.get(0).getQueueType().equals("RANKED_SOLO_5x5")) {
                solo = list.get(0);
                flex = list.get(1);
            } else {
                solo = list.get(1);
                flex = list.get(0);
            }
            dto.setSoloRankDefeat(solo.getLosses());
            dto.setSoloRankWin(solo.getWins());
            dto.setSoloRankTier(solo.getTier()+" "+solo.getRank());
            dto.setSoloRankLP(solo.getLeaguePoints());

            dto.setFlexRankDefeat(flex.getLosses());
            dto.setFlexRankWin(flex.getWins());
            dto.setFlexRankTier(flex.getTier()+" "+flex.getRank());
            dto.setFlexRankLP(flex.getLeaguePoints());
            return dto;
        } else if (list.size() == 1) {
            TierInfoDto temp = list.get(0);
            if (temp.getQueueType().equals("RANKED_SOLO_5x5")) {
                dto.setSoloRankDefeat(temp.getLosses());
                dto.setSoloRankWin(temp.getWins());
                dto.setSoloRankTier(temp.getTier() + " " + temp.getRank());
                dto.setSoloRankLP(temp.getLeaguePoints());
            } else {
                dto.setFlexRankDefeat(temp.getLosses());
                dto.setFlexRankWin(temp.getWins());
                dto.setFlexRankTier(temp.getTier()+" "+temp.getRank());
                dto.setFlexRankLP(temp.getLeaguePoints());
            }
            return dto;
        }
        return dto;
    }


    // ------------------------------------ 다듬음 ------------------------------------ 👊
    /** gameNmae, tagLine으로 직접적으로 puuid 얻기 (cache로 API 호출 최소화 진행) 👊
     *
     * @param gameName
     * @param tagLine
     * @return
     * @throws CannotFoundSummoner
     */
    public String getSummonerPuuid(String gameName, String tagLine) throws CannotFoundSummoner {
        return getSummonerInfo(gameName, tagLine).getPuuid();
    }

    /** puuid로 프로필 (아이콘, 레벨) 얻기 (실제 API 요청) 👊
     *
     * @param puuid
     * @return
     * @throws CannotFoundSummoner
     */
    public ProfileResDto getProfileInfo(String puuid) throws CannotFoundSummoner {
        // uuid 정보 얻기
        String url = baseUrlKR + "/lol/summoner/v4/summoners/by-puuid/" + puuid;

        HttpEntity<?> entity = makeEntity();

        try {
            ResponseEntity<ProfileDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ProfileDto.class
            );
            ProfileDto body = response.getBody();
            return ProfileResDto.of(body);

        } catch (HttpClientErrorException.TooManyRequests e){
            throw new TooManyRequestFail("profile request LIMIT >>>");
        } catch (HttpClientErrorException.NotFound e) {
            // 404 에러일 경우 직접 메시지 던짐
            throw new CannotFoundSummoner(puuid + " 소환사를 찾을 수 없습니다.");
        } catch (RestClientException e) {
            log.error(" Riot API ERROR : {}", e.getMessage());
            throw new CannotFoundSummoner("소환사 정보를 가져오는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 티어별 랭킹 정보 조회 (Challenger, Grandmaster, Master) (실제 API 요청) 👊
     * 반환 타입: FromRiotRankerResDto
     */
    public FromRiotRankerResDto getLeagueByTier(Tier tier) {
        String url = KR_BASE_URL;
        if (tier == Tier.CHALLENGER) {
            url += "/lol/league/v4/challengerleagues/by-queue/RANKED_SOLO_5x5";
        } else if (tier == Tier.GRANDMASTER) {
            url += "/lol/league/v4/grandmasterleagues/by-queue/RANKED_SOLO_5x5";
        } else if (tier == Tier.MASTER) {
            url += "/lol/league/v4/masterleagues/by-queue/RANKED_SOLO_5x5";
        } else {
            throw new IllegalArgumentException("지원하지 않는 티어입니다: " + tier);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<FromRiotRankerResDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    FromRiotRankerResDto.class
            );
            return response.getBody();

        } catch (HttpClientErrorException.TooManyRequests e){
            throw new TooManyRequestFail("Too Many Request AT Find Challenger Ranking");
        } catch (Exception e){
            log.warn(e.getMessage());
            return null;
        }

    }

    /** 솔로랭크 티어, 자유랭크 티어 세팅하기 (실제 API 요청) 👊
     *
     * @param dto
     * @return
     */
    public SummonerDto getSummonerTierInfo(SummonerDto dto){
        log.info("RiotApiService : dto : {}", dto.toString());
        String url = baseUrlKR + "/lol/league/v4/entries/by-puuid/"+dto.getPuuid() ;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<TierInfoDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<TierInfoDto> tierInfoDtos = response.getBody();
            log.info("tier INFORM : {}",tierInfoDtos.toString());
            return setSummonerDtoV2(dto, tierInfoDtos);
        }
        catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /** 숙련도 가져오기 조회 (실제 API 요청) 👊
     *
     * @param puuid
     * @return
     * @throws CannotFoundSummoner
     */
    public List<MasteryDto> getMasteryInfo(String puuid) throws CannotFoundSummoner {
        String url = baseUrlKR + "/lol/champion-mastery/v4/champion-masteries/by-puuid/" + puuid;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<List<MasteryDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody();
        } catch (HttpClientErrorException.TooManyRequests e){
            throw new TooManyRequestFail("profile request LIMIT >>>");
        } catch (HttpClientErrorException.NotFound e) {
            // 404 에러일 경우 직접 메시지 던짐
            throw new CannotFoundSummoner(puuid + " 소환사를 찾을 수 없습니다.");
        } catch (RestClientException e) {
            log.error(" Riot API ERROR : {}", e.getMessage());
            throw new CannotFoundSummoner("소환사 정보를 가져오는 중 오류가 발생했습니다.");
        }

    }

    /** Match-Id 페이지네이션으로 가져오기 (실제 API 요청) 👊
     *
     * @param puuid
     * @param page
     * @return
     * @throws CannotFoundSummoner
     */
    public String[] getSummonerMatches(String puuid, int page) throws CannotFoundSummoner {
        int pageSize = 20;
        int startIdx = (page-1)*pageSize;
        int count = pageSize;
        String url = UriComponentsBuilder.fromHttpUrl(baseUrlAsia + "/lol/match/v5/matches/by-puuid/" + puuid + "/ids")
                .queryParam("start", startIdx)   // 시작 인덱스
                .queryParam("count", count)   // 가져올 개수
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        try{
            ResponseEntity<String[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException.TooManyRequests e){
            throw new TooManyRequestFail("profile request LIMIT >>>");
        } catch (HttpClientErrorException.NotFound e) {
            // 404 에러일 경우 직접 메시지 던짐
            throw new CannotFoundSummoner(puuid + " 소환사를 찾을 수 없습니다.");
        } catch (RestClientException e) {
            log.error(" Riot API ERROR : {}", e.getMessage());
            throw new CannotFoundSummoner("소환사 정보를 가져오는 중 오류가 발생했습니다.");
        }
    }

    /** MatchId를 통해서 매치에 대한 상세 정보를 가져옴 (실제 API 요청) 👊
     *
     * @param matchId
     * @return
     */
    public MatchDto getMatchInfo(String matchId) {
        String url = baseUrlAsia + "/lol/match/v5/matches/"+matchId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        try{
            ResponseEntity<MatchDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException.TooManyRequests e){
            throw new TooManyRequestFail("라이엇 API 요청 한도 초과 되었어요.");
        }
        catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /** TEMP로 씀 (실제 API 요청) 👊
     *
     * @param matchIds
     * @return
     */
    public List<MatchModelDto> getMatchModel(String[] matchIds){
        List<MatchModelDto> dtos = new ArrayList<>();
        for (String matchId : matchIds) {
            String url = baseUrlAsia + "/lol/match/v5/matches/"+matchId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Riot-Token", apiKey);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            try{
                ResponseEntity<MatchModelDto> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<>() {}
                );
                dtos.add(response.getBody());
            } catch (Exception e) {
                log.error(e.getMessage());
                return null;
            }
        }
        return dtos;
    }

    /** gameName, tagLine으로 RiotAccountDto 얻기 - 여기서 puuid 얻을 수 있음 (cache로 API 호출 최소화 진행) 👊
     *
     * @param gameName
     * @param tagLine
     * @return
     * @throws CannotFoundSummoner
     */
    public RiotAccountDto getSummonerInfo(String gameName, String tagLine) throws CannotFoundSummoner {
        String cacheKey = makeCacheKey(gameName, tagLine);
        // key로 캐시를 얻고
        CachedAccount cachedAccount = accountCache.get(cacheKey);
        if (cachedAccount != null && !cachedAccount.isExpired()) { // 아직 유효 하다면,
            return cachedAccount.account();
        }

        // 최초 접근 (크리티컬 섹션 드가기 전)
        Object lock = accountLocks.computeIfAbsent(cacheKey, ignored -> new Object());
        synchronized (lock) {
            cachedAccount = accountCache.get(cacheKey);
            if (cachedAccount != null && !cachedAccount.isExpired()) {
                return cachedAccount.account();
            }

            RiotAccountDto account = requestSummonerInfo(gameName, tagLine);
            accountCache.put(cacheKey, new CachedAccount(account));
            return account;
        }
    }

    /** gameName, tagLine으로 RiotAccountDto 얻기 (실제 API 요청) 👊
     *
     * @param gameName
     * @param tagLine
     * @return
     * @throws CannotFoundSummoner
     */
    private RiotAccountDto requestSummonerInfo(String gameName, String tagLine) throws CannotFoundSummoner {
        String url = baseUrlAsia + "/riot/account/v1/accounts/by-riot-id/" + gameName +"/" + tagLine;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<RiotAccountDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RiotAccountDto.class
            );
            RiotAccountDto body = response.getBody();
            if (body == null) {
                throw new CannotFoundSummoner("소환사 정보를 가져오는 중 오류가 발생했습니다.");
            }
            log.info("RiotAPI SERVICE : RiotAccountDto : {}", body);
            return body;

        } catch (HttpClientErrorException.NotFound e) {
            // 404 에러일 경우 직접 메시지 던짐
            throw new CannotFoundSummoner(gameName + "#" + tagLine + " 소환사를 찾을 수 없습니다.");
        } catch (RestClientException e) {
            log.error(" Riot API ERROR : {}", e.getMessage());
            throw new CannotFoundSummoner("소환사 정보를 가져오는 중 오류가 발생했습니다.");
        }
    }

    /** puuid를 통해서 RiotAccountDto ; 정보 얻기 - 유효 Summoner 검증용 (실제 API 요청) 👊
     *
     * @param puuid
     * @return
     * @throws CannotFoundSummoner
     * @throws TooManyRequestFail
     */
    public RiotAccountDto getSummonerByPuuid(String puuid) throws CannotFoundSummoner, TooManyRequestFail {
        // uuid 정보 얻기
        String url = baseUrlAsia + "/riot/account/v1/accounts/by-puuid/" + puuid;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<RiotAccountDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RiotAccountDto.class
            );
            return response.getBody();

        } catch (HttpClientErrorException.TooManyRequests e){
            log.info("GET Summoner Inform By Puuid");
            throw new TooManyRequestFail("Too Many Request At Find Summoner Inform By Puuid : "+ puuid);
        }
        catch (HttpClientErrorException.NotFound e) {
            // 404 에러일 경우 직접 메시지 던짐
            throw new CannotFoundSummoner(puuid + " 소환사를 찾을 수 없습니다.");
        } catch (RestClientException e) {
            log.error(" Riot API ERROR : {}", e.getMessage());
            throw new CannotFoundSummoner("소환사 정보를 가져오는 중 오류가 발생했습니다.");
        }
    }

    /** matchId를 통해서 timeLine을 통해 이벤트 얻기 위해서 가져 오기 (실제 API 요청) 👊
     *
     * @param matchId
     * @return
     */
    public String getMatchTimeline(String matchId) {
        String url = baseUrlAsia + "/lol/match/v5/matches/" + matchId + "/timeline";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            // 원본 데이터를 그대로 넘겨주기 위해 String.class로 받습니다.
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return response.getBody();

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("타임라인 API 호출 LIMIT 초과 - 매치 ID: {}", matchId);
            throw new TooManyRequestFail("Too Many Request At Find Timeline By MatchId : " + matchId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("해당 매치의 타임라인을 찾을 수 없습니다. (404) - 매치 ID: {}", matchId);
            return null;
        } catch (Exception e) {
            log.error("타임라인 데이터를 가져오는 중 오류 발생 ({}): {}", matchId, e.getMessage());
            return null;
        }
    }


}
