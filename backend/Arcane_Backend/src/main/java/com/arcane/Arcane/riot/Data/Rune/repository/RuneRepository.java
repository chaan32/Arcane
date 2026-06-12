package com.arcane.Arcane.riot.Data.Rune.repository;

import com.arcane.Arcane.riot.Data.Rune.domain.Rune;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RuneRepository extends JpaRepository<Rune, Long> {
    Optional<Rune> findById(Long id);
}
