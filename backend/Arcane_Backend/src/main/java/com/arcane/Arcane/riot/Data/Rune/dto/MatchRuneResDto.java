package com.arcane.Arcane.riot.Data.Rune.dto;

import com.arcane.Arcane.riot.Match.dto.perks.PerksDto;
import com.arcane.Arcane.riot.Match.dto.perks.StyleDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchRuneResDto {
    // 메인 룬 : 정밀 등 -> 다 선택
    private MatchMainRuneResDto mainRune;
    // 서브 룬 : 결의 등 2개 선택
    private MatchSubRuneResDto subRune;
    // 스탯 룬 : ~~
    private MatchStatRuneResDto statRune;

    public static MatchRuneResDto of (PerksDto perksDto){

        List<StyleDto> styles = perksDto.getStyles();
        return MatchRuneResDto.builder()
                .mainRune(MatchMainRuneResDto.of(styles.get(0)))
                .subRune(MatchSubRuneResDto.of(styles.get(1)))
                .statRune(MatchStatRuneResDto.of(perksDto.getStatPerks()))
                .build();
    }
}
