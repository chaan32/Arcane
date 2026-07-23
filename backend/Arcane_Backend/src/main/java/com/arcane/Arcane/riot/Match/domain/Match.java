package com.arcane.Arcane.riot.Match.domain;

import com.arcane.Arcane.riot.Match.dto.MatchDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "match_info")
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    // part 1 : 게임에 대한 정보
    @Column(name = "match_id", nullable = false, unique = true)
    private String matchId;

    @Column(name = "game_creation", nullable = false)
    private long gameCreation; // 게임 생성 시각 (타임스탬프)

    @Column(name = "game_duration", nullable = false)
    private long gameDuration; // 게임 시간 (초)

    @Column(name = "game_end_timestamp", nullable = false)
    private long gameEndTimestamp; // 게임 종료 시각 (타임스탬프)

    @Column(name = "game_mode", nullable = false)
    private String gameMode; // 게임 모드

    @Column(name = "game_version")
    private String gameVersion; // Riot Data Dragon 기준 게임 버전

    @Column(name = "queue_id", nullable = false)
    private Integer queueId;

    // 지금 이 매치에 참여자들의 기록 리스트
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @Column(nullable = false)
    private List<MatchParticipant> participants;

    public Match(MatchDto matchDto, List<MatchParticipant> participants){
        this.matchId = matchDto.getMetadata().getMatchId();
    }
    public void setParticipants(List<MatchParticipant> participants){
        this.participants = participants;
    }

    public MatchParticipant getMyDataByPuuid(String puuid) {
        for (MatchParticipant participant : participants) {
            if (participant.getSummoner().getPuuid().equals(puuid)) {
                return participant;
            }
        }
        return null;
    }
    public boolean hasCompleteParticipantsFor(String puuid) {
        return participants != null
                && participants.size() == 10
                && participants.stream()
                .anyMatch(participant ->
                        participant.getSummoner() != null
                                && puuid.equals(participant.getSummoner().getPuuid())
                );
    }
}
