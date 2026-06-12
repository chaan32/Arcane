package com.arcane.Arcane.riot.RiotInform.dto.Ranker;


import lombok.Data;

import java.util.List;

@Data
public class ChallengerLeagueDto {
    private String tier;
    private String leagueId;
    private String queue;
    private String name;
    private List<ChallengerDto> entries;
}
