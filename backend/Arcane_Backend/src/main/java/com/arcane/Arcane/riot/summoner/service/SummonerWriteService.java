package com.arcane.Arcane.riot.summoner.service;

import com.arcane.Arcane.riot.summoner.domain.Summoner;
import com.arcane.Arcane.riot.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummonerWriteService {
    private final SummonerRepository summonerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Summoner saveAndFlush(Summoner summoner) {
        return summonerRepository.saveAndFlush(summoner);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Summoner insertIgnoreAndFind(Summoner summoner) {
        summonerRepository.insertIgnore(summoner);
        return summonerRepository.findSummonerByPuuid(summoner.getPuuid())
                .orElseThrow();
    }
}
