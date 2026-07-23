package com.arcane.worker.summoner.repository;

import com.arcane.worker.summoner.entity.SummonerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface SummonerRepository extends JpaRepository<SummonerEntity, Long> {
    List<SummonerEntity> findAllByPuuidOrderByIdAsc(String puuid);



    Optional<SummonerEntity> findSummonerEntityByPuuid(String puuid);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO summoner (
                puuid,
                game_name,
                trimmed_game_name,
                tag_line,
                create_at,
                update_at
            ) VALUES (
                :puuid,
                :gameName,
                :trimmedGameName,
                :tagLine,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE
                game_name = :gameName,
                trimmed_game_name = :trimmedGameName,
                tag_line = :tagLine,
                update_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int upsertIdentity(
            @Param("puuid") String puuid,
            @Param("gameName") String gameName,
            @Param("trimmedGameName") String trimmedGameName,
            @Param("tagLine") String tagLine
    );
}
