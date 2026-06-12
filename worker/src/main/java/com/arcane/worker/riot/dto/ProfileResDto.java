package com.arcane.worker.riot.dto;

public record ProfileResDto(
        Integer profileIconId,
        Integer summonerLevel
) {
    public static ProfileResDto of(ProfileDto profileDto) {
        if (profileDto == null) {
            return null;
        }

        return new ProfileResDto(
                profileDto.getProfileIconId(),
                profileDto.getSummonerLevel()
        );
    }
}
