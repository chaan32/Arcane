package com.arcane.worker.riot.service;

import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.exception.fail.CannotFoundSummoner;
import com.arcane.worker.exception.fail.TooManyRequestFail;
import com.arcane.worker.ranker.tier.Tier;
import com.arcane.worker.riot.dto.FromRiotRankerResDto;
import com.arcane.worker.riot.dto.ProfileDto;
import com.arcane.worker.riot.dto.ProfileResDto;
import com.arcane.worker.riot.dto.RiotAccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiotApiService {
    @Value("${riot.api-key}")
    private String apiKey;

    private static final String ASIA_BASE_URL = "https://asia.api.riotgames.com";
    private static final String KR_BASE_URL = "https://kr.api.riotgames.com";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 티어별 랭킹 정보 조회 (Challenger, Grandmaster, Master)
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
            log.info(logMessage(
                    "RiotApiService.getLeagueByTier",
                    "Riot 랭킹 요청",
                    "tier=" + tier
            ));
            ResponseEntity<FromRiotRankerResDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    FromRiotRankerResDto.class
            );
            FromRiotRankerResDto body = response.getBody();
            if (body == null || body.getEntries() == null) {
                throw new IllegalStateException("Riot ranking API returned empty body. tier=" + tier);
            }
            log.info(logMessage(
                    "RiotApiService.getLeagueByTier",
                    "Riot 랭킹 성공",
                    "tier=" + tier + " | count=" + body.getEntries().size()
            ));
            return body;

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn(logMessage(
                    "RiotApiService.getLeagueByTier",
                    "Riot 429",
                    "tier=" + tier + " | status=" + e.getStatusCode()
            ));
            throw new TooManyRequestFail("Too Many Request at ranking API. tier=" + tier);
        } catch (HttpClientErrorException e) {
            log.error(logMessage(
                            "RiotApiService.getLeagueByTier",
                            "Riot 요청 실패",
                            "tier=" + tier + " | status=" + e.getStatusCode()
                    ),
                    e
            );
            throw new IllegalStateException(
                    "Riot ranking API failed. tier=" + tier + ", status=" + e.getStatusCode(),
                    e
            );
        } catch (RestClientException e) {
            log.error(logMessage(
                            "RiotApiService.getLeagueByTier",
                            "Riot 연결 실패",
                            "tier=" + tier + " | reason=" + e.getMessage()
                    ),
                    e
            );
            throw new IllegalStateException("Riot ranking API connection failed. tier=" + tier, e);
        }

    }

    // puuid를 통해서 소환사 정보 획득하기
    public RiotAccountDto getSummonerByPuuid(String puuid) throws CannotFoundSummoner, TooManyRequestFail {
        // uuid 정보 얻기
        String url = ASIA_BASE_URL + "/riot/account/v1/accounts/by-puuid/" + puuid;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            log.info(logMessage(
                    "RiotApiService.getSummonerByPuuid",
                    "Riot 계정 요청",
                    "puuid=" + puuid
            ));
            ResponseEntity<RiotAccountDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RiotAccountDto.class
            );
            return response.getBody();

        } catch (HttpClientErrorException.TooManyRequests e){
            log.warn(logMessage(
                    "RiotApiService.getSummonerByPuuid",
                    "Riot 429",
                    "puuid=" + puuid
            ));
            throw new TooManyRequestFail("Too Many Request At Find Summoner Inform By Puuid : "+ puuid);
        }
        catch (HttpClientErrorException.NotFound e) {
            // 404 에러일 경우 직접 메시지 던짐
            log.info(logMessage(
                    "RiotApiService.getSummonerByPuuid",
                    "Riot 계정 없음",
                    "puuid=" + puuid
            ));
            throw new CannotFoundSummoner(puuid + " 소환사를 찾을 수 없습니다.");
        } catch (RestClientException e) {
            log.error(logMessage(
                            "RiotApiService.getSummonerByPuuid",
                            "Riot 계정 실패",
                            "puuid=" + puuid + " | reason=" + e.getMessage()
                    ),
                    e
            );
            throw new CannotFoundSummoner("소환사 정보를 가져오는 중 오류가 발생했습니다.");
        }
    }

    public ProfileResDto getProfileInfo(String puuid) throws CannotFoundSummoner, TooManyRequestFail {
        String url = KR_BASE_URL + "/lol/summoner/v4/summoners/by-puuid/" + puuid;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            log.info(logMessage(
                    "RiotApiService.getProfileInfo",
                    "Riot 프로필 요청",
                    "puuid=" + puuid
            ));
            ResponseEntity<ProfileDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ProfileDto.class
            );
            return ProfileResDto.of(response.getBody());
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn(logMessage(
                    "RiotApiService.getProfileInfo",
                    "Riot 429",
                    "puuid=" + puuid
            ));
            throw new TooManyRequestFail("profile request LIMIT >>>");
        } catch (HttpClientErrorException.NotFound e) {
            log.info(logMessage(
                    "RiotApiService.getProfileInfo",
                    "Riot 프로필 없음",
                    "puuid=" + puuid
            ));
            throw new CannotFoundSummoner(puuid + " 소환사를 찾을 수 없습니다.");
        } catch (RestClientException e) {
            log.error(logMessage(
                            "RiotApiService.getProfileInfo",
                            "Riot 프로필 실패",
                            "puuid=" + puuid + " | reason=" + e.getMessage()
                    ),
                    e
            );
            throw new CannotFoundSummoner("소환사 정보를 가져오는 중 오류가 발생했습니다.");
        }
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("Riot API 요청", method, status, detail);
    }

}
