package com.arcane.Arcane.riot.Ranker.domain;

import lombok.Getter;

@Getter
public enum Tier {

    CHALLENGER("challenger"),
    GRANDMASTER("grandmaster"),
    MASTER("master");


    private final String key;

    Tier(String key) {
        this.key = key;
    }
}
