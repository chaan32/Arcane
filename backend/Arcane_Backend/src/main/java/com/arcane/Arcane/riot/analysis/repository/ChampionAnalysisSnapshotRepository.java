package com.arcane.Arcane.riot.analysis.repository;

import com.arcane.Arcane.riot.analysis.domain.ChampionAnalysisSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChampionAnalysisSnapshotRepository extends JpaRepository<ChampionAnalysisSnapshot, Long> {
    Optional<ChampionAnalysisSnapshot> findBySnapshotId (String snapshotId);

    Optional<ChampionAnalysisSnapshot> findByActiveTrue();

    List<ChampionAnalysisSnapshot> findTop10ByOrderByCreatedAtDesc();
}
