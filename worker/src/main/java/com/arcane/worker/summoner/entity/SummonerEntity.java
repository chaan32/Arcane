package com.arcane.worker.summoner.entity;

import com.arcane.worker.ranker.dto.RiotRankerDto;
import com.arcane.worker.ranker.tier.Tier;
import com.arcane.worker.riot.dto.ProfileResDto;
import com.arcane.worker.riot.dto.RiotAccountDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "summoner",
        uniqueConstraints = { //unique 제약조건 추가
                @UniqueConstraint(
                        name = "uk_summoner_puuid",
                        columnNames = "puuid"
                )
        },
        indexes = { //인덱스 추가하기
                @Index(
                        name = "idx_summoner_name_tag",
                        columnList = "trimmed_game_name, tag_line"
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummonerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "puuid", length = 100, nullable = false)
    private String puuid;

    @Column(name = "game_name")
    private String gameName;

    @Column(name = "tag_line")
    private String tagLine;

    @Column(name = "trimmed_game_name")
    private String trimmedGameName;

    @Column(name = "solo_rank_tier", length = 20)
    private String soloRankTier;

    @Column(name = "solo_rank_lp")
    private Integer soloRankLP;

    @Column(name = "solo_rank_win")
    private Integer soloRankWin;

    @Column(name = "solo_rank_defeat")
    private Integer soloRankDefeat;

    @Column(name = "icon")
    private Integer iconId;

    @Column(name = "level")
    private Integer level;

    @Column(name = "create_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at")
    private LocalDateTime updatedAt;

    public SummonerEntity(RiotAccountDto dto) {
        this.gameName = dto.gameName();
        this.trimmedGameName = dto.gameName().replace(" ", "");
        this.tagLine = dto.tagLine();
        this.puuid = dto.puuid();
    }

    public SummonerEntity updateTier(RiotRankerDto rankerDto) {
        this.soloRankLP = rankerDto.getLeaguePoints();
        this.soloRankWin = rankerDto.getWins();
        this.soloRankDefeat = rankerDto.getLosses();
        return this;
    }

    public void setSoloRankTier(Tier tier) {
        this.soloRankTier = tier.getKey();
    }

    public void updateProfile(ProfileResDto profileResDto) {
        if (profileResDto == null) {
            return;
        }

        this.iconId = profileResDto.profileIconId();
        this.level = profileResDto.summonerLevel();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
