package com.arcane.Arcane.web.Guide.service;

import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Data.Champion.repository.ChampionRepository;
import com.arcane.Arcane.web.Guide.domain.Guide;
import com.arcane.Arcane.web.Guide.dto.GuideDto;
import com.arcane.Arcane.web.Guide.dto.GuideSearchDto;
import com.arcane.Arcane.web.Guide.repository.GuideRepository;
import com.arcane.Arcane.web.User.domain.OAuthProvider;
import com.arcane.Arcane.web.User.domain.Role;
import com.arcane.Arcane.web.User.domain.User;
import com.arcane.Arcane.web.User.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuideSearchService {

    private static final String SEED_TITLE_PREFIX = "[ES-BENCH]";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_SEED_COUNT = 2_000;
    private static final DateTimeFormatter ES_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final GuideRepository guideRepository;
    private final UserRepository userRepository;
    private final ChampionRepository championRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${arcane.elasticsearch.url:http://localhost:9200}")
    private String elasticsearchUrl;

    @Value("${arcane.elasticsearch.guide-index:arcane_guide_posts}")
    private String guideIndexName;

    @Transactional(readOnly = true)
    public GuideSearchDto.GuideSearchResponse searchWithDatabase(String keyword, int limit) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int safeLimit = clampLimit(limit);
        long startedAt = System.nanoTime();

        Page<Guide> page = guideRepository.searchByTitleOrContentContaining(
                normalizedKeyword,
                PageRequest.of(0, safeLimit)
        );
        List<GuideDto.GuideListResponseDto> results = page.getContent().stream()
                .map(GuideDto.GuideListResponseDto::new)
                .toList();

        long elapsedMs = elapsedMs(startedAt);
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide-search",
                "searchWithDatabase",
                "SUCCESS",
                "keyword=" + normalizedKeyword + ", resultCount=" + results.size() + ", elapsedMs=" + elapsedMs
        );

        return new GuideSearchDto.GuideSearchResponse(
                "database",
                normalizedKeyword,
                safeLimit,
                elapsedMs,
                page.getTotalElements(),
                results
        );
    }

    @Transactional(readOnly = true)
    public GuideSearchDto.GuideSearchResponse searchWithElasticsearch(String keyword, int limit) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int safeLimit = clampLimit(limit);
        long startedAt = System.nanoTime();

        JsonNode root = requestElasticsearchSearch(normalizedKeyword, safeLimit);
        JsonNode hitsNode = root.path("hits").path("hits");
        List<Long> guideIds = new ArrayList<>();
        for (JsonNode hit : hitsNode) {
            long guideId = hit.path("_source").path("id").asLong(hit.path("_id").asLong());
            if (guideId > 0) {
                guideIds.add(guideId);
            }
        }

        Map<Long, Guide> guideById = new HashMap<>();
        if (!guideIds.isEmpty()) {
            guideRepository.findByIdIn(guideIds)
                    .forEach(guide -> guideById.put(guide.getId(), guide));
        }

        List<GuideDto.GuideListResponseDto> results = guideIds.stream()
                .map(guideById::get)
                .filter(guide -> guide != null)
                .map(GuideDto.GuideListResponseDto::new)
                .toList();

        long totalHits = root.path("hits").path("total").path("value").asLong(results.size());
        long elapsedMs = elapsedMs(startedAt);

        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide-search",
                "searchWithElasticsearch",
                "SUCCESS",
                "keyword=" + normalizedKeyword + ", resultCount=" + results.size() + ", elapsedMs=" + elapsedMs
        );

        return new GuideSearchDto.GuideSearchResponse(
                "elasticsearch",
                normalizedKeyword,
                safeLimit,
                elapsedMs,
                totalHits,
                results
        );
    }

    @Transactional
    public GuideSearchDto.GuideSeedResponse seedBenchmarkGuides(int requestedCount) {
        int safeCount = Math.max(1, Math.min(requestedCount, MAX_SEED_COUNT));
        long currentSeedCount = guideRepository.countByTitleStartingWith(SEED_TITLE_PREFIX);
        int insertCount = Math.max(0, safeCount - (int) currentSeedCount);

        if (insertCount == 0) {
            return new GuideSearchDto.GuideSeedResponse(
                    safeCount,
                    0,
                    currentSeedCount,
                    guideRepository.count()
            );
        }

        User author = findOrCreateBenchmarkAuthor();
        List<Champion> champions = championRepository.findAll().stream()
                .filter(champion -> Boolean.TRUE.equals(champion.getActive()))
                .sorted(Comparator.comparing(Champion::getId))
                .toList();
        if (champions.isEmpty()) {
            throw new IllegalStateException("공략 seed 생성을 위한 champion 데이터가 없습니다.");
        }

        List<Guide> guides = new ArrayList<>(insertCount);
        for (int offset = 0; offset < insertCount; offset++) {
            int sequence = (int) currentSeedCount + offset + 1;
            Champion champion = champions.get(sequence % champions.size());
            guides.add(Guide.builder()
                    .title(buildSeedTitle(sequence, champion))
                    .content(buildSeedContent(sequence, champion))
                    .champion(champion)
                    .author(author)
                    .build());
        }

        guideRepository.saveAll(guides);
        long totalSeedCount = guideRepository.countByTitleStartingWith(SEED_TITLE_PREFIX);

        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide-search",
                "seedBenchmarkGuides",
                "SUCCESS",
                "requestedCount=" + safeCount + ", insertedCount=" + insertCount + ", totalSeededCount=" + totalSeedCount
        );

        return new GuideSearchDto.GuideSeedResponse(
                safeCount,
                insertCount,
                totalSeedCount,
                guideRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public GuideSearchDto.GuideReindexResponse reindexAllGuides() {
        long startedAt = System.nanoTime();
        recreateGuideIndex();

        List<Guide> guides = guideRepository.findAllByOrderByUpdatedAtDesc();
        if (!guides.isEmpty()) {
            bulkIndex(guides);
        }

        long elapsedMs = elapsedMs(startedAt);
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "guide-search",
                "reindexAllGuides",
                "SUCCESS",
                "indexName=" + guideIndexName + ", indexedCount=" + guides.size() + ", elapsedMs=" + elapsedMs
        );

        return new GuideSearchDto.GuideReindexResponse(
                guideIndexName,
                guides.size(),
                elapsedMs,
                "공략 Elasticsearch 인덱스를 재생성했습니다."
        );
    }

    @Transactional(readOnly = true)
    public GuideSearchDto.GuideSearchBenchmarkResponse benchmark(String keyword, int limit, int iterations) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int safeLimit = clampLimit(limit);
        int safeIterations = Math.max(1, Math.min(iterations, 30));
        List<Long> dbSamples = new ArrayList<>(safeIterations);
        List<Long> esSamples = new ArrayList<>(safeIterations);
        int lastDbResultCount = 0;
        int lastEsResultCount = 0;

        for (int i = 0; i < safeIterations; i++) {
            GuideSearchDto.GuideSearchResponse databaseResponse = searchWithDatabase(normalizedKeyword, safeLimit);
            dbSamples.add(databaseResponse.elapsedMs());
            lastDbResultCount = databaseResponse.results().size();

            GuideSearchDto.GuideSearchResponse elasticsearchResponse = searchWithElasticsearch(normalizedKeyword, safeLimit);
            esSamples.add(elasticsearchResponse.elapsedMs());
            lastEsResultCount = elasticsearchResponse.results().size();
        }

        return new GuideSearchDto.GuideSearchBenchmarkResponse(
                normalizedKeyword,
                safeLimit,
                safeIterations,
                toMetric(dbSamples, lastDbResultCount),
                toMetric(esSamples, lastEsResultCount)
        );
    }

    private JsonNode requestElasticsearchSearch(String keyword, int limit) {
        Map<String, Object> request = Map.of(
                "size", limit,
                "query", Map.of(
                        "multi_match", Map.of(
                                "query", keyword,
                                "fields", List.of("title^3", "content", "championNameKo^2", "championNameEn", "authorName"),
                                "type", "best_fields"
                        )
                ),
                "sort", List.of(Map.of("updatedAt", Map.of("order", "desc")))
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
            throw new IllegalStateException("Elasticsearch 공략 검색에 실패했습니다. index=" + guideIndexName, e);
        }
    }

    private void recreateGuideIndex() {
        try {
            restTemplate.delete(indexUrl());
        } catch (HttpClientErrorException.NotFound ignored) {
            // 삭제할 기존 인덱스가 없는 정상 케이스다.
        } catch (RestClientException e) {
            throw new IllegalStateException("기존 Elasticsearch 공략 인덱스 삭제에 실패했습니다. index=" + guideIndexName, e);
        }

        Map<String, Object> body = Map.of(
                "settings", Map.of(
                        "index", Map.of("max_ngram_diff", 18),
                        "analysis", Map.of(
                                "tokenizer", Map.of(
                                        "guide_ngram_tokenizer", Map.of(
                                                "type", "ngram",
                                                "min_gram", 2,
                                                "max_gram", 20,
                                                "token_chars", List.of("letter", "digit")
                                        )
                                ),
                                "analyzer", Map.of(
                                        "guide_ngram_analyzer", Map.of(
                                                "type", "custom",
                                                "tokenizer", "guide_ngram_tokenizer",
                                                "filter", List.of("lowercase")
                                        )
                                )
                        )
                ),
                "mappings", Map.of(
                        "properties", Map.of(
                                "id", Map.of("type", "long"),
                                "title", Map.of("type", "text", "analyzer", "guide_ngram_analyzer", "search_analyzer", "standard"),
                                "content", Map.of("type", "text", "analyzer", "guide_ngram_analyzer", "search_analyzer", "standard"),
                                "authorName", Map.of("type", "text", "analyzer", "guide_ngram_analyzer", "search_analyzer", "standard"),
                                "championNameKo", Map.of("type", "text", "analyzer", "guide_ngram_analyzer", "search_analyzer", "standard"),
                                "championNameEn", Map.of("type", "text", "analyzer", "guide_ngram_analyzer", "search_analyzer", "standard"),
                                "updatedAt", Map.of("type", "date", "format", "strict_date_optional_time||yyyy-MM-dd'T'HH:mm:ss")
                        )
                )
        );

        try {
            restTemplate.put(indexUrl(), body);
        } catch (RestClientException e) {
            throw new IllegalStateException("Elasticsearch 공략 인덱스 생성에 실패했습니다. index=" + guideIndexName, e);
        }
    }

    private void bulkIndex(List<Guide> guides) {
        StringBuilder bulkBody = new StringBuilder();
        for (Guide guide : guides) {
            try {
                bulkBody.append(objectMapper.writeValueAsString(Map.of(
                        "index", Map.of("_index", guideIndexName, "_id", String.valueOf(guide.getId()))
                ))).append('\n');
                bulkBody.append(objectMapper.writeValueAsString(toElasticsearchDocument(guide))).append('\n');
            } catch (Exception e) {
                throw new IllegalStateException("공략 문서를 Elasticsearch bulk payload로 변환하지 못했습니다. guideId=" + guide.getId(), e);
            }
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
                throw new IllegalStateException("Elasticsearch bulk indexing 중 일부 문서 저장에 실패했습니다. response=" + body);
            }
        } catch (RestClientException e) {
            throw new IllegalStateException("Elasticsearch bulk indexing 요청에 실패했습니다. index=" + guideIndexName, e);
        }
    }

    private Map<String, Object> toElasticsearchDocument(Guide guide) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", guide.getId());
        document.put("title", nullToBlank(guide.getTitle()));
        document.put("content", nullToBlank(guide.getContent()));
        document.put("authorName", displayName(guide.getAuthor()));
        document.put("championId", guide.getChampion().getId());
        document.put("championNameKo", nullToBlank(guide.getChampion().getNameKo()));
        document.put("championNameEn", nullToBlank(guide.getChampion().getNameEn()));
        document.put("updatedAt", guide.getUpdatedAt() == null ? null : ES_DATE_FORMATTER.format(guide.getUpdatedAt()));
        return document;
    }

    private User findOrCreateBenchmarkAuthor() {
        Optional<User> existingBenchmarkUser = userRepository.findByLoginId("guide_benchmark_system");
        if (existingBenchmarkUser.isPresent()) {
            return existingBenchmarkUser.get();
        }

        Optional<User> adminUser = userRepository.findById(5L);
        if (adminUser.isPresent()) {
            return adminUser.get();
        }

        User benchmarkUser = User.builder()
                .loginId("guide_benchmark_system")
                .loginPw(User.OAUTH_PASSWORD_PLACEHOLDER)
                .nickName("공략성능측정")
                .provider(OAuthProvider.LOCAL)
                .onboardingCompleted(true)
                .role(Role.ADMIN)
                .build();
        return userRepository.save(benchmarkUser);
    }

    private String buildSeedTitle(int sequence, Champion champion) {
        return "%s %04d %s 공략 검색 성능 측정".formatted(
                SEED_TITLE_PREFIX,
                sequence,
                nullToBlank(champion.getNameKo())
        );
    }

    private String buildSeedContent(int sequence, Champion champion) {
        String championName = nullToBlank(champion.getNameKo());
        String lane = switch (sequence % 5) {
            case 0 -> "탑";
            case 1 -> "정글";
            case 2 -> "미드";
            case 3 -> "원딜";
            default -> "서폿";
        };
        String focus = switch (sequence % 6) {
            case 0 -> "라인전 운영";
            case 1 -> "오브젝트 시야 장악";
            case 2 -> "한타 포지셔닝";
            case 3 -> "아이템 빌드";
            case 4 -> "룬 선택";
            default -> "스플릿 운영";
        };

        return """
                # %s %s 공략 실험 데이터

                이 문서는 공략 게시판의 MySQL LIKE 검색과 Elasticsearch 전문 검색 성능을 비교하기 위해 생성된 벤치마크 데이터입니다.

                - 챔피언: %s
                - 포지션: %s
                - 핵심 주제: %s

                라인전 운영, 오브젝트 시야, 와드 위치, 갱킹 회피, 한타 포지셔닝, 아이템 빌드, 룬 선택, 스킬 순서, 스플릿 운영을 설명합니다.
                검색 성능 비교용 공통 키워드는 arcane-search-benchmark 입니다.
                Elasticsearch와 데이터베이스 contain 검색의 차이를 측정하기 위한 본문입니다.
                """.formatted(championName, sequence, championName, lane, focus);
    }

    private GuideSearchDto.GuideSearchMetric toMetric(List<Long> samples, int lastResultCount) {
        long min = samples.stream().mapToLong(Long::longValue).min().orElse(0L);
        long max = samples.stream().mapToLong(Long::longValue).max().orElse(0L);
        long average = Math.round(samples.stream().mapToLong(Long::longValue).average().orElse(0));
        return new GuideSearchDto.GuideSearchMetric(average, min, max, lastResultCount);
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private String displayName(User user) {
        if (user.getNickName() != null && !user.getNickName().isBlank()) {
            return user.getNickName();
        }
        if (user.getGameName() != null && !user.getGameName().isBlank()) {
            return user.getGameName();
        }
        return user.getLoginId();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private long elapsedMs(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private String indexUrl() {
        return elasticsearchBaseUrl() + "/" + guideIndexName;
    }

    private String elasticsearchBaseUrl() {
        return elasticsearchUrl.endsWith("/")
                ? elasticsearchUrl.substring(0, elasticsearchUrl.length() - 1)
                : elasticsearchUrl;
    }
}
