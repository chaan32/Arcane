package com.arcane.worker.riot.service;

import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.exception.fail.RiotApiFatalException;
import com.arcane.worker.exception.fail.RiotApiRateLimitException;
import com.arcane.worker.exception.fail.RiotApiRetryableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiotMatchApiService {
    private static final String ASIA_BASE_URL = "https://asia.api.riotgames.com";
    private static final long MIN_REQUEST_INTERVAL_MS = 1_250L;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private final Object throttleMonitor = new Object();
    private long nextRequestAt = 0L;

    @Value("${riot.api-key}")
    private String apiKey;

    public List<String> getMatchIdsByPuuid(String puuid, int count, Integer queueId) {
        String url = ASIA_BASE_URL
                + "/lol/match/v5/matches/by-puuid/"
                + puuid
                + "/ids?start=0&count="
                + count
                + (queueId == null ? "" : "&queue=" + queueId);

        try {
            throttle();
            log.info(logMessage(
                    "RiotMatchApiService.getMatchIdsByPuuid",
                    "매치 ID 요청",
                    "puuid=" + puuid + " | count=" + count + " | queueId=" + queueId
            ));

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    String.class
            );

            JsonNode body = objectMapper.readTree(response.getBody());
            List<String> matchIds = new ArrayList<>();
            if (body.isArray()) {
                for (JsonNode node : body) {
                    if (!node.isNull() && !node.asText().isBlank()) {
                        matchIds.add(node.asText());
                    }
                }
            }

            log.info(logMessage(
                    "RiotMatchApiService.getMatchIdsByPuuid",
                    "매치 ID 성공",
                    "puuid=" + puuid + " | count=" + matchIds.size()
            ));
            return matchIds;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw rateLimit("매치 ID 429", e);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn(logMessage(
                    "RiotMatchApiService.getMatchIdsByPuuid",
                    "매치 ID 없음",
                    "puuid=" + puuid
            ));
            return List.of();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new RiotApiFatalException("Riot API 인증 실패. status=" + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            throw new RiotApiRetryableException("Riot API 서버 오류. status=" + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new RiotApiRetryableException("Riot API 연결 실패. reason=" + e.getMessage(), e);
        } catch (Exception e) {
            throw new RiotApiRetryableException("Riot 매치 ID 응답 처리 실패. reason=" + e.getMessage(), e);
        }
    }

    public JsonNode getMatchDetail(String matchId) {
        String url = ASIA_BASE_URL + "/lol/match/v5/matches/" + matchId;

        try {
            throttle();
            log.info(logMessage(
                    "RiotMatchApiService.getMatchDetail",
                    "매치 상세 요청",
                    "matchId=" + matchId
            ));

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    String.class
            );

            JsonNode body = objectMapper.readTree(response.getBody());
            log.info(logMessage(
                    "RiotMatchApiService.getMatchDetail",
                    "매치 상세 성공",
                    "matchId=" + matchId
            ));
            return body;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw rateLimit("매치 상세 429", e);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn(logMessage(
                    "RiotMatchApiService.getMatchDetail",
                    "매치 상세 없음",
                    "matchId=" + matchId
            ));
            return null;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new RiotApiFatalException("Riot API 인증 실패. status=" + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            throw new RiotApiRetryableException("Riot API 서버 오류. status=" + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new RiotApiRetryableException("Riot API 연결 실패. reason=" + e.getMessage(), e);
        } catch (Exception e) {
            throw new RiotApiRetryableException("Riot 매치 상세 응답 처리 실패. reason=" + e.getMessage(), e);
        }
    }

    private RiotApiRateLimitException rateLimit(String status, HttpClientErrorException.TooManyRequests e) {
        long retryAfterSeconds = retryAfterSeconds(e.getResponseHeaders());
        log.warn(logMessage(
                "RiotMatchApiService.rateLimit",
                status,
                "retryAfterSeconds=" + retryAfterSeconds
        ));
        return new RiotApiRateLimitException(status, retryAfterSeconds);
    }

    private long retryAfterSeconds(HttpHeaders headers) {
        if (headers != null) {
            String retryAfter = headers.getFirst("Retry-After");
            if (retryAfter != null && !retryAfter.isBlank()) {
                try {
                    return Math.max(1L, Long.parseLong(retryAfter));
                } catch (NumberFormatException ignored) {
                    return 120L;
                }
            }
        }

        return 120L;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);
        return headers;
    }

    private void throttle() {
        synchronized (throttleMonitor) {
            long now = System.currentTimeMillis();
            long waitMs = nextRequestAt - now;
            if (waitMs > 0) {
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RiotApiRetryableException("Riot API throttle interrupted.", e);
                }
            }

            nextRequestAt = System.currentTimeMillis() + MIN_REQUEST_INTERVAL_MS;
        }
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("Riot 매치 수집", method, status, detail);
    }
}
