package com.arcane.Arcane.riot.Match.dto.v4;

import com.arcane.Arcane.riot.Match.dto.v3.MetaDataResDto;
import com.arcane.Arcane.riot.Match.dto.v3.MyDataResDto;
import com.arcane.Arcane.riot.Match.dto.v3.ParticipantsResDto;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class MinimalMatchResDto {
    private MetaDataResDto metaData;
    private List<MinimalParticipantsResDto> participants;
    private MinimalMyDataResDto myData;

    public static MinimalMatchResDto of(MetaDataResDto metaDataResDto, ParticipantsResDto participantsResDto, MyDataResDto myDataResDto){

        List<MinimalParticipantsResDto> participantsResDtos = new ArrayList<>();
        participantsResDtos.add(MinimalParticipantsResDto.of(participantsResDto.getPlayer0()));
        participantsResDtos.add(MinimalParticipantsResDto.of(participantsResDto.getPlayer1()));
        participantsResDtos.add(MinimalParticipantsResDto.of(participantsResDto.getPlayer2()));
        participantsResDtos.add(MinimalParticipantsResDto.of(participantsResDto.getPlayer3()));
        participantsResDtos.add(MinimalParticipantsResDto.of(participantsResDto.getPlayer4()));
        participantsResDtos.add(MinimalParticipantsResDto.of(participantsResDto.getPlayer5()));
        participantsResDtos.add(MinimalParticipantsResDto.of(participantsResDto.getPlayer6()));
        participantsResDtos.add(MinimalParticipantsResDto.of(participantsResDto.getPlayer7()));
        participantsResDtos.add(MinimalParticipantsResDto.of(participantsResDto.getPlayer8()));
        participantsResDtos.add(MinimalParticipantsResDto.of(participantsResDto.getPlayer9()));

        return MinimalMatchResDto.builder()
                .metaData(metaDataResDto)
                .myData(MinimalMyDataResDto.of(myDataResDto))
                .participants(participantsResDtos)
                .build();
    }

}
