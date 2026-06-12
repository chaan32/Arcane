package com.arcane.Arcane.riot.Match.dto.perks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatPerksDto {
    private int defense;
    private int flex;
    private int offense;
}
