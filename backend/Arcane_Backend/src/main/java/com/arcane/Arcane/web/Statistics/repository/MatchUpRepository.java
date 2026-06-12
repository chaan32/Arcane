package com.arcane.Arcane.web.Statistics.repository;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.web.Statistics.domain.Champion.ChampionStatsByPosition;
import com.arcane.Arcane.web.Statistics.domain.MatchUp.MatchUp;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchUpRepository extends JpaRepository<MatchUp, Long> {
    List<MatchUp> findByOwnerStats(ChampionStatsByPosition stats);
    @Query("SELECT m.opponent FROM MatchUp m WHERE m.ownerStats = :stats ORDER BY m.relativeWinRate ASC")
    List<Champion> findTop3CountersByStats(@Param("stats") ChampionStatsByPosition stats, Pageable pageable);
}
