package com.arcane.Arcane.riot.Ranker.dto;


import lombok.Data;

@Data
public class RiotRankerDto {

    private String puuid;

    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
    private String rank;

    private Boolean veteran;
    private Boolean inactive;
    private Boolean freshBlood;
    private Boolean hotStreak;
}