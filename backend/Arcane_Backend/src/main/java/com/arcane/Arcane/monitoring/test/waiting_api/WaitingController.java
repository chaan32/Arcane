package com.arcane.Arcane.monitoring.test.waiting_api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/waiting")
@RequiredArgsConstructor
@Slf4j
public class WaitingController {
    private final WaitingService waitingService;

    @PostMapping
    public ResponseEntity<String> register(@RequestBody WaitingRequest waitingRequest) {
        try{
            log.info("[api 호출] 웨이팅 등록 요청 들어옴");
            String result = waitingService.waitingRegister(waitingRequest.getName(), waitingRequest.getPhone());
            return ResponseEntity.ok(result);
        }
         catch (IllegalArgumentException e){
            log.error("[api 경고] 웨이팅 등록 실패 : {} ", e.getMessage());
             return ResponseEntity.badRequest().body("웨이팅 등록에 실패했습니다 : " + e.getMessage());
         }
    }

    @GetMapping
    public String getWaitingStatus(@RequestBody WaitingRequest waitingRequest) {
        String phone = waitingRequest.getPhone();
        try{
            log.info("[api 호출] 웨이팅 순번 조회 요청 들어옴 - 연락처 : {}", phone);
            int waitingNumber = waitingService.getWaitingNumber(phone);
            return "현재 대기 순번은 " + waitingNumber + "번입니다. ";
        } catch (IllegalArgumentException e){
            log.warn("[api 경고] 웨이팅 조회 실패 (미등록 번호) : {}", phone);
            return "조회 실패 : "+e.getMessage();
        } catch (Exception e){
            log.error("[api 오류] 웨이팅 조회 중 서버 에러 발생 : {}", e.getMessage());
            return "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        }

    }

}
