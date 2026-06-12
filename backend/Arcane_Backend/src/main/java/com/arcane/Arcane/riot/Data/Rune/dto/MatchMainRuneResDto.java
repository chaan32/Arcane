package com.arcane.Arcane.riot.Data.Rune.dto;


import com.arcane.Arcane.riot.Match.dto.perks.SelectionDto;
import com.arcane.Arcane.riot.Match.dto.perks.StyleDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchMainRuneResDto { // 정밀, 결의 등

    private String typeDesc;
    private int typeId;

    private RuneResDto mainRune; // 정복자, 집공, 여진 등
    private RuneResDto rune1; // 맨 윗줄
    private RuneResDto rune2; // 중간 줄
    private RuneResDto rune3; // 맨 아래 줄

    public static MatchMainRuneResDto of (StyleDto styleDto){
        List<SelectionDto> selections = styleDto.getSelections();
        return MatchMainRuneResDto.builder()
                .typeId(styleDto.getStyle())
                .mainRune(RuneResDto.of(selections.get(0)))
                .rune1(RuneResDto.of(selections.get(1)))
                .rune2(RuneResDto.of(selections.get(2)))
                .rune3(RuneResDto.of(selections.get(3)))
                .build();
    }
}
