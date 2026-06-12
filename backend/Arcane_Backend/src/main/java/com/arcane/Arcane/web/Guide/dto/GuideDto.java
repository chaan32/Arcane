package com.arcane.Arcane.web.Guide.dto;

// patchNote 그대로 베낌

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.arcane.Arcane.web.Guide.domain.Guide;
import com.arcane.Arcane.web.User.domain.User;

public class GuideDto {
    @Getter
    @NoArgsConstructor
    public static class GuideRequest {
        private String title;
        private String content;
        private Long championId;
    }

    @Getter
    public static class GuideResponseDto {
        private Long id;
        private String title;
        private String content;
        private String summary;
        private String authorName;
        private Long authorId;
        private Long championId;
        private String championNameKo;
        private String championNameEn;
        private String championImageFull;
        private Integer views;
        private Integer likes;
        private Integer dislikes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public GuideResponseDto(Guide guide) {
            this.id = guide.getId();
            this.title = guide.getTitle();
            this.content = guide.getContent();
            this.summary = buildSummary(guide.getContent());
            this.authorName = displayName(guide.getAuthor());
            this.authorId = guide.getAuthor().getId();
            this.championId = guide.getChampion().getId();
            this.championNameKo = guide.getChampion().getNameKo();
            this.championNameEn = guide.getChampion().getNameEn();
            this.championImageFull = guide.getChampion().getImageFull();
            this.createdAt = guide.getCreatedAt();
            this.updatedAt = guide.getUpdatedAt();
            this.views = guide.getViews();
            this.likes = guide.getLikes();
            this.dislikes = guide.getDislikes();
        }
    }

    @Getter
    public static class GuideUploadResponseDto {
        private Long id;
        private String title;
        private String authorName;
        private Long authorId;
        private Long championId;

        public GuideUploadResponseDto(Guide guide) {
            this.id = guide.getId();
            this.title = guide.getTitle();
            this.authorName = displayName(guide.getAuthor());
            this.authorId = guide.getAuthor().getId();
            this.championId = guide.getChampion().getId();
        }
    }

    @Getter
    public static class GuideListResponseDto {
        private Long id;
        private String title;
        private String content;
        private String summary;
        private String authorName;
        private Long authorId;
        private Long championId;
        private String championNameKo;
        private String championNameEn;
        private String championImageFull;
        private Integer views;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public GuideListResponseDto(Guide guide) {
            this.id = guide.getId();
            this.title = guide.getTitle();
            this.content = guide.getContent();
            this.summary = buildSummary(guide.getContent());
            this.authorName = displayName(guide.getAuthor());
            this.authorId = guide.getAuthor().getId();
            this.championId = guide.getChampion().getId();
            this.championNameKo = guide.getChampion().getNameKo();
            this.championNameEn = guide.getChampion().getNameEn();
            this.championImageFull = guide.getChampion().getImageFull();
            this.views = guide.getViews();
            this.createdAt = guide.getCreatedAt();
            this.updatedAt = guide.getUpdatedAt();
        }
    }

    private static String displayName(User user) {
        if (user.getNickName() != null && !user.getNickName().isBlank()) {
            return user.getNickName();
        }
        if (user.getGameName() != null && !user.getGameName().isBlank()) {
            return user.getGameName();
        }
        return user.getLoginId();
    }

    private static String buildSummary(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String plainText = content
                .replaceAll("!\\[[^]]*]\\([^)]*\\)", "")
                .replaceAll("\\[[^]]*]\\([^)]*\\)", "")
                .replaceAll("[#*_`>\\-]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (plainText.length() <= 90) {
            return plainText;
        }
        return plainText.substring(0, 90) + "...";
    }
}
