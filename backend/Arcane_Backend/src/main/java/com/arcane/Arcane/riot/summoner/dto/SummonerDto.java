package com.arcane.Arcane.riot.summoner.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummonerDto {
    private Long id;

    private String gameName;


    private String tagLine;


    private String puuid;

    // 솔랭 정보
    private String soloRankTier;
    private Integer soloRankLP;
    private Integer soloRankWin;
    private Integer soloRankDefeat;

    // 자랭 정보
    private String flexRankTier;
    private Integer flexRankLP;
    private Integer flexRankWin;
    private Integer flexRankDefeat;

    public static SummonerDto setInform(String gameName, String tagLine, String puuid){
        return SummonerDto.builder()
                .gameName(gameName)
                .tagLine(tagLine)
                .puuid(puuid)
                .build();
    }
    public void test(){
        this.flexRankLP--;
    }

    public SummonerDto(String puuid) {
        this.puuid = puuid;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request{
        private String gameName;
        private String tagLine;
        private String trimmedGameName;
    }
}
