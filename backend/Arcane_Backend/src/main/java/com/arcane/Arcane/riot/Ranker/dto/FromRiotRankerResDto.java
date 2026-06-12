package com.arcane.Arcane.riot.Ranker.dto;

import lombok.Data;

import java.util.List;

@Data
public class FromRiotRankerResDto {
    private String tier;
    private String leagueId;
    private String queue;
    private String name;
    private List<RiotRankerDto> entries;
}
