package com.arcane.Arcane.riot.Match.service;

import com.arcane.Arcane.riot.Data.Champion.ChampionService;
import com.arcane.Arcane.riot.Match.domain.Match;
import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import com.arcane.Arcane.riot.Match.repository.MatchRepository;
import com.arcane.Arcane.riot.RiotInform.service.RiotApiService;
import com.arcane.Arcane.riot.summoner.domain.Summoner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchServiceTest {

    @Test
    void repairsMatchWhenParticipantPuuidsAreDuplicated() {
        RiotApiService riotApiService = mock(RiotApiService.class);
        MatchTimelineService matchTimelineService = mock(MatchTimelineService.class);
        ChampionService championService = mock(ChampionService.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        MatchWriteService matchWriteService = mock(MatchWriteService.class);

        MatchService service = new MatchService(
                riotApiService,
                matchTimelineService,
                championService,
                matchRepository,
                matchWriteService
        );

        List<MatchParticipant> duplicatedParticipants = IntStream.range(0, 10)
                .mapToObj(index -> participant("same-puuid"))
                .toList();
        List<MatchParticipant> refreshedParticipants = IntStream.range(0, 10)
                .mapToObj(index -> participant("puuid-" + index))
                .toList();

        Match existingMatch = Match.builder()
                .matchId("KR_TEST_MATCH")
                .participants(duplicatedParticipants)
                .build();
        Match incomingMatch = Match.builder()
                .matchId("KR_TEST_MATCH")
                .participants(refreshedParticipants)
                .build();
        Match repairedMatch = Match.builder()
                .matchId("KR_TEST_MATCH")
                .participants(refreshedParticipants)
                .build();

        when(matchRepository.findMatchByMatchId("KR_TEST_MATCH"))
                .thenReturn(Optional.of(existingMatch));
        when(matchWriteService.repairParticipants(existingMatch, refreshedParticipants))
                .thenReturn(repairedMatch);

        Match result = service.saveIfAbsent(incomingMatch);

        assertThat(result).isSameAs(repairedMatch);
        verify(matchWriteService).repairParticipants(existingMatch, refreshedParticipants);
    }

    private MatchParticipant participant(String puuid) {
        Summoner summoner = Summoner.builder()
                .puuid(puuid)
                .build();
        return MatchParticipant.builder()
                .summoner(summoner)
                .build();
    }
}
