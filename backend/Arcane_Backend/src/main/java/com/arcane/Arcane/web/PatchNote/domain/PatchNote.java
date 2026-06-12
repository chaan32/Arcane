package com.arcane.Arcane.web.PatchNote.domain;


import com.arcane.Arcane.web.User.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatchNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer views;

    @Column(nullable = false)
    private Integer likes;

    @Column(nullable = false)
    private Integer dislikes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author; // 작성자 (User 엔티티와 연결함)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    /// -----------

    @PrePersist
    protected void on() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.views = 0;
        this.likes = 0;
        this.dislikes = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public PatchNote(String title, String content, User author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public void update(String title, String content){
        this.title = title;
        this.content = content;
    }

    public void like(){
        this.likes++;
    }
    public void dislike(){
        this.dislikes++;
    }
    public void view(){
        this.views++;
    }

    public static PatchNote of(String title, String content, User author){
        return PatchNote.builder().title(title).content(content).author(author).build();
    }
}


/*
        AccessLevel이 뭔가요 : 불완전한 객체를 만들지 않도록 하고 빌더를 사용하도록 유도함

        content에 사진도 들어가야 하는데 텍스트로 해도 괜찮을까요 -> S3와 같은 곳에 저장해놔야 함 따라서 경로를 문자열로 저장하는 게 맞다 ㅇㅇ
        <p>이번 패치로 신규 챔피언이 출시되었습니다!</p>
            <img src="https://your-server.com/images/patch-note-image.jpg">
        <p>자세한 내용은 아래를 참고하세요.</p>


        preUpdate와 prePersist는 뭔가요 -> 이벤트가 발생할 떄마다 자동으로 호출되는 것ㅅ들
*/