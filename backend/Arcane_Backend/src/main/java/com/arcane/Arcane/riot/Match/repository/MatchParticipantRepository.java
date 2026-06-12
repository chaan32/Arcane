package com.arcane.Arcane.riot.Match.repository;

import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {

    @Query("""
            SELECT m.matchId
            FROM MatchParticipant mp
            JOIN mp.match m
            WHERE mp.summoner.id = :summonerId
            ORDER BY m.gameEndTimestamp DESC
            """)
    List<String> findLatestMatchIdsBySummonerId(@Param("summonerId") Long summonerId);

    default String[] findLatestMatchIdArrayBySummonerId(Long summonerId) {
        return findLatestMatchIdsBySummonerId(summonerId).toArray(String[]::new);
    }
}
