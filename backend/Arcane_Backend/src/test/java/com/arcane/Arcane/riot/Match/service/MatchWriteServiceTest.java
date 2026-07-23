package com.arcane.Arcane.riot.Match.service;

import com.arcane.Arcane.riot.Match.domain.Match;
import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import com.arcane.Arcane.riot.Match.repository.MatchParticipantRepository;
import com.arcane.Arcane.riot.Match.repository.MatchRepository;
import com.arcane.Arcane.riot.summoner.domain.Summoner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MatchWriteServiceTest {

    @Test
    void rejectsDuplicatedParticipantPuuidsBeforeWritingMatch() {
        MatchRepository matchRepository = mock(MatchRepository.class);
        MatchParticipantRepository participantRepository =
                mock(MatchParticipantRepository.class);
        MatchWriteService service = new MatchWriteService(
                matchRepository,
                participantRepository
        );

        List<MatchParticipant> participants = IntStream.range(0, 10)
                .mapToObj(index -> participant("same-puuid"))
                .toList();
        Match match = Match.builder()
                .matchId("KR_TEST_MATCH")
                .participants(participants)
                .build();

        assertThatThrownBy(() -> service.saveNewMatch(match))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUUID");

        verifyNoInteractions(matchRepository, participantRepository);
    }

    private MatchParticipant participant(String puuid) {
        return MatchParticipant.builder()
                .summoner(Summoner.builder().puuid(puuid).build())
                .build();
    }
}
