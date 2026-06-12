package com.arcane.Arcane.riot.analysis.domain;

public enum ChampionTier {
    OP("OP"),
    TIER_1("1티어"),
    TIER_2("2티어"),
    TIER_3("3티어"),
    TIER_4("4티어");

    private final String label;

    ChampionTier(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
