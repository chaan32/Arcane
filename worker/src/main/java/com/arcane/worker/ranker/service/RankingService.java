package com.arcane.worker.ranker.service;

import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.exception.fail.CannotFoundSummoner;
import com.arcane.worker.exception.fail.TooManyRequestFail;
import com.arcane.worker.kafka.event.dto.RankingUpdateRequestedEvent;
import com.arcane.worker.ranker.dto.RiotRankerDto;
import com.arcane.worker.ranker.tier.Tier;
import com.arcane.worker.redis.RedisService;
import com.arcane.worker.redis.dto.RedisRankerDto;
import com.arcane.worker.riot.dto.FromRiotRankerResDto;
import com.arcane.worker.riot.dto.ProfileResDto;
import com.arcane.worker.riot.dto.RiotAccountDto;
import com.arcane.worker.riot.service.RiotApiService;
import com.arcane.worker.summoner.entity.SummonerEntity;
import com.arcane.worker.summoner.service.SummonerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {
    private final SummonerService summonerService;
    private final RiotApiService riotApiService;
    private final RedisService redisService;

    public void updateRanking(RankingUpdateRequestedEvent event) throws InterruptedException {
        log.info(logMessage(
                "RankingService.updateRanking",
                "작업 시작",
                "jobId=" + event.jobId() + " | traceId=" + event.traceId()
        ));

        int limit = resolveLimit(event);

        updateTier(Tier.CHALLENGER, limit);
        updateTier(Tier.GRANDMASTER, limit);
        updateTier(Tier.MASTER, limit);
        redisService.updateGlobalRanking();
        log.info(logMessage(
                "RankingService.updateRanking",
                "전체 랭킹 병합",
                "jobId=" + event.jobId() + " | traceId=" + event.traceId()
        ));

        downloadRankersProfile();

        log.info(logMessage(
                "RankingService.updateRanking",
                "작업 완료",
                "jobId=" + event.jobId() + " | traceId=" + event.traceId()
        ));
    }

    private void updateTier(Tier tier, int limit) throws InterruptedException {
        log.info(logMessage(
                "RankingService.updateTier",
                "티어 조회 시작",
                "tier=" + tier + " | limit=" + limit
        ));

        FromRiotRankerResDto leagueByTier = requestLeagueByTier(tier);
        Queue<RedisRankerDto> redisRankerDtos = getRankerObject(leagueByTier, tier, limit);
        redisService.updateRedisRanking(redisRankerDtos, tier);

        log.info(logMessage(
                "RankingService.updateTier",
                "티어 저장 완료",
                "tier=" + tier + " | count=" + redisRankerDtos.size() + " | limit=" + limit
        ));
    }

    private FromRiotRankerResDto requestLeagueByTier(Tier tier) throws InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return riotApiService.getLeagueByTier(tier);
            } catch (TooManyRequestFail e) {
                if (attempt < 2) {
                    log.warn(logMessage(
                            "RankingService.requestLeagueByTier",
                            "429 대기",
                            "tier=" + tier + " | retryAfterSeconds=125 | attempt=" + (attempt + 1) + "/3"
                    ));
                    Thread.sleep(125000);
                    continue;
                }
                throw e;
            }
        }

        throw new IllegalStateException("Riot ranking API retry exhausted. tier=" + tier);
    }

    private Queue<RedisRankerDto> getRankerObject(
            FromRiotRankerResDto dtos,
            Tier tier,
            int limit
    ) throws InterruptedException {
        Queue<RedisRankerDto> redisRankerDtos = new LinkedList<>();

        if (dtos == null || dtos.getEntries() == null) {
            log.warn(logMessage(
                    "RankingService.getRankerObject",
                    "티어 응답 비어있음",
                    "tier=" + tier
            ));
            return redisRankerDtos;
        }

        List<RiotRankerDto> entries = dtos.getEntries();
        entries.sort((left, right) -> Integer.compare(
                right.getLeaguePoints(),
                left.getLeaguePoints()
        ));

        int processedCount = 0;
        for (RiotRankerDto entry : entries) {
            if (processedCount >= limit) {
                break;
            }
            SummonerEntity summoner = updateScore(entry, tier);
            if (summoner == null) {
                continue;
            }

            redisRankerDtos.add(RedisRankerDto.of(summoner));
            processedCount++;
        }

        return redisRankerDtos;
    }

    private int resolveLimit(RankingUpdateRequestedEvent event) {
        if (event.payload() == null || event.payload().limit() == null || event.payload().limit() <= 0) {
            return Integer.MAX_VALUE;
        }

        return event.payload().limit();
    }

    private SummonerEntity updateScore(RiotRankerDto ranker, Tier tier) throws InterruptedException {
        String puuid = ranker.getPuuid();
        SummonerEntity summoner = getSummoner(puuid);

        if (summoner == null) {
            return null;
        }

        return summonerService.updateRankerScore(summoner.getPuuid(), tier, ranker);
    }

    private RiotAccountDto getNewSummonerInformation(String puuid) throws InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                RiotAccountDto summonerByPuuid = riotApiService.getSummonerByPuuid(puuid);
                if (summonerByPuuid != null) {
                    return summonerByPuuid;
                }
            } catch (TooManyRequestFail e) {
                if (attempt < 2) {
                    log.warn(logMessage(
                            "RankingService.getNewSummonerInformation",
                            "429 대기",
                            "retryAfterSeconds=125 | attempt=" + (attempt + 1) + "/3 | puuid=" + puuid
                    ));
                    Thread.sleep(125000);
                }
            } catch (CannotFoundSummoner e) {
                log.info(logMessage(
                        "RankingService.getNewSummonerInformation",
                        "소환사 없음",
                        "puuid=" + puuid
                ));
                return null;
            }
        }

        log.error(logMessage(
                "RankingService.getNewSummonerInformation",
                "조회 실패",
                "retryCount=3 | puuid=" + puuid
        ));
        return null;
    }

    private void downloadRankersProfile() throws InterruptedException {
        log.info(logMessage(
                "RankingService.downloadRankersProfile",
                "프로필 동기화 시작",
                "tiers=challenger,grandmaster,master"
        ));

        storeProfile(redisService.getRankersPuuid("ranking:challenger"));
        log.info(logMessage(
                "RankingService.downloadRankersProfile",
                "프로필 동기화 완료",
                "tier=" + Tier.CHALLENGER
        ));

        storeProfile(redisService.getRankersPuuid("ranking:grandmaster"));
        log.info(logMessage(
                "RankingService.downloadRankersProfile",
                "프로필 동기화 완료",
                "tier=" + Tier.GRANDMASTER
        ));

        storeProfile(redisService.getRankersPuuid("ranking:master"));
        log.info(logMessage(
                "RankingService.downloadRankersProfile",
                "프로필 동기화 완료",
                "tier=" + Tier.MASTER
        ));
    }

    private void storeProfile(List<String> puuids) throws InterruptedException {
        int index = 1;

        for (String puuid : puuids) {
            SummonerEntity summoner = getSummoner(puuid);
            if (summoner == null) {
                continue;
            }

            if (summoner.getIconId() == null || summoner.getLevel() == null) {
                log.info(logMessage(
                        "RankingService.storeProfile",
                        "프로필 요청",
                        "index=" + index++ + " | puuid=" + puuid
                ));

                ProfileResDto profileResDto = requestProfileToRiot(puuid);
                summonerService.updateProfile(puuid, profileResDto);
            }
        }
    }

    private ProfileResDto requestProfileToRiot(String puuid) throws InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                ProfileResDto profileInfo = riotApiService.getProfileInfo(puuid);
                if (profileInfo != null) {
                    return profileInfo;
                }
            } catch (TooManyRequestFail e) {
                if (attempt < 2) {
                    log.warn(logMessage(
                            "RankingService.requestProfileToRiot",
                            "429 대기",
                            "retryAfterSeconds=125 | attempt=" + (attempt + 1) + "/3 | puuid=" + puuid
                    ));
                    Thread.sleep(125000);
                }
            } catch (CannotFoundSummoner e) {
                log.info(logMessage(
                        "RankingService.requestProfileToRiot",
                        "프로필 없음",
                        "puuid=" + puuid
                ));
                return null;
            }
        }

        log.error(logMessage(
                "RankingService.requestProfileToRiot",
                "프로필 조회 실패",
                "retryCount=3 | puuid=" + puuid
        ));
        return null;
    }

    private SummonerEntity getSummoner(String puuid) throws InterruptedException {
        Optional<SummonerEntity> optionalSummoner = summonerService.getSummonerByPuuid(puuid);

        if (optionalSummoner.isPresent()) {
            return optionalSummoner.get();
        }

        log.info(logMessage(
                "RankingService.getSummoner",
                "DB 소환사 없음",
                "puuid=" + puuid
        ));

        RiotAccountDto riotAccountDto = getNewSummonerInformation(puuid);
        if (riotAccountDto == null) {
            return null;
        }

        return summonerService.saveSummoner(riotAccountDto);
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("랭킹 업데이트", method, status, detail);
    }
}
