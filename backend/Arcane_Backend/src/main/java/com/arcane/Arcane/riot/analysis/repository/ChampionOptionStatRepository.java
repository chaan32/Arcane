package com.arcane.Arcane.riot.analysis.repository;

import com.arcane.Arcane.riot.analysis.domain.ChampionOptionStat;
import com.arcane.Arcane.riot.analysis.domain.ChampionOptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChampionOptionStatRepository
        extends JpaRepository<ChampionOptionStat, Long> {

    List<ChampionOptionStat> findBySnapshotIdAndChampionIdAndTeamPositionAndOptionType(
            String snapshotId,
            Long championId,
            String teamPosition,
            ChampionOptionType optionType
    );
}
