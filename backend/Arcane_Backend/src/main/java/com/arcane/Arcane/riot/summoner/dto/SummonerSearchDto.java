package com.arcane.Arcane.riot.summoner.dto;

import java.util.List;

public class SummonerSearchDto {

    public record SummonerSearchResponse(
            String engine,
            String keyword,
            int limit,
            long elapsedMs,
            long totalHits,
            List<SummonerKeywordResDto> results
    ) {
    }

    public record SummonerSearchBenchmarkResponse(
            String keyword,
            int limit,
            int iterations,
            SummonerSearchMetric database,
            SummonerSearchMetric elasticsearch,
            Double improvementPercent
    ) {
    }

    public record SummonerSearchMetric(
            long averageMs,
            long minMs,
            long maxMs,
            int lastResultCount
    ) {
    }

    public record SummonerSearchReindexResponse(
            String indexName,
            int indexedCount,
            long elapsedMs,
            String message
    ) {
    }
}
