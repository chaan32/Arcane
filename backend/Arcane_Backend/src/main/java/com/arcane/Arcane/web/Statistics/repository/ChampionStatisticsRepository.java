package com.arcane.Arcane.web.Statistics.repository;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.web.Statistics.domain.Champion.ChampionStatsByPosition;
import com.arcane.Arcane.web.Statistics.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChampionStatisticsRepository extends JpaRepository<ChampionStatsByPosition, Long> {
    List<ChampionStatsByPosition> findByPosition(Position position);

    // "Champion" 필드와 "Position" 필드를 모두 사용해서 데이터를 찾는 메서드
    Optional<ChampionStatsByPosition> findByChampionAndPosition(Champion champion, Position position);

    List<ChampionStatsByPosition> findByChampion(Champion champion);
}
