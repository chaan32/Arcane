package com.arcane.Arcane.riot.RiotInform.dto;

import lombok.Data;

@Data
public class TierInfoDto {
    private String tier;         // GOLD, SILVER 등
    private String rank;         // I, II, III, IV 등
    private int leaguePoints;    // 17, 99 등
    private int wins;
    private int losses;
    private String queueType;
}
