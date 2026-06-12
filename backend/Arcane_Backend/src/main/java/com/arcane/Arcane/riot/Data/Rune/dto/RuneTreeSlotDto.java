package com.arcane.Arcane.riot.Data.Rune.dto;

import com.arcane.Arcane.riot.Data.Rune.domain.RuneSlot;
import com.arcane.Arcane.riot.Data.Rune.domain.Rune;
import lombok.Builder;

import java.util.Comparator;
import java.util.List;

@Builder
public record RuneTreeSlotDto(
        Long id,
        List<RuneTreeRuneDto> runes
) {
    public static RuneTreeSlotDto from(RuneSlot slot) {
        return RuneTreeSlotDto.builder()
                .id(slot.getId())
                .runes(slot.getRunes().stream()
                        .sorted(Comparator.comparing(Rune::getId))
                        .map(RuneTreeRuneDto::from)
                        .toList())
                .build();
    }
}
