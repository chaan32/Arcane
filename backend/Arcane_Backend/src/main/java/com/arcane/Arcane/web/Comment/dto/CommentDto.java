package com.arcane.Arcane.web.Comment.dto;

import com.arcane.Arcane.web.Comment.domain.Comment;
import com.arcane.Arcane.web.User.domain.Role;
import com.arcane.Arcane.web.User.domain.User;
import lombok.*;

import java.time.LocalDateTime;

public class CommentDto {
    @Getter
    @Setter
    public static class CommentRequest {
        private String content;
        private Long commentId;
    }


    @Getter
    @AllArgsConstructor
    @Builder
    public static class CommentResponse {
        private String authorName;
        private Long authorId;
        private Long id;
        private String content;
        private LocalDateTime createdAt;
        private Role role;

        public static CommentResponse of(Comment comment, User author){
            return CommentResponse.builder()
                    .authorName(displayName(author))
                    .authorId(author.getId())
                    .id(comment.getId())
                    .content(comment.getContent())
                    .createdAt(comment.getCreatedAt())
                    .role(author.getRole())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class CommentDetailDto {
        private String authorName;
        private Long authorId;
        private Long id;
        private String content;
        private LocalDateTime createdAt;
        private Role role;
        private Integer likes;
        private Integer dislikes;

        public static CommentDetailDto of(Comment comment, User author) {
            return CommentDetailDto.builder()
                    .authorName(displayName(comment.getAuthor()))
                    .authorId(comment.getAuthor().getId())
                    .id(comment.getId())
                    .content(comment.getContent())
                    .createdAt(comment.getCreatedAt())
                    .role(comment.getAuthor().getRole())
                    .likes(comment.getLikes())
                    .dislikes(comment.getDislikes())
                    .build();
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
}
