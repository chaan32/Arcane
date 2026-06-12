package com.arcane.Arcane.riot.Match.repository;

import com.arcane.Arcane.riot.Match.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    @EntityGraph(attributePaths = {"participants", "participants.summoner"})
    public Optional<Match> findMatchByMatchId(String matchId);

    // IGNORE을 씀으로써 match_Id (unique key)가 있으면 insert 무시하기
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO match_info (
                match_id,
                game_creation,
                game_duration,
                game_end_timestamp,
                game_mode,
                queue_id
            ) VALUES (
                :#{#matchEntity.matchId},
                :#{#matchEntity.gameCreation},
                :#{#matchEntity.gameDuration},
                :#{#matchEntity.gameEndTimestamp},
                :#{#matchEntity.gameMode},
                :#{#matchEntity.queueId}
            )
            """, nativeQuery = true)
    int insertIgnore(@Param("matchEntity") Match match);


}
