package com.arcane.Arcane.riot.Data.Champion.repository;

import com.arcane.Arcane.riot.Data.Champion.ChampionSkin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChampionSkinRepository extends JpaRepository<ChampionSkin, Long> {
}
