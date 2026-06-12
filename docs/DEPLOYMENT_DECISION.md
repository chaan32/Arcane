# Arcane Deployment Decision

## Decision

현재 Arcane 배포는 Kubernetes가 아니라 Docker 기반으로 진행한다.

## Why Docker First

- 현재 프로젝트는 API Server, Worker Server, AI Server, Frontend, MySQL, Redis, Kafka, MongoDB, Elasticsearch, Logstash, Kibana, Prometheus, Grafana로 구성되어 있다.
- 이미 Docker Compose로 주요 인프라가 구성되어 있어 로컬/단일 서버 배포 환경을 빠르게 재현할 수 있다.
- 지금 단계의 핵심은 컨테이너화, 환경 변수 분리, 서비스 간 네트워크 연결, 로그/메트릭 확인이다.
- Kubernetes는 Ingress, Secret, ConfigMap, PVC, HPA, Rolling Update, Service Discovery까지 함께 운영해야 하므로 현재 단계에서는 운영 복잡도가 더 크다.

## When To Move To Kubernetes

아래 조건이 필요해지면 Kubernetes 전환을 검토한다.

- API Server, Worker Server, AI Server를 각각 2개 이상 복제해서 운영해야 한다.
- 무중단 배포, Rolling Update, Rollback이 필수 요구사항이 된다.
- Worker 작업량에 따라 자동 스케일링이 필요하다.
- Kafka, Redis, DB를 외부 관리형 서비스로 분리하고 애플리케이션만 클러스터에서 운영한다.
- 운영 환경의 Secret, ConfigMap, Ingress, TLS, 리소스 제한을 표준화해야 한다.

## Docker Images

각 실행 단위는 독립 이미지로 빌드한다.

```bash
docker build -t arcane-api ./backend/Arcane_Backend
docker build -t arcane-worker ./worker
docker build -t arcane-ai ./ai/Arcane_AI
docker build -t arcane-frontend ./frontend/Arcane_Frontend
```

Frontend는 `NEXT_PUBLIC_API_URL`이 빌드 시점에 반영되므로 배포 주소가 다르면 build arg로 주입한다.

```bash
docker build \
  --build-arg NEXT_PUBLIC_API_URL=http://localhost:8080 \
  --build-arg NEXT_PUBLIC_DDRAGON_VERSION=16.11.1 \
  -t arcane-frontend ./frontend/Arcane_Frontend
```

## Local Infra

이제 루트 Compose 파일 하나로 인프라와 모든 애플리케이션 서버를 함께 실행한다.

기존에 `backend/Arcane_Backend`에서 인프라 Compose를 띄워둔 상태라면 같은 컨테이너 이름과 포트가 충돌하므로 먼저 내린다. 볼륨은 삭제하지 않으므로 DB, MongoDB, Kafka, Grafana 데이터는 유지된다.

```bash
cd /Users/haechan/Desktop/Arcane/backend/Arcane_Backend
docker compose down
```

그 다음 루트에서 전체 스택을 빌드하고 실행한다.

```bash
cd /Users/haechan/Desktop/Arcane
docker compose up -d --build
```

기존 `backend/Arcane_Backend/docker-compose.yml`은 인프라 단독 실행용으로 남겨두고, 전체 실행 기준은 루트 `docker-compose.yml`로 둔다.

포트는 아래 기준으로 열린다.

- Frontend: http://localhost:3000
- API Server: http://localhost:8080
- Worker Server: http://localhost:7749
- AI Server: http://localhost:8864
- MySQL: localhost:3307
- Redis: localhost:6379
- Kafka: localhost:9092
- MongoDB: localhost:27017
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001
