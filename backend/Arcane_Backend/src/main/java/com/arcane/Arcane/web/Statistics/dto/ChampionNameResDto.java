package com.arcane.Arcane.web.Statistics.dto;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChampionNameResDto {
    private Long championId;
    private String championName;
    private String championNameEn;
    private String championImageFull;
    private String version;

    public static ChampionNameResDto of(Champion champion) {
        return ChampionNameResDto.builder()
                .championId(champion.getId())
                .championName(champion.getNameKo())
                .championNameEn(champion.getNameEn())
                .championImageFull(champion.getImageFull())
                .version(champion.getVersion())
                .build();
    }
}
