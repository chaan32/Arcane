# Arcane Operations History

이 문서는 Arcane 프로젝트에서 실제로 진행한 주요 구현, 장애 대응, 배포 정리 작업을 기록한다.

## 1. 저장소 정리

### AS-IS

프론트엔드, 백엔드, AI 서버가 각각 별도 Git 저장소 흔적을 가지고 있었고, 루트 저장소 기준으로 GitHub에 올릴 때 하위 프로젝트가 제대로 포함되지 않는 문제가 있었다.

### TO-BE

루트 `/Arcane` 폴더를 단일 Git 저장소 기준으로 정리했다.

- 하위 프로젝트의 nested `.git` 제거
- 루트 `.gitignore` 작성
- `docs` 폴더로 문서 정리
- Docker, EC2, Vercel, CI/CD 파일을 루트 기준으로 관리

## 2. Docker 기반 실행 구조 정리

### AS-IS

로컬 실행과 배포 실행 방식이 섞여 있었고, 프론트까지 EC2 Docker Compose에 포함할지 Vercel로 분리할지 기준이 불명확했다.

### TO-BE

로컬용과 EC2용 Compose를 분리했다.

- `docker-compose.yml`: 로컬 개발/통합 실행용
- `docker-compose.ec2.yml`: EC2 운영 배포용
- `docker-compose.ec2.build.yml`: EC2에서 직접 빌드할 때만 쓰는 override

운영 구조는 다음과 같이 정리했다.

- Frontend: Vercel
- API / Worker / AI / Infra: EC2 Docker Compose
- Images: GHCR
- Deploy: GitHub Actions

## 3. Vercel + EC2 배포 구조 구축

### AS-IS

프론트와 백엔드를 모두 로컬에서 실행했고, 실제 외부 접근 가능한 배포 환경이 없었다.

### TO-BE

프론트는 Vercel, 백엔드/인프라는 EC2로 분리 배포했다.

- Frontend: `https://www.ar-cane.site`
- API: `https://api.ar-cane.site`
- Vercel 환경변수: `NEXT_PUBLIC_API_URL=https://api.ar-cane.site`
- EC2 `.env.ec2`: CORS, OAuth redirect, Riot API Key, JWT, AWS S3, DB 계정 관리

## 4. 도메인 / HTTPS / OAuth 연결

### AS-IS

OAuth redirect, Vercel callback, API callback의 역할이 섞여 있었다.

### TO-BE

역할을 아래처럼 분리했다.

- 프론트 OAuth callback: `https://www.ar-cane.site/oauth/callback`
- Google backend callback: `https://api.ar-cane.site/login/oauth2/code/google`
- Naver backend callback: `https://api.ar-cane.site/login/oauth2/code/naver`

Nginx와 Certbot을 통해 API 도메인에 HTTPS를 적용했다.

## 5. GitHub Actions CI/CD 구성

### AS-IS

EC2에 직접 접속해 git pull, docker build, docker compose 실행을 수동으로 처리해야 했다.

### TO-BE

GitHub Actions 기반 배포 흐름을 구성했다.

1. `main` push
2. API / Worker / AI Docker image build
3. GHCR push
4. EC2 SSH 접속
5. `git pull`
6. GHCR image pull
7. `docker compose --env-file .env.ec2 -f docker-compose.ec2.yml up -d --no-build`

필요 Secrets:

- `EC2_HOST`
- `EC2_USER`
- `EC2_PROJECT_DIR`
- `EC2_SSH_KEY`
- `GHCR_USERNAME`
- `GHCR_TOKEN`

## 6. GHCR 권한 문제 해결

### 증상

EC2 배포 단계에서 아래 오류가 발생했다.

```text
failed to resolve reference ... 403 Forbidden
```

### 원인

GHCR package가 private이거나, EC2에서 pull할 때 사용하는 token에 `read:packages` 권한이 없었다.

### 해결

- GHCR token 권한 확인
- package visibility / repository 연결 확인
- GitHub Actions secrets에 `GHCR_TOKEN` 등록
- EC2 deploy job에서 `docker login ghcr.io` 수행

## 7. Nginx WebSocket 프록시 문제 해결

### 증상

API 로그에 아래 메시지가 반복됐다.

```text
Handshake failed due to invalid Upgrade header: null
```

### 원인

Nginx가 WebSocket Upgrade 헤더를 Spring Boot로 전달하지 않거나, Spring Security가 `/ws/chat` handshake 요청을 OAuth login으로 리다이렉트했다.

### 해결

Nginx `api.ar-cane.site` 443 server block에 아래 설정을 추가했다.

```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
proxy_read_timeout 3600;
proxy_send_timeout 3600;
```

Spring Security에서는 `/ws/**`를 명확하게 permit 처리했다.

검증:

```bash
curl -i --http1.1 -N \
  -H "Origin: https://www.ar-cane.site" \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  -H "Sec-WebSocket-Version: 13" \
  https://api.ar-cane.site/ws/chat
```

정상 결과:

```text
HTTP/1.1 101
Upgrade: websocket
```

## 8. Riot Data Dragon 버전 하드코딩 제거

### AS-IS

챔피언, 아이템, 룬 이미지 URL이 특정 패치 버전에 하드코딩되어 있었다.
새 챔피언이나 아이템 변경이 있을 때 이미지가 깨질 수 있었다.

### TO-BE

Data Dragon 메타데이터를 동기화하고, 프론트 이미지 URL에서 현재 game version을 우선 사용하도록 개선했다.

- game version 저장
- Data Dragon metadata에 `version`, `isActive` 추가
- Worker game-data-sync Kafka 작업 설계
- 분석 snapshot에 `patchVersion` 저장
- 프론트 이미지 URL에서 version 우선 사용

## 9. Riot API 호출 최적화

### AS-IS

전적 검색 요청 안에서 Riot API 호출, 데이터 저장, AI 점수 계산이 함께 수행될 수 있었다.
외부 API 지연이나 실패가 사용자 응답 시간에 직접 영향을 줬다.

### TO-BE

- DB 우선 조회
- Redis cache
- cache miss 중복 요청 방지를 위한 lock
- MatchId deduplication
- MongoDB match raw data reuse
- Worker 기반 장시간 수집 분리

실제 수집에서 300명의 최근 15게임 기준 4,500개 후보 MatchId 중 2,586개 Unique MatchId를 추출했고, 이후 대규모 수집에서는 약 7만 건의 participant raw data 저장 흐름을 확인했다.

## 10. Kafka 기반 Worker 분리

### AS-IS

랭킹 업데이트, 데이터 수집, 챔피언 분석 같은 장시간 작업이 API 서버 흐름과 강하게 연결될 위험이 있었다.

### TO-BE

API 서버는 Kafka에 작업 명령만 발행하고, Worker 서버가 실제 수집/분석 작업을 처리하도록 분리했다.

작업 예시:

- Ranking update
- Dataset collect
- Champion analysis
- Game data sync
- AI score request experiment

작업 상태는 Redis에 저장하고, 완료/실패 이벤트를 Kafka로 다시 발행한다.

## 11. 챔피언 분석 파이프라인

### AS-IS

챔피언 통계 페이지에서 조회 시점마다 무거운 계산이 발생할 수 있었다.

### TO-BE

MongoDB participant raw data를 기반으로 Worker가 사전 계산한 뒤 MySQL 분석 테이블에 저장한다.

저장/계산 대상:

- champion tier
- champion position statistics
- item usage
- rune usage
- summoner spell usage
- matchup / synergy 예정
- snapshot patch version

티어 산정은 단순 승률이 아니라 승률, 픽률, 표본 수, 최근성, 신뢰도를 함께 고려하는 방식으로 설계했다.

## 12. Elasticsearch 적용 지점

### 공략글 검색

공략글 내용 검색에서 MySQL `LIKE %keyword%` 방식은 데이터가 늘어날수록 인덱스 효율이 떨어진다.

개선 방향:

- 공략글 title/content/championName 색인
- Elasticsearch full-text search
- MySQL은 원본/권한/상세 조회 담당

### 소환사 자동완성

`gameName`, `tagLine`, `riotId`를 Elasticsearch에 색인하고, n-gram 기반 자동완성을 적용할 수 있도록 설계했다.

측정 예시:

| 검색어 | DB LIKE 평균 | Elasticsearch 평균 | 개선율 |
| --- | ---: | ---: | ---: |
| `hide on bush#kr` | 27ms | 15ms | 44.44% |
| `hide` | 31ms | 10ms | 67.74% |
| `a` | 48ms | 9ms | 81.25% |

## 13. Observability 구성

### Prometheus / Grafana

서버 상태와 지표 중심.

- uptime
- heap usage
- process CPU
- live threads
- HTTP request count
- response time

### ELK

로그 검색과 장애 원인 추적 중심.

- API logs
- Worker logs
- jobId
- traceId
- 실패 reason
- Kafka consumer 처리 흐름

Elasticsearch는 서비스 검색과 로그 검색 양쪽에서 사용하지만, 인덱스를 분리해 사용한다.

## 14. AWS S3 이미지 업로드

### AS-IS

공략글 이미지가 본문에 직접 포함되거나 로컬 처리에 가까운 구조였다.

### TO-BE

AWS S3에 guide image를 업로드하고, 백엔드 API가 업로드 URL을 반환하도록 구성했다.

관련 환경변수:

- `AWS_REGION`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_S3_BUCKET`
- `AWS_S3_PUBLIC_BASE_URL`
- `AWS_S3_GUIDE_IMAGE_PREFIX`
- `AWS_S3_GUIDE_IMAGE_MAX_BYTES`

파일 크기 제한 문제는 Spring multipart 설정으로 조정했다.

## 15. Riot Production API Key 신청

### 진행 내용

Riot Developer Portal에서 Arcane product를 등록했다.

Product URL:

```text
https://www.ar-cane.site
```

사이트 소유권 검증을 위해 Vercel public asset에 `riot.txt`를 추가했다.

검증 URL:

```text
https://www.ar-cane.site/riot.txt
```

Production API Key 승인 후 더 높은 한도가 필요하면 Expanded Rate Limit을 별도로 요청한다.

## 16. 현재 남은 운영 작업

- Riot Production API Key 심사 대기
- Production key 발급 후 EC2 `.env.ec2`의 `RIOT_API_KEY` 교체
- 대규모 데이터 재수집
- 챔피언 분석 재실행
- 관리자 페이지에서 작업 진행률/로그 확인
- Grafana / Kibana 접근 정책 정리
- EC2 비용과 인스턴스 스펙 조정
- 운영 DB 백업 전략 정리
