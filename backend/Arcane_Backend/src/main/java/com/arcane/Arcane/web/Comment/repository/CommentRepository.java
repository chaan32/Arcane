package com.arcane.Arcane.web.Comment.repository;

import com.arcane.Arcane.web.Comment.domain.Comment;
import com.arcane.Arcane.web.Guide.domain.Guide;
import com.arcane.Arcane.web.PatchNote.domain.PatchNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPatchNote(PatchNote patchNote);
    List<Comment> findByGuide(Guide guid);
}
