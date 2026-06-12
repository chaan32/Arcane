package com.arcane.Arcane.common.Redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.arcane.Arcane.riot.Ranker.domain.Tier;
import com.arcane.Arcane.riot.Ranker.dto.RedisRankerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Queue;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;


    public void updateRedisRanking(Queue<RedisRankerDto> rankersInfoDtos, Tier tier) {

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
                        log.error("JSON 변환 오류: {}", puuid, e);
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
            log.info("[Redis] {} 티어 동기화 완료 (Temp -> Real 교체)", tier);
        } else {
            log.warn("[Redis] {} 티어 업데이트 실패: 임시 키가 생성되지 않음", tier);
        }
    }

    public void storeCurrentPatchVersionAtRedis(String currentPatchVersion){
        // 저장
        try{
            redisTemplate.opsForValue().set("patchVersion", currentPatchVersion);
        } catch (Exception e){
            log.error("Redis 패치 버전 저장 중 오류 발생 : {}", e.getMessage());
        }
    }
    public String getCurrentPatchVersionFromRedis(){
        return redisTemplate.opsForValue().get("patchVersion");
    }
}
