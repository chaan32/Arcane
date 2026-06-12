package com.arcane.Arcane.monitoring.test.waiting_api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WaitingService {

    private int currentWaitingCount = 0;
    private final Map<String, Integer> registeredPhone = new HashMap<>();

    public String waitingRegister(String name, String phone){
        String maskPhoneNumber = maskPhoneNumber(phone);
        // 1. 인포 : 일반적인 비즈니스 흐름 기록
        log.info("[웨이팅 요청] 이름 : {}, 연락처 : {}", name, maskPhoneNumber);


        // 2. 에러 : 비즈니스오류
        if (registeredPhone.containsKey(phone)) {

            log.warn("[웨이팅 등록 실패] 중복 등록 시도 발생 ! 연락처 : {}", maskPhoneNumber);
            throw new IllegalArgumentException("이미 대기 등록된 연락처 입니다.");
        }

        // 3. 워닝 : 시스템 경고성 메세지
        if (currentWaitingCount >= 2) {
            log.warn("[웨이팅 경고] 대기열 마감 임박 : 현재 대기 인원 : {}명", currentWaitingCount);
        }

        currentWaitingCount++;
        registeredPhone.put(phone, currentWaitingCount);

        // 4. 인포 : 비지니스 흐름 기록
        log.info("[웨이팅 완료] 대기번호 : {}번 발금 완료 | 고객명 : {}",currentWaitingCount, name);
        return name + "님, 대기번호 " + currentWaitingCount + "번이 발급되었습니다.";
    }
    public int getWaitingNumber(String phone){
        if (!registeredPhone.containsKey(phone)){
            log.warn("[웨이팅 조회 실패] 미등록 번호 조회 시도 : {}", maskPhoneNumber(phone));
            throw new IllegalArgumentException("웨이팅 명단에 없는 연락처 입니다. 먼저 웨이팅을 등록해주세요");
        }
        return registeredPhone.get(phone);
    }



    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 13) return "****";
        return phone.substring(0, 4) + "****" + phone.substring(8);
    }
}
