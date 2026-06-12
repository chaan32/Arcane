package com.arcane.worker.redis;


import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.ranker.tier.Tier;
import com.arcane.worker.redis.dto.RedisRankerDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;



    public void updateRedisRanking(Queue<RedisRankerDto> rankersInfoDtos, Tier tier) {
        if (rankersInfoDtos == null || rankersInfoDtos.isEmpty()) {
            throw new IllegalStateException("Redis ranking update skipped because ranker list is empty. tier=" + tier);
        }

        // real ^ temp를 두고, temp에 작성하고 항상 real을 보여주며, 다 작성하면 real로 이름을 변경하기


        // 1. 사용자에게 보여지는 진짜 키 (Real Key)
        String realKey = "ranking:" + tier.getKey(); // 예: ranking:CHALLENGER

        // 2. 작업용 임시 키 (Temp Key)
        String tempKey = "temp:" + realKey; // 예: temp:ranking:CHALLENGER

//        String rankingKey = "ranking:" + tier.getKey(); // 예: ranking:CHALLENGER

        // TEMP -> 새로 업데이트 하는 레디스 지우고 초기화
        redisTemplate.delete(tempKey);

        // 파이프라인으로 대량 데이터를 TEMP 에다가 고속 저장
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for (RedisRankerDto redisDto : rankersInfoDtos) {
                    String puuid = redisDto.getPuuid();
                    // ZSet을 통해서 빠르게 puuid - lp로 정렬을 함
                    operations.opsForZSet().add(tempKey, puuid, redisDto.getLp());


                    try {
                        // Redis에서 저장할 형태
                        String jsonInfo = objectMapper.writeValueAsString(redisDto);

                        // 정보 저장 ( puuid : json으로 저장된 객체 정보 )
                        operations.opsForValue().set(puuid, jsonInfo);

                    } catch (JsonProcessingException e) {
                        log.error(logMessage(
                                        "RedisService.updateRedisRanking",
                                        "JSON 변환 실패",
                                        "puuid=" + puuid
                                ),
                                e
                        );
                    }
                }
                return null;
            }
        });

        // tempKey를 realKey로 이름 변경 (Atomic Swap)
        // 기존 realKey(과거의 데이터)는 자동으로 삭제되고, realKey가 tempKey의 데이터로 복사 됨
        // 원자적이라서 끊김이 없다고 함
        if (Boolean.TRUE.equals(redisTemplate.hasKey(tempKey))) {
            redisTemplate.rename(tempKey, realKey);
            log.info(logMessage(
                    "RedisService.updateRedisRanking",
                    "Redis 랭킹 저장",
                    "tier=" + tier + " | count=" + rankersInfoDtos.size() + " | tempKey=" + tempKey + " | realKey=" + realKey
            ));
        } else {
            throw new IllegalStateException("Redis ranking temp key was not created. tier=" + tier + ", tempKey=" + tempKey);
        }
    }

    public void storeCurrentPatchVersionAtRedis(String currentPatchVersion){
        // 저장
        try{
            redisTemplate.opsForValue().set("patchVersion", currentPatchVersion);
        } catch (Exception e){
            log.error(logMessage(
                            "RedisService.storeCurrentPatchVersionAtRedis",
                            "패치 버전 저장 실패",
                            "version=" + currentPatchVersion + " | reason=" + e.getMessage()
                    ),
                    e
            );
        }
    }
    public String getCurrentPatchVersionFromRedis(){
        return redisTemplate.opsForValue().get("patchVersion");
    }

    public void updateGlobalRanking() {
        String globalKey = "ranking:all";
        String challengerKey = "ranking:challenger";
        List<String> otherKeys = List.of("ranking:grandmaster", "ranking:master");
        redisTemplate.opsForZSet().unionAndStore(challengerKey, otherKeys, globalKey);
        log.info(logMessage(
                "RedisService.updateGlobalRanking",
                "전체 랭킹 병합",
                "globalKey=" + globalKey + " | sourceKeys=" + challengerKey + "," + otherKeys
        ));
    }

    public List<String> getRankersPuuid(String key) {
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);

        if (typedTuples == null) {
            return List.of();
        }

        List<String> puuidList = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            if (typedTuple.getValue() != null) {
                puuidList.add(typedTuple.getValue());
            }
        }
        return puuidList;
    }

    public List<String> getRankersPuuid(String key, int limit) {
        int safeLimit = Math.max(1, limit);
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, safeLimit - 1);

        if (typedTuples == null) {
            return List.of();
        }

        List<String> puuidList = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            if (typedTuple.getValue() != null) {
                puuidList.add(typedTuple.getValue());
            }
        }

        log.info(logMessage(
                "RedisService.getRankersPuuid",
                "랭커 PUUID 조회",
                "key=" + key + " | limit=" + safeLimit + " | count=" + puuidList.size()
        ));
        return puuidList;
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("Redis 랭킹 저장", method, status, detail);
    }
}
