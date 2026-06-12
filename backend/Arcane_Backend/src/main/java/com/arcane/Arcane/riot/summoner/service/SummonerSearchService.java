package com.arcane.Arcane.riot.summoner.service;

import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.riot.summoner.domain.Summoner;
import com.arcane.Arcane.riot.summoner.dto.SummonerKeywordResDto;
import com.arcane.Arcane.riot.summoner.dto.SummonerSearchDto;
import com.arcane.Arcane.riot.summoner.repository.SummonerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummonerSearchService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final DateTimeFormatter ES_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final SummonerRepository summonerRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${arcane.elasticsearch.url:http://localhost:9200}")
    private String elasticsearchUrl;

    @Value("${arcane.elasticsearch.summoner-index:arcane_summoners}")
    private String summonerIndexName;

    @Transactional(readOnly = true)
    public List<SummonerKeywordResDto> search(String keyword, int limit) {
        try {
            return searchWithElasticsearch(keyword, limit).results();
        } catch (RuntimeException e) {
            log.warn(
                    ApiLogSupport.BUSINESS_FLOW,
                    "summoner-search",
                    "search",
                    "FALLBACK_DATABASE",
                    "keyword=" + normalizeKeyword(keyword).raw() + ", reason=" + e.getMessage()
            );
            return searchWithDatabase(keyword, limit).results();
        }
    }

    @Transactional(readOnly = true)
    public SummonerSearchDto.SummonerSearchResponse searchWithDatabase(String keyword, int limit) {
        SearchKeyword searchKeyword = normalizeKeyword(keyword);
        int safeLimit = clampLimit(limit);
        long startedAt = System.nanoTime();

        List<Summoner> summoners;
        if (searchKeyword.hasHash() && !searchKeyword.tagLine().isBlank()) {
            summoners = summonerRepository.findByGameNameContainingIgnoreCaseAndTagLineContainingIgnoreCase(
                    searchKeyword.gameName(),
                    searchKeyword.tagLine()
            );
        } else {
            summoners = summonerRepository.findByGameNameContainingIgnoreCase(searchKeyword.gameName());
        }

        List<SummonerKeywordResDto> results = summoners.stream()
                .filter(Objects::nonNull)
                .limit(safeLimit)
                .map(SummonerKeywordResDto::of)
                .toList();

        long elapsedMs = elapsedMs(startedAt);
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "summoner-search",
                "searchWithDatabase",
                "SUCCESS",
                "keyword=" + searchKeyword.raw() + ", resultCount=" + results.size() + ", elapsedMs=" + elapsedMs
        );

        return new SummonerSearchDto.SummonerSearchResponse(
                "database",
                searchKeyword.raw(),
                safeLimit,
                elapsedMs,
                summoners.size(),
                results
        );
    }

    @Transactional(readOnly = true)
    public SummonerSearchDto.SummonerSearchResponse searchWithElasticsearch(String keyword, int limit) {
        SearchKeyword searchKeyword = normalizeKeyword(keyword);
        int safeLimit = clampLimit(limit);
        long startedAt = System.nanoTime();

        JsonNode root = requestElasticsearchSearch(searchKeyword, safeLimit);
        JsonNode hitsNode = root.path("hits").path("hits");
        List<SummonerKeywordResDto> results = new ArrayList<>();
        for (JsonNode hit : hitsNode) {
            JsonNode source = hit.path("_source");
            if (!source.isMissingNode()) {
                results.add(toSummonerKeywordResDto(source, hit.path("_id").asLong()));
            }
        }

        long totalHits = root.path("hits").path("total").path("value").asLong(results.size());
        long elapsedMs = elapsedMs(startedAt);
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "summoner-search",
                "searchWithElasticsearch",
                "SUCCESS",
                "keyword=" + searchKeyword.raw() + ", resultCount=" + results.size() + ", elapsedMs=" + elapsedMs
        );

        return new SummonerSearchDto.SummonerSearchResponse(
                "elasticsearch",
                searchKeyword.raw(),
                safeLimit,
                elapsedMs,
                totalHits,
                results
        );
    }

    @Transactional(readOnly = true)
    public SummonerSearchDto.SummonerSearchReindexResponse reindexAllSummoners() {
        long startedAt = System.nanoTime();
        recreateSummonerIndex();

        List<Summoner> summoners = summonerRepository.findAll();
        List<Summoner> searchableSummoners = summoners.stream()
                .filter(this::hasSearchableIdentity)
                .toList();
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "summoner-search",
                "reindexAllSummoners",
                "BULK_READY",
                "indexName=" + summonerIndexName + ", totalCount=" + summoners.size()
                        + ", searchableCount=" + searchableSummoners.size()
                        + ", skippedInvalidCount=" + (summoners.size() - searchableSummoners.size())
        );

        int indexedCount = 0;
        if (!searchableSummoners.isEmpty()) {
            indexedCount = bulkIndex(searchableSummoners);
        }

        long elapsedMs = elapsedMs(startedAt);
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "summoner-search",
                "reindexAllSummoners",
                "SUCCESS",
                "indexName=" + summonerIndexName + ", indexedCount=" + indexedCount
                        + ", skippedInvalidCount=" + (summoners.size() - searchableSummoners.size()) + ", elapsedMs=" + elapsedMs
        );

        return new SummonerSearchDto.SummonerSearchReindexResponse(
                summonerIndexName,
                indexedCount,
                elapsedMs,
                "소환사 Elasticsearch 인덱스를 재생성했습니다."
        );
    }

    @Transactional(readOnly = true)
    public SummonerSearchDto.SummonerSearchBenchmarkResponse benchmark(String keyword, int limit, int iterations) {
        SearchKeyword searchKeyword = normalizeKeyword(keyword);
        int safeLimit = clampLimit(limit);
        int safeIterations = Math.max(1, Math.min(iterations, 30));
        List<Long> dbSamples = new ArrayList<>(safeIterations);
        List<Long> esSamples = new ArrayList<>(safeIterations);
        int lastDbResultCount = 0;
        int lastEsResultCount = 0;

        for (int i = 0; i < safeIterations; i++) {
            SummonerSearchDto.SummonerSearchResponse databaseResponse = searchWithDatabase(searchKeyword.raw(), safeLimit);
            dbSamples.add(databaseResponse.elapsedMs());
            lastDbResultCount = databaseResponse.results().size();

            SummonerSearchDto.SummonerSearchResponse elasticsearchResponse = searchWithElasticsearch(searchKeyword.raw(), safeLimit);
            esSamples.add(elasticsearchResponse.elapsedMs());
            lastEsResultCount = elasticsearchResponse.results().size();
        }

        SummonerSearchDto.SummonerSearchMetric databaseMetric = toMetric(dbSamples, lastDbResultCount);
        SummonerSearchDto.SummonerSearchMetric elasticsearchMetric = toMetric(esSamples, lastEsResultCount);
        Double improvementPercent = databaseMetric.averageMs() == 0
                ? null
                : Math.round(((databaseMetric.averageMs() - elasticsearchMetric.averageMs()) * 10000.0 / databaseMetric.averageMs())) / 100.0;

        return new SummonerSearchDto.SummonerSearchBenchmarkResponse(
                searchKeyword.raw(),
                safeLimit,
                safeIterations,
                databaseMetric,
                elasticsearchMetric,
                improvementPercent
        );
    }

    private JsonNode requestElasticsearchSearch(SearchKeyword keyword, int limit) {
        Map<String, Object> request = Map.of(
                "size", limit,
                "query", buildSearchQuery(keyword),
                "sort", List.of(
                        Map.of("_score", Map.of("order", "desc")),
                        Map.of("updatedAt", Map.of("order", "desc", "missing", "_last"))
                )
        );

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    indexUrl() + "/_search",
                    request,
                    JsonNode.class
            );
            JsonNode body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Elasticsearch 검색 응답이 비어 있습니다.");
            }
            return body;
        } catch (RestClientException e) {
            throw new IllegalStateException("Elasticsearch 소환사 검색에 실패했습니다. index=" + summonerIndexName, e);
        }
    }

    private Map<String, Object> buildSearchQuery(SearchKeyword keyword) {
        if (keyword.hasHash() && !keyword.tagLine().isBlank()) {
            return Map.of(
                    "bool", Map.of(
                        "must", List.of(
                                    Map.of("wildcard", Map.of(
                                            "gameName.keyword", Map.of(
                                                    "value", "*" + escapeWildcard(keyword.gameName()) + "*",
                                                    "case_insensitive", true
                                            )
                                    )),
                                    Map.of("wildcard", Map.of(
                                            "tagLine.keyword", Map.of(
                                                    "value", "*" + escapeWildcard(keyword.tagLine()) + "*",
                                                    "case_insensitive", true
                                            )
                                    ))
                            ),
                            "should", List.of(
                                    Map.of("term", Map.of("searchKey.keyword", keyword.compactSearchKey())),
                                    Map.of("term", Map.of("displayName.keyword", keyword.displayName()))
                            )
                    )
            );
        }

        if (keyword.hasHash()) {
            return Map.of(
                    "wildcard", Map.of(
                            "gameName.keyword", Map.of(
                                    "value", "*" + escapeWildcard(keyword.gameName()) + "*",
                                    "case_insensitive", true
                            )
                    )
            );
        }

        return Map.of(
                "multi_match", Map.of(
                        "query", keyword.gameName(),
                        "fields", List.of("gameName^3", "trimmedGameName^3", "displayName^2", "searchKey^4", "tagLine"),
                        "type", "best_fields"
                )
        );
    }

    private SummonerKeywordResDto toSummonerKeywordResDto(JsonNode source, long fallbackId) {
        return SummonerKeywordResDto.builder()
                .id(source.path("id").asLong(fallbackId))
                .puuid(source.path("puuid").asText(null))
                .gameName(source.path("gameName").asText(""))
                .tagLine(source.path("tagLine").asText(""))
                .icon(source.path("icon").asInt(29))
                .level(source.path("level").asInt(0))
                .soloRank(source.path("soloRank").asText(null))
                .soloRankLp(source.path("soloRankLp").isMissingNode() || source.path("soloRankLp").isNull()
                        ? null
                        : source.path("soloRankLp").asInt())
                .build();
    }

    private void recreateSummonerIndex() {
        try {
            restTemplate.delete(indexUrl());
        } catch (HttpClientErrorException.NotFound ignored) {
            // 삭제할 기존 인덱스가 없는 정상 케이스다.
        } catch (RestClientException e) {
            throw new IllegalStateException("기존 Elasticsearch 소환사 인덱스 삭제에 실패했습니다. index=" + summonerIndexName, e);
        }

        Map<String, Object> body = Map.of(
                "settings", Map.of(
                        "index", Map.of("max_ngram_diff", 19),
                        "analysis", Map.of(
                                "normalizer", Map.of(
                                        "lowercase_normalizer", Map.of(
                                                "type", "custom",
                                                "filter", List.of("lowercase")
                                        )
                                ),
                                "tokenizer", Map.of(
                                        "summoner_ngram_tokenizer", Map.of(
                                                "type", "ngram",
                                                "min_gram", 1,
                                                "max_gram", 20,
                                                "token_chars", List.of("letter", "digit")
                                        )
                                ),
                                "analyzer", Map.of(
                                        "summoner_ngram_analyzer", Map.of(
                                                "type", "custom",
                                                "tokenizer", "summoner_ngram_tokenizer",
                                                "filter", List.of("lowercase")
                                        )
                                )
                        )
                ),
                "mappings", Map.of(
                        "properties", summonerIndexProperties()
                )
        );

        try {
            restTemplate.put(indexUrl(), body);
        } catch (RestClientException e) {
            throw new IllegalStateException("Elasticsearch 소환사 인덱스 생성에 실패했습니다. index=" + summonerIndexName, e);
        }
    }

    private Map<String, Object> textWithKeywordMapping() {
        return Map.of(
                "type", "text",
                "analyzer", "summoner_ngram_analyzer",
                "search_analyzer", "standard",
                "fields", Map.of(
                        "keyword", Map.of(
                                "type", "keyword",
                                "normalizer", "lowercase_normalizer"
                        )
                )
        );
    }

    private Map<String, Object> summonerIndexProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", Map.of("type", "long"));
        properties.put("puuid", Map.of("type", "keyword"));
        properties.put("gameName", textWithKeywordMapping());
        properties.put("trimmedGameName", textWithKeywordMapping());
        properties.put("tagLine", textWithKeywordMapping());
        properties.put("displayName", textWithKeywordMapping());
        properties.put("searchKey", textWithKeywordMapping());
        properties.put("icon", Map.of("type", "integer"));
        properties.put("level", Map.of("type", "integer"));
        properties.put("soloRank", Map.of("type", "keyword"));
        properties.put("soloRankLp", Map.of("type", "integer"));
        properties.put("updatedAt", Map.of("type", "date", "format", "strict_date_optional_time||yyyy-MM-dd'T'HH:mm:ss"));
        return properties;
    }

    private int bulkIndex(List<Summoner> summoners) {
        StringBuilder bulkBody = new StringBuilder();
        int indexedCount = 0;
        for (Summoner summoner : summoners) {
            if (!hasSearchableIdentity(summoner)) {
                continue;
            }
            try {
                bulkBody.append(objectMapper.writeValueAsString(Map.of(
                        "index", Map.of("_index", summonerIndexName, "_id", String.valueOf(summoner.getId()))
                ))).append('\n');
                bulkBody.append(objectMapper.writeValueAsString(toElasticsearchDocument(summoner))).append('\n');
                indexedCount++;
            } catch (Exception e) {
                throw new IllegalStateException("소환사 문서를 Elasticsearch bulk payload로 변환하지 못했습니다. summonerId=" + summoner.getId(), e);
            }
        }

        if (indexedCount == 0) {
            return 0;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "x-ndjson", StandardCharsets.UTF_8));
        HttpEntity<String> request = new HttpEntity<>(bulkBody.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    elasticsearchBaseUrl() + "/_bulk",
                    HttpMethod.POST,
                    request,
                    JsonNode.class
            );
            JsonNode body = response.getBody();
            if (body != null && body.path("errors").asBoolean(false)) {
                throw new IllegalStateException("Elasticsearch 소환사 bulk indexing 중 일부 문서 저장에 실패했습니다. failures=" + summarizeBulkFailures(body));
            }
        } catch (RestClientException e) {
            throw new IllegalStateException("Elasticsearch 소환사 bulk indexing 요청에 실패했습니다. index=" + summonerIndexName, e);
        }
        return indexedCount;
    }

    private List<String> summarizeBulkFailures(JsonNode body) {
        List<String> failures = new ArrayList<>();
        for (JsonNode item : body.path("items")) {
            JsonNode indexNode = item.path("index");
            JsonNode errorNode = indexNode.path("error");
            if (!errorNode.isMissingNode()) {
                failures.add("id=%s status=%s type=%s reason=%s".formatted(
                        indexNode.path("_id").asText("-"),
                        indexNode.path("status").asText("-"),
                        errorNode.path("type").asText("-"),
                        errorNode.path("reason").asText("-")
                ));
                if (failures.size() >= 5) {
                    break;
                }
            }
        }
        return failures;
    }

    private Map<String, Object> toElasticsearchDocument(Summoner summoner) {
        String gameName = searchableText(summoner.getGameName());
        String trimmedGameName = searchableText(summoner.getTrimmedGameName());
        String tagLine = searchableText(summoner.getTagLine());
        if (gameName == null || tagLine == null) {
            throw new IllegalArgumentException("검색 가능한 gameName/tagLine이 없습니다.");
        }
        if (trimmedGameName == null) {
            trimmedGameName = gameName.replace(" ", "");
        }
        String displayName = gameName + "#" + tagLine;
        String searchKey = trimmedGameName.toLowerCase(Locale.ROOT) + "#" + tagLine.toLowerCase(Locale.ROOT);

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", summoner.getId());
        putIfSearchable(document, "puuid", summoner.getPuuid());
        document.put("gameName", gameName);
        document.put("trimmedGameName", trimmedGameName);
        document.put("tagLine", tagLine);
        document.put("displayName", displayName);
        document.put("searchKey", searchKey);
        document.put("icon", summoner.getIconId());
        document.put("level", summoner.getLevel());
        putIfSearchable(document, "soloRank", summoner.getSoloRankTier());
        document.put("soloRankLp", summoner.getSoloRankLP());
        if (summoner.getUpdatedAt() != null) {
            document.put("updatedAt", ES_DATE_FORMATTER.format(summoner.getUpdatedAt()));
        }
        return document;
    }

    private boolean hasSearchableIdentity(Summoner summoner) {
        return summoner != null
                && hasSearchableToken(summoner.getGameName())
                && hasSearchableToken(summoner.getTagLine());
    }

    private SummonerSearchDto.SummonerSearchMetric toMetric(List<Long> samples, int lastResultCount) {
        long min = samples.stream().mapToLong(Long::longValue).min().orElse(0L);
        long max = samples.stream().mapToLong(Long::longValue).max().orElse(0L);
        long average = Math.round(samples.stream().mapToLong(Long::longValue).average().orElse(0));
        return new SummonerSearchDto.SummonerSearchMetric(average, min, max, lastResultCount);
    }

    private SearchKeyword normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        String raw = keyword.trim().toLowerCase(Locale.ROOT);
        if (!raw.contains("#")) {
            return new SearchKeyword(raw, raw, "", false);
        }

        String[] parts = raw.split("#", 2);
        String gameName = parts[0].trim();
        String tagLine = parts.length > 1 ? parts[1].trim() : "";
        return new SearchKeyword(raw, gameName, tagLine, true);
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String searchableText(String value) {
        if (!hasSearchableToken(value)) {
            return null;
        }
        return value.trim();
    }

    private void putIfSearchable(Map<String, Object> document, String key, String value) {
        String searchableValue = searchableText(value);
        if (searchableValue != null) {
            document.put(key, searchableValue);
        }
    }

    private boolean hasSearchableToken(String value) {
        return value != null && value.chars().anyMatch(Character::isLetterOrDigit);
    }

    private String escapeWildcard(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("?", "\\?");
    }

    private long elapsedMs(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private String indexUrl() {
        return elasticsearchBaseUrl() + "/" + summonerIndexName;
    }

    private String elasticsearchBaseUrl() {
        return elasticsearchUrl.endsWith("/")
                ? elasticsearchUrl.substring(0, elasticsearchUrl.length() - 1)
                : elasticsearchUrl;
    }

    private record SearchKeyword(String raw, String gameName, String tagLine, boolean hasHash) {
        private String displayName() {
            return gameName + "#" + tagLine;
        }

        private String compactSearchKey() {
            return gameName.replace(" ", "") + "#" + tagLine;
        }
    }
}
