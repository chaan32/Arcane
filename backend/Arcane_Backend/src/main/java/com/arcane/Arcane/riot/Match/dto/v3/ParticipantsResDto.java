package com.arcane.Arcane.riot.Match.dto.v3;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ParticipantsResDto {
    private ParticipantDto player0;
    private ParticipantDto player1;
    private ParticipantDto player2;
    private ParticipantDto player3;
    private ParticipantDto player4;

    private ParticipantDto player5;
    private ParticipantDto player6;
    private ParticipantDto player7;
    private ParticipantDto player8;
    private ParticipantDto player9;



    public void addPlayer0(MatchParticipant participant, Champion champion) {
        this.player0 = ParticipantDto.of(participant, champion);
    }
    public void addPlayer1(MatchParticipant participant, Champion champion) {
        this.player1 = ParticipantDto.of(participant, champion);
    }
    public void addPlayer2(MatchParticipant participant, Champion champion) {
        this.player2 = ParticipantDto.of(participant, champion);
    }
    public void addPlayer3(MatchParticipant participant, Champion champion) {
        this.player3 = ParticipantDto.of(participant, champion);
    }
    public void addPlayer4(MatchParticipant participant, Champion champion) {
        this.player4 = ParticipantDto.of(participant, champion);
    }
    public void addPlayer5(MatchParticipant participant, Champion champion) {
        this.player5 = ParticipantDto.of(participant, champion);
    }
    public void addPlayer6(MatchParticipant participant, Champion champion) {
        this.player6 = ParticipantDto.of(participant, champion);
    }
    public void addPlayer7(MatchParticipant participant, Champion champion) {
        this.player7 = ParticipantDto.of(participant, champion);
    }
    public void addPlayer8(MatchParticipant participant, Champion champion) {
        this.player8 = ParticipantDto.of(participant, champion);
    }
    public void addPlayer9(MatchParticipant participant, Champion champion) {
        this.player9 = ParticipantDto.of(participant, champion);
    }
}
