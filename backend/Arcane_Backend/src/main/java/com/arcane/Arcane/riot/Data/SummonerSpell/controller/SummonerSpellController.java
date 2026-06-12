package com.arcane.Arcane.riot.Data.SummonerSpell.controller;

import com.arcane.Arcane.riot.Data.SummonerSpell.SmmrSpell;
import com.arcane.Arcane.riot.Data.SummonerSpell.SummonerSpellService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/summoner-spell")
public class SummonerSpellController {
    private final SummonerSpellService summonerSpellService;

    @Operation(summary = "소환사 주문 ID로 정보 조회", description = "고유 ID를 사용하여 특정 소환사 주문 정보를 조회합니다.")
    @GetMapping("{id}")
    public ResponseEntity<SmmrSpell> getSspell(Long id){
        return ResponseEntity.ok(summonerSpellService.getSmmrSpellById(id));
    }
}
