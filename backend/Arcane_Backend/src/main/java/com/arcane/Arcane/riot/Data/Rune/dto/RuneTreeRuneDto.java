package com.arcane.Arcane.riot.Data.Rune.dto;

import com.arcane.Arcane.riot.Data.Rune.domain.Rune;
import lombok.Builder;

@Builder
public record RuneTreeRuneDto(
        Long id,
        String name,
        String key,
        String icon,
        String description
) {
    public static RuneTreeRuneDto from(Rune rune) {
        return RuneTreeRuneDto.builder()
                .id(rune.getId())
                .name(rune.getName())
                .key(rune.getRuneKey())
                .icon(rune.getIcon())
                .description(rune.getShortDesc())
                .build();
    }
}
