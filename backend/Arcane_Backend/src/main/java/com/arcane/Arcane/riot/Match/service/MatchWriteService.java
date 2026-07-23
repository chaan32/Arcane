package com.arcane.Arcane.riot.Match.service;

import com.arcane.Arcane.riot.Match.domain.Match;
import com.arcane.Arcane.riot.Match.domain.MatchParticipant;
import com.arcane.Arcane.riot.Match.repository.MatchParticipantRepository;
import com.arcane.Arcane.riot.Match.repository.MatchRepository;
import com.arcane.Arcane.riot.summoner.domain.Summoner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MatchWriteService {
    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Match saveNewMatch(Match newMatch) {
        validateParticipants(newMatch.getParticipants());

        // match_id 기준으로 해당 Match 정보가 있으면 무시 (Ignore)
        matchRepository.insertIgnore(newMatch);

        // 그 다음에 다시 조회하기 (위에서 뭐가 됐든 저장을 하긴 하니깐 무조건 리턴 됨)
        Match savedMatch = matchRepository.findMatchByMatchId(newMatch.getMatchId())
                .orElseThrow(() -> new IllegalStateException(
                        "전적 저장 후 조회할 수 없습니다. matchId=" + newMatch.getMatchId()
                ));

        List<MatchParticipant> savedParticipants = savedMatch.getParticipants();
        if (savedParticipants == null || savedParticipants.size() != 10) {
            return repairParticipants(savedMatch, newMatch.getParticipants());
        }

        return savedMatch;
    }

    @Transactional
    public Match repairParticipants(
            Match existingMatch,
            List<MatchParticipant> refreshedParticipants
    ) {
        validateParticipants(refreshedParticipants);

        List<MatchParticipant> oldParticipants = existingMatch.getParticipants();
        if (oldParticipants != null && !oldParticipants.isEmpty()) {
            matchParticipantRepository.deleteAll(oldParticipants);
            matchParticipantRepository.flush();
        }

        refreshedParticipants.forEach(participant ->
                participant.setMatch(existingMatch)
        );

        matchParticipantRepository.saveAllAndFlush(
                refreshedParticipants
        );

        existingMatch.setParticipants(refreshedParticipants);
        return existingMatch;
    }

    private void validateParticipants(List<MatchParticipant> participants) {
        if (participants == null || participants.size() != 10) {
            throw new IllegalStateException("전적 참가자 데이터가 10명이 아닙니다.");
        }

        long uniquePuuidCount = participants.stream()
                .map(MatchParticipant::getSummoner)
                .filter(Objects::nonNull)
                .map(Summoner::getPuuid)
                .filter(Objects::nonNull)
                .filter(puuid -> !puuid.isBlank())
                .distinct()
                .count();

        if (uniquePuuidCount != 10) {
            throw new IllegalStateException(
                    "전적 참가자의 PUUID가 없거나 중복되었습니다."
            );
        }
    }
}
