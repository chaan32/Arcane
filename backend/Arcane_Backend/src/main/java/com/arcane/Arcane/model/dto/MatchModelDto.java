package com.arcane.Arcane.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.arcane.Arcane.riot.Match.dto.MetadataDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchModelDto {
    @JsonProperty("metadata")
    private MetadataDto metadata;

    @JsonProperty("info")
    private InfoModelDto info;
}
