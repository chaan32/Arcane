package com.arcane.Arcane.riot.Match.dto.perks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class SelectionDto {
    private int perk; // 룬의 고유 ID
    private int var1;
    private int var2;
    private int var3;
}
