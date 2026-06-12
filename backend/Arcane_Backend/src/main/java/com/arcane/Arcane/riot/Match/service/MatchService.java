package com.arcane.Arcane.riot.Match.service;

import com.arcane.Arcane.common.Exception.Normal.CannotFoundChampion;
import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Data.Champion.ChampionService;
import com.arcane.Arcane.riot.Match.domain.Match;
import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import com.arcane.Arcane.riot.Match.dto.MatchDto;
import com.arcane.Arcane.riot.Match.dto.v3.ParticipantsResDto;
import com.arcane.Arcane.riot.Match.repository.MatchParticipantRepository;
import com.arcane.Arcane.riot.Match.repository.MatchRepository;
import com.arcane.Arcane.riot.RiotInform.service.RiotApiService;
import com.arcane.Arcane.riot.summoner.domain.Summoner;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchService {
    private final RiotApiService riotApiService;
    private final MatchTimelineService matchTimelineService;
    private final ChampionService championService;
    private final MatchRepository matchRepository;
    private final MatchWriteService matchWriteService;
    private final ConcurrentMap<String, Object> matchCreationLocks = new ConcurrentHashMap<>();



    public Optional<Match> getMatchByMatchId(String matchId) {
        return matchRepository.findMatchByMatchId(matchId);
    }

    public Match saveIfAbsent(Match newMatch){
        String matchId = newMatch.getMatchId();
        Object lock = matchCreationLocks.computeIfAbsent(matchId, ignored -> new Object());

        synchronized (lock) {
            return matchRepository.findMatchByMatchId(matchId)
                    .orElseGet(() -> {
                        try {
                            return matchWriteService.saveNewMatch(newMatch);
                        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
                            return findMatchAfterConcurrentSave(matchId, e);
                        }
                    });
        }
    }



    public Map<String, List<JsonNode>> findEventsByMatchId(String matchId) {
        return matchTimelineService.getEventsTimelineByMatchId(matchId);
    }

    private Match findMatchAfterConcurrentSave(String matchId, RuntimeException cause) {
        for (int attempt = 0; attempt < 3; attempt++) {
            Optional<Match> match = matchRepository.findMatchByMatchId(matchId);
            if (match.isPresent()) {
                return match.get();
            }
            sleepAfterConcurrentSave(attempt);
        }

        return matchRepository.findMatchByMatchId(matchId)
                .orElseThrow(() -> cause);
    }

    private void sleepAfterConcurrentSave(int attempt) {
        try {
            Thread.sleep(50L * (attempt + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    /** 참여자 정보에 챔피언 정보 넣기
     *
     * @param participants
     * @return
     */
    private ParticipantsResDto addParticipant(List<MatchParticipant> participants) {

        ParticipantsResDto participantsResDto = new ParticipantsResDto();
        Long championId0 = participants.get(0).getChampionId();
        Champion champion0 = championService.getChampionById(championId0).orElseThrow(()->new CannotFoundChampion(championId0+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer0(participants.get(0),champion0);

        Long championId1 = participants.get(1).getChampionId();
        Champion champion1 = championService.getChampionById(championId1).orElseThrow(()->new CannotFoundChampion(championId1+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer1(participants.get(1),champion1);

        Long championId2 = participants.get(2).getChampionId();
        Champion champion2 = championService.getChampionById(championId2).orElseThrow(()->new CannotFoundChampion(championId2+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer2(participants.get(2),champion2);

        Long championId3 = participants.get(3).getChampionId();
        Champion champion3 = championService.getChampionById(championId3).orElseThrow(()->new CannotFoundChampion(championId3+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer3(participants.get(3),champion3);

        Long championId4 = participants.get(4).getChampionId();
        Champion champion4 = championService.getChampionById(championId4).orElseThrow(()->new CannotFoundChampion(championId4+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer4(participants.get(4),champion4);



        Long championId5 = participants.get(5).getChampionId();
        Champion champion5 = championService.getChampionById(championId5).orElseThrow(()->new CannotFoundChampion(championId5+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer5(participants.get(5),champion5);

        Long championId6 = participants.get(6).getChampionId();
        Champion champion6 = championService.getChampionById(championId6).orElseThrow(()->new CannotFoundChampion(championId6+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer6(participants.get(6),champion6);

        Long championId7 = participants.get(7).getChampionId();
        Champion champion7 = championService.getChampionById(championId7).orElseThrow(()->new CannotFoundChampion(championId7+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer7(participants.get(7),champion7);

        Long championId8 = participants.get(8).getChampionId();
        Champion champion8 = championService.getChampionById(championId8).orElseThrow(()->new CannotFoundChampion(championId8+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer8(participants.get(8),champion8);

        Long championId9 = participants.get(9).getChampionId();
        Champion champion9 = championService.getChampionById(championId9).orElseThrow(()->new CannotFoundChampion(championId9+"에 해당하는 챔피언이 없습니다."));
        participantsResDto.addPlayer9(participants.get(9),champion9);

        return participantsResDto;
    }
    private boolean isClassicGame(Match match){
        return match.getGameMode().equals("CLASSIC");
    }
    private boolean isClassicGame(MatchDto match){
        return match.getInfo().getGameMode().equals("CLASSIC");
    }
}
