package com.arcane.Arcane.common.Logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class ApiLogSupport {
    public static final String REQUEST_START = "[API][HTTP 요청][{}][요청 시작] method={} | uri={} | user={} | clientIp={} | query={}";
    public static final String REQUEST_SUCCESS = "[API][HTTP 요청][{}][요청 성공] method={} | uri={} | status={} | elapsedMs={} | user={}";
    public static final String REQUEST_FAIL = "[API][HTTP 요청][{}][요청 실패] method={} | uri={} | status={} | elapsedMs={} | user={} | exception={} | message={}";
    public static final String EXCEPTION_HANDLED = "[API][예외 처리][GlobalExceptionHandler][응답 생성] status={} | method={} | uri={} | exception={} | message={}";
    public static final String BUSINESS_FLOW = "[API][비즈니스 흐름][{}][{}] result={} | detail={}";

    private static final String API_SERVER_NAME = "API";

    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "token",
            "access_token",
            "refresh_token",
            "password",
            "loginpw",
            "login_pw",
            "clientsecret",
            "client_secret",
            "authorization"
    );

    private ApiLogSupport() {
    }

    public static String api(String task, String method, String status, String detail) {
        return message(API_SERVER_NAME, task, method, status, detail);
    }

    public static String message(String server, String task, String method, String status, String detail) {
        return "[" + clean(server) + "]"
                + "[" + clean(task) + "]"
                + "[" + clean(method) + "]"
                + "[" + clean(status) + "] "
                + clean(detail);
    }

    public static HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    public static String method(HttpServletRequest request) {
        return request == null ? "-" : request.getMethod();
    }

    public static String uri(HttpServletRequest request) {
        return request == null ? "-" : request.getRequestURI();
    }

    public static String query(HttpServletRequest request) {
        if (request == null) {
            return "-";
        }
        return sanitizeQuery(request.getQueryString());
    }

    public static String user(HttpServletRequest request) {
        if (request == null) {
            return "anonymous";
        }

        Principal principal = request.getUserPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return "anonymous";
        }

        return principal.getName();
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "-";
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    public static String exceptionName(Throwable throwable) {
        return throwable == null ? "-" : throwable.getClass().getSimpleName();
    }

    public static String exceptionMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "-";
        }

        return clean(throwable.getMessage());
    }

    public static String sanitizeQuery(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            return "-";
        }

        return Arrays.stream(queryString.split("&"))
                .map(ApiLogSupport::maskQueryPair)
                .collect(Collectors.joining("&"));
    }

    private static String maskQueryPair(String pair) {
        int valueSeparator = pair.indexOf('=');
        String key = valueSeparator < 0 ? pair : pair.substring(0, valueSeparator);
        String normalizedKey = key.toLowerCase(Locale.ROOT);

        boolean sensitive = SENSITIVE_QUERY_KEYS.stream().anyMatch(normalizedKey::contains);
        if (!sensitive) {
            return pair;
        }

        return key + "=***";
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        return value.replaceAll("\\s+", " ").trim();
    }
}
