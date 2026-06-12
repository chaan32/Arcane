package com.arcane.Arcane.riot.Data.Champion;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ChampionDto {

    private String id;    // "Xerath"
    private String key;   // "101"
    private String name;  // "제라스"
    private String title; // "초월한 마법사"
    private String blurb; // 설명

    // --- [기존에 만든 Info, Stats 재사용] ---
    // (Info에는 attack, defense, magic, difficulty만 남겨두셨죠?)
    private ChampionInfo info;
    private ChampionStats stats;

    private List<String> tags; // "Mage", "Support"
    private String partype;    // "마나"

    // --- [이미지] ---
    private ImageDto image;

    // --- [리스트 데이터] ---
    private List<SkinDto> skins;
    private List<SpellDto> spells;
    private PassiveDto passive;

    // ==========================================
    //  내부 DTO 정의 (Inner Classes)
    // ==========================================

    @Getter @Setter
    @ToString
    public static class SkinDto {
        private String id;      // "101000"
        private int num;        // 0, 1, 2...
        private String name;    // "default"
        private boolean chromas;
    }

    @Getter @Setter
    @ToString
    public static class SpellDto {
        private String id;           // "XerathArcanopulseChargeUp"
        private String name;         // "비전 파동"
        private String description;  // 스킬 설명
        private String tooltip;      // 툴팁 (복잡한 수식 포함)

        @JsonProperty("cooldownBurn")
        private String cooldownBurn; // "9/8/7/6/5"

        @JsonProperty("costBurn")
        private String costBurn;     // "80/90/100/110/120"

        private ImageDto image;      // 스킬 아이콘
    }

    @Getter @Setter
    @ToString
    public static class PassiveDto {
        private String name;
        private String description;
        private ImageDto image;
    }

    @Getter @Setter
    @ToString
    public static class ImageDto {
        private String full;   // "Xerath.png"
        private String sprite;
        private String group;
    }
}
