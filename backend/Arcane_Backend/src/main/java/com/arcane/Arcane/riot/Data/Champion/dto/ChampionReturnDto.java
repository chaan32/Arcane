package com.arcane.Arcane.riot.Data.Champion.dto;

import com.arcane.Arcane.riot.Data.Champion.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class ChampionReturnDto {
    private Long id;
    private String nameEn;
    private String nameKo;
    private String title;
    private String blurb;
    private String imageFull;
    private String version;
    private List<String> tags;

    private ChampionInfo info;
    private ChampionStats stats;
    private ChampionPassive passive;

    private List<SpellDto> spells;
    private List<SkinDto> skins;


    public ChampionReturnDto(Champion entity) {
        this.id = entity.getId();
        this.nameEn = entity.getNameEn();
        this.nameKo = entity.getNameKo();
        this.title = entity.getTitle();
        this.blurb = entity.getBlurb();
        this.imageFull = entity.getImageFull();
        this.version = entity.getVersion();

        if (entity.getTags() != null) {
            this.tags = List.of(entity.getTags().split(","));
        }

        this.info = entity.getInfo();
        this.stats = entity.getStats();
        this.passive = entity.getPassive();

        this.spells = entity.getSpells().stream()
                .map(SpellDto::new)
                .collect(Collectors.toList());


        this.skins = entity.getSkins().stream()
                .map(SkinDto::new)
                .collect(Collectors.toList());
    }

    // 스펠
    @Getter
    public static class SpellDto {
        private String id;
        private String name;
        private String description;
        private String spellKey;
        private String imageFull;
        private String version;
        private String cooldown;
        private String cost;

        public SpellDto(ChampionSpell spell) {
            this.id = spell.getSpellId();
            this.name = spell.getName();
            this.description = spell.getDescription();
            this.spellKey = spell.getSpellKey();
            this.imageFull = spell.getImageFull();
            this.version = spell.getVersion();
            this.cooldown = spell.getCooldownBurn();
            this.cost = spell.getCostBurn();
        }
    }

    // 스킨
    @Getter
    public static class SkinDto {
        private String id;
        private int num;
        private String name;
        private boolean chromas;

        public SkinDto(ChampionSkin skin) {
            this.id = skin.getSkinId();
            this.num = skin.getNum();
            this.name = skin.getName();
            this.chromas = skin.isChromas();
        }
    }
}
