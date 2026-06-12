package com.arcane.Arcane.riot.Match.service;

import com.arcane.Arcane.riot.RiotInform.service.RiotApiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchTimelineService {
    private final RiotApiService riotApiService;
    private final ObjectMapper objectMapper;

    public List<JsonNode> getEventsTimelineByPuuid(String matchId, String targetPuuid) {
        // 1. 순수 원본 JSON String 가져오기
        String matchTimeline = riotApiService.getMatchTimeline(matchId);
        List<JsonNode> userEvents = new ArrayList<>();

        if (matchTimeline == null || matchTimeline.isEmpty()) {
            log.warn("매치 타임라인 데이터를 가져오지 못했습니다. matchId: {}", matchId);
            return userEvents;
        }

        try {
            // 2. String JSON을 다루기 쉬운 JsonNode(트리 구조)로 파싱
            JsonNode rootNode = objectMapper.readTree(matchTimeline);

            // 3. metadata > participants 배열에서 targetPuuid의 인덱스 찾아 Participant ID 구하기 (인덱스 + 1)
            int participantId = -1;
            JsonNode participantsArray = rootNode.path("metadata").path("participants");

            for (int i = 0; i < participantsArray.size(); i++) {
                if (participantsArray.get(i).asText().equals(targetPuuid)) {
                    participantId = i + 1;
                    break;
                }
            }

            if (participantId == -1) {
                log.warn("해당 PUUID({})를 가진 유저가 매치({})에 존재하지 않습니다.", targetPuuid, matchId);
                return userEvents; // 빈 리스트 반환
            }

            // 4. info > frames를 순회하며 해당 Participant ID가 관여한 이벤트만 수집
            JsonNode frames = rootNode.path("info").path("frames");

            for (JsonNode frame : frames) {
                JsonNode events = frame.path("events");
                for (JsonNode event : events) {
                    if (isUserInvolved(event, participantId)) {
                        userEvents.add(event);
                    }
                }
            }

            log.info("PUUID: {} 의 이벤트 총 {}개 추출 완료", targetPuuid, userEvents.size());

        } catch (JsonProcessingException e) {
            log.error("타임라인 JSON 파싱 중 오류 발생: {}", e.getMessage());
        }

        // 최종적으로 정제된 이벤트 리스트 반환
        return userEvents;
    }

    public Map<String, List<JsonNode>> getEventsTimelineByMatchId(String matchId) {
        String matchTimeline = riotApiService.getMatchTimeline(matchId);
        Map<String, List<JsonNode>> eventsByPuuid = new LinkedHashMap<>();

        if (matchTimeline == null || matchTimeline.isEmpty()) {
            log.warn("매치 타임라인 데이터를 가져오지 못했습니다. matchId: {}", matchId);
            return eventsByPuuid;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(matchTimeline);
            JsonNode participantsArray = rootNode.path("metadata").path("participants");

            for (JsonNode participantPuuid : participantsArray) {
                eventsByPuuid.put(participantPuuid.asText(), new ArrayList<>());
            }

            JsonNode frames = rootNode.path("info").path("frames");

            for (JsonNode frame : frames) {
                JsonNode events = frame.path("events");
                for (JsonNode event : events) {
                    int participantId = event.path("participantId").asInt(-1);

                    if (participantId < 1 || participantId > participantsArray.size()) {
                        continue;
                    }

                    String puuid = participantsArray.get(participantId - 1).asText();
                    eventsByPuuid.computeIfAbsent(puuid, ignored -> new ArrayList<>()).add(event);
                }
            }

            log.info("matchId: {} 의 참가자별 타임라인 이벤트 추출 완료", matchId);
        } catch (JsonProcessingException e) {
            log.error("타임라인 JSON 파싱 중 오류 발생: {}", e.getMessage());
        }

        return eventsByPuuid;
    }

    private boolean isUserInvolved(JsonNode event, int participantId) {
        // 이벤트 타입별로 주체를 나타내는 필드명이 다르기 때문에 모두 검사합니다.

        // 아이템 구매, 레벨업 등 단일 주체
        if (event.path("participantId").asInt(-1) == participantId) return true;
        // 와드 설치 등
        if (event.path("creatorId").asInt(-1) == participantId) return true;
        // 킬, 건물 파괴 등
        if (event.path("killerId").asInt(-1) == participantId) return true;
        // 사망 피격자
        if (event.path("victimId").asInt(-1) == participantId) return true;

        // 킬 어시스트 (배열 형태)
        JsonNode assistIds = event.path("assistingParticipantIds");
        if (assistIds.isArray()) {
            for (JsonNode idNode : assistIds) {
                if (idNode.asInt() == participantId) {
                    return true;
                }
            }
        }

        return false;
    }
}
