package com.arcane.Arcane.riot.Data.Champion.repository;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChampionRepository extends JpaRepository<Champion, Long> {
    Optional<Champion> findByNameKo(String championName);
    Optional<Champion> findByNameEn(String championName);
    @Query("""
            SELECT champ.id
            FROM Champion champ
            """)
    List<Long> findChampionIdAll();
}