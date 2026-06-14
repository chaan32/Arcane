# Arcane Project Status

작성일: 2026-06-14

## 한 줄 요약

Arcane은 League of Legends 전적 검색, 챔피언 분석, 공략, 댓글, 실시간 채팅, AI 점수 실험을 포함한 풀스택 서비스다. 현재 Vercel 프론트엔드와 EC2 Docker Compose 백엔드/인프라 배포가 완료되었고, GitHub Actions 기반 CI/CD 흐름이 동작한다.

## 운영 URL

| 대상 | URL |
| --- | --- |
| Frontend | https://www.ar-cane.site |
| API Server | https://api.ar-cane.site |
| Riot verification | https://www.ar-cane.site/riot.txt |

## 저장소 상태

루트 `/Users/haechan/Desktop/Arcane`를 단일 Git 저장소로 사용한다.

하위 프로젝트:

- `frontend/Arcane_Frontend`: Next.js frontend
- `backend/Arcane_Backend`: Spring Boot API server
- `worker`: Spring Boot worker server
- `ai/Arcane_AI`: FastAPI AI server
- `docker`: Nginx, Logstash, Prometheus 설정
- `docs`: 프로젝트 문서

과거 nested Git 저장소 흔적은 제거했고, GitHub에는 루트 저장소 기준으로 push한다.

## 배포 상태

### Frontend

- Vercel Git integration 사용
- Root Directory: `frontend/Arcane_Frontend`
- Framework: Next.js
- Build Command: `npm run build`
- Environment:
  - `NEXT_PUBLIC_API_URL=https://api.ar-cane.site`

### Backend / Worker / AI / Infra

- AWS EC2 Ubuntu 24.04
- Docker Compose 실행
- Nginx + Certbot으로 API HTTPS 처리
- API domain: `api.ar-cane.site`
- Docker images는 GHCR에서 pull
- `docker-compose.ec2.yml` 기준 운영

### CI/CD

- `.github/workflows/deploy-ec2.yml`
- `main` push 시 Docker image build / GHCR push / EC2 deploy
- 성공 확인 완료

## 주요 인프라

| 구성 | 역할 |
| --- | --- |
| Vercel | Next.js frontend hosting |
| EC2 | backend, worker, AI, database, broker, monitoring container 실행 |
| Nginx | HTTPS termination, API reverse proxy, WebSocket proxy |
| GHCR | API / Worker / AI Docker image registry |
| GitHub Actions | image build, push, EC2 deploy |
| MySQL | users, guides, comments, chat, champion analysis tables |
| MongoDB | match participant raw data |
| Redis | ranking cache, job status, cache lock |
| Kafka | async job orchestration |
| Elasticsearch | guide search, summoner autocomplete, log search |
| Logstash / Kibana | logs |
| Prometheus / Grafana | metrics |

## 기능 상태

### 완료/운영 가능

- Riot ID 기반 소환사 검색
- 최근 전적 조회
- 랭킹 데이터 캐시
- Google/Naver OAuth login
- JWT authentication
- 공략글 작성/조회
- S3 기반 공략 이미지 업로드
- 댓글
- 실시간 STOMP 채팅
- 관리자 페이지
- Worker 기반 데이터 수집
- Worker 기반 챔피언 분석
- 챔피언 티어리스트/상세 페이지 실제 데이터 연동
- Vercel + EC2 배포
- GitHub Actions CI/CD

### 실험/개선 진행

- AI score Kafka 연결
- Elasticsearch 기반 공략글 검색 성능 비교
- Elasticsearch 기반 소환사 자동완성
- 대규모 데이터 재수집
- Riot Production API Key 심사
- Expanded Rate Limit 요청 준비

## 최근 해결한 문제

### WebSocket handshake 실패

증상:

```text
Handshake failed due to invalid Upgrade header: null
```

해결:

- Nginx에 `Upgrade` / `Connection` proxy header 추가
- Spring Security에서 `/ws/**` permit 처리
- `curl --http1.1` WebSocket handshake 검증 결과 `HTTP/1.1 101` 확인

### GHCR pull 403

증상:

```text
403 Forbidden
```

해결:

- `GHCR_TOKEN` 권한 확인
- package access 설정 확인
- EC2 deploy job에서 `docker login ghcr.io` 수행

### Vercel 404

증상:

Vercel 배포 후 404가 표시됐다.

해결:

- Root Directory를 `frontend/Arcane_Frontend`로 설정
- Build Command / Install Command 확인
- Vercel redeploy

### Riot site verification

증상:

Riot Production API Key 신청 후 Product URL 검증 필요.

해결:

- `frontend/Arcane_Frontend/public/riot.txt` 추가
- `https://www.ar-cane.site/riot.txt` 접근 가능하도록 Vercel 배포

## 검증 명령

API health:

```bash
curl https://api.ar-cane.site/actuator/health
```

WebSocket proxy:

```bash
curl -i --http1.1 -N \
  -H "Origin: https://www.ar-cane.site" \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  -H "Sec-WebSocket-Version: 13" \
  https://api.ar-cane.site/ws/chat
```

EC2 services:

```bash
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml ps
```

API logs:

```bash
docker logs -f arcane-api
```

## 남은 작업

1. Riot Production API Key 승인 대기
2. 승인 후 EC2 `.env.ec2`의 `RIOT_API_KEY` 교체
3. 대규모 데이터 수집 재실행
4. 챔피언 분석 재실행
5. 관리자 페이지에서 수집/분석 상태 확인
6. Elasticsearch 검색 기능 운영 반영
7. Grafana / Kibana 접근 보안 정리
8. DB backup strategy 정리
9. EC2 비용 최적화
