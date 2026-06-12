package com.arcane.Arcane.common.Logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class ApiLoggingInterceptor implements HandlerInterceptor {
    private static final String START_TIME_ATTRIBUTE = ApiLoggingInterceptor.class.getName() + ".startTime";
    private static final String TRACE_ID_ATTRIBUTE = ApiLoggingInterceptor.class.getName() + ".traceId";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        String traceId = resolveTraceId(request);
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        MDC.put(TRACE_ID_MDC_KEY, traceId);

        log.info(ApiLogSupport.api(
                httpTask(request),
                handlerName(handler),
                "요청 시작",
                "method=" + ApiLogSupport.method(request)
                        + " | uri=" + ApiLogSupport.uri(request)
                        + " | user=" + ApiLogSupport.user(request)
                        + " | clientIp=" + ApiLogSupport.clientIp(request)
                        + " | query=" + ApiLogSupport.query(request)
        ));

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        restoreTraceId(request);
        long elapsedMs = elapsedMs(request);
        int status = response.getStatus();

        try {
            if (exception == null && status < 400) {
                log.info(ApiLogSupport.api(
                        httpTask(request),
                        handlerName(handler),
                        "요청 성공",
                        "method=" + ApiLogSupport.method(request)
                                + " | uri=" + ApiLogSupport.uri(request)
                                + " | status=" + status
                                + " | elapsedMs=" + elapsedMs
                                + " | user=" + ApiLogSupport.user(request)
                ));
                return;
            }

            if (status >= 500 || exception != null && status < 400) {
                log.error(ApiLogSupport.api(
                                httpTask(request),
                                handlerName(handler),
                                "요청 실패",
                                "method=" + ApiLogSupport.method(request)
                                        + " | uri=" + ApiLogSupport.uri(request)
                                        + " | status=" + status
                                        + " | elapsedMs=" + elapsedMs
                                        + " | user=" + ApiLogSupport.user(request)
                                        + " | exception=" + ApiLogSupport.exceptionName(exception)
                                        + " | message=" + ApiLogSupport.exceptionMessage(exception)
                        ),
                        exception
                );
                return;
            }

            log.warn(ApiLogSupport.api(
                    httpTask(request),
                    handlerName(handler),
                    "요청 경고",
                    "method=" + ApiLogSupport.method(request)
                            + " | uri=" + ApiLogSupport.uri(request)
                            + " | status=" + status
                            + " | elapsedMs=" + elapsedMs
                            + " | user=" + ApiLogSupport.user(request)
                            + " | exception=" + ApiLogSupport.exceptionName(exception)
                            + " | message=" + ApiLogSupport.exceptionMessage(exception)
            ));
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private long elapsedMs(HttpServletRequest request) {
        Object startedAt = request.getAttribute(START_TIME_ATTRIBUTE);
        if (!(startedAt instanceof Long startTime)) {
            return -1L;
        }
        return System.currentTimeMillis() - startTime;
    }

    private String handlerName(Object handler) {
        if (handler == null) {
            return "-";
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return handler.getClass().getSimpleName();
        }

        return handlerMethod.getBeanType().getSimpleName() + "." + handlerMethod.getMethod().getName();
    }

    private String httpTask(HttpServletRequest request) {
        return "HTTP 요청 - " + requestPurpose(request);
    }

    private String requestPurpose(HttpServletRequest request) {
        String path = ApiLogSupport.uri(request);
        String method = ApiLogSupport.method(request);

        if (path.startsWith("/api/v1/summoner/tier")) {
            return "소환사 티어 조회";
        }
        if (path.startsWith("/api/v1/summoner/mastery")) {
            return "챔피언 숙련도 조회";
        }
        if (path.startsWith("/api/v1/summoner/matches")) {
            return "전적 목록 조회";
        }
        if (path.startsWith("/api/v1/summoner/match/timeline")) {
            return "전적 빌드 타임라인 조회";
        }
        if (path.startsWith("/api/v1/summoner/match/summary")) {
            return "전적 상세 요약 조회";
        }
        if (path.startsWith("/api/v1/summoner/match-id")) {
            return "전적 매치 ID 조회";
        }
        if (path.startsWith("/api/v1/summoner/matchInfo")) {
            return "전적 상세 조회";
        }
        if (path.startsWith("/api/v1/summoner/most")) {
            return "모스트 챔피언 조회";
        }
        if (path.startsWith("/api/v1/summoner/profile")) {
            return "소환사 프로필 조회";
        }
        if (path.startsWith("/api/v1/summoner/contain")) {
            return "소환사 검색어 자동완성";
        }
        if (path.startsWith("/api/v1/summoner/getPython")) {
            return "AI 점수 서버 요청";
        }
        if (path.startsWith("/api/v1/summoner")) {
            return "소환사 정보 처리";
        }

        if (path.startsWith("/api/v1/ranker/all")) {
            return "전체 랭킹 조회";
        }
        if (path.startsWith("/api/v1/ranker/challenger")) {
            return "챌린저 랭킹 조회";
        }
        if (path.startsWith("/api/v1/ranker/grandmaster")) {
            return "그랜드마스터 랭킹 조회";
        }
        if (path.startsWith("/api/v1/ranker/master")) {
            return "마스터 랭킹 조회";
        }
        if (path.startsWith("/api/v1/ranker")) {
            return "랭킹 조회";
        }

        if (path.startsWith("/api/v1/admin/dashboard")) {
            return "관리자 대시보드 조회";
        }
        if (path.startsWith("/api/v1/admin/ranking-scheduler")) {
            return "랭킹 자동 업데이트 설정";
        }
        if (path.startsWith("/api/v1/admin/server-status")) {
            return "서버 상태 조회";
        }
        if (path.startsWith("/api/v1/admin/logs")) {
            return "관리자 로그 조회";
        }
        if (path.startsWith("/api/v1/admin/ranking-update")) {
            return "랭킹 업데이트 작업 발행";
        }
        if (path.startsWith("/api/v1/admin")) {
            return "관리자 기능 처리";
        }

        if (path.startsWith("/api/v1/chat/rooms") && "GET".equals(method)) {
            return "채팅방 목록 조회";
        }
        if (path.startsWith("/api/v1/chat/guides") && path.endsWith("/room")) {
            return "공략 채팅방 생성 또는 조회";
        }
        if (path.startsWith("/api/v1/chat/rooms") && path.endsWith("/messages")) {
            return "채팅 메시지 전송";
        }
        if (path.startsWith("/api/v1/chat/rooms") && path.endsWith("/block")) {
            return "채팅방 차단";
        }
        if (path.startsWith("/api/v1/chat/rooms") && "DELETE".equals(method)) {
            return "채팅방 목록 삭제";
        }
        if (path.startsWith("/api/v1/chat")) {
            return "채팅 기능 처리";
        }

        if (path.startsWith("/api/v1/strategy/upload")) {
            return "공략 작성";
        }
        if (path.startsWith("/api/v1/strategy/find/all")) {
            return "공략 목록 조회";
        }
        if (path.startsWith("/api/v1/strategy/find/champion")) {
            return "챔피언별 공략 조회";
        }
        if (path.startsWith("/api/v1/strategy/find")) {
            return "공략 상세 조회";
        }
        if (path.startsWith("/api/v1/strategy/edit")) {
            return "공략 수정";
        }
        if (path.startsWith("/api/v1/strategy/delete")) {
            return "공략 삭제";
        }
        if (path.startsWith("/api/v1/strategy/detail") && path.endsWith("/likes")) {
            return "공략 좋아요 처리";
        }
        if (path.startsWith("/api/v1/strategy/detail") && path.endsWith("/dislikes")) {
            return "공략 싫어요 처리";
        }
        if (path.startsWith("/api/v1/strategy")) {
            return "공략 기능 처리";
        }

        if (path.startsWith("/api/v1/comment") && path.contains("/write/guide")) {
            return "공략 댓글 작성";
        }
        if (path.startsWith("/api/v1/comment") && path.contains("/write/patchNote")) {
            return "패치노트 댓글 작성";
        }
        if (path.startsWith("/api/v1/comment") && path.endsWith("/edit")) {
            return "댓글 수정";
        }
        if (path.startsWith("/api/v1/comment") && path.endsWith("/delete")) {
            return "댓글 삭제";
        }
        if (path.startsWith("/api/v1/comment/show") && path.endsWith("/likes")) {
            return "댓글 좋아요 처리";
        }
        if (path.startsWith("/api/v1/comment/show") && path.endsWith("/dislikes")) {
            return "댓글 싫어요 처리";
        }
        if (path.startsWith("/api/v1/comment")) {
            return "댓글 조회";
        }

        if (path.startsWith("/api/v1/patchnote/riot/champion")) {
            return "챔피언 패치노트 조회";
        }
        if (path.startsWith("/api/v1/patchnote/riot/cache")) {
            return "공식 패치노트 캐시 삭제";
        }
        if (path.startsWith("/api/v1/patchnote/riot")) {
            return "공식 패치노트 목록 조회";
        }
        if (path.startsWith("/api/v1/patchnote/upload")) {
            return "패치노트 작성";
        }
        if (path.startsWith("/api/v1/patchnote/find/all")) {
            return "패치노트 목록 조회";
        }
        if (path.startsWith("/api/v1/patchnote/find")) {
            return "패치노트 상세 조회";
        }
        if (path.startsWith("/api/v1/patchnote/edit")) {
            return "패치노트 수정";
        }
        if (path.startsWith("/api/v1/patchnote/delete")) {
            return "패치노트 삭제";
        }
        if (path.startsWith("/api/v1/patchnote/show") && path.endsWith("/likes")) {
            return "패치노트 좋아요 처리";
        }
        if (path.startsWith("/api/v1/patchnote/show") && path.endsWith("/dislikes")) {
            return "패치노트 싫어요 처리";
        }
        if (path.startsWith("/api/v1/patchnote")) {
            return "패치노트 기능 처리";
        }

        if (path.startsWith("/api/v1/user/login")) {
            return "일반 로그인";
        }
        if (path.startsWith("/api/v1/user/signup")) {
            return "회원가입";
        }
        if (path.startsWith("/api/v1/user/onboarding")) {
            return "OAuth 온보딩 저장";
        }
        if (path.startsWith("/api/v1/user/me/nickname")) {
            return "내 닉네임 수정";
        }
        if (path.startsWith("/api/v1/user/me")) {
            return "내 정보 조회";
        }
        if (path.startsWith("/api/v1/user/oauth/link-intent")) {
            return "소셜 계정 연동 준비";
        }
        if (path.startsWith("/api/v1/user/check/nickName")) {
            return "닉네임 중복 확인";
        }
        if (path.startsWith("/api/v1/user/check/loginId")) {
            return "로그인 ID 중복 확인";
        }
        if (path.startsWith("/api/v1/user/check/gameName")) {
            return "라이엇 계정명 중복 확인";
        }
        if (path.startsWith("/api/v1/user")) {
            return "사용자 기능 처리";
        }

        if (path.startsWith("/api/v1/statistics/tier")) {
            return "챔피언 티어 통계 조회";
        }
        if (path.startsWith("/api/v1/statistics/championDetail")) {
            return "챔피언 상세 통계 조회";
        }
        if (path.startsWith("/api/v1/statistics/champions/all")) {
            return "전체 챔피언 통계 조회";
        }
        if (path.startsWith("/api/v1/statistics")) {
            return "통계 기능 처리";
        }

        if (path.startsWith("/api/v1/champion/id")) {
            return "챔피언 ID 조회";
        }
        if (path.startsWith("/api/v1/champion/name")) {
            return "챔피언 이름 조회";
        }
        if (path.startsWith("/api/v1/champion")) {
            return "챔피언 데이터 조회";
        }
        if (path.startsWith("/api/v1/rune")) {
            return "룬 데이터 조회";
        }
        if (path.startsWith("/api/v1/summoner-spell")) {
            return "소환사 주문 데이터 조회";
        }

        if (path.startsWith("/api/v1/tune/learning-data")) {
            return "AI 학습 데이터 조회";
        }
        if (path.startsWith("/api/v1/tune/learning")) {
            return "AI 튜닝 페이지 조회";
        }
        if (path.startsWith("/api/v1/tune/submit")) {
            return "AI 점수 라벨 제출";
        }
        if (path.startsWith("/api/v1/tune")) {
            return "AI 튜닝 기능 처리";
        }

        if (path.startsWith("/import/update-ranker/directly")) {
            return "직접 랭킹 업데이트";
        }
        if (path.startsWith("/import/store-rankers")) {
            return "티어별 랭커 저장";
        }
        if (path.startsWith("/import/get/current-patchVersion")) {
            return "현재 패치 버전 조회";
        }
        if (path.startsWith("/import/champion")) {
            return "챔피언 데이터 적재";
        }
        if (path.startsWith("/import/rune")) {
            return "룬 데이터 적재";
        }
        if (path.startsWith("/import/spell")) {
            return "소환사 주문 데이터 적재";
        }
        if (path.startsWith("/import/iconAndLevel")) {
            return "소환사 아이콘 레벨 적재";
        }
        if (path.startsWith("/import")) {
            return "데이터 적재 기능 처리";
        }

        if (path.startsWith("/api/waiting")) {
            return "대기열 테스트 처리";
        }
        if (path.startsWith("/actuator")) {
            return "서버 모니터링 지표 조회";
        }

        return "일반 API 처리";
    }

    private String resolveTraceId(HttpServletRequest request) {
        String headerTraceId = request.getHeader("X-Trace-Id");
        if (headerTraceId != null && !headerTraceId.isBlank()) {
            return TraceIds.normalize(headerTraceId);
        }

        return TraceIds.newTraceId();
    }

    private void restoreTraceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TRACE_ID_ATTRIBUTE);
        if (traceId instanceof String value && !value.isBlank()) {
            MDC.put(TRACE_ID_MDC_KEY, value);
        }
    }
}
