package com.arcane.Arcane.riot.Data.Champion;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable; // ★ 이거 임포트 필수!!

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// ★ 1. Persistable 인터페이스 구현 선언
public class Champion implements Persistable<Long> {

    @Id
    private Long id; // 라이엇 ID

    private String nameEn;
    private String nameKo;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String blurb;

    private String imageFull;
    private String tags;

    private String version;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Embedded
    private ChampionInfo info;

    @Embedded
    private ChampionStats stats;

    @OneToMany(mappedBy = "champion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChampionSpell> spells = new ArrayList<>();

    @OneToMany(mappedBy = "champion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChampionSkin> skins = new ArrayList<>();

    @Embedded
    private ChampionPassive passive;

    // --- [★ 2. 여기가 제일 중요합니다] ---

    @Transient // DB 컬럼 아님
    private boolean isNew = true; // 기본값을 true로 설정

    @Override
    public Long getId() {
        return id;
    }

    // "나는 ID가 있어도 무조건 새 데이터(INSERT)다" 라고 뻥을 쳐야 자식까지 다 들어갑니다.
    @Override
    public boolean isNew() {
        return isNew;
    }

    // (선택사항) 나중에 조회했을 때는 false로 바꿔주는 로직
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    // --- [연관관계 메서드] ---
    public void addSpell(ChampionSpell spell) {
        this.spells.add(spell);
        spell.setChampion(this);
    }

    public void addSkin(ChampionSkin skin) {
        this.skins.add(skin);
        skin.setChampion(this);
    }
}
