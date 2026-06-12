package com.arcane.Arcane.web.Statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterChampionResDto {
    private Long championId;
    private String championNameEn;
    private String championImageFull;
    private String championImgUrl;
}
