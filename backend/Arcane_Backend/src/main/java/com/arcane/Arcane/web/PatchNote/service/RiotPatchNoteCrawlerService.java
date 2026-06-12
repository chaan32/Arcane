package com.arcane.Arcane.web.PatchNote.service;

import com.arcane.Arcane.riot.Data.Champion.Champion;
import com.arcane.Arcane.riot.Data.Champion.repository.ChampionRepository;
import com.arcane.Arcane.web.PatchNote.dto.RiotPatchNoteDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiotPatchNoteCrawlerService {
    private static final String PATCH_NOTE_LIST_URL = "https://www.leagueoflegends.com/ko-kr/news/tags/patch-notes/";
    private static final String OFFICIAL_SITE_URL = "https://www.leagueoflegends.com";
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final int MAX_FETCH_ATTEMPTS = 3;
    private static final Pattern PATCH_VERSION_PATTERN = Pattern.compile("(\\d{2})\\.(\\d{1,2})");
    private static final Pattern PATCH_URL_PATTERN = Pattern.compile("patch-(\\d{2})-(\\d{1,2})");
    private static final List<PatchArticle> FALLBACK_2026_PATCH_ARTICLES = List.of(
            new PatchArticle("26.11", "리그 오브 레전드 26.11 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-11-notes/", "2026-05-27T18:00:00.000Z"),
            new PatchArticle("26.10", "리그 오브 레전드 26.10 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-10-notes/", "2026-05-12T18:00:00.000Z"),
            new PatchArticle("26.9", "리그 오브 레전드 26.9 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-9-notes/", "2026-04-28T18:00:00.000Z"),
            new PatchArticle("26.8", "리그 오브 레전드 26.8 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-8-notes/", "2026-04-14T18:00:00.000Z"),
            new PatchArticle("26.7", "리그 오브 레전드 26.7 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-7-notes/", "2026-03-31T18:00:00.000Z"),
            new PatchArticle("26.6", "리그 오브 레전드 26.6 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-6-notes/", "2026-03-17T18:00:00.000Z"),
            new PatchArticle("26.5", "26.5 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-5-notes/", "2026-03-03T19:00:00.000Z"),
            new PatchArticle("26.4", "리그 오브 레전드 26.4 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-26-4-notes/", "2026-02-18T19:00:00.000Z"),
            new PatchArticle("26.3", "26.3 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-26-3-notes/", "2026-02-03T19:00:00.000Z"),
            new PatchArticle("26.2", "26.2 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-26-2-notes/", "2026-01-21T19:00:00.000Z"),
            new PatchArticle("26.1", "26.1 패치 노트", "https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-26-1-notes/", "2026-01-07T19:00:00.000Z")
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper objectMapper;
    private final ChampionRepository championRepository;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<Integer, PatchArticleCacheEntry> patchArticleCache = new ConcurrentHashMap<>();
    private final Object patchArticleLock = new Object();

    public RiotPatchNoteDto.PatchNoteListResponse findPatchNotes(int fromYear) {
        List<RiotPatchNoteDto.PatchNoteSummary> patches = readPatchArticles(fromYear).stream()
                .map(article -> new RiotPatchNoteDto.PatchNoteSummary(
                        article.patchVersion(),
                        article.title(),
                        article.url(),
                        article.publishedAt()
                ))
                .toList();

        return new RiotPatchNoteDto.PatchNoteListResponse(patches);
    }

    public RiotPatchNoteDto.ChampionPatchResponse findChampionPatchNotes(String championName, int fromYear) {
        String normalizedChampionName = normalize(championName);
        String cacheKey = normalizedChampionName + ":" + fromYear;
        CacheEntry cached = cache.get(cacheKey);

        if (cached != null && cached.isAlive()) {
            return cached.response();
        }

        try {
            RiotPatchNoteDto.ChampionPatchResponse response = crawlChampionPatchNotes(championName, fromYear);
            cache.put(cacheKey, new CacheEntry(response, Instant.now().plus(CACHE_TTL)));
            return response;
        } catch (Exception e) {
            log.warn("공식 패치노트 크롤링 실패: championName={}, fromYear={}, reason={}",
                    championName, fromYear, getRootMessage(e));
            return new RiotPatchNoteDto.ChampionPatchResponse(championName, List.of());
        }
    }

    public void clearCache() {
        cache.clear();
        patchArticleCache.clear();
    }

    private RiotPatchNoteDto.ChampionPatchResponse crawlChampionPatchNotes(String championName, int fromYear) throws Exception {
        Set<String> championNames = resolveChampionNames(championName);
        List<PatchArticle> articles = readPatchArticles(fromYear);
        List<RiotPatchNoteDto.ChampionPatchNote> patches = new ArrayList<>();

        for (PatchArticle article : articles) {
            try {
                String detailHtml = fetchHtml(article.url());
                JsonNode articleRoot = readNextData(detailHtml);
                String patchBody = findTextContaining(articleRoot, "patch-notes-container").orElse("");
                List<RiotPatchNoteDto.ChampionPatchChange> changes = extractChampionChanges(patchBody, championNames);

                if (!changes.isEmpty()) {
                    patches.add(new RiotPatchNoteDto.ChampionPatchNote(
                            article.patchVersion(),
                            article.title(),
                            article.url(),
                            article.publishedAt(),
                            changes
                    ));
                }
            } catch (Exception e) {
                log.warn("공식 패치노트 상세 크롤링 실패: patchVersion={}, url={}, reason={}",
                        article.patchVersion(), article.url(), getRootMessage(e));
            }
        }

        return new RiotPatchNoteDto.ChampionPatchResponse(championName, patches);
    }

    private List<PatchArticle> readPatchArticles(int fromYear) {
        PatchArticleCacheEntry cached = patchArticleCache.get(fromYear);
        if (cached != null && cached.isAlive()) {
            return cached.articles();
        }

        synchronized (patchArticleLock) {
            cached = patchArticleCache.get(fromYear);
            if (cached != null && cached.isAlive()) {
                return cached.articles();
            }

            List<PatchArticle> articles;
            try {
                articles = crawlPatchArticles(fromYear);
            } catch (Exception e) {
                log.warn("공식 패치노트 목록 크롤링 실패. 폴백 목록 사용: fromYear={}, reason={}",
                        fromYear, getRootMessage(e));
                articles = fallbackPatchArticles(fromYear);
            }

            if (articles.isEmpty()) {
                articles = fallbackPatchArticles(fromYear);
            }

            patchArticleCache.put(fromYear, new PatchArticleCacheEntry(articles, Instant.now().plus(CACHE_TTL)));
            return articles;
        }
    }

    private List<PatchArticle> crawlPatchArticles(int fromYear) throws Exception {
        String listHtml = fetchHtml(PATCH_NOTE_LIST_URL);
        JsonNode root = readNextData(listHtml);
        return extractPatchArticles(root, fromYear);
    }

    private String fetchHtml(String url) {
        String normalizedUrl = normalizeUrl(url);
        RestClientException lastException = null;

        for (int attempt = 1; attempt <= MAX_FETCH_ATTEMPTS; attempt++) {
            try {
                HttpRequest request = buildPatchNoteRequest(
                        normalizedUrl,
                        attempt == 1 ? HttpClient.Version.HTTP_2 : HttpClient.Version.HTTP_1_1
                );
                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

                if (response.statusCode() >= 400) {
                    throw new RestClientException("공식 패치노트 HTTP 오류: status=" + response.statusCode());
                }

                String body = response.body();
                if (body == null || body.isBlank()) {
                    throw new RestClientException("공식 패치노트 응답이 비어 있습니다.");
                }
                return body;
            } catch (IOException | InterruptedException | IllegalArgumentException | RestClientException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                lastException = new RestClientException("공식 패치노트 요청 실패: " + normalizedUrl, e);
                if (attempt < MAX_FETCH_ATTEMPTS) {
                    sleepBeforeRetry(attempt);
                }
            }
        }

        throw lastException == null
                ? new RestClientException("공식 패치노트 요청 실패: " + normalizedUrl)
                : lastException;
    }

    private HttpRequest buildPatchNoteRequest(String url, HttpClient.Version version) {
        return HttpRequest.newBuilder(URI.create(url))
                .version(version)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .build();
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(250L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private JsonNode readNextData(String html) throws Exception {
        Document document = Jsoup.parse(html);
        Element nextData = document.getElementById("__NEXT_DATA__");

        if (nextData == null) {
            throw new IllegalStateException("__NEXT_DATA__를 찾을 수 없습니다.");
        }

        return objectMapper.readTree(nextData.data());
    }

    private List<PatchArticle> extractPatchArticles(JsonNode root, int fromYear) {
        Map<String, PatchArticle> articlesByUrl = new LinkedHashMap<>();
        collectPatchArticles(root, fromYear, articlesByUrl);
        return new ArrayList<>(articlesByUrl.values());
    }

    private List<PatchArticle> fallbackPatchArticles(int fromYear) {
        return FALLBACK_2026_PATCH_ARTICLES.stream()
                .filter(article -> parsePatchMajor(article.patchVersion()) >= fromYear % 100)
                .toList();
    }

    private void collectPatchArticles(JsonNode node, int fromYear, Map<String, PatchArticle> articlesByUrl) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            String title = node.path("title").asText("");
            String url = node.path("action").path("payload").path("url").asText("");
            String publishedAt = node.path("publishedAt").asText(node.path("analytics").path("publishDate").asText(""));
            String patchVersion = extractPatchVersion(title, url);

            if (isPatchArticle(title, url, publishedAt, patchVersion, fromYear)) {
                String absoluteUrl = normalizeUrl(url);
                articlesByUrl.putIfAbsent(absoluteUrl, new PatchArticle(patchVersion, title, absoluteUrl, publishedAt));
            }

            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                collectPatchArticles(children.next(), fromYear, articlesByUrl);
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectPatchArticles(child, fromYear, articlesByUrl);
            }
        }
    }

    private boolean isPatchArticle(String title, String url, String publishedAt, String patchVersion, int fromYear) {
        if (!title.contains("패치 노트") || !url.contains("/news/game-updates/") || patchVersion.isBlank()) {
            return false;
        }

        int publishedYear = parseYear(publishedAt);
        if (publishedYear >= fromYear) {
            return true;
        }

        int patchMajor = parsePatchMajor(patchVersion);
        return patchMajor >= fromYear % 100;
    }

    private List<RiotPatchNoteDto.ChampionPatchChange> extractChampionChanges(String patchBody, Set<String> championNames) {
        if (patchBody.isBlank()) {
            return List.of();
        }

        Document document = Jsoup.parse(patchBody);
        List<RiotPatchNoteDto.ChampionPatchChange> changes = new ArrayList<>();

        for (Element title : document.select("h3.change-title")) {
            if (!matchesChampionName(title.text(), championNames)) {
                continue;
            }

            changes.addAll(extractChangeBlock(title));
        }

        return changes;
    }

    private List<RiotPatchNoteDto.ChampionPatchChange> extractChangeBlock(Element title) {
        Map<String, List<String>> itemsBySection = new LinkedHashMap<>();
        String currentSection = "변경 내용";

        Element cursor = title.nextElementSibling();
        while (cursor != null && !isNextChangeTitle(cursor)) {
            if (cursor.hasClass("change-detail-title")) {
                String sectionTitle = cleanText(cursor.text());
                currentSection = sectionTitle.isBlank() ? currentSection : sectionTitle;
            } else if (isTextBlock(cursor)) {
                addItem(itemsBySection, currentSection, cursor.text());
            } else if (cursor.is("ul, ol")) {
                Elements listItems = cursor.select("> li");
                for (Element listItem : listItems) {
                    addItem(itemsBySection, currentSection, listItem.text());
                }
            }
            cursor = cursor.nextElementSibling();
        }

        List<RiotPatchNoteDto.ChampionPatchChange> changes = new ArrayList<>();
        itemsBySection.forEach((sectionTitle, items) -> {
            if (!items.isEmpty()) {
                changes.add(new RiotPatchNoteDto.ChampionPatchChange(sectionTitle, items));
            }
        });
        return changes;
    }

    private boolean isNextChangeTitle(Element element) {
        return element.is("h3.change-title") || element.is("header.header-primary");
    }

    private boolean isTextBlock(Element element) {
        return element.is("blockquote, p") || element.hasClass("blockquote");
    }

    private void addItem(Map<String, List<String>> itemsBySection, String sectionTitle, String value) {
        String text = cleanText(value);
        if (text.isBlank()) {
            return;
        }
        itemsBySection.computeIfAbsent(sectionTitle, ignored -> new ArrayList<>()).add(text);
    }

    private Set<String> resolveChampionNames(String championName) {
        Set<String> names = new LinkedHashSet<>();
        addName(names, championName);

        championRepository.findByNameKo(championName)
                .or(() -> championRepository.findByNameEn(championName))
                .ifPresent(champion -> addChampionNames(names, champion));

        if (names.size() == 1) {
            championRepository.findAll().stream()
                    .filter(champion -> normalize(champion.getNameKo()).equals(normalize(championName))
                            || normalize(champion.getNameEn()).equals(normalize(championName)))
                    .findFirst()
                    .ifPresent(champion -> addChampionNames(names, champion));
        }

        return names;
    }

    private void addChampionNames(Set<String> names, Champion champion) {
        addName(names, champion.getNameKo());
        addName(names, champion.getNameEn());
    }

    private void addName(Set<String> names, String name) {
        if (name != null && !name.isBlank()) {
            names.add(name);
        }
    }

    private boolean matchesChampionName(String value, Set<String> championNames) {
        String normalizedValue = normalize(value);
        return championNames.stream()
                .map(this::normalize)
                .anyMatch(name -> normalizedValue.equals(name)
                        || (name.length() >= 3 && normalizedValue.contains(name)));
    }

    private Optional<String> findTextContaining(JsonNode node, String needle) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }

        if (node.isTextual() && node.asText().contains(needle)) {
            return Optional.of(node.asText());
        }

        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            Optional<String> found = findTextContaining(children.next(), needle);
            if (found.isPresent()) {
                return found;
            }
        }

        return Optional.empty();
    }

    private String extractPatchVersion(String title, String url) {
        Matcher titleMatcher = PATCH_VERSION_PATTERN.matcher(title);
        if (titleMatcher.find()) {
            return titleMatcher.group(1) + "." + Integer.parseInt(titleMatcher.group(2));
        }

        Matcher urlMatcher = PATCH_URL_PATTERN.matcher(url);
        if (urlMatcher.find()) {
            return urlMatcher.group(1) + "." + Integer.parseInt(urlMatcher.group(2));
        }

        return "";
    }

    private int parseYear(String value) {
        try {
            return Integer.parseInt(value.substring(0, 4));
        } catch (Exception ignored) {
            return -1;
        }
    }

    private int parsePatchMajor(String patchVersion) {
        try {
            return Integer.parseInt(patchVersion.split("\\.")[0]);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private String normalizeUrl(String url) {
        if (url.startsWith("http")) {
            return url;
        }
        return OFFICIAL_SITE_URL + url;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String cleanText(String value) {
        return value == null ? "" : value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String getRootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private record PatchArticle(String patchVersion, String title, String url, String publishedAt) {
    }

    private record CacheEntry(RiotPatchNoteDto.ChampionPatchResponse response, Instant expiresAt) {
        boolean isAlive() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    private record PatchArticleCacheEntry(List<PatchArticle> articles, Instant expiresAt) {
        boolean isAlive() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
