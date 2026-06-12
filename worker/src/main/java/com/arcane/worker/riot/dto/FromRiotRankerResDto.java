package com.arcane.worker.riot.dto;

import com.arcane.worker.ranker.dto.RiotRankerDto;
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
