package com.arcane.Arcane.riot.Match.dto.perks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StyleDto {

    private String description; // "primaryStyle" 또는 "subStyle"
    private int style; // 메인 룬 빌드 ID (예: 8000은 정밀)

    @JsonProperty("selections")
    private List<SelectionDto> selections;
}
