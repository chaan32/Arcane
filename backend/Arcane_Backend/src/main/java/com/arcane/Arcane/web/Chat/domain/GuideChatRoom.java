package com.arcane.Arcane.web.Chat.domain;

import com.arcane.Arcane.web.Guide.domain.Guide;
import com.arcane.Arcane.web.User.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "guide_chat_room",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_guide_chat_room_guide_reader", columnNames = {"guide_id", "reader_id"})
        }
)
public class GuideChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id", nullable = false)
    private User reader;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean blocked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_id")
    private User blockedBy;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "author_deleted", nullable = false)
    private boolean authorDeleted = false;

    @Column(name = "reader_deleted", nullable = false)
    private boolean readerDeleted = false;

    private GuideChatRoom(Guide guide, User author, User reader) {
        this.guide = guide;
        this.author = author;
        this.reader = reader;
    }

    public static GuideChatRoom of(Guide guide, User reader) {
        if (guide == null) {
            throw new IllegalArgumentException("공략 정보가 필요합니다.");
        }
        if (reader == null) {
            throw new IllegalArgumentException("채팅을 시작할 사용자가 필요합니다.");
        }
        return new GuideChatRoom(guide, guide.getAuthor(), reader);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasParticipant(User user) {
        if (user == null) {
            return false;
        }
        return author.getId().equals(user.getId()) || reader.getId().equals(user.getId());
    }

    public boolean isVisibleTo(User user) {
        if (user == null) {
            return false;
        }
        if (author.getId().equals(user.getId())) {
            return !authorDeleted;
        }
        if (reader.getId().equals(user.getId())) {
            return !readerDeleted;
        }
        return false;
    }

    public void block(User user) {
        if (!hasParticipant(user)) {
            throw new IllegalArgumentException("이 채팅방을 차단할 수 없습니다.");
        }
        this.blocked = true;
        this.blockedBy = user;
        this.blockedAt = LocalDateTime.now();
        touch();
    }

    public void hideFor(User user) {
        if (!hasParticipant(user)) {
            throw new IllegalArgumentException("이 채팅방을 목록에서 삭제할 수 없습니다.");
        }
        if (author.getId().equals(user.getId())) {
            this.authorDeleted = true;
            return;
        }
        this.readerDeleted = true;
    }

    public void restoreForAll() {
        this.authorDeleted = false;
        this.readerDeleted = false;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
