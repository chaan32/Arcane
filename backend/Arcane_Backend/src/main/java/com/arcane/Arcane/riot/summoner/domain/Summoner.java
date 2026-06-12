package com.arcane.Arcane.riot.summoner.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.arcane.Arcane.riot.Match.domain.MatchParticipant;

import com.arcane.Arcane.riot.Match.dto.ParticipantDto;

import com.arcane.Arcane.riot.Ranker.domain.Tier;
import com.arcane.Arcane.riot.Ranker.dto.RiotRankerDto;
import com.arcane.Arcane.riot.RiotInform.dto.ProfileResDto;
import com.arcane.Arcane.riot.RiotInform.dto.Ranker.ChallengerDto;

import com.arcane.Arcane.riot.RiotInform.dto.RiotAccountDto;
import com.arcane.Arcane.riot.summoner.dto.SummonerDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes =
        {
                @Index(name = "summoner_puuid", columnList = "puuid"),
                @Index(name = "summoner_name", columnList = "trimmed_game_name, tag_line")
        })
public class Summoner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trimmed_game_name", length = 100, nullable = false)
    private String trimmedGameName;

    @Column(name = "game_name", length = 100, nullable = false)
    private String gameName;

    @Column(name="tag_line", length = 20, nullable = false)
    private String tagLine;

    @Column(name = "puuid", length = 100, nullable = false, unique = true)
    private String puuid;

    @Column(name = "icon")
    private Integer iconId;
    @Column(name = "level")
    private Integer level;

    // 솔랭 정보
    @Column(name = "solo_rank_tier", length = 20)
    private String soloRankTier;

    @Column(name = "solo_rank_lp")
    private Integer soloRankLP;

    @Column(name = "solo_rank_win")
    private Integer soloRankWin;

    @Column(name = "solo_rank_defeat")
    private Integer soloRankDefeat;

    // 자랭 정보
    @Column(name = "flexRankTier", length = 20)
    private String flexRankTier;

    @Column(name = "flexRankLP")
    private Integer flexRankLP;

    @Column(name = "flex_rank_win")
    private Integer flexRankWin;

    @Column(name = "flex_rank_defeat")
    private Integer flexRankDefeat;

    @Column(name = "create_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "summoner")
    @JsonIgnore
    private List<MatchParticipant> matchHistory;



    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public static Summoner update(SummonerDto dto) {
        return Summoner.builder()
                .gameName(dto.getGameName())
                .tagLine(dto.getTagLine())
                .puuid(dto.getPuuid())
                .soloRankTier(dto.getSoloRankTier())
                .soloRankLP(dto.getSoloRankLP())
                .soloRankWin(dto.getSoloRankWin())
                .soloRankDefeat(dto.getSoloRankDefeat())
                .flexRankTier(dto.getFlexRankTier())
                .flexRankLP(dto.getFlexRankLP())
                .flexRankWin(dto.getFlexRankWin())
                .flexRankDefeat(dto.getFlexRankDefeat())
                .trimmedGameName(dto.getGameName().replace(" ",""))
                .build();
    }
    public Summoner updateTier(SummonerDto dto){
        this.soloRankTier = dto.getSoloRankTier();
        this.soloRankLP = dto.getSoloRankLP();
        this.soloRankWin = dto.getSoloRankWin();
        this.soloRankDefeat = dto.getSoloRankDefeat();
        this.flexRankTier = dto.getFlexRankTier();
        this.flexRankLP = dto.getFlexRankLP();
        this.flexRankWin = dto.getFlexRankWin();
        this.flexRankDefeat = dto.getFlexRankDefeat();
        this.gameName = dto.getGameName();
        this.tagLine = dto.getTagLine();
        this.trimmedGameName = dto.getGameName().replace(" ", "");
        return this;
    }

    public Summoner updateTier(ChallengerDto dto) {
        this.soloRankTier = "challenger";
        this.soloRankLP = dto.getLeaguePoints();
        this.soloRankWin = dto.getWins();
        this.soloRankDefeat = dto.getLosses();
        return this;
    }
    public Summoner updateTier(RiotRankerDto dto) {
        this.soloRankLP = dto.getLeaguePoints();
        this.soloRankWin = dto.getWins();
        this.soloRankDefeat = dto.getLosses();

        // 참고: 챌린저/그마/마스터는 Ranker 테이블에서 Tier(Enum)로 관리하므로
        // Summoner 테이블의 soloRankTier 문자열 컬럼은
        // 굳이 업데이트하지 않거나, 필요하다면 "Challenger" 등으로 별도 파라미터를 받아야 합니다.

        return this;
    }

    public Summoner updateTierV2(RiotRankerDto rankerResDto, Tier tier) {
        this.soloRankTier = tier.getKey();
        this.soloRankLP = rankerResDto.getLeaguePoints();
        this.soloRankWin = rankerResDto.getWins();
        this.soloRankDefeat = rankerResDto.getLosses();
        return this;
    }

    public Summoner (String gameName, String tagLine, String puuid) {
        this.trimmedGameName = gameName.replace(" ","");
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.puuid = puuid;
    }
    public Summoner (RiotRankerDto ranker, String gameName, String tagLine, Tier tier){
        this.puuid = ranker.getPuuid();
        this.gameName = gameName;
        this.trimmedGameName = gameName.replace(" ","");
        this.tagLine = tagLine;
        this.soloRankDefeat = ranker.getLosses();
        this.soloRankWin = ranker.getWins();
        this.soloRankLP = ranker.getLeaguePoints();
        this.soloRankTier = tier.getKey();

    }

    public Summoner(ParticipantDto dto) {
        this.gameName = dto.getRiotIdGameName();
        this.trimmedGameName = dto.getRiotIdGameName().replace(" ","");
        this.tagLine = dto.getRiotIdTagline();
        this.puuid = dto.getPuuid();
    }
    public Summoner(RiotAccountDto dto) {
        this.gameName = dto.getGameName();
        this.trimmedGameName = dto.getGameName().replace(" ","");
        this.tagLine = dto.getTagLine();
        this.puuid = dto.getPuuid();
    }

    public void setSoloRankTier(Tier tier){
        this.soloRankTier = tier.getKey();
    }

    public void updateProfile(ProfileResDto profileResDto){
        this.iconId = profileResDto.getProfileIconId();
        this.level = profileResDto.getSummonerLevel();
    }

    public void markRefreshed() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateIdentity(RiotAccountDto dto){
        this.gameName = dto.getGameName();
        this.trimmedGameName = dto.getGameName().replace(" ", "");
        this.tagLine = dto.getTagLine();
    }

    public void updateIdentity(ParticipantDto dto) {
        this.gameName = dto.getRiotIdGameName();
        this.trimmedGameName = dto.getRiotIdGameName().replace(" ","");
        this.tagLine = dto.getRiotIdTagline();
    }


}
