package com.arcane.Arcane.riot.Ranker.controller;

import com.arcane.Arcane.riot.Ranker.dto.RankerFinalResDto;
import com.arcane.Arcane.riot.Ranker.dto.RankerResDto;
import com.arcane.Arcane.riot.Ranker.service.RankerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Queue;

@RestController
@RequestMapping("/api/v1/ranker")
@Slf4j
@RequiredArgsConstructor
public class RankerController {

    private final RankerService rankerService;

    @Operation(summary = "챌린저 랭킹 조회", description = "페이지 번호를 기반으로 챌린저 티어의 랭킹 목록을 조회합니다.")
    @GetMapping("/challenger/{page}")
    public ResponseEntity<RankerFinalResDto> getChallRanks(@PathVariable int page){
        Queue<RankerResDto> rankersByKey = rankerService.getRankersByKey("ranking:challenger", page);
        return ResponseEntity.ok(RankerFinalResDto.from(rankersByKey, 3L, (long) page));
    }

    @Operation(summary = "그랜드마스터 랭킹 조회", description = "페이지 번호를 기반으로 그랜드마스터 티어의 랭킹 목록을 조회합니다.")
    @GetMapping("/grandmaster/{page}")
    public ResponseEntity<RankerFinalResDto> getGrandMasterRanks(@PathVariable int page){
        Queue<RankerResDto> rankersByKey = rankerService.getRankersByKey("ranking:grandmaster", page);
        return ResponseEntity.ok(RankerFinalResDto.from(rankersByKey, 7L, (long) page));
    }
    @Operation(summary = "마스터 랭킹 조회", description = "페이지 번호를 기반으로 마스터 티어의 랭킹 목록을 조회합니다.")
    @GetMapping("/master/{page}")
    public ResponseEntity<RankerFinalResDto> getMASTERRanks(@PathVariable int page){

        Queue<RankerResDto> rankersByKey = rankerService.getRankersByKey("ranking:master", page);
        Long masterPageSize = rankerService.getMasterPageSize();

        return ResponseEntity.ok(RankerFinalResDto.from(rankersByKey, masterPageSize, (long) page));
    }

    @Operation(summary = "전체 통합 랭킹 조회", description = "티어 구분 없이 전체 통합 랭킹 목록을 페이지별로 조회합니다.")
    @GetMapping("/all/{page}")
    public ResponseEntity<RankerFinalResDto> getALLRanks(@PathVariable int page){
        Queue<RankerResDto> rankersByKey = rankerService.getRankersByKey("ranking:all", page);
        Long allPageSize = rankerService.getAllPageSize();
        return ResponseEntity.ok(RankerFinalResDto.from(rankersByKey, allPageSize, (long) page));
    }

}
