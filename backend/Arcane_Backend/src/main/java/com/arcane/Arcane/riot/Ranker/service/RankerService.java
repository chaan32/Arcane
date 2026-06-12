package com.arcane.Arcane.riot.Ranker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arcane.Arcane.common.Exception.Fail.TooManyRequestFail;
import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.riot.Ranker.domain.Tier;
import com.arcane.Arcane.riot.Ranker.dto.RankerResDto;
import com.arcane.Arcane.riot.Ranker.dto.RedisRankerDto;
import com.arcane.Arcane.riot.Ranker.dto.RiotRankerDto;
import com.arcane.Arcane.riot.RiotInform.dto.RiotAccountDto;
import com.arcane.Arcane.riot.RiotInform.service.RiotApiService;
import com.arcane.Arcane.riot.summoner.domain.Summoner;
import com.arcane.Arcane.riot.summoner.service.SummonerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RankerService {
    private final SummonerService summonerService;
    private final RiotApiService riotApiService;

    private static final int PAGE_SIZE = 100;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;


    public void saveChallenger() throws InterruptedException {
        // 챌린저 티어 유저들이 RiotRankerDto 담겨져 있음 puuid로 구분 해야 함
        List<RiotRankerDto> challengers = riotApiService.getLeagueByTier(Tier.CHALLENGER).getEntries();
        challengers.sort((a, b) -> Integer.compare(b.getLeaguePoints(), a.getLeaguePoints()));
        saveRankersList(challengers, Tier.CHALLENGER);
    }
    public void saveGrandMasters() throws InterruptedException {
        // 그랜드 마스터 티어 유저들이 RiotRankerDto 담겨져 있음 puuid로 구분 해야 함
        List<RiotRankerDto> grandMasters = riotApiService.getLeagueByTier(Tier.GRANDMASTER).getEntries();
        grandMasters.sort((a, b) -> Integer.compare(b.getLeaguePoints(), a.getLeaguePoints()));
        saveRankersList(grandMasters, Tier.GRANDMASTER);
    }
    public void saveMasters() throws InterruptedException {
        // 마스터 티어 유저들이 RiotRankerDto 담겨져 있음 puuid로 구분 해야 함
        List<RiotRankerDto> masters = riotApiService.getLeagueByTier(Tier.MASTER).getEntries();
        masters.sort((a, b) -> Integer.compare(b.getLeaguePoints(), a.getLeaguePoints()));
        saveRankersList(masters, Tier.MASTER);
    }


    private void saveRankersList(List<RiotRankerDto> rankerDtoList, Tier tier) throws InterruptedException {
        int i = 1;
        if (tier == Tier.MASTER){
            i = 1001;
        }
        else if (tier == Tier.GRANDMASTER){
            i = 301;
        }

        for (RiotRankerDto riotRankerDto : rankerDtoList) {
            log.info("rank : {} puuid : {}", i++, riotRankerDto.getPuuid());

            Optional<Summoner> optionalSummoner = summonerService.getSummonerByPuuid(riotRankerDto.getPuuid()); // puuid로 우리 DB에 Summoner 저장되어 있는지 체크
            if (optionalSummoner.isEmpty()){ // 저장되어 있지 않는 사람이야
                RiotAccountDto newInform = getNewSummonerInformation(riotRankerDto.getPuuid());
                if (newInform == null){
                    continue;
                }
                summonerService.saveSummoner(newInform);
            }
        }
    }

    public RiotAccountDto getNewSummonerInformation(String puuid) throws InterruptedException {

        for (int i=0; i<3; i++){
            try{
                RiotAccountDto summonerByPuuid = riotApiService.getSummonerByPuuid(puuid);
                if (summonerByPuuid != null) {
                    return summonerByPuuid;
                }
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

    public Summoner updateScore(RiotRankerDto ranker) throws InterruptedException {
        String puuid = ranker.getPuuid();
        Summoner summoner = getSummoner(puuid);
        if (summoner == null) return null;
        return calculateScore(summoner, ranker);
    }

    @Transactional
    protected Summoner getSummoner(String puuid) throws InterruptedException {
        Optional<Summoner> smmr = summonerService.getSummonerByPuuid(puuid);
        if (smmr.isEmpty()){
            log.info("{} :  테이블에 없음", puuid);
            RiotAccountDto riotAccountDto = getNewSummonerInformation(puuid);
            if(riotAccountDto==null) return null;
            return summonerService.saveSummoner(riotAccountDto);
        }
        return smmr.get();
    }


    @Transactional
    protected Summoner calculateScore(Summoner summoner, RiotRankerDto rankerDto){
        Integer soloRankLP = summoner.getSoloRankLP();
        Integer leaguePoints = rankerDto.getLeaguePoints();
        if (soloRankLP != null && soloRankLP.equals(leaguePoints)){
            return summoner;
        }
        else {
            return summoner.updateTier(rankerDto);
        }
    }
    public Queue<RankerResDto> getRankersByKey(String key, int page){
        // 인덱스 계산
        long start = (long) (page - 1) * PAGE_SIZE;
        long end = start + PAGE_SIZE - 1;
        Set<ZSetOperations.TypedTuple<String>> rankData = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);

        if (rankData == null){
            return new LinkedList<>();
        }

        // puuid를 ZSet에서 뽑아서 리스트에 넣는 과정
        List<String> puuidList = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : rankData) {
            puuidList.add(tuple.getValue()); // ZSet에 저장된 값(Value)이 puuid임
        }

        // 한번에 가져오는 과정
        List<String> jsonInfoList = redisTemplate.opsForValue().multiGet(puuidList);

        // 나갈 데이터
        Queue<RankerResDto> rankerQueue = new LinkedList<>();

        for (String jsonData : jsonInfoList) {
            if (jsonData != null){
                try{
                    RedisRankerDto redisDto = objectMapper.readValue(jsonData, RedisRankerDto.class);

                    // (2) 변환 메서드 호출 (LP를 따로 넘길 필요 없음)
                    rankerQueue.add(RankerResDto.from(redisDto));
                } catch (JsonMappingException e) {
                    throw new RuntimeException(e);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return rankerQueue;
    }
    public List<String> getRankersPuuid(String key){
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);
        List<String> puuidList = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            puuidList.add(typedTuple.getValue());
        }
        return puuidList;
    }


    public void updateGlobalRanking(){
        String globalKey = "ranking:all";
        String challengerKey = "ranking:challenger";
        List<String> otherKeys = List.of("ranking:grandmaster","ranking:master");
        redisTemplate.opsForZSet().unionAndStore(challengerKey, otherKeys, globalKey);
    }

    public Long getMasterPageSize(){
        Long count = redisTemplate.opsForZSet().size("ranking:master");
        return count/100 + 1;
    }
    public Long getAllPageSize(){
        Long count = redisTemplate.opsForZSet().size("ranking:all");
        return count/100 + 1;
    }
}