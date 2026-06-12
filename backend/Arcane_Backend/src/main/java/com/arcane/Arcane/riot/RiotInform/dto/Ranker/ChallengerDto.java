package com.arcane.Arcane.riot.RiotInform.dto.Ranker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChallengerDto {
    private String puuid;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
    private String rank;
}
