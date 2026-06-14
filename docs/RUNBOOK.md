# Arcane Local Runbook

작성일: 2026-06-04

> 로컬 개발 기준 문서다. 현재 운영 배포 기준은 [DEPLOYMENT_VERCEL_EC2.md](DEPLOYMENT_VERCEL_EC2.md), [GITHUB_ACTIONS_CICD.md](GITHUB_ACTIONS_CICD.md)를 함께 본다.

## 실행 구성

개발 중 권장 구성:

- MySQL: Docker
- Redis: Docker
- Spring Boot: IntelliJ 또는 `./gradlew bootRun`
- Next.js: `npm run dev`
- AI FastAPI: `uvicorn`

## 1. MySQL / Redis 실행

경로:

```bash
cd /Users/haechan/Desktop/Arcane/backend/Arcane_Backend
```

실행:

```bash
docker compose up -d arcane-db redis
```

포트:

- MySQL: `localhost:3307`
- Redis: `localhost:6379`

상태 확인:

```bash
docker ps -a --filter "name=arcane"
```

MySQL 접속:

```bash
docker exec -it arcane-db mysql -uroot -p
```

주의:

- MySQL CLI는 `-u` 소문자를 쓴다.
- `mysql -U root -p`는 PostgreSQL 스타일이라 맞지 않는다.

## 2. 백엔드 실행

경로:

```bash
cd /Users/haechan/Desktop/Arcane/backend/Arcane_Backend
```

실행:

```bash
./gradlew bootRun
```

기본 URL:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

필요 환경:

- Java 21
- MySQL
- Redis
- Riot API key
- JWT secret
- OAuth client id/secret

## 3. 프론트엔드 실행

경로:

```bash
cd /Users/haechan/Desktop/Arcane/frontend/Arcane_Frontend
```

설치:

```bash
npm install
```

실행:

```bash
npm run dev
```

기본 URL:

```text
http://localhost:3000
```

권장 `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## 4. AI 서버 실행

경로:

```bash
cd /Users/haechan/Desktop/Arcane/ai/Arcane_AI
```

가상환경:

```bash
python -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
```

실행:

```bash
python -m uvicorn main:app --host 127.0.0.1 --port 8864 --reload
```

확인:

```bash
curl http://localhost:8864/health
curl http://localhost:8864/random
```

예측 API:

```text
POST http://localhost:8864/predict
```

주의:

- 현재 백엔드는 `/predict`가 아니라 `/random`을 호출한다.
- 실제 모델 점수를 쓰려면 백엔드 `PythonService`와 match participant feature 변환이 필요하다.

## 5. OAuth 로컬 설정

Google:

- JavaScript origin: `http://localhost:3000`
- Redirect URI: `http://localhost:8080/login/oauth2/code/google`

Naver:

- Service URL: `http://localhost:3000`
- Callback URL: `http://localhost:8080/login/oauth2/code/naver`

주의:

- OAuth redirect URI는 프론트가 아니라 백엔드 Spring Security URL이다.
- 로그인 성공 후 백엔드가 프론트 `/oauth/callback`으로 다시 보낸다.

## 6. 자주 나는 문제

### 프론트 API URL이 `undefined`가 되는 경우

원인:

- `NEXT_PUBLIC_API_URL`이 없거나, 코드에서 공통 `API_URL`을 쓰지 않음

해결:

- `constants/api.ts` 기본값 확인
- 하드코딩된 `http://localhost:8080`을 공통 `API_URL`로 통일

### Riot API 429

원인:

- 같은 검색에서 `/profile`, `/tier`, `/matches`, `/mastery`가 동시에 외부 API를 호출
- DB hit 없이 매번 Riot API 호출

해결:

- 일반 검색은 `refresh=false`
- 전적 갱신 버튼만 `refresh=true`
- DB 우선 조회 유지

### 중복 insert / deadlock

원인:

- 동시에 같은 `summoner.puuid` 또는 `match_info.match_id` 저장 시도

해결:

- `INSERT IGNORE`
- insert 후 반드시 DB 재조회
- service lock 유지

### Next.js `.next` 캐시 문제

증상:

```text
ENOENT: no such file or directory, open '.next/server/app/.../page.js'
```

해결:

```bash
rm -rf .next
npm run dev
```

주의:

- `rm -rf`는 destructive 명령이라 실행 전 확인하고 사용한다.

### Hikari connection closed

원인:

- MySQL 컨테이너 재시작
- DB connection wait timeout
- Hikari pool의 stale connection

해결:

- 백엔드 재시작
- DB 컨테이너 확인
- Hikari maxLifetime 설정 검토

## 7. 권장 검증 순서

1. Docker MySQL/Redis 실행
2. 백엔드 실행
3. Swagger 접속
4. 프론트 실행
5. 홈 검색
6. 소환사 페이지 진입
7. 전적 갱신 버튼
8. OAuth 로그인
9. 공략 작성
10. 채팅 송수신
11. AI 서버 `/health`
12. 백엔드 Python random endpoint

## 8. 운영 배포 확인

운영 URL:

```text
Frontend: https://www.ar-cane.site
API: https://api.ar-cane.site
```

EC2 프로젝트 경로:

```bash
cd /home/ubuntu/opt/arcane/Arcane
```

컨테이너 상태:

```bash
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml ps
```

API 로그:

```bash
docker logs -f arcane-api
```

Worker 로그:

```bash
docker logs -f arcane-worker
```

API health:

```bash
curl https://api.ar-cane.site/actuator/health
```

Riot site verification:

```bash
curl https://www.ar-cane.site/riot.txt
```

## 9. WebSocket 운영 점검

Nginx 설정 확인:

```bash
sudo nginx -T | grep -n "server_name api.ar-cane.site" -A30
```

필수 설정:

```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

설정 검증:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

WebSocket handshake 확인:

```bash
curl -i --http1.1 -N \
  -H "Origin: https://www.ar-cane.site" \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  -H "Sec-WebSocket-Version: 13" \
  https://api.ar-cane.site/ws/chat
```

정상 응답:

```text
HTTP/1.1 101
Upgrade: websocket
```

## 10. CI/CD 수동 확인

GitHub Actions에서 실제 배포는 `deploy-ec2.yml`을 실행한다.

- `ci.yml`: compile/build 검증
- `deploy-ec2.yml`: Docker image build, GHCR push, EC2 deploy

배포 후 EC2에서 image 갱신 여부를 확인한다.

```bash
docker images | grep arcane
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}"
```
