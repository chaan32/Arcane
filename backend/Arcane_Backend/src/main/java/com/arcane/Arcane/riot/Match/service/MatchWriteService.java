package com.arcane.Arcane.riot.Match.service;

import com.arcane.Arcane.riot.Match.domain.Match;
import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import com.arcane.Arcane.riot.Match.repository.MatchParticipantRepository;
import com.arcane.Arcane.riot.Match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchWriteService {
    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Match saveNewMatch(Match newMatch){

        // match_id 기준으로 해당 Match 정보가 있으면 무시 (Ignore)
        matchRepository.insertIgnore(newMatch);

        // 그 다음에 다시 조회하기 (위에서 뭐가 됐든 저장을 하긴 하니깐 무조건 리턴 됨)
        Match savedMatch = matchRepository.findMatchByMatchId(newMatch.getMatchId())
                .orElseThrow();

        if (savedMatch.getParticipants().isEmpty()) {
            List<MatchParticipant> participants = newMatch.getParticipants();
            if (participants != null && !participants.isEmpty()) {
                participants.forEach(participant -> participant.setMatch(savedMatch));
                matchParticipantRepository.saveAllAndFlush(participants);
                savedMatch.setParticipants(participants);
            }
        }

        return savedMatch;
    }

}
