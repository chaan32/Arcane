package com.arcane.Arcane.common.Logging;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReadableActivityLog {
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("\\b(INFO|WARN|ERROR)\\b");
    private static final Pattern TRACE_PATTERN = Pattern.compile("\\btrace=([^\\s]+)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("(^| \\| )([a-zA-Z][a-zA-Z0-9_-]*)=([^|]*)(?= \\| |$)");
    private static final Set<String> NOISY_ADMIN_URIS = Set.of(
            "/api/v1/admin/logs",
            "/api/v1/admin/dashboard",
            "/api/v1/admin/server-status"
    );

    private ReadableActivityLog() {
    }

    public static String describe(HttpServletRequest request, int status, Throwable throwable) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        boolean failed = throwable != null || status >= 400;
        boolean rateLimited = status == 429 || containsRateLimitMessage(throwable == null ? null : throwable.getMessage());
        String riotId = riotIdFromQuery(query).orElse("해당 소환사");

        return sanitize(describe(uri, query, riotId, failed, rateLimited));
    }

    public static Optional<Map<String, Object>> parse(String line, String source) {
        Map<String, String> fields = fields(line);
        String uri = fields.get("uri");

        if (uri == null || line.contains("[요청 시작]") || NOISY_ADMIN_URIS.contains(uri)) {
            return Optional.empty();
        }

        Integer status = parseInteger(fields.get("status")).orElse(null);
        Long elapsedMs = parseLong(fields.get("elapsedMs")).orElse(null);
        String message = fields.get("message");
        boolean failed = line.contains("[요청 실패]") || (status != null && status >= 400);
        boolean rateLimited = status != null && status == 429 || containsRateLimitMessage(line) || containsRateLimitMessage(message);
        String riotId = riotIdFromQuery(fields.get("query")).orElse("해당 소환사");
        String activity = fields.get("activity");

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("occurredAt", extract(TIMESTAMP_PATTERN, line).orElse(null));
        entry.put("level", extract(LEVEL_PATTERN, line).orElse(failed ? "ERROR" : "INFO"));
        entry.put("category", category(uri));
        entry.put("message", activity == null || activity.isBlank()
                ? describe(uri, fields.get("query"), riotId, failed, rateLimited)
                : activity.trim());
        entry.put("detail", detail(uri, fields, message));
        entry.put("status", status);
        entry.put("elapsedMs", elapsedMs);
        entry.put("user", blankToNull(fields.get("user")));
        entry.put("traceId", extract(TRACE_PATTERN, line).orElse(null));
        entry.put("source", source);

        return Optional.of(entry);
    }

    private static String describe(String uri, String query, String riotId, boolean failed, boolean rateLimited) {
        ActionPhrase action = actionPhrase(uri, query, riotId);

        if (!failed) {
            return action.success();
        }

        if (rateLimited) {
            return action.failureStem() + " Riot API 요청 제한으로 실패했습니다.";
        }

        return action.failureStem() + " 실패했습니다.";
    }

    private static ActionPhrase actionPhrase(String uri, String query, String riotId) {
        if (uri == null) {
            return new ActionPhrase("요청을 처리했습니다.", "요청을 처리하다가");
        }

        if (uri.startsWith("/api/v1/summoner/matches")) {
            boolean refresh = containsQueryValue(query, "refresh", "true");
            return refresh
                    ? new ActionPhrase(riotId + "로 전적을 갱신했습니다.", riotId + "로 전적을 갱신하다가")
                    : new ActionPhrase(riotId + "로 전적을 검색했습니다.", riotId + "로 전적을 검색하다가");
        }

        if (uri.startsWith("/api/v1/summoner/profile")) {
            return new ActionPhrase(riotId + "로 소환사 프로필을 조회했습니다.", riotId + "로 소환사 프로필을 조회하다가");
        }

        if (uri.startsWith("/api/v1/summoner/tier")) {
            return new ActionPhrase(riotId + "로 티어 정보를 조회했습니다.", riotId + "로 티어 정보를 조회하다가");
        }

        if (uri.startsWith("/api/v1/summoner/mastery")) {
            return new ActionPhrase(riotId + "로 챔피언 숙련도를 조회했습니다.", riotId + "로 챔피언 숙련도를 조회하다가");
        }

        if (uri.startsWith("/api/v1/summoner/contain")) {
            return new ActionPhrase("소환사 검색어 자동완성을 조회했습니다.", "소환사 검색어 자동완성을 조회하다가");
        }

        if (uri.startsWith("/api/v1/admin/ranking-update")) {
            return new ActionPhrase("관리자가 랭킹 업데이트를 요청했습니다.", "랭킹 업데이트를 요청하다가");
        }

        if (uri.startsWith("/api/v1/admin/dataset-collection")) {
            return new ActionPhrase("관리자가 Riot 매치 데이터 수집을 요청했습니다.", "Riot 매치 데이터 수집을 요청하다가");
        }

        if (uri.startsWith("/api/v1/admin/champion-analysis")) {
            return new ActionPhrase("관리자가 챔피언 분석 작업을 요청했습니다.", "챔피언 분석 작업을 요청하다가");
        }

        if (uri.startsWith("/api/v1/admin/game-data-sync")) {
            return new ActionPhrase("관리자가 게임 데이터 동기화를 요청했습니다.", "게임 데이터 동기화를 요청하다가");
        }

        if (uri.startsWith("/api/v1/strategy")) {
            return new ActionPhrase("공략 글을 처리했습니다.", "공략 글을 처리하다가");
        }

        if (uri.startsWith("/api/v1/comment")) {
            return new ActionPhrase("댓글을 처리했습니다.", "댓글을 처리하다가");
        }

        if (uri.startsWith("/api/v1/chat")) {
            return new ActionPhrase("채팅 요청을 처리했습니다.", "채팅 요청을 처리하다가");
        }

        if (uri.startsWith("/api/v1/storage/guide-images")) {
            return new ActionPhrase("공략 글 이미지를 업로드했습니다.", "공략 글 이미지를 업로드하다가");
        }

        return new ActionPhrase("API 요청을 처리했습니다.", "API 요청을 처리하다가");
    }

    private static String category(String uri) {
        if (uri == null) return "API";
        if (uri.startsWith("/api/v1/summoner")) return "전적/소환사";
        if (uri.startsWith("/api/v1/admin")) return "관리자 작업";
        if (uri.startsWith("/api/v1/strategy")) return "공략";
        if (uri.startsWith("/api/v1/comment")) return "댓글";
        if (uri.startsWith("/api/v1/chat")) return "채팅";
        if (uri.startsWith("/api/v1/storage")) return "파일 업로드";
        return "API";
    }

    private static String detail(String uri, Map<String, String> fields, String message) {
        StringBuilder detail = new StringBuilder();
        appendDetail(detail, "method", fields.get("method"));
        appendDetail(detail, "uri", uri);
        appendDetail(detail, "query", fields.get("query"));
        appendDetail(detail, "message", message);
        return detail.toString();
    }

    private static void appendDetail(StringBuilder detail, String key, String value) {
        String normalized = blankToNull(value);
        if (normalized == null || normalized.equals("-")) {
            return;
        }

        if (!detail.isEmpty()) {
            detail.append(" · ");
        }
        detail.append(key).append("=").append(normalized);
    }

    private static Map<String, String> fields(String line) {
        Map<String, String> fields = new LinkedHashMap<>();
        String payload = line;
        int payloadStart = line.lastIndexOf("] ");
        if (payloadStart >= 0) {
            payload = line.substring(payloadStart + 2).trim();
        }

        Matcher matcher = FIELD_PATTERN.matcher(payload);
        while (matcher.find()) {
            fields.put(matcher.group(2), matcher.group(3).trim());
        }
        return fields;
    }

    private static Optional<String> riotIdFromQuery(String query) {
        Map<String, String> params = queryParams(query);
        String gameName = params.get("gameName");
        String tagLine = params.get("tagLine");

        if (blankToNull(gameName) != null && blankToNull(tagLine) != null) {
            return Optional.of(gameName + "#" + tagLine);
        }

        return Optional.empty();
    }

    private static Map<String, String> queryParams(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query == null || query.isBlank() || query.equals("-")) {
            return params;
        }

        for (String pair : query.split("&")) {
            int separatorIndex = pair.indexOf('=');
            if (separatorIndex < 0) {
                continue;
            }

            String key = decode(pair.substring(0, separatorIndex));
            String value = decode(pair.substring(separatorIndex + 1));
            params.put(key, value);
        }
        return params;
    }

    private static boolean containsQueryValue(String query, String key, String expectedValue) {
        return expectedValue.equalsIgnoreCase(queryParams(query).get(key));
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return value;
        }
    }

    private static Optional<String> extract(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.ofNullable(blankToNull(matcher.group(1)));
    }

    private static Optional<Integer> parseInteger(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static boolean containsRateLimitMessage(String value) {
        if (value == null) {
            return false;
        }
        String lowerValue = value.toLowerCase();
        return lowerValue.contains("429")
                || lowerValue.contains("too many requests")
                || lowerValue.contains("rate limit")
                || lowerValue.contains("ratelimit")
                || value.contains("요청 제한")
                || value.contains("리미트");
    }

    private static String sanitize(String value) {
        return value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ActionPhrase(String success, String failureStem) {
    }
}
