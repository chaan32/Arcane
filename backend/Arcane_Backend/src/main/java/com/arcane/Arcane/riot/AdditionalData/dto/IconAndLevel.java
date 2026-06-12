package com.arcane.Arcane.riot.AdditionalData.dto;

import com.arcane.Arcane.riot.RiotInform.dto.ProfileResDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IconAndLevel {
    private Integer id;
    private String mainPuuid;
    private String puuid1;
    private String puuid2;
    private String puuid3;
    private String puuid4;
    private String puuid5;
    private Integer iconId;
    private Integer level;
    private Integer lp;
    private int type;

    public IconAndLevel(int id){
        this.id = id;
    }
    public IconAndLevel(int id, String puuid, int lp){
        this.id = id;
        this.mainPuuid = puuid;
        this.lp = lp;
    }
    public void setProfile(ProfileResDto profile){
        this.iconId = profile.getProfileIconId();
        this.level = profile.getSummonerLevel();
    }
}
