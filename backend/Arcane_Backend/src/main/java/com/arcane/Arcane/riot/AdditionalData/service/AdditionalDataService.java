package com.arcane.Arcane.riot.AdditionalData.service;

import com.arcane.Arcane.common.Exception.Fail.TooManyRequestFail;
import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.common.Redis.RedisService;
import com.arcane.Arcane.riot.Ranker.service.RankerService;
import com.arcane.Arcane.riot.RiotInform.dto.ProfileResDto;
import com.arcane.Arcane.riot.RiotInform.dto.RiotAccountDto;
import com.arcane.Arcane.riot.RiotInform.service.RiotApiService;
import com.arcane.Arcane.riot.summoner.domain.Summoner;
import com.arcane.Arcane.riot.summoner.service.SummonerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalDataService {

    private final RiotApiService riotApiService;
    private final RankerService rankerService;
    private final SummonerService summonerService;
    private final RestTemplate restTemplate;
    private final RedisService redisService;

    public void downloadRankersProfile() throws InterruptedException {
        List<String> challengerPuuid =
                rankerService.getRankersPuuid("ranking:challenger");
        List<String> gmPuuid =
                rankerService.getRankersPuuid("ranking:grandmaster");
        List<String> masterPuuid =
                rankerService.getRankersPuuid("ranking:master");
        storeProfile(challengerPuuid);
        log.info("<< CHALLENGER ICON & LEVEL DONE >>");
        storeProfile(gmPuuid);
        log.info("<< GRANDMASTER ICON & LEVEL DONE >>");
        storeProfile(masterPuuid);
        log.info("<< MASTER ICON & LEVEL DONE >>");
    }


    private void storeProfile(List<String> puuids) throws InterruptedException {
        List<Summoner> list = new ArrayList<>();
        int i=1;
        for (String puuid : puuids) {
            Optional<Summoner> optionalSummoner = summonerService.getSummonerByPuuid(puuid);
            Summoner summoner = null;
            if (optionalSummoner.isEmpty()){ // 저장되어 있지 않는 사람이야
                RiotAccountDto newInform = rankerService.getNewSummonerInformation(puuid);
                if (newInform == null){
                    continue;
                }
                summoner = summonerService.saveSummoner(newInform);
            }
            else {
                summoner = optionalSummoner.get();
            }

            if (summoner.getIconId() == null || summoner.getLevel() == null){ // icon이나 level이 null이면
                log.info("{} --- {} : icon & level request ",i++, puuid);
                ProfileResDto profileResDto = requestProfileToRiot(puuid);
                summoner.updateProfile(profileResDto);
                list.add(summoner);
            }

        }
        summonerService.updateSummoners(list);
    }

    private ProfileResDto requestProfileToRiot(String puuid) throws InterruptedException {
        for (int i=0; i<3; i++){
            try{
                ProfileResDto profileInfo = riotApiService.getProfileInfo(puuid);
                if (profileInfo!=null) return profileInfo;
            } catch (TooManyRequestFail e){
                if (i < 2) {
                    log.warn("API 요청 제한(429). {}초 후 재시도... (시도 {}/3)", 125, i + 1);
                    Thread.sleep(125000);
                }
            } catch (CannotFoundSummoner e) {
                log.info("소환사 정보 없음 (404). 재시도 안 함.");
                return null;
            }
        }
        log.error("3회 재시도 실패. PUUID: {}", puuid);
        return null;
    }



    public String getCurrentPatchVersion(){
        String patchVersionUrl = "https://ddragon.leagueoflegends.com/api/versions.json";
        try {
            // RestTemplate을 사용하여 문자열 배열로 받기
            ResponseEntity<String[]> response = restTemplate.getForEntity(patchVersionUrl, String[].class);
            String[] versions = response.getBody();

            if (versions != null && versions.length > 0) {
                redisService.storeCurrentPatchVersionAtRedis(versions[0]);
                return versions[0]; // 가장 최신 버전 반환
            }
        } catch (Exception e) {
            log.error("패치 버전을 가져오는 중 오류 발생: {}", e.getMessage());
        }
        return "error";
    }

    public String getCurrentPatchVersionFromRedis(){
        String currentPatchVersionFromRedis = redisService.getCurrentPatchVersionFromRedis();
        return currentPatchVersionFromRedis==null ? getCurrentPatchVersion() : currentPatchVersionFromRedis;
    }
    @Scheduled(cron = "0 0 0 * * THU", zone = "Asia/Seoul")
    public void storeCurrentPatchVersionAuto(){
        // 매주 목요일 0시에 redis에 저장하기
        redisService.storeCurrentPatchVersionAtRedis(getCurrentPatchVersion());
    }
}
