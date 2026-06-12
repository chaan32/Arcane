package com.arcane.Arcane.riot.Data.Rune.dto;

import com.arcane.Arcane.riot.Data.Rune.domain.RunePath;
import com.arcane.Arcane.riot.Data.Rune.domain.RuneSlot;
import lombok.Builder;

import java.util.Comparator;
import java.util.List;

@Builder
public record RuneTreePathDto(
        Long id,
        String name,
        String key,
        String icon,
        List<RuneTreeSlotDto> slots
) {
    public static RuneTreePathDto from(RunePath path) {
        return RuneTreePathDto.builder()
                .id(path.getId())
                .name(path.getName())
                .key(path.getRuneKey())
                .icon(path.getIcon())
                .slots(path.getSlots().stream()
                        .sorted(Comparator.comparing(RuneSlot::getId))
                        .map(RuneTreeSlotDto::from)
                        .toList())
                .build();
    }
}
