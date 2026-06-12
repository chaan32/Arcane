package com.arcane.Arcane.web.PatchNote.dto;

import com.arcane.Arcane.web.PatchNote.domain.PatchNote;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

public class PatchNoteDto {

    @Getter
    @NoArgsConstructor
    public static class PatchNoteRequest {
        private String title;
        private String content;
    }

    @Getter
    public static class PatchNoteResponseDto {
        private Long id;
        private String title;
        private String content;
        private String authorName;
        private Integer views;
        private Integer likes;
        private Integer dislikes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public PatchNoteResponseDto(PatchNote patchNote) {
            this.id = patchNote.getId();
            this.title = patchNote.getTitle();
            this.content = patchNote.getContent();
            this.authorName = patchNote.getAuthor().getGameName(); // 작성자 이름
            this.createdAt = patchNote.getCreatedAt();
            this.updatedAt = patchNote.getUpdatedAt();
            this.views = patchNote.getViews();
            this.likes = patchNote.getLikes();
            this.dislikes = patchNote.getDislikes();
        }
    }

    @Getter
    public static class PatchNoteUploadResponseDto {
        private Long id;
        private String title;
        private String authorName;
        private Long authorId;
        public PatchNoteUploadResponseDto(PatchNote patchNote) {
            this.id = patchNote.getId();
            this.title = patchNote.getTitle();
            this.authorName = patchNote.getAuthor().getGameName();
            this.authorId = patchNote.getAuthor().getId();
        }
    }

    @Getter
    public static class PatchNoteListResponseDto {
        private Long id;
        private String title;
        private String authorName;
        private Long authorId;
        private Integer views;
        private LocalDateTime createdAt;


        public PatchNoteListResponseDto(PatchNote patchNote) {
            this.id = patchNote.getId();
            this.title = patchNote.getTitle();
            this.authorName = patchNote.getAuthor().getGameName();
            this.views = patchNote.getViews();
            this.createdAt = patchNote.getCreatedAt();
            this.authorId = patchNote.getAuthor().getId();
        }
    }
}
