package com.arcane.Arcane.riot.Match.dto.minimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinimalParticipantDto {
    // 통계 계산에 반드시 필요한 필드들
    @JsonProperty("puuid")
    private String puuid;

    @JsonProperty("championId")
    private long championId;

    @JsonProperty("win")
    private boolean win;

    @JsonProperty("kills")
    private int kills;

    @JsonProperty("deaths")
    private int deaths;

    @JsonProperty("assists")
    private int assists;

    @JsonProperty("totalDamageDealtToChampions")
    private long totalDamageDealtToChampions;
}