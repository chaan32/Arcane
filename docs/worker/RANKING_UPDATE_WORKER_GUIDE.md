# Worker Ranking Update Guide

이 문서는 `worker`가 1차적으로 담당할 작업인 **랭킹 업데이트**를 구현하기 위해 추가로 작성해야 하는 코드와 동작 원리를 정리한다.

현재 목표는 다음 흐름이다.

```text
API 서버 관리자 요청
-> Kafka topic: arcane.ranking.update.requested
-> worker RankingUpdateConsumer
-> Riot 랭커 API 조회
-> summoner 테이블 저장/갱신
-> Redis ranking:* 키 갱신
-> Kafka completed/failed topic 발행
```

## 1. 핵심 판단

백엔드의 `Summoner` domain class를 그대로 worker에 복사하지 않는다.

이유:

- 백엔드 `Summoner`는 `MatchParticipant`, 여러 DTO, Riot API 응답 DTO 등 의존성이 많다.
- worker는 전체 백엔드 도메인을 소유하는 서버가 아니라, 랭킹 갱신 작업만 처리하는 작업 서버다.
- 따라서 worker에는 `summoner` 테이블 중 랭킹 갱신에 필요한 컬럼만 매핑한 최소 entity를 둔다.

worker에서 필요한 최소 데이터:

- Riot API에서 받은 랭커 정보: `puuid`, `leaguePoints`, `wins`, `losses`
- PUUID로 조회한 Riot 계정 정보: `gameName`, `tagLine`
- DB 갱신 대상: `summoner` 테이블의 솔랭 티어/LP/승/패
- Redis 저장 대상: `puuid`, `gameName`, `tagLine`, `tier`, `lp`, `wins`, `losses`, `icon`, `level`

## 2. 추가해야 하는 패키지 구조

```text
worker/src/main/java/com/arcane/worker
 ├─ ranker
 │  ├─ domain
 │  │  └─ Tier.java
 │  ├─ dto
 │  │  ├─ FromRiotRankerResDto.java
 │  │  └─ RiotRankerDto.java
 │  └─ service
 │     └─ RankingService.java
 ├─ summoner
 │  ├─ domain
 │  │  └─ SummonerEntity.java
 │  ├─ repository
 │  │  └─ SummonerRepository.java
 │  └─ service
 │     └─ SummonerService.java
 ├─ riot
 │  ├─ dto
 │  │  └─ RiotAccountDto.java
 │  └─ service
 │     └─ RiotApiService.java
 └─ redis
    ├─ RedisService.java
    └─ dto
       └─ RedisRankerDto.java
```

이미 있는 파일도 있고, 일부는 수정이 필요하다.

## 3. Tier enum

파일:

```text
src/main/java/com/arcane/worker/ranker/domain/Tier.java
```

코드:

```java
package com.arcane.worker.ranker.domain;

public enum Tier {
    CHALLENGER("challenger"),
    GRANDMASTER("grandmaster"),
    MASTER("master");

    private final String key;

    Tier(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
```

설명:

- `Tier`: worker가 갱신할 최상위 랭크 구간을 표현한다.
- `CHALLENGER`, `GRANDMASTER`, `MASTER`: Riot 랭커 API에서 각각 따로 조회해야 하는 티어다.
- `key`: Redis key suffix로 쓸 문자열이다.
- `getKey()`: `ranking:challenger`, `ranking:grandmaster`, `ranking:master` 같은 Redis key를 만들 때 사용한다.

## 4. Riot 랭커 DTO

파일:

```text
src/main/java/com/arcane/worker/ranker/dto/RiotRankerDto.java
```

코드:

```java
package com.arcane.worker.ranker.dto;

import lombok.Data;

@Data
public class RiotRankerDto {
    private String puuid;
    private Integer leaguePoints;
    private Integer wins;
    private Integer losses;
    private String rank;
    private Boolean veteran;
    private Boolean inactive;
    private Boolean freshBlood;
    private Boolean hotStreak;
}
```

설명:

- `puuid`: Riot 계정의 고유 ID다. DB 조회와 Redis value key의 기준이 된다.
- `leaguePoints`: LP 값이다. Redis ZSet score로 사용한다.
- `wins`, `losses`: 랭킹 화면에 보여줄 승/패 데이터다.
- `rank`: 마스터 이상에서는 보통 세부 division 의미가 크지 않지만 Riot 응답에 포함될 수 있다.
- `veteran`, `inactive`, `freshBlood`, `hotStreak`: Riot 응답 부가 상태다. 현재 Redis 저장에는 쓰지 않아도 된다.

파일:

```text
src/main/java/com/arcane/worker/ranker/dto/FromRiotRankerResDto.java
```

코드:

```java
package com.arcane.worker.ranker.dto;

import lombok.Data;

import java.util.List;

@Data
public class FromRiotRankerResDto {
    private String tier;
    private String leagueId;
    private String queue;
    private String name;
    private List<RiotRankerDto> entries;
}
```

설명:

- `entries`: 실제 랭커 목록이다.
- `tier`, `leagueId`, `queue`, `name`: Riot league 응답 메타데이터다.
- worker의 랭킹 갱신 핵심은 `entries`를 순회하는 것이다.

## 5. SummonerEntity

파일:

```text
src/main/java/com/arcane/worker/summoner/domain/SummonerEntity.java
```

코드:

```java
package com.arcane.worker.summoner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "summoner",
        indexes = {
                @Index(name = "summoner_puuid", columnList = "puuid"),
                @Index(name = "summoner_name", columnList = "trimmed_game_name, tag_line")
        }
)
@Getter
@NoArgsConstructor
public class SummonerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trimmed_game_name", length = 100, nullable = false)
    private String trimmedGameName;

    @Column(name = "game_name", length = 100, nullable = false)
    private String gameName;

    @Column(name = "tag_line", length = 20, nullable = false)
    private String tagLine;

    @Column(name = "puuid", length = 100, nullable = false, unique = true)
    private String puuid;

    @Column(name = "icon")
    private Integer iconId;

    @Column(name = "level")
    private Integer level;

    @Column(name = "solo_rank_tier", length = 20)
    private String soloRankTier;

    @Column(name = "solo_rank_lp")
    private Integer soloRankLP;

    @Column(name = "solo_rank_win")
    private Integer soloRankWin;

    @Column(name = "solo_rank_defeat")
    private Integer soloRankDefeat;

    public SummonerEntity(String gameName, String tagLine, String puuid) {
        this.gameName = gameName;
        this.trimmedGameName = gameName.replace(" ", "");
        this.tagLine = tagLine;
        this.puuid = puuid;
    }

    public void updateIdentity(String gameName, String tagLine) {
        this.gameName = gameName;
        this.trimmedGameName = gameName.replace(" ", "");
        this.tagLine = tagLine;
    }

    public void updateSoloRank(String tier, Integer lp, Integer wins, Integer losses) {
        this.soloRankTier = tier;
        this.soloRankLP = lp;
        this.soloRankWin = wins;
        this.soloRankDefeat = losses;
    }
}
```

메소드 설명:

- `SummonerEntity(String gameName, String tagLine, String puuid)`
  - DB에 없는 소환사를 새로 저장할 때 사용한다.
  - `trimmedGameName`은 기존 백엔드 테이블과 같은 규칙으로 공백 제거 값을 저장한다.

- `updateIdentity(String gameName, String tagLine)`
  - Riot ID가 바뀐 경우 DB의 이름/태그를 최신화한다.
  - `puuid`는 같은 계정의 고정 식별자이므로 변경하지 않는다.

- `updateSoloRank(String tier, Integer lp, Integer wins, Integer losses)`
  - Riot 랭커 API 결과로 솔랭 티어/LP/승/패를 갱신한다.
  - 트랜잭션 안에서 호출되면 JPA dirty checking으로 DB에 반영된다.

라인별 핵심:

- `@Entity`: JPA가 이 클래스를 DB 테이블과 연결한다.
- `@Table(name = "summoner")`: 기존 백엔드가 쓰는 `summoner` 테이블에 연결한다.
- `@Column(name = "solo_rank_lp")`: 실제 DB 컬럼명과 Java 필드를 매핑한다.
- `@NoArgsConstructor`: JPA가 entity를 생성할 때 기본 생성자가 필요하다.

## 6. SummonerRepository

파일:

```text
src/main/java/com/arcane/worker/summoner/repository/SummonerRepository.java
```

코드:

```java
package com.arcane.worker.summoner.repository;

import com.arcane.worker.summoner.domain.SummonerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummonerRepository extends JpaRepository<SummonerEntity, Long> {

    Optional<SummonerEntity> findByPuuid(String puuid);
}
```

설명:

- `JpaRepository<SummonerEntity, Long>`: `summoner` 테이블에 대한 기본 CRUD를 제공한다.
- `findByPuuid(String puuid)`: Riot 랭커 API가 주는 PUUID로 기존 DB row를 찾는다.
- `Optional<SummonerEntity>`: DB에 없을 수도 있으므로 Optional로 받는다.

## 7. SummonerService

파일:

```text
src/main/java/com/arcane/worker/summoner/service/SummonerService.java
```

코드:

```java
package com.arcane.worker.summoner.service;

import com.arcane.worker.ranker.domain.Tier;
import com.arcane.worker.ranker.dto.RiotRankerDto;
import com.arcane.worker.riot.dto.RiotAccountDto;
import com.arcane.worker.summoner.domain.SummonerEntity;
import com.arcane.worker.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SummonerService {

    private final SummonerRepository summonerRepository;

    public Optional<SummonerEntity> findByPuuid(String puuid) {
        return summonerRepository.findByPuuid(puuid);
    }

    public SummonerEntity saveOrUpdateRanker(
            RiotRankerDto ranker,
            RiotAccountDto account,
            Tier tier
    ) {
        SummonerEntity summoner = summonerRepository.findByPuuid(ranker.getPuuid())
                .orElseGet(() -> summonerRepository.save(
                        new SummonerEntity(
                                account.gameName(),
                                account.tagLine(),
                                ranker.getPuuid()
                        )
                ));

        summoner.updateIdentity(account.gameName(), account.tagLine());
        summoner.updateSoloRank(
                tier.getKey(),
                ranker.getLeaguePoints(),
                ranker.getWins(),
                ranker.getLosses()
        );

        return summoner;
    }
}
```

메소드 설명:

- `findByPuuid(String puuid)`
  - DB에 이미 저장된 소환사가 있는지 확인한다.
  - 현재는 직접 쓸 일이 적어도, 디버깅이나 별도 로직에서 필요할 수 있다.

- `saveOrUpdateRanker(RiotRankerDto ranker, RiotAccountDto account, Tier tier)`
  - worker 랭킹 갱신의 DB 반영 핵심 메소드다.
  - DB에 있으면 기존 row를 업데이트한다.
  - DB에 없으면 `SummonerEntity`를 새로 저장한다.
  - 최종적으로 Redis 저장에 쓸 수 있도록 `SummonerEntity`를 반환한다.

라인별 핵심:

- `summonerRepository.findByPuuid(ranker.getPuuid())`
  - 같은 계정인지 PUUID 기준으로 확인한다.
- `.orElseGet(...)`
  - 없는 경우에만 새 entity를 저장한다.
- `summoner.updateIdentity(...)`
  - 게임 이름과 태그라인을 최신화한다.
- `summoner.updateSoloRank(...)`
  - 티어, LP, 승/패를 최신화한다.
- `return summoner`
  - 이후 `RedisRankerDto.of(summoner)`로 변환하기 위해 반환한다.

## 8. RedisRankerDto 수정

파일:

```text
src/main/java/com/arcane/worker/redis/dto/RedisRankerDto.java
```

코드:

```java
package com.arcane.worker.redis.dto;

import com.arcane.worker.summoner.domain.SummonerEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisRankerDto {
    private String puuid;
    private String gameName;
    private String tagLine;
    private String tier;
    private Integer lp;
    private Integer wins;
    private Integer losses;
    private Integer level;
    private Integer icon;

    public static RedisRankerDto of(SummonerEntity summoner) {
        return RedisRankerDto.builder()
                .puuid(summoner.getPuuid())
                .gameName(summoner.getGameName())
                .tagLine(summoner.getTagLine())
                .tier(summoner.getSoloRankTier())
                .lp(summoner.getSoloRankLP())
                .wins(summoner.getSoloRankWin())
                .losses(summoner.getSoloRankDefeat())
                .icon(summoner.getIconId())
                .level(summoner.getLevel())
                .build();
    }
}
```

메소드 설명:

- `of(SummonerEntity summoner)`
  - DB에 저장/갱신된 소환사 entity를 Redis 저장용 DTO로 변환한다.
  - Redis에는 entity 자체를 넣지 않고 JSON 직렬화 가능한 DTO만 저장한다.

라인별 핵심:

- `.puuid(summoner.getPuuid())`: Redis value key와 ZSet member의 기준값이다.
- `.tier(summoner.getSoloRankTier())`: challenger/grandmaster/master 구분이다.
- `.lp(summoner.getSoloRankLP())`: Redis ZSet score 정렬 기준이다.
- `.icon(...)`, `.level(...)`: 현재 값이 없으면 null이어도 된다.

## 9. RedisService 수정 포인트

현재 `RedisService`는 `RedisRankerDto`, `Tier` import가 필요하다.

파일:

```text
src/main/java/com/arcane/worker/redis/RedisService.java
```

필요 import:

```java
import com.arcane.worker.ranker.domain.Tier;
import com.arcane.worker.redis.dto.RedisRankerDto;
```

`updateGlobalRanking()`도 worker RedisService로 옮기는 것이 좋다.

```java
public void updateGlobalRanking() {
    String globalKey = "ranking:all";
    String challengerKey = "ranking:challenger";
    List<String> otherKeys = List.of("ranking:grandmaster", "ranking:master");
    redisTemplate.opsForZSet().unionAndStore(challengerKey, otherKeys, globalKey);
}
```

필요 import:

```java
import java.util.List;
```

메소드 설명:

- `updateRedisRanking(Queue<RedisRankerDto> rankersInfoDtos, Tier tier)`
  - 특정 티어의 Redis 랭킹을 통째로 새로 쓴다.
  - `temp:ranking:{tier}`에 먼저 쓰고 마지막에 `ranking:{tier}`로 rename한다.
  - 중간에 읽는 사용자가 깨진 데이터를 보지 않게 하기 위한 atomic swap 방식이다.

- `updateGlobalRanking()`
  - `ranking:challenger`, `ranking:grandmaster`, `ranking:master`를 합쳐 `ranking:all`을 만든다.
  - 프론트의 전체 랭킹 탭이 이 key를 읽는 구조라면 필요하다.

## 10. RiotApiService 추가 메소드

파일:

```text
src/main/java/com/arcane/worker/riot/service/RiotApiService.java
```

추가할 메소드:

```java
import com.arcane.worker.ranker.domain.Tier;
import com.arcane.worker.ranker.dto.FromRiotRankerResDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
```

```java
public FromRiotRankerResDto getLeagueByTier(Tier tier) {
    String url = baseUrlKR + "/lol/league/v4/" + tier.getKey() + "leagues/by-queue/RANKED_SOLO_5x5";

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Riot-Token", apiKey);

    ResponseEntity<FromRiotRankerResDto> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            FromRiotRankerResDto.class
    );

    return response.getBody();
}
```

```java
public RiotAccountDto getSummonerByPuuid(String puuid) {
    String url = UriComponentsBuilder
            .fromHttpUrl(baseUrlAsia + "/riot/account/v1/accounts/by-puuid/{puuid}")
            .buildAndExpand(puuid)
            .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Riot-Token", apiKey);

    ResponseEntity<RiotAccountDto> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            RiotAccountDto.class
    );

    return response.getBody();
}
```

메소드 설명:

- `getLeagueByTier(Tier tier)`
  - challenger/grandmaster/master 중 하나의 Riot 랭커 목록을 가져온다.
  - 응답의 `entries`를 순회해서 DB와 Redis를 갱신한다.

- `getSummonerByPuuid(String puuid)`
  - 랭커 API는 PUUID 중심으로 온다.
  - Redis와 DB에는 `gameName`, `tagLine`도 필요하므로 PUUID로 Riot account API를 한 번 더 호출한다.

라인별 핵심:

- `headers.set("X-Riot-Token", apiKey)`: Riot API 인증 헤더다.
- `restTemplate.exchange(...)`: HTTP GET 요청을 보내고 DTO로 응답을 받는다.
- `return response.getBody()`: service 계층에서 사용할 Riot 응답 객체를 반환한다.

## 11. RankingService 구현

파일:

```text
src/main/java/com/arcane/worker/ranker/service/RankingService.java
```

코드:

```java
package com.arcane.worker.ranker.service;

import com.arcane.worker.kafka.event.dto.RankingUpdateRequestedEvent;
import com.arcane.worker.ranker.domain.Tier;
import com.arcane.worker.ranker.dto.FromRiotRankerResDto;
import com.arcane.worker.ranker.dto.RiotRankerDto;
import com.arcane.worker.redis.RedisService;
import com.arcane.worker.redis.dto.RedisRankerDto;
import com.arcane.worker.riot.dto.RiotAccountDto;
import com.arcane.worker.riot.service.RiotApiService;
import com.arcane.worker.summoner.domain.SummonerEntity;
import com.arcane.worker.summoner.service.SummonerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RankingService {

    private final SummonerService summonerService;
    private final RiotApiService riotApiService;
    private final RedisService redisService;

    public void updateRanking(RankingUpdateRequestedEvent event) {
        log.info("[worker][RANKING_UPDATE][START] jobId={} traceId={}", event.jobId(), event.traceId());

        updateTier(Tier.CHALLENGER);
        updateTier(Tier.GRANDMASTER);
        updateTier(Tier.MASTER);
        redisService.updateGlobalRanking();

        log.info("[worker][RANKING_UPDATE][SUCCESS] jobId={} traceId={}", event.jobId(), event.traceId());
    }

    private void updateTier(Tier tier) {
        FromRiotRankerResDto league = riotApiService.getLeagueByTier(tier);
        Queue<RedisRankerDto> redisRankers = buildRedisRankers(league, tier);

        redisService.updateRedisRanking(redisRankers, tier);

        log.info("[worker][RANKING_UPDATE][TIER_SUCCESS] tier={} count={}", tier, redisRankers.size());
    }

    private Queue<RedisRankerDto> buildRedisRankers(FromRiotRankerResDto league, Tier tier) {
        Queue<RedisRankerDto> redisRankers = new LinkedList<>();
        List<RiotRankerDto> entries = league == null ? List.of() : league.getEntries();

        if (entries == null) {
            return redisRankers;
        }

        entries.sort((left, right) -> Integer.compare(
                right.getLeaguePoints(),
                left.getLeaguePoints()
        ));

        for (RiotRankerDto ranker : entries) {
            RiotAccountDto account = riotApiService.getSummonerByPuuid(ranker.getPuuid());
            if (account == null) {
                continue;
            }

            SummonerEntity summoner = summonerService.saveOrUpdateRanker(ranker, account, tier);
            redisRankers.add(RedisRankerDto.of(summoner));
        }

        return redisRankers;
    }
}
```

메소드 설명:

- `updateRanking(RankingUpdateRequestedEvent event)`
  - Kafka consumer가 호출하는 worker 랭킹 업데이트 진입점이다.
  - challenger, grandmaster, master를 순서대로 갱신한다.
  - 마지막에 전체 랭킹 `ranking:all`을 만든다.

- `updateTier(Tier tier)`
  - 특정 티어 하나를 Riot API에서 가져온다.
  - DB 저장/갱신과 Redis DTO 생성을 수행한다.
  - Redis에 해당 티어 랭킹을 반영한다.

- `buildRedisRankers(FromRiotRankerResDto league, Tier tier)`
  - Riot 응답의 `entries`를 Redis에 저장할 DTO queue로 변환한다.
  - 각 ranker의 PUUID로 Riot account 정보를 조회한다.
  - DB의 `summoner` row를 저장/갱신한다.
  - 갱신된 entity를 `RedisRankerDto`로 변환한다.

라인별 핵심:

- `updateTier(Tier.CHALLENGER)`: 챌린저 랭킹 갱신.
- `redisService.updateGlobalRanking()`: 티어별 갱신 후 전체 랭킹 생성.
- `entries.sort(...)`: LP 기준 내림차순 정렬.
- `riotApiService.getSummonerByPuuid(...)`: Riot ID 이름/태그 조회.
- `summonerService.saveOrUpdateRanker(...)`: DB 저장/갱신.
- `redisRankers.add(...)`: Redis에 넣을 DTO queue 구성.

## 12. Kafka Consumer 연결

파일:

```text
src/main/java/com/arcane/worker/kafka/consumer/RankingUpdateConsumer.java
```

추가 import:

```java
import com.arcane.worker.ranker.service.RankingService;
```

필드 추가:

```java
private final RankingService rankingService;
```

try 블록 안의 TODO를 교체:

```java
rankingService.updateRanking(requestedEvent);
```

전체 흐름:

```java
try {
    rankingService.updateRanking(requestedEvent);

    kafkaTemplate.send(
            completedTopic,
            requestedEvent.jobId(),
            new RankingUpdateCompletedEvent(
                    UUID.randomUUID().toString().substring(0, 10),
                    requestedEvent.jobId(),
                    LocalDateTime.now(),
                    requestedEvent.requestedAt(),
                    "랭킹 업데이트 완료"
            )
    );

    ack.acknowledge();
}
```

설명:

- consumer는 Kafka 메시지를 받는 역할만 담당한다.
- 실제 랭킹 업데이트는 `RankingService`에 위임한다.
- 성공하면 completed topic으로 이벤트를 보낸다.
- 성공 후 `ack.acknowledge()`로 Kafka에 처리 완료를 알린다.
- 실패하면 failed topic으로 이벤트를 보내고, 현재 정책상 ack 처리한다.

## 13. JPA ddl-auto 주의

현재 worker 설정:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

개발 중에는 `update`로 둘 수 있지만, worker entity가 기존 DB 테이블을 건드릴 수 있다.

더 안전한 설정:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

차이:

- `update`: entity 기준으로 DB 스키마 변경을 시도할 수 있다.
- `validate`: entity와 DB 스키마가 맞는지만 확인하고 변경하지 않는다.

worker가 기존 DB를 안전하게 사용하려면 최종적으로는 `validate`가 더 낫다.

## 14. 최종 동작 원리

1. API 서버가 관리자 요청을 받는다.
2. API 서버가 `RankingUpdateRequestedEvent`를 Kafka requested topic으로 발행한다.
3. worker의 `RankingUpdateConsumer`가 메시지를 받는다.
4. consumer가 `RankingService.updateRanking(event)`를 호출한다.
5. `RankingService`가 challenger, grandmaster, master를 순서대로 처리한다.
6. 각 티어마다 `RiotApiService.getLeagueByTier(tier)`로 랭커 목록을 가져온다.
7. 각 랭커마다 `RiotApiService.getSummonerByPuuid(puuid)`로 이름/태그를 조회한다.
8. `SummonerService.saveOrUpdateRanker(...)`가 DB `summoner` row를 저장하거나 갱신한다.
9. 갱신된 `SummonerEntity`를 `RedisRankerDto`로 바꾼다.
10. `RedisService.updateRedisRanking(...)`가 `ranking:{tier}` Redis key를 갱신한다.
11. 모든 티어 완료 후 `RedisService.updateGlobalRanking()`이 `ranking:all`을 만든다.
12. worker가 completed topic으로 성공 이벤트를 발행한다.
13. 실패하면 failed topic으로 실패 이벤트를 발행한다.

## 15. 1차 구현 체크리스트

- [ ] `Tier` enum 추가
- [ ] `RiotRankerDto` 추가
- [ ] `FromRiotRankerResDto` 추가
- [ ] `SummonerEntity` 추가
- [ ] `SummonerRepository` 추가
- [ ] `SummonerService` 구현
- [ ] `RedisRankerDto.of(SummonerEntity)`로 수정
- [ ] `RedisService` import 정리
- [ ] `RedisService.updateGlobalRanking()` 추가
- [ ] `RiotApiService.getLeagueByTier(Tier)` 추가
- [ ] `RiotApiService.getSummonerByPuuid(String)` 추가
- [ ] `RankingService.updateRanking(...)` 구현
- [ ] `RankingUpdateConsumer`에서 `RankingService` 호출
- [ ] worker `ddl-auto`를 최종적으로 `validate`로 전환할지 결정

