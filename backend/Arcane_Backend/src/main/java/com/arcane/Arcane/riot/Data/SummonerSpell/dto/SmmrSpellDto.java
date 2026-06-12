package com.arcane.Arcane.riot.Data.SummonerSpell.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
public class SmmrSpellDto {
    private String id;
    private String name;
    private String description;
    private String key;
    private ImageDto image;

    @Getter
    @Setter
    @ToString
    public static class ImageDto {
        private String full;
    }
}
