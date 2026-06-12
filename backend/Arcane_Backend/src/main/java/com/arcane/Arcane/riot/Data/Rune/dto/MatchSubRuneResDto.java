package com.arcane.Arcane.riot.Data.Rune.dto;

import com.arcane.Arcane.riot.Match.dto.perks.SelectionDto;
import com.arcane.Arcane.riot.Match.dto.perks.StyleDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchSubRuneResDto {

    private int styleId;

    // 보조 룬으로 2개 선택한 것
    private RuneResDto rune1;
    private RuneResDto rune2;

    public static MatchSubRuneResDto of (StyleDto subStyle){
        List<SelectionDto> selections = subStyle.getSelections();
        return MatchSubRuneResDto.builder()
                .styleId(subStyle.getStyle())
                .rune1(RuneResDto.of(selections.get(0)))
                .rune2(RuneResDto.of(selections.get(1)))
                .build();
    }
}
