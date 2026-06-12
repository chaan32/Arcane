package com.arcane.Arcane.riot.Data.Rune;

import com.arcane.Arcane.riot.Data.Rune.dto.RuneResponseDto;
import com.arcane.Arcane.riot.Data.Rune.dto.RuneTreePathDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rune")
public class RuneController  {

    private final RuneService runeService;

    @Operation(summary = "룬 ID로 정보 조회", description = "고유 ID를 사용하여 특정 룬의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public RuneResponseDto getChampInfo(@PathVariable Long id){
        return runeService.getRuneInfoById(id);
    }

    @Operation(summary = "전체 룬 트리 조회", description = "룬 경로, 슬롯, 룬 목록을 트리 형태로 조회합니다.")
    @GetMapping("/tree")
    public List<RuneTreePathDto> getRuneTree() {
        return runeService.getRuneTree();
    }

}
