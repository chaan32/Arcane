package com.arcane.Arcane.web.Guide.repository;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.arcane.Arcane.web.Guide.domain.Guide;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GuideRepository extends JpaRepository<Guide, Long> {
    @EntityGraph(attributePaths = {"author", "champion"})
    List<Guide> findByChampion(Champion champion);

    @EntityGraph(attributePaths = {"author", "champion"})
    List<Guide> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"author", "champion"})
    List<Guide> findByIdIn(Collection<Long> ids);

    @Query(
            value = """
            SELECT guide.*
            FROM guide guide
            WHERE LOWER(guide.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(guide.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY guide.updated_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM guide guide
            WHERE LOWER(guide.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(guide.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """,
            nativeQuery = true
    )
    Page<Guide> searchByTitleOrContentContaining(@Param("keyword") String keyword, Pageable pageable);

    long countByTitleStartingWith(String titlePrefix);
}
