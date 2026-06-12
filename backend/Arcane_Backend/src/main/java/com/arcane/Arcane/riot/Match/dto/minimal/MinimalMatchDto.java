package com.arcane.Arcane.riot.Match.dto.minimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinimalMatchDto {
    @JsonProperty("info")
    private MinimalInfoDto info;
}
