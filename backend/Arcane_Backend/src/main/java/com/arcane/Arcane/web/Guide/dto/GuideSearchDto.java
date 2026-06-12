package com.arcane.Arcane.web.Guide.dto;

import java.util.List;

public class GuideSearchDto {

    public record GuideSearchResponse(
            String engine,
            String keyword,
            int limit,
            long elapsedMs,
            long totalHits,
            List<GuideDto.GuideListResponseDto> results
    ) {
    }

    public record GuideSearchBenchmarkResponse(
            String keyword,
            int limit,
            int iterations,
            GuideSearchMetric database,
            GuideSearchMetric elasticsearch
    ) {
    }

    public record GuideSearchMetric(
            long averageMs,
            long minMs,
            long maxMs,
            int lastResultCount
    ) {
    }

    public record GuideSeedResponse(
            int requestedCount,
            int insertedCount,
            long totalSeededCount,
            long totalGuideCount
    ) {
    }

    public record GuideReindexResponse(
            String indexName,
            int indexedCount,
            long elapsedMs,
            String message
    ) {
    }
}
