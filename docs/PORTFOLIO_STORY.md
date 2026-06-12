# Arcane Portfolio Story

작성일: 2026-06-04

## 프로젝트 소개

Arcane은 League of Legends 데이터를 기반으로 소환사 전적, 챔피언 통계, 랭킹, 공략, 실시간 채팅, AI 점수 실험을 제공하는 분석 플랫폼이다.

단순 전적 검색을 넘어서 다음 문제를 직접 다뤘다.

- 외부 API rate limit
- 중복 데이터 저장
- DB deadlock
- Redis 랭킹 캐시
- OAuth/JWT 인증
- STOMP WebSocket 채팅
- Markdown 기반 공략
- 비정형 웹 데이터 수집과 ML 모델 실험

## 핵심 기술 스택

Frontend:

- Next.js
- React
- TypeScript
- Tailwind CSS
- React Query
- STOMP client

Backend:

- Java 21
- Spring Boot
- JPA
- MySQL
- Redis
- Spring Security
- OAuth2
- JWT
- STOMP WebSocket

AI:

- FastAPI
- pandas
- scikit-learn
- ExtraTreesRegressor
- Playwright
- pymysql

## 스토리 1. Riot API 호출 최적화

문제:

- 전적 검색 시 `/profile`, `/tier`, `/matches`, `/mastery` 요청이 동시에 발생했다.
- DB에 이미 있는 데이터도 Riot API를 다시 호출했다.
- Riot API 429 Too Many Requests가 발생했다.

개선:

- 기본 검색은 `refresh=false`로 고정했다.
- DB에 저장된 소환사/티어/전적이 있으면 DB에서 반환했다.
- 전적 갱신 버튼을 누를 때만 `refresh=true`를 붙였다.
- `gameName#tagLine` 기준 짧은 Riot account cache를 추가했다.

효과:

- DB hit 시 외부 Riot API 호출을 크게 줄일 수 있다.
- 429 발생 가능성을 낮춘다.
- 일반 검색과 강제 갱신의 책임이 명확해졌다.

면접 표현:

> 전적 검색은 사용자가 반복해서 누르기 쉬운 기능이라 외부 API에 바로 의존하면 rate limit과 응답 지연이 같이 발생했습니다. 그래서 일반 검색과 갱신 요청을 `refresh` 파라미터로 분리하고, DB hit 시 Riot API를 호출하지 않는 구조로 바꿨습니다.

## 스토리 2. 중복 insert와 deadlock 완화

문제:

- 동시에 여러 요청이 같은 match를 저장했다.
- `match_info.match_id`, `summoner.puuid` unique key에서 duplicate insert가 발생했다.
- Hibernate SQL Error 1062, deadlock, lock wait timeout 로그가 반복됐다.

개선:

- 중복 가능성이 있는 insert를 MySQL `INSERT IGNORE`로 처리했다.
- insert가 성공했든 무시됐든 DB에서 다시 조회했다.
- service 레벨에서 matchId/puuid 기준 lock을 추가했다.

효과:

- 중복 key 예외가 애플리케이션 에러로 터지는 빈도를 줄였다.
- 이미 저장된 row는 조용히 재사용한다.
- 참가자 저장 시 기존 `Match` 엔티티를 기준으로 연결한다.

면접 표현:

> 단순히 DuplicateKeyException을 catch하는 방식은 DB와 Hibernate 로그가 이미 오염된 뒤라 근본 해결이 아니었습니다. 중복을 정상 흐름으로 보고 DB 레벨에서 `INSERT IGNORE` 처리한 뒤, 항상 재조회해서 영속 엔티티를 기준으로 후속 작업을 진행하게 했습니다.

## 스토리 3. Redis 기반 랭킹 캐시

문제:

- 챌린저/그마/마스터 랭킹 데이터는 양이 많고 주기적으로 갱신된다.
- 갱신 중 사용자가 조회하면 데이터 일관성이 깨질 수 있다.

개선:

- Redis에 랭킹 데이터를 캐싱했다.
- temp key에 새 랭킹을 만든 뒤 rename으로 실제 key를 교체하는 atomic swap 방식을 사용했다.
- Redis pipeline으로 대량 저장 지연을 줄였다.

효과:

- 갱신 중에도 사용자는 기존 정상 데이터를 볼 수 있다.
- 교체 순간만 atomic하게 반영된다.
- DB/Riot API 부하를 줄인다.

면접 표현:

> 랭킹은 read-heavy 데이터라 DB보다 Redis 캐시가 적합했습니다. 갱신 중 half-written 상태를 노출하지 않기 위해 temp key에 먼저 완성하고 rename으로 교체하는 방식을 사용했습니다.

## 스토리 4. OAuth/JWT 인증과 온보딩

문제:

- Google/Naver OAuth 사용자는 비밀번호가 없다.
- 기존 일반 로그인 테이블 구조와 충돌했다.
- 첫 로그인 후 서비스 내 닉네임을 따로 받아야 했다.
- 나중에 다른 소셜 계정을 같은 Arcane 계정에 연결하고 싶었다.

개선:

- OAuth 계정 식별 정보는 `oauth_accounts` 테이블로 분리했다.
- `users.login_pw`는 OAuth placeholder 또는 nullable 구조로 대응했다.
- OAuth 성공 시 JWT를 발급하고 프론트 콜백으로 전달했다.
- `needsOnboarding` 값으로 최초 닉네임 설정 화면을 분기했다.
- `/me`에서 소셜 계정 연동을 시작할 수 있게 했다.

효과:

- OAuth와 일반 로그인 구조를 한 User 모델 안에서 관리할 수 있다.
- 서비스 닉네임과 소셜 프로필을 분리했다.
- 계정 연동 흐름을 명시적으로 만들었다.

면접 표현:

> OAuth provider의 subject를 바로 users에 넣기보다, User와 OAuthAccount를 분리했습니다. 덕분에 한 사용자가 Google과 Naver를 모두 연결하는 구조를 만들 수 있었습니다.

## 스토리 5. STOMP WebSocket 채팅

문제:

- 공략 작성자와 독자가 실시간으로 대화할 수 있어야 했다.
- REST polling은 비효율적이고 UX가 좋지 않다.
- 같은 방이 중복 표시되거나 unread count가 방 수로 보이는 문제가 있었다.

개선:

- Spring STOMP WebSocket을 사용했다.
- `/pub/chat/rooms/{roomId}/messages`로 전송한다.
- `/sub/chat/rooms/{roomId}`를 구독한다.
- Principal에서 현재 사용자를 가져와 서버에서 권한을 확인한다.
- 메시지 읽음 처리를 STOMP read event로 구현했다.
- 프론트에서 room id 기준 dedupe와 unread message count를 계산한다.

효과:

- 실시간 메시지 전송이 가능하다.
- 방 중복 표시를 줄였다.
- unread badge가 실제 읽지 않은 상대 메시지 기준으로 동작한다.

면접 표현:

> 채팅은 단순 REST 요청으로도 가능하지만, 실시간성과 읽음 상태 갱신 때문에 STOMP WebSocket으로 전환했습니다. room 단위 구독을 사용해서 필요한 대화방 상태만 갱신하도록 만들었습니다.

## 스토리 6. AI 점수 모델 실험

문제:

- KDA만으로는 게임 기여도를 설명하기 어렵다.
- 딜량, 시야, CS, 승패, 연속킬, 포지션 등 여러 지표를 종합한 점수가 필요했다.

실험:

- Arcane DB의 `match_participant` 지표를 feature로 사용했다.
- 외부 점수 label을 붙여 회귀 모델을 학습하는 실험을 했다.
- ExtraTreesRegressor로 약 15,120 clean rows를 학습했다.

현재 결과:

- MAE 약 3.82
- RMSE 약 5.24
- R2 약 0.883
- 주요 feature: win, kda, kills, deaths, damage

주의:

- 외부 서비스 label 대량 수집은 약관/윤리 리스크가 있다.
- 포트폴리오에서는 "실험"으로 제한해서 표현하는 것이 안전하다.
- 서비스화 방향은 Riot 공식 API 기반 자체 점수 공식으로 전환하는 것이 좋다.

면접 표현:

> 현재 AI 점수는 제품 완성 기능이라기보다 모델링 가능성을 검증한 실험입니다. 실제 서비스화한다면 외부 서비스 점수를 무단 label로 쓰기보다, Riot 공식 API 지표 기반 자체 점수 공식을 설계하고 그 점수를 기준으로 모델을 학습시키는 방향이 맞다고 판단했습니다.

## 포트폴리오에서 강조할 것

- 외부 API 의존 서비스를 안정화한 경험
- DB unique 충돌과 동시성 문제를 직접 해결한 경험
- Redis 캐시와 atomic swap 구조
- OAuth/JWT 인증 흐름 구현
- STOMP WebSocket 채팅 구현
- 프론트 UX와 백엔드 API 계약을 함께 조정한 경험
- ML 실험의 한계와 리스크를 인지하고 안전한 대안을 설계한 점

## 포트폴리오에서 조심할 것

- "DeepLOL 점수를 무단 수집해 학습했다"처럼 쓰지 않는다.
- 실제 측정하지 않은 성능 수치를 확정적으로 쓰지 않는다.
- Redis Pub/Sub/Kafka를 구현했다고 쓰지 않는다. 현재는 Spring simple broker 기반 STOMP다.
- AI 점수를 완성된 서비스 품질처럼 과장하지 않는다.
- API key, OAuth secret, JWT secret이 포함된 화면을 캡처하지 않는다.

## 추천 프로젝트 설명 문장

> Arcane은 Riot API 기반 League of Legends 분석 플랫폼입니다. 단순 전적 조회를 넘어 DB 우선 조회와 refresh 전략으로 외부 API 호출을 줄이고, unique key 중복 저장 문제를 `INSERT IGNORE`와 재조회 흐름으로 완화했습니다. 또한 OAuth/JWT 인증, STOMP WebSocket 채팅, Redis 랭킹 캐시, Markdown 공략 게시판, AI 점수 모델 실험까지 풀스택으로 구현했습니다.
