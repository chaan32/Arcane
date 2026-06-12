package com.arcane.Arcane.riot.Match.mongo;

import com.arcane.Arcane.riot.Match.dto.InfoDto;
import com.arcane.Arcane.riot.Match.dto.MatchDto;
import com.arcane.Arcane.riot.Match.dto.ParticipantDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document(collection = "riot_match_participant_raw")
@CompoundIndex(
        name = "uk_match_participant",
        def = "{'matchId': 1, 'puuid': 1}",
        unique = true
)
public class MatchParticipantRawDocument {

    @Id
    private String id;

    @Indexed
    private String matchId;

    @Indexed
    private String puuid;

    private Integer participantIndex;

    private String riotIdGameName;
    private String riotIdTagline;

    @Indexed
    private Long championId;

    @Indexed
    private Integer queueId;

    private String gameMode;

    private String gameVersion;

    @Indexed
    private Long gameEndTimestamp;

    private Long gameDuration;

    private Boolean win;
    private String teamPosition;

    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Float kda;

    private Integer champLevel;
    private Integer totalMinionKills;

    private Long totalDamageDealtToChampions;
    private Long totalDamageTaken;

    private Integer visionScore;
    private Integer wardsKilled;
    private Integer wardsPlaced;
    private Integer visionWardsBoughtInGame;

    private Map<String, Object> participantPayload;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static String makeId(String matchId, ParticipantDto participant) {
        return matchId + ":" + participant.getPuuid();
    }

    public static MatchParticipantRawDocument create(
            String matchId,
            MatchDto matchDto,
            ParticipantDto participant,
            int participantIndex,
            Map<String, Object> participantPayload
    ) {
        LocalDateTime now = LocalDateTime.now();
        InfoDto info = matchDto.getInfo();

        return MatchParticipantRawDocument.builder()
                .id(makeId(matchId, participant))
                .matchId(matchId)
                .puuid(participant.getPuuid())
                .participantIndex(participantIndex)
                .riotIdGameName(participant.getRiotIdGameName())
                .riotIdTagline(participant.getRiotIdTagline())
                .championId(participant.getChampionId())
                .queueId(info.getQueueId())
                .gameMode(info.getGameMode())
                .gameVersion(info.getGameVersion())
                .gameEndTimestamp(info.getGameEndTimestamp())
                .gameDuration(info.getGameDuration())
                .participantPayload(participantPayload)
                .createdAt(now)
                .updatedAt(now)
                .build()
                .updateCalculatedFields(participant);
    }

    public MatchParticipantRawDocument updateFrom(
            MatchDto matchDto,
            ParticipantDto participant,
            int participantIndex,
            Map<String, Object> participantPayload
    ) {
        InfoDto info = matchDto.getInfo();

        this.participantIndex = participantIndex;
        this.riotIdGameName = participant.getRiotIdGameName();
        this.riotIdTagline = participant.getRiotIdTagline();
        this.championId = participant.getChampionId();
        this.queueId = info.getQueueId();
        this.gameMode = info.getGameMode();
        this.gameVersion = info.getGameVersion();
        this.gameEndTimestamp = info.getGameEndTimestamp();
        this.gameDuration = info.getGameDuration();
        this.participantPayload = participantPayload;
        this.updatedAt = LocalDateTime.now();

        return updateCalculatedFields(participant);
    }

    private MatchParticipantRawDocument updateCalculatedFields(ParticipantDto participant) {
        this.win = participant.getWin();
        this.teamPosition = participant.getTeamPosition();
        this.kills = participant.getKills();
        this.deaths = participant.getDeaths();
        this.assists = participant.getAssists();
        this.kda = participant.getKda();
        this.champLevel = participant.getChampLevel();
        this.totalMinionKills = safe(participant.getTotalMinionsKilled())
                + safe(participant.getNeutralMinionsKilled());
        this.totalDamageDealtToChampions = participant.getTotalDamageDealtToChampions();
        this.totalDamageTaken = participant.getTotalDamageTaken();
        this.visionScore = participant.getVisionScore();
        this.wardsKilled = participant.getWardKilled();
        this.wardsPlaced = participant.getWardPlaced();
        this.visionWardsBoughtInGame = participant.getVisionWardsBoughtInGame();
        return this;
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
