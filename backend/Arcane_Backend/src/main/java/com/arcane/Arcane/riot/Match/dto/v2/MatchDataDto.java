package com.arcane.Arcane.riot.Match.dto.v2;

import com.arcane.Arcane.riot.Match.domain.Match;
import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDataDto {

    // part 1 : 게임에 대한 정보
    private String matchId;
    private long gameCreation; // 게임 생성 시각 (타임스탬프)
    private long gameDuration; // 게임 시간 (초)
    private long gameEndTimestamp; // 게임 종료 시각 (타임스탬프)
    private String gameMode; // 게임 모드


    private PlayerDataDto player01;
    private PlayerDataDto player02;
    private PlayerDataDto player03;
    private PlayerDataDto player04;
    private PlayerDataDto player05;


    private PlayerDataDto player06;
    private PlayerDataDto player07;
    private PlayerDataDto player08;
    private PlayerDataDto player09;
    private PlayerDataDto player10;

    public static MatchDataDto of(List<MatchParticipant> players) {
        return MatchDataDto.builder()
                .player01(PlayerDataDto.of(players.get(0)))
                .player02(PlayerDataDto.of(players.get(1)))
                .player03(PlayerDataDto.of(players.get(2)))
                .player04(PlayerDataDto.of(players.get(3)))
                .player05(PlayerDataDto.of(players.get(4)))
                .player06(PlayerDataDto.of(players.get(5)))
                .player07(PlayerDataDto.of(players.get(6)))
                .player08(PlayerDataDto.of(players.get(7)))
                .player09(PlayerDataDto.of(players.get(8)))
                .player10(PlayerDataDto.of(players.get(9)))
                .build();
    }
    public void setMatchInform(Match match){
        this.matchId = match.getMatchId();
        this.gameCreation = match.getGameCreation();
        this.gameDuration = match.getGameDuration();
        this.gameEndTimestamp = match.getGameEndTimestamp();
        this.gameMode = match.getGameMode();
    }
}
