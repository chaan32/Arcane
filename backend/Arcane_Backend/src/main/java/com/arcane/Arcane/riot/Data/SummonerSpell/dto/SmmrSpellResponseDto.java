package com.arcane.Arcane.riot.Data.SummonerSpell.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SmmrSpellResponseDto {
    private String type;
    private String version;

    private Map<String, SmmrSpellDto> data;
}
