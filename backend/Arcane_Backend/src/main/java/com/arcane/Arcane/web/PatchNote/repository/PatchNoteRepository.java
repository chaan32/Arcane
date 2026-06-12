package com.arcane.Arcane.web.PatchNote.repository;

import com.arcane.Arcane.web.PatchNote.domain.PatchNote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatchNoteRepository extends JpaRepository<PatchNote, Long> {
}
