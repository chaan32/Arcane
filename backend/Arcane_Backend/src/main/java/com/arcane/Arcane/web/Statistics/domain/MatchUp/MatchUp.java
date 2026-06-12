package com.arcane.Arcane.web.Statistics.domain.MatchUp;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.web.Statistics.domain.Champion.ChampionStatsByPosition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

// Matchup.java (수정 후)
@Entity
@Getter
@NoArgsConstructor
public class MatchUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 매치업 정보의 주인 (예: 가렌-TOP의 통계 정보)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_stats_id")
    private ChampionStatsByPosition ownerStats; // <-- 참조 타입 변경

    // 상대방 챔피언 (상대방의 포지션은 주인과 같다고 가정)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_champion_id")
    private Champion opponent;


    // 상대 승률
    private Float relativeWinRate;

    // 상대 횟수
    private Integer gamesPlayed;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void setCreatedAt(){
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void setUpdatedAt(){
        this.updatedAt = LocalDateTime.now();
    }
    // 생성자 수정
    public MatchUp(ChampionStatsByPosition ownerStats, Champion opponent, Float relativeWinRate) {
        this.ownerStats = ownerStats;
        this.opponent = opponent;
        this.relativeWinRate = relativeWinRate;
        this.gamesPlayed = ThreadLocalRandom.current().nextInt(25, 1001);
    }

}
