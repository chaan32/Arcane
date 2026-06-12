package com.arcane.Arcane.web.Guide.domain;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.web.User.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Builder로 객체 생성 유도
public class Guide {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_id", nullable = false)
    private Champion champion;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void on() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.views = 0;
        this.likes = 0;
        this.dislikes = 0;
    }

    // 엔티티 업데이트 직전에 호출됨
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Guide(String title, String content,Champion champion, User author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.champion = champion;
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

}
