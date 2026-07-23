package com.arcane.worker.summoner.service;

import com.arcane.worker.ranker.dto.RiotRankerDto;
import com.arcane.worker.ranker.tier.Tier;
import com.arcane.worker.riot.dto.ProfileResDto;
import com.arcane.worker.riot.dto.RiotAccountDto;
import com.arcane.worker.summoner.entity.SummonerEntity;
import com.arcane.worker.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SummonerService {

    private final SummonerRepository summonerRepository;

    public Optional<SummonerEntity> getSummonerByPuuid(String puuid) {
        return summonerRepository.findSummonerEntityByPuuid(puuid);
    }

//    public SummonerEntity saveSummoner(RiotAccountDto riotAccountDto) {
//        return summonerRepository.findSummonerEntityByPuuid(riotAccountDto.puuid())
//                .orElseGet(() -> summonerRepository.save(new SummonerEntity(riotAccountDto)));
//    }
    public SummonerEntity saveSummoner(RiotAccountDto dto){
        if (dto==null || dto.puuid() == null || dto.puuid().isBlank()){
            throw new IllegalArgumentException("puuid가 비어 있습니다.");
        }
        String trimmedGameName = dto.gameName().replace(" ", "");

        summonerRepository.upsertIdentity(
                dto.puuid(), dto.gameName(), trimmedGameName, dto.tagLine()
        );
        return summonerRepository.findSummonerEntityByPuuid(dto.puuid())
                .orElseThrow(() -> new IllegalStateException(
                        "소환사 UPSERT 후 조회에 실패했습니다. puuid=" + dto.puuid()
                ));
    }

    public SummonerEntity updateRankerScore(
            String puuid,
            Tier tier,
            RiotRankerDto rankerDto
    ) {
        Optional<SummonerEntity> optionalSummoner =
                summonerRepository.findSummonerEntityByPuuid(puuid);

        if (optionalSummoner.isEmpty()) {
            return null;
        }

        SummonerEntity summoner = optionalSummoner.get();
        if (shouldUpdateRankInfo(summoner, rankerDto)) {
            summoner.updateTier(rankerDto);
        }

        summoner.setSoloRankTier(tier);
        return summoner;
    }

    private boolean shouldUpdateRankInfo(SummonerEntity summoner, RiotRankerDto rankerDto) {
        return !equalsNullable(summoner.getSoloRankLP(), rankerDto.getLeaguePoints())
                || !equalsNullable(summoner.getSoloRankWin(), rankerDto.getWins())
                || !equalsNullable(summoner.getSoloRankDefeat(), rankerDto.getLosses());
    }

    private boolean equalsNullable(Integer left, Integer right) {
        if (left == null) {
            return right == null;
        }

        return left.equals(right);
    }

    public void updateProfile(String puuid, ProfileResDto profileResDto) {
        if (profileResDto == null) {
            return;
        }

        summonerRepository
                .findSummonerEntityByPuuid(puuid)
                .ifPresent(summoner -> summoner.updateProfile(profileResDto));
    }
}
