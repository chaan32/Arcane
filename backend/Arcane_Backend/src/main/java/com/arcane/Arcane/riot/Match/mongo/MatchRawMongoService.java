package com.arcane.Arcane.riot.Match.mongo;

import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.riot.Match.dto.MatchDto;
import com.arcane.Arcane.riot.Match.dto.ParticipantDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchRawMongoService {

    private final MatchRawDocumentRepository matchRawDocumentRepository;
    private final ObjectMapper objectMapper;

    public void saveParticipants(String matchId, MatchDto matchDto) {
        if (matchDto == null || matchDto.getInfo() == null || matchDto.getInfo().getParticipants() == null) {
            log.warn(logMessage("saveParticipants", "스킵", "matchId=" + matchId + " | reason=match_or_participants_null"));
            return;
        }

        List<ParticipantDto> participants = matchDto.getInfo().getParticipants();

        for (int index = 0; index < participants.size(); index++) {
            ParticipantDto participant = participants.get(index);
            if (participant.getPuuid() == null || participant.getPuuid().isBlank()) {
                log.warn(logMessage(
                        "saveParticipants",
                        "스킵",
                        "matchId=" + matchId + " | participantIndex=" + index + " | reason=puuid_blank"
                ));
                continue;
            }

            saveOne(matchId, matchDto, participant, index);
        }
        log.info(logMessage("saveParticipants", "완료", "matchId=" + matchId + " | count=" + participants.size()));
    }

    private void saveOne(String matchId, MatchDto matchDto, ParticipantDto participantDto, int index) {
        try{
            // 식별값 생성
            String documentId = MatchParticipantRawDocument.makeId(matchId, participantDto);

            Map<String, Object> participantPayload = objectMapper.convertValue(participantDto, new TypeReference<>() {});

            MatchParticipantRawDocument document = matchRawDocumentRepository.findById(documentId)
                    .map(existing -> existing.updateFrom(matchDto, participantDto, index, participantPayload))
                    .orElseGet(() -> MatchParticipantRawDocument.create(
                            matchId,
                            matchDto,
                            participantDto,
                            index,
                            participantPayload
                    ));

            matchRawDocumentRepository.save(document);
        } catch (RuntimeException e) {
            log.warn(logMessage(
                            "saveOne",
                            "실패",
                            "matchId=" + matchId
                                    + " | puuid=" + participantDto.getPuuid()
                                    + " | reason=" + e.getMessage()
                    ),
                    e
            );
        }
    }

    private String logMessage(String method, String status, String detail) {
        return ApiLogSupport.api("Mongo 참가자 매치 저장", "MatchRawMongoService." + method, status, detail);
    }
}
