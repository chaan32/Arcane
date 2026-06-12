package com.arcane.Arcane.riot.Match.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataDto {

    @JsonProperty("matchId")
    private String matchId;

    @JsonProperty("participants")
    private List<String> participants; // puuid 문자열 list
}
