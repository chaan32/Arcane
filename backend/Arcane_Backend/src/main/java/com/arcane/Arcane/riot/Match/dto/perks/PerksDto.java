package com.arcane.Arcane.riot.Match.dto.perks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PerksDto {

    @JsonProperty("statPerks")
    private StatPerksDto statPerks;

    @JsonProperty("styles")
    private List<StyleDto> styles;

    public static PerksDto createMock(){
        // 1. 최종적으로 반환될 PerksDto 객체 생성
        PerksDto perks = new PerksDto();

        // 2. 스탯 파편(Stat Perks) 정보 생성
        StatPerksDto statPerks = new StatPerksDto();
        statPerks.setOffense(5008); // 공격: 적응형 능력치 +9
        statPerks.setFlex(5008);   // 유연: 적응형 능력치 +9
        statPerks.setDefense(5002); // 방어: 방어력 +6

        perks.setStatPerks(statPerks);

        // 3. 메인 룬 빌드 (정밀) 정보 생성
        StyleDto primaryStyle = new StyleDto();
        primaryStyle.setDescription("primaryStyle");
        primaryStyle.setStyle(8000); // 8000: 정밀(Precision) 빌드 ID
        primaryStyle.setSelections(List.of(
                new SelectionDto(8010, 0, 0, 0), // 핵심 룬: 정복자
                new SelectionDto(9101, 0, 0, 0), // 승전보
                new SelectionDto(9104, 0, 0, 0), // 전설: 민첩함
                new SelectionDto(8014, 0, 0, 0)  // 최후의 일격
        ));

        // 4. 보조 룬 빌드 (지배) 정보 생성
        StyleDto subStyle = new StyleDto();
        subStyle.setDescription("subStyle");
        subStyle.setStyle(8100); // 8100: 지배(Domination) 빌드 ID
        subStyle.setSelections(List.of(
                new SelectionDto(8139, 0, 0, 0), // 피의 맛
                new SelectionDto(8141, 0, 0, 0)  // deepWard
        ));

        // 5. 메인 룬과 보조 룬을 최종 객체에 리스트로 담기
        perks.setStyles(List.of(primaryStyle, subStyle));

        return perks;
    }
}