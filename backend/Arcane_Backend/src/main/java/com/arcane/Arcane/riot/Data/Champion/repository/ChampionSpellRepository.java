package com.arcane.Arcane.riot.Data.Champion.repository;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Data.Champion.ChampionSpell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ChampionSpellRepository extends JpaRepository<ChampionSpell, Long> {
    List<ChampionSpell> getSpellsFindByChampion(Champion champion);

}