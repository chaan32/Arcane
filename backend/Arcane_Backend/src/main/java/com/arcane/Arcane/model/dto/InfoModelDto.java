package com.arcane.Arcane.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InfoModelDto {

    @JsonProperty("participants")
    private List<OurParticipantDto> participants;

    @JsonProperty("gameCreation")
    private long gameCreation; // 게임 생성 시각 (타임스탬프)

    @JsonProperty("gameDuration")
    private long gameDuration; // 게임 시간 (초)

    @JsonProperty("gameEndTimestamp")
    private long gameEndTimestamp; // 게임 종료 시각 (타임스탬프)

    @JsonProperty("gameMode")
    private String gameMode; // 게임 모드

    @JsonProperty("queueId")
    private Integer queueId; // 솔랭인지 자랭인지 뭔지 등등
}
