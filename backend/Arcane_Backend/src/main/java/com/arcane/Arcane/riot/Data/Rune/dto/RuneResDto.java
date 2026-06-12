package com.arcane.Arcane.riot.Data.Rune.dto;

import com.arcane.Arcane.riot.Match.dto.perks.SelectionDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RuneResDto {
    private int id;
    private String desc;

    public static RuneResDto of (SelectionDto dto){
        return RuneResDto.builder()
                .id(dto.getPerk())
                .build();
    }
}
