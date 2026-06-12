# ARCANE

League of Legends 전적, 챔피언 분석, 공략, 채팅, AI 점수 실험을 포함한 풀스택 분석 플랫폼입니다.

## Overview

Arcane은 Riot API 기반 데이터를 활용해 소환사 전적 조회, 챔피언 통계, 상위 랭커 조회, 패치노트 확인, 공략 작성, 실시간 채팅을 제공합니다. 외부 API rate limit, 중복 저장, OAuth 인증, WebSocket 채팅, Redis 랭킹 캐시 같은 실제 서비스형 문제를 직접 다루는 데 초점을 둔 프로젝트입니다.

## Tech Stack

Frontend:

- Next.js
- React
- TypeScript
- Tailwind CSS
- React Query
- STOMP client

Backend:

- Java 21
- Spring Boot 3.4.1
- Spring Security
- OAuth2 Client
- JWT
- JPA/Hibernate
- MySQL
- Redis
- STOMP WebSocket
- Swagger

AI:

- FastAPI
- pandas
- scikit-learn
- joblib
- pymysql
- Playwright

## Main Features

### Summoner Search

- `gameName#tagLine` 기반 소환사 검색
- 최근 검색, 즐겨찾기, 검색 저장 ON/OFF
- 프로필, 랭크, 최근 전적, 포지션 분포, 최근 많이 플레이한 챔피언 표시
- `refresh=false` 기본 조회와 `refresh=true` 전적 갱신 분리
- `updateAt` 기반 최근 갱신 시간과 15분 갱신 쿨다운

### Riot API Optimization

- DB 우선 조회
- Riot Account 정보 단기 캐시
- `summoner.puuid`, `match_info.match_id` unique 충돌 완화
- `INSERT IGNORE` 후 재조회 패턴
- 중복 insert와 deadlock 가능성 감소

### Champion Analytics

- 전체/라인별 챔피언 티어 목록
- 챔피언 상세 빌드, 룬, 스펠, 기본 정보
- Data Dragon champion id/name 매핑 기반 이미지 처리

### Ranking

- Challenger, Grandmaster, Master 랭킹 조회
- Redis 기반 랭킹 캐시
- temp key 생성 후 rename 방식의 atomic swap 구조

### OAuth and User

- Google/Naver OAuth 로그인
- JWT 기반 API 인증
- 최초 로그인 후 닉네임 온보딩
- 내 정보 조회, 닉네임 변경
- 현재 계정에 다른 소셜 로그인 연결
- JWT 만료 시 명확한 JSON 에러 응답

### Guide and Comment

- 로그인 사용자 공략 작성
- Markdown 본문
- 챔피언 선택
- 공략 목록/상세
- 댓글 작성
- 긴 본문 저장을 위한 `LONGTEXT`

### Realtime Chat

- 공략 작성자와 독자 간 1:1 채팅
- Spring STOMP WebSocket
- 방 단위 구독
- 메시지 읽음 처리
- unread badge
- 차단
- 내 목록에서 대화 삭제

### Patch Notes

- 2026년 이후 공식 패치노트 목록 조회
- 챔피언별 패치노트 검색
- 공식 링크 제공
- 서버 크롤링 실패 대비 retry, cache, fallback 목록

### AI Score Experiment

- Arcane DB의 `match_participant` 데이터를 feature로 사용
- 외부 label 기반 회귀 모델 실험
- FastAPI `/predict`
- ExtraTreesRegressor 학습 모델

현재 AI 모델은 실험 단계입니다. 백엔드 전적 점수는 아직 실제 `/predict`가 아니라 랜덤 점수 서버 호출 기반입니다.

## Local Development

### 1. MySQL and Redis

```bash
cd /Users/haechan/Desktop/Arcane/backend/Arcane_Backend
docker compose up -d arcane-db redis
```

Ports:

- MySQL: `localhost:3307`
- Redis: `localhost:6379`

### 2. Backend

```bash
cd /Users/haechan/Desktop/Arcane/backend/Arcane_Backend
./gradlew bootRun
```

Backend URL:

```text
http://localhost:8080
```

### 3. Frontend

```bash
cd /Users/haechan/Desktop/Arcane/frontend/Arcane_Frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:3000
```

Recommended `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 4. AI Server

```bash
cd /Users/haechan/Desktop/Arcane/ai/Arcane_AI
python -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
python -m uvicorn main:app --host 127.0.0.1 --port 8864 --reload
```

AI URL:

```text
http://localhost:8864
```

## Required Environment Variables

Backend:

```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/arcane_db?serverTimezone=Asia/Seoul
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=...
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
RIOT_API_KEY=...
JWT_SECRET=...
JWT_EXPIRATION=3600000
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
MODELING_PYTHON_URL=http://localhost:8864
OAUTH2_SUCCESS_REDIRECT_URI=http://localhost:3000/oauth/callback
OAUTH2_FAILURE_REDIRECT_URI=http://localhost:3000/oauth/callback
```

AI:

```env
ARCANE_DB_HOST=127.0.0.1
ARCANE_DB_PORT=3307
ARCANE_DB_NAME=arcane_db
ARCANE_DB_USER=root
ARCANE_DB_PASSWORD=...
DEEPLOL_CRAWL_DELAY_SECONDS=2.0
```

## Security Notice

Do not commit API keys, OAuth secrets, JWT secrets, `.env` files, generated datasets, or model artifacts.

If any secret was previously committed, rotate it from the provider console and remove the tracked file from Git.

## Current Status

See:

- `PROJECT_STATUS.md`
- `SECURITY_CHECKLIST.md`
- `RUNBOOK.md`
- `PORTFOLIO_STORY.md`
- `DEMO_SCRIPT.md`
- `TODO_PRIORITY.md`

## Disclaimer

Arcane is not endorsed by Riot Games and does not reflect the views or opinions of Riot Games or anyone officially involved in producing or managing League of Legends. League of Legends and Riot Games are trademarks or registered trademarks of Riot Games, Inc.
