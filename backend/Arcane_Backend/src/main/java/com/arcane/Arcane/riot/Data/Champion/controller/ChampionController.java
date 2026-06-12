package com.arcane.Arcane.riot.Data.Champion.controller;


import com.arcane.Arcane.riot.Data.Champion.ChampionService;
import com.arcane.Arcane.riot.Data.Champion.dto.ChampionReturnDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/champion")
public class ChampionController {

    private final ChampionService championService;

    @Operation(summary = "챔피언 ID로 정보 조회", description = "고유 ID를 사용하여 특정 챔피언의 상세 정보를 조회합니다.")
    @GetMapping("/id/{id}")
    public ChampionReturnDto getChampInfo(@PathVariable Long id){
        return championService.getChampionInfoDto(id);
    }
    @Operation(summary = "챔피언 영문명으로 정보 조회", description = "영문 이름을 사용하여 특정 챔피언의 상세 정보를 조회합니다.")
    @GetMapping("/name/{nameEn}")
    public ChampionReturnDto getChampInfoByName(@PathVariable String nameEn){
        return championService.getChampionInfoDtoByName(nameEn);
    }
}
