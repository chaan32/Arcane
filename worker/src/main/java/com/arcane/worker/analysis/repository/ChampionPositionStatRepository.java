package com.arcane.worker.analysis.repository;

import com.arcane.worker.analysis.domain.ChampionPositionStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChampionPositionStatRepository
        extends JpaRepository<ChampionPositionStat, Long> {

    List<ChampionPositionStat> findBySnapshotId(String snapshotId);

    List<ChampionPositionStat> findBySnapshotIdAndChampionId(String snapshotId, Long championId);
}
