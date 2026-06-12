package com.arcane.Arcane.riot.Match.dto.minimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinimalInfoDto {
    @JsonProperty("participants")
    private List<MinimalParticipantDto> participants;

    // 이 필드를 추가합니다.
    @JsonProperty("gameDuration")
    private long gameDuration; // 게임 시간 (초 단위)
}