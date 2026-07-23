package com.arcane.Arcane.riot.summoner.repository;

import com.arcane.Arcane.riot.summoner.domain.Summoner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SummonerRepository extends JpaRepository<Summoner, Long> {
    Optional<Summoner> findSummonerByTrimmedGameNameAndTagLine(String gameName, String tagLine);
    List<Summoner> findAllByPuuidOrderByIdAsc(String puuid);

    default Optional<Summoner> findSummonerByPuuid(String puuid) {
        return findAllByPuuidOrderByIdAsc(puuid).stream().findFirst();
    }

    List<Summoner> findByGameNameContainingIgnoreCase(String keyword);
    List<Summoner> findByGameNameContainingIgnoreCaseAndTagLineContainingIgnoreCase(String gameName, String tagLine);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO summoner (
                trimmed_game_name,
                game_name,
                tag_line,
                puuid,
                icon,
                level,
                solo_rank_tier,
                solo_rank_lp,
                solo_rank_win,
                solo_rank_defeat,
                flex_rank_tier,
                flex_ranklp,
                flex_rank_win,
                flex_rank_defeat,
                create_at,
                update_at
            ) VALUES (
                :#{#summoner.trimmedGameName},
                :#{#summoner.gameName},
                :#{#summoner.tagLine},
                :#{#summoner.puuid},
                :#{#summoner.iconId},
                :#{#summoner.level},
                :#{#summoner.soloRankTier},
                :#{#summoner.soloRankLP},
                :#{#summoner.soloRankWin},
                :#{#summoner.soloRankDefeat},
                :#{#summoner.flexRankTier},
                :#{#summoner.flexRankLP},
                :#{#summoner.flexRankWin},
                :#{#summoner.flexRankDefeat},
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """, nativeQuery = true)
    int insertIgnore(@Param("summoner") Summoner summoner);
}
