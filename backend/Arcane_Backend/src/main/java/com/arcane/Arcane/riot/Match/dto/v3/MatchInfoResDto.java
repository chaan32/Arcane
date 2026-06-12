package com.arcane.Arcane.riot.Match.dto.v3;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchInfoResDto {
    private MetaDataResDto metaData;
    private MyDataResDto myData;
    private ParticipantsResDto participants;
    public static MatchInfoResDto of (MetaDataResDto metaData, MyDataResDto myData, ParticipantsResDto participants) {
        return MatchInfoResDto.builder()
                .metaData(metaData)
                .myData(myData)
                .participants(participants)
                .build();
    }
}
