package com.arcane.Arcane.web.Chat.domain;

import com.arcane.Arcane.web.User.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guide_chat_message")
public class GuideChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private GuideChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    public void markRead() {
        this.read = true;
    }

    private GuideChatMessage(GuideChatRoom room, User sender, String content) {
        this.room = room;
        this.sender = sender;
        this.content = content;
    }

    public static GuideChatMessage of(GuideChatRoom room, User sender, String content) {
        if (room == null) {
            throw new IllegalArgumentException("채팅방 정보가 필요합니다.");
        }
        if (sender == null) {
            throw new IllegalArgumentException("메시지 작성자가 필요합니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메시지를 입력해주세요.");
        }
        return new GuideChatMessage(room, sender, content.trim());
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
