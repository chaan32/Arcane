package com.arcane.Arcane.riot.Data.Champion;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Embeddable // Entity에 포함시키기 위해 추가
public class ChampionStats {

    private Double hp;

    @JsonProperty("hpperlevel")
    private Double hpPerLevel;

    private Double mp;

    @JsonProperty("mpperlevel")
    private Double mpPerLevel;

    @JsonProperty("movespeed")
    private Double moveSpeed;

    private Double armor;

    @JsonProperty("armorperlevel")
    private Double armorPerLevel;

    @JsonProperty("spellblock")
    private Double spellBlock; // 마법 저항력

    @JsonProperty("spellblockperlevel")
    private Double spellBlockPerLevel;

    @JsonProperty("attackrange")
    private Double attackRange;

    @JsonProperty("hpregen")
    private Double hpRegen;

    @JsonProperty("hpregenperlevel")
    private Double hpRegenPerLevel;

    @JsonProperty("mpregen")
    private Double mpRegen;

    @JsonProperty("mpregenperlevel")
    private Double mpRegenPerLevel;

    private Double crit;

    @JsonProperty("critperlevel")
    private Double critPerLevel;

    @JsonProperty("attackdamage")
    private Double attackDamage;

    @JsonProperty("attackdamageperlevel")
    private Double attackDamagePerLevel;

    @JsonProperty("attackspeedperlevel")
    private Double attackSpeedPerLevel;

    @JsonProperty("attackspeed")
    private Double attackSpeed;
}
