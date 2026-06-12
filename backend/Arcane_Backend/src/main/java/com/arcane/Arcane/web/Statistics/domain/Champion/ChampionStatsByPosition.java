package com.arcane.Arcane.web.Statistics.domain.Champion;


import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Match.dto.perks.PerksDto;
import com.arcane.Arcane.riot.Match.dto.perks.PerksDtoConverter;
import com.arcane.Arcane.web.Statistics.domain.MatchUp.MatchUp;
import com.arcane.Arcane.web.Statistics.domain.Position;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Getter
@NoArgsConstructor
@Table(uniqueConstraints = { // champion_id와 position의 조합이 항상 고유하도록 제약조건 설정
        @UniqueConstraint(columnNames = {"champion_id", "position"})
})
public class ChampionStatsByPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 챔피언 정보인가?
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "champion_id")
    private Champion champion;

    // 어떤 포지션의 대한 정보인지?
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Column(name = "tier")
    private Integer tier;

    @Column(name = "score")
    private Integer score;

    @Column(name = "pick_rate")
    private Float pickRate;

    @Column(name = "win_rate")
    private Float winRate;

    @Column(name = "ban_rate")
    private Float banRate;

    @OneToMany(mappedBy = "ownerStats", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MatchUp> matchUps = new HashSet<>();

    @Column(name = "total_games_played")
    private Integer totalGamesPlayed = 0;

    private Integer summoner1Id;
    private Integer summoner2Id;
    private Integer summoner3Id;
    private Integer summoner4Id;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = PerksDtoConverter.class)
    private PerksDto perks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MatchUp addMatchUp(Champion opponent, Float winRate) {
        MatchUp newMatchUp = new MatchUp(this, opponent, winRate);
        totalGamesPlayed+= newMatchUp.getGamesPlayed();
        this.matchUps.add(newMatchUp);
        return newMatchUp;
    }

    @PrePersist
    public void setCreatedAt(){
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void setUpdatedAt(){
        this.updatedAt = LocalDateTime.now();
    }
    public ChampionStatsByPosition(Champion champion, Position position) {
        this.champion = champion;
        this.position = position;
        this.tier = ThreadLocalRandom.current().nextInt(1,6);
        this.score = ThreadLocalRandom.current().nextInt(20,75);
        this.pickRate = ThreadLocalRandom.current().nextFloat((float)5.1, (float)55.29);
        this.winRate = ThreadLocalRandom.current().nextFloat((float)44.0, (float)59.8);
        this.banRate = ThreadLocalRandom.current().nextFloat((float)1.0, (float)20.8);
        this.perks = PerksDto.createMock();
        this.summoner1Id = 4;
        this.summoner2Id = 7;
        this.summoner3Id = 4;
        this.summoner4Id = 12;
    }
}
