package com.arcane.worker.analysis.repository;

import com.arcane.worker.analysis.domain.ChampionRuneStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChampionRuneStatRepository
        extends JpaRepository<ChampionRuneStat, Long> {

    List<ChampionRuneStat> findBySnapshotIdAndChampionIdAndTeamPosition(
            String snapshotId,
            Long championId,
            String teamPosition
    );
}
