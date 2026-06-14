# Riot Production API Key Guide

Arcane에서 Riot API를 공개 서비스 수준으로 사용하기 위한 Production API Key 신청 절차를 정리한다.

## API Key 단계

| 단계 | 용도 | 특징 |
| --- | --- | --- |
| Development API Key | 개발/테스트 | 24시간마다 만료 |
| Personal API Key | 개인/소규모 비공개 프로젝트 | 공개 서비스용으로 부적합 |
| Production API Key | 공개 서비스/상용 서비스 | Riot 심사 필요 |
| Expanded Rate Limit | Production 승인 후 추가 한도 | 별도 상위 key가 아니라 한도 증설 요청 |

가장 높은 실서비스 조합은 다음과 같다.

```text
Production API Key + Expanded Rate Limit
```

## Arcane Product 정보

| 항목 | 값 |
| --- | --- |
| Product Name | `Arcane` |
| Product URL | `https://www.ar-cane.site` |
| API Server URL | `https://api.ar-cane.site` |
| Game Focus | `League of Legends` |
| Tournament | `No` |

## Product Description

Riot Developer Portal의 Product Description에는 아래 내용을 사용할 수 있다.

```text
Arcane is a League of Legends web application that helps players review match history, understand champion trends, and make data-informed gameplay decisions.

The product provides summoner search, recent match history lookup, champion detail pages, champion tier lists, and champion statistics by role. Players can search for a Riot ID, view recent matches, review participant-level performance, and compare champion performance using win rate, pick rate, item usage, rune usage, summoner spell usage, and role-specific statistics.

We use Riot APIs to retrieve account, summoner, league, and match data. The main APIs used are ACCOUNT-V1, SUMMONER-V4, LEAGUE-V4, MATCH-V5, and CHAMPION-MASTERY-V4. Riot API data is processed on our backend server only and is never exposed directly to the client.

To reduce unnecessary Riot API traffic and respect rate limits, Arcane stores reusable match raw data in MongoDB, stores relational service data in MySQL, and uses Redis caching for frequently accessed data. Match IDs are deduplicated before collection, and long-running data collection or champion analysis tasks are processed asynchronously through Kafka and Worker servers instead of being executed during user-facing API requests.

Champion analysis results are precomputed from collected match data and stored in analysis tables so that users can view statistics without triggering repeated Riot API calls. We also track job status, retry failed operations when appropriate, and store failure states to monitor data collection reliability.

Arcane does not provide gambling, betting, wagering, tournament operation, or any service that determines match outcomes. The purpose of the product is to provide educational and analytical insights for League of Legends players. The Riot API key is stored only as a server-side environment variable and is not included in frontend code, public repositories, or client-side requests.

Product URL: https://www.ar-cane.site
API Server URL: https://api.ar-cane.site
```

## 사용 API

Arcane에서 사용하는 Riot API 범위는 아래와 같다.

- `ACCOUNT-V1`
- `SUMMONER-V4`
- `LEAGUE-V4`
- `MATCH-V5`
- `CHAMPION-MASTERY-V4`
- `LOL-STATUS-V4`

Data Dragon은 정적 게임 데이터 동기화에 사용한다.

## 사이트 소유권 검증

Riot Developer Portal은 Production Key 심사 전에 Product URL 소유권 검증을 요구한다.

요구사항:

1. 안내 화면에 표시된 UUID를 복사한다.
2. 순수 텍스트 파일을 만든다.
3. 파일명은 반드시 소문자 `riot.txt`로 저장한다.
4. Product URL 루트에서 접근 가능해야 한다.

Arcane에서는 아래 경로에 파일을 둔다.

```text
frontend/Arcane_Frontend/public/riot.txt
```

Vercel 배포 후 접근 URL:

```text
https://www.ar-cane.site/riot.txt
```

현재 검증값:

```text
fdfe6907-4c45-4b42-9037-4757e7d6a4e7
```

배포 후 Riot Developer Portal에서 `VERIFY URL`을 누른다.

## Production Key 적용

승인 후 발급된 key는 EC2 `.env.ec2`에만 저장한다.

```env
RIOT_API_KEY=RGAPI-...
```

적용:

```bash
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml up -d api-server worker-server
```

GitHub, Vercel, frontend 환경변수에는 Riot API Key를 넣지 않는다.

## Expanded Rate Limit 요청 기준

Production 기본 한도로 부족하면 추가 한도 증설을 요청한다.

요청 근거로 준비할 내용:

- 실제 서비스 URL
- 사용자 수 또는 예상 트래픽
- 호출하는 endpoint 목록
- 현재 rate limit에 걸린 로그
- 캐싱/중복 제거/비동기 처리 구조
- 왜 더 높은 한도가 필요한지

Arcane에서 강조할 수 있는 최적화:

- Redis cache
- MatchId deduplication
- MongoDB raw data reuse
- Worker async collection
- precomputed champion analysis
- failed job state tracking

## 주의사항

- API Key는 클라이언트에 노출하지 않는다.
- 공개 GitHub 저장소에 key를 commit하지 않는다.
- Riot 공식 서비스처럼 보이게 디자인하거나 문구를 작성하지 않는다.
- 도박, 베팅, wagering 기능을 제공하지 않는다.
- Riot 고지 문구를 사이트에 표시한다.
