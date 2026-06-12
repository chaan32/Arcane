package com.arcane.Arcane.riot.Match.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

public interface MatchRawDocumentRepository extends MongoRepository<MatchParticipantRawDocument, String> {
    List<MatchParticipantRawDocument> findByMatchId(String matchId);
    List<MatchParticipantRawDocument> findByPuuidOrderByGameEndTimestampDesc (String puuid);
}
