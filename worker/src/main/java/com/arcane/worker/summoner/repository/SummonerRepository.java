package com.arcane.worker.summoner.repository;

import com.arcane.worker.summoner.entity.SummonerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummonerRepository extends JpaRepository<SummonerEntity, Long> {
    Optional<SummonerEntity> findSummonerEntityByPuuid(String puuid);
}
