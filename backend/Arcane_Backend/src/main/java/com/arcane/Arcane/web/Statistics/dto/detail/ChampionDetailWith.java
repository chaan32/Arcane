package com.arcane.Arcane.web.Statistics.dto.detail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ChampionDetailWith {
    private String championName;
    private String championNameEn;
    private Integer gameCount;
    private Float winRate;
}
