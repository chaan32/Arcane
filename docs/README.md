# Arcane Documentation

이 폴더는 Arcane 프로젝트의 설계, 배포, 운영, 트러블슈팅, 포트폴리오 정리 문서를 모아둔 곳이다.

## 빠른 이동

| 문서 | 내용 |
| --- | --- |
| [ARCANE_ARCHITECTURE.md](ARCANE_ARCHITECTURE.md) | 전체 시스템 아키텍처와 핵심 플로우 |
| [PROJECT_STATUS.md](PROJECT_STATUS.md) | 현재 프로젝트 상태와 남은 작업 |
| [OPERATIONS_HISTORY.md](OPERATIONS_HISTORY.md) | 지금까지 진행한 주요 작업 이력 |
| [DEPLOYMENT_VERCEL_EC2.md](DEPLOYMENT_VERCEL_EC2.md) | Vercel 프론트 + EC2 백엔드 배포 가이드 |
| [GITHUB_ACTIONS_CICD.md](GITHUB_ACTIONS_CICD.md) | GitHub Actions, GHCR, EC2 자동 배포 구조 |
| [RIOT_API_PRODUCTION.md](RIOT_API_PRODUCTION.md) | Riot Production API Key 신청과 사이트 검증 절차 |
| [RUNBOOK.md](RUNBOOK.md) | 로컬 실행, 장애 대응, 기본 운영 명령 |
| [SECURITY_CHECKLIST.md](SECURITY_CHECKLIST.md) | 비밀값, 키 관리, Git 추적 주의사항 |
| [PORTFOLIO_STORY.md](PORTFOLIO_STORY.md) | 포트폴리오용 AS-IS / TO-BE 정리 |
| [DEMO_SCRIPT.md](DEMO_SCRIPT.md) | 시연 순서와 체크 포인트 |

## 서비스별 문서

| 영역 | 문서 |
| --- | --- |
| Frontend | [frontend/Arcane_Frontend/README.md](frontend/Arcane_Frontend/README.md), [FRONTEND_OPTIMIZATION_REPORT.md](frontend/Arcane_Frontend/FRONTEND_OPTIMIZATION_REPORT.md) |
| Backend | [backend/Arcane_Backend/README.md](backend/Arcane_Backend/README.md), [ARCANE_LOGIC_FLOW.md](backend/Arcane_Backend/ARCANE_LOGIC_FLOW.md) |
| Worker | [worker/RANKING_UPDATE_WORKER_GUIDE.md](worker/RANKING_UPDATE_WORKER_GUIDE.md), [worker/HELP.md](worker/HELP.md) |
| AI | [ai/Arcane_AI/DATASET_CRAWLER.md](ai/Arcane_AI/DATASET_CRAWLER.md) |

## 성능/검색 실험 문서

| 문서 | 내용 |
| --- | --- |
| [backend/Arcane_Backend/GUIDE_SEARCH_ELASTICSEARCH_REPORT.md](backend/Arcane_Backend/GUIDE_SEARCH_ELASTICSEARCH_REPORT.md) | 공략글 내용 검색 MySQL LIKE vs Elasticsearch 비교 |
| [backend/Arcane_Backend/SUMMONER_SEARCH_ELASTICSEARCH_REPORT.md](backend/Arcane_Backend/SUMMONER_SEARCH_ELASTICSEARCH_REPORT.md) | 소환사 자동완성 LIKE vs Elasticsearch 비교 |
| [backend/Arcane_Backend/OBSERVABILITY_GUIDE.md](backend/Arcane_Backend/OBSERVABILITY_GUIDE.md) | Actuator, Prometheus, Grafana, ELK 관측성 구성 |

## 현재 운영 URL

| 대상 | URL |
| --- | --- |
| Frontend | https://www.ar-cane.site |
| API | https://api.ar-cane.site |
| Riot verification | https://www.ar-cane.site/riot.txt |

## 핵심 운영 흐름

1. `main` 브랜치에 push한다.
2. Vercel은 `frontend/Arcane_Frontend`를 자동 배포한다.
3. GitHub Actions는 API, Worker, AI Docker image를 빌드한다.
4. 이미지는 GHCR에 push된다.
5. EC2는 GHCR image를 pull하고 `docker-compose.ec2.yml`로 컨테이너를 재기동한다.
6. Nginx는 `https://api.ar-cane.site` 요청을 `localhost:8080`으로 프록시한다.

## 자주 확인하는 명령

EC2 컨테이너 상태:

```bash
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml ps
```

API 로그:

```bash
docker logs -f arcane-api
```

Nginx 설정 검증:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

WebSocket 프록시 검증:

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
