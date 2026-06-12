package com.arcane.Arcane.riot.analysis.repository;

import com.arcane.Arcane.riot.analysis.domain.ChampionMatchUpStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChampionMatchUpStatRepository
        extends JpaRepository<ChampionMatchUpStat, Long> {

    List<ChampionMatchUpStat> findBySnapshotIdAndChampionIdAndTeamPosition(
            String snapshotId,
            Long championId,
            String teamPosition
    );
}
