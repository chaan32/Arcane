package com.arcane.Arcane.web.Statistics.dto;

import com.arcane.Arcane.web.Statistics.dto.detail.ChampionDetailBuild;
import com.arcane.Arcane.web.Statistics.dto.detail.ChampionDetailChampInfo;
import com.arcane.Arcane.web.Statistics.dto.detail.ChampionDetailList;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChampionDetailDto {
    private ChampionDetailChampInfo detailChampInfo;
    private ChampionDetailBuild detailChampBuild;
    private ChampionDetailList relativeWinRate;
    private ChampionDetailList synergyChampion;

    public ChampionDetailDto(ChampionDetailChampInfo detailChampInfo,
                             ChampionDetailBuild detailChampBuild,
                             ChampionDetailList relativeWinRate,
                             ChampionDetailList synergyChampion) {
        this.detailChampInfo = detailChampInfo;
        this.detailChampBuild = detailChampBuild;
        this.relativeWinRate = relativeWinRate;
        this.synergyChampion = synergyChampion;
    }
}
