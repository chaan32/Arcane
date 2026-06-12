package com.arcane.Arcane.web.Statistics.controller;

import com.arcane.Arcane.web.Statistics.dto.ChampionDetailDto;
import com.arcane.Arcane.web.Statistics.dto.ChampionNameResDto;
import com.arcane.Arcane.web.Statistics.dto.TierResponseDto;
import com.arcane.Arcane.web.Statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Queue;

@RestController
@RequestMapping("/api/v1/statistics")
@Slf4j
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "포지션별 챔피언 티어 조회", description = "특정 포지션(TOP, JUNGLE 등)의 챔피언 티어 리스트 및 통계를 조회합니다.")
    @GetMapping("/tier/{position}")
    public ResponseEntity<List<TierResponseDto>> getPositionTier(@PathVariable String position){
        List<TierResponseDto> tierResponseList = statisticsService.getTierResponseList(position);

        return ResponseEntity.ok(tierResponseList);
    }

    @Operation(summary = "챔피언 상세 분석 정보 조회", description = "특정 챔피언의 포지션별 빌드, 승률, 상성 등 상세 분석 데이터를 조회합니다.")
    @GetMapping("/championDetail/{championName}")
    public ResponseEntity<List<ChampionDetailDto>> getChampionDetail(@PathVariable String championName){
        List<ChampionDetailDto> dtos = statisticsService.getChampionDetail(championName);
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "전체 챔피언 이름 목록 조회", description = "시스템에 등록된 모든 챔피언의 이름 리스트를 조회합니다.")
    @GetMapping("/champions/all")
    public ResponseEntity<Queue<ChampionNameResDto>> getAllChampionsName() {
        Queue<ChampionNameResDto> dtos = statisticsService.getAllName();
        return ResponseEntity.ok(dtos);
    }

}
