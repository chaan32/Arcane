package com.arcane.Arcane.web.Comment.domain;

import com.arcane.Arcane.web.Guide.domain.Guide;
import com.arcane.Arcane.web.PatchNote.domain.PatchNote;
import com.arcane.Arcane.web.User.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author; // 작성자 (User 엔티티와 연결함)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer likes = 0;

    @Column(nullable = false)
    private Integer dislikes = 0;

    // 어떤 패치노트의 댓글인지?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patch_note_id", nullable = true)
    private PatchNote patchNote;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = true) // 공략 게시판을 위한 FK (null 허용)
    private Guide guide;




    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void like(){
        this.likes++;
    }
    public void dislike(){
        this.dislikes++;
    }

    public void update(String content){
        this.content = content;
    }


    @Builder
    public Comment(String content, User author, PatchNote patchNote){
        this.content = content;
        this.author = author;
        this.patchNote = patchNote;
    }
    @Builder
    public Comment(String content, User author, Guide guide){
        this.content = content;
        this.author = author;
        this.guide = guide;
    }

    public static Comment of(PatchNote patchNote, String content, User author){
        return Comment.builder()
                .patchNote(patchNote)
                .content(content)
                .author(author)
                .build();
    }

}
