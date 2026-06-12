package com.arcane.Arcane.web.Chat.dto;

import com.arcane.Arcane.web.Chat.domain.GuideChatMessage;
import com.arcane.Arcane.web.Chat.domain.GuideChatRoom;
import com.arcane.Arcane.web.User.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class GuideChatDto {
    @Getter
    @NoArgsConstructor
    public static class SendMessageRequest {
        private String content;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class UserBriefResponse {
        private Long id;
        private String name;
        private String profileImage;

        public static UserBriefResponse of(User user) {
            return UserBriefResponse.builder()
                    .id(user.getId())
                    .name(displayName(user))
                    .profileImage(user.getProfileImage())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class MessageResponse {
        private Long id;
        private Long roomId;
        private UserBriefResponse sender;
        private String content;
        private LocalDateTime createdAt;
        private boolean read;

        public static MessageResponse of(GuideChatMessage message) {
            return MessageResponse.builder()
                    .id(message.getId())
                    .roomId(message.getRoom().getId())
                    .sender(UserBriefResponse.of(message.getSender()))
                    .content(message.getContent())
                    .createdAt(message.getCreatedAt())
                    .read(message.isRead())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class RoomResponse {
        private Long id;
        private Long guideId;
        private String guideTitle;
        private List<UserBriefResponse> participants;
        private List<MessageResponse> messages;
        private boolean blocked;
        private UserBriefResponse blockedBy;
        private LocalDateTime updatedAt;

        public static RoomResponse of(GuideChatRoom room, List<GuideChatMessage> messages) {
            return RoomResponse.builder()
                    .id(room.getId())
                    .guideId(room.getGuide().getId())
                    .guideTitle(room.getGuide().getTitle())
                    .participants(List.of(
                            UserBriefResponse.of(room.getAuthor()),
                            UserBriefResponse.of(room.getReader())
                    ))
                    .messages(messages.stream().map(MessageResponse::of).toList())
                    .blocked(room.isBlocked())
                    .blockedBy(room.getBlockedBy() == null ? null : UserBriefResponse.of(room.getBlockedBy()))
                    .updatedAt(room.getUpdatedAt())
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
