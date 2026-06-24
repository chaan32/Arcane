# Arcane Observability Guide

## 1. 로그 흐름: Spring Boot -> File -> Logstash -> Elasticsearch -> Kibana/Grafana

API Server는 `backend/Arcane_Backend/logs/arcane-api-log.log`에 로그를 남긴다.

Worker Server는 `worker/logs/arcane-worker-log.log`에 로그를 남긴다.

Logstash는 `docker/logstash.conf`에서 API/Worker 로그 파일을 모두 읽고, 로그 한 줄을 grok/date filter로 파싱한 뒤 Elasticsearch의 `arcane-logs-YYYY.MM.dd` 인덱스에 저장한다.

Kibana는 Elasticsearch에 저장된 인덱스를 사람이 검색하고 시각화할 수 있게 해주는 UI다. 즉, Kibana는 로그를 저장하지 않는다. 저장소는 Elasticsearch이고 Kibana는 조회 도구다.

Grafana의 `Elasticsearch Logs` datasource도 같은 `arcane-logs-*` 인덱스를 바라본다.

관리자 페이지의 로그 화면은 API 서버 컨테이너가 파일 시스템에서 직접 로그 파일을 읽는다. Docker 실행 시 API 컨테이너 내부 경로는 아래처럼 잡힌다.

| 로그 | 호스트 경로 | API 컨테이너 내부 경로 |
| --- | --- | --- |
| API 로그 | `./backend/Arcane_Backend/logs` | `/app/logs` |
| Worker 로그 | `./worker/logs` | `/app/worker-logs` |

### Kibana에서 로그 보기

1. `http://localhost:5601` 접속
2. 왼쪽 메뉴에서 `Stack Management` 선택
3. `Data Views` 선택
4. `Create data view` 선택
5. Name 또는 Index pattern에 `arcane-logs-*` 입력
6. Timestamp field는 `@timestamp` 선택
7. 생성 후 `Discover` 메뉴에서 로그 확인

Logstash filter를 수정한 뒤 `level`, `logger`, `log_message`, `service` 같은 새 필드가 안 보이면 `Stack Management` -> `Data Views` -> `Arcane Logs` -> `Refresh field list`를 누른다.

검색 예시:

```text
level : "ERROR"
```

```text
traceId : "abcd1234"
```

```text
service : "worker"
```

```text
log_message : *랭킹*
```

## 2. 메트릭 흐름: Spring Boot Actuator -> Prometheus -> Grafana

API Server와 Worker Server는 Spring Boot Actuator의 `/actuator/prometheus` endpoint에서 JVM, HTTP 요청, DB connection, Kafka consumer 같은 메트릭을 Prometheus 포맷으로 노출한다.

Prometheus는 `prometheus.yml`에 정의된 target을 15초마다 호출해서 시계열 데이터를 저장한다.

Grafana는 Prometheus를 datasource로 사용해서 메트릭을 dashboard로 보여준다. 즉, Grafana도 직접 데이터를 수집하지 않는다. 수집과 저장은 Prometheus가 한다.

### Grafana에서 메트릭 보기

1. `http://localhost:3001` 접속
2. 로그인
   - ID: `admin`
   - Password: `admin`
3. 왼쪽 메뉴에서 `Dashboards` 선택
4. `Arcane / Arcane Spring Boot Overview` 열기

### Prometheus target 확인

`http://localhost:9090/targets` 접속 후 아래 target이 `UP`인지 확인한다.

- `arcane-api`
- `arcane-worker`

`DOWN`이면 해당 Spring Boot 컨테이너가 실행 중인지 먼저 확인한다.

```bash
docker compose ps api-server worker-server prometheus grafana
```

각 서버의 Actuator endpoint를 직접 확인할 수도 있다.

```bash
curl http://localhost:8080/actuator/prometheus
curl http://localhost:7749/actuator/prometheus
```

Docker Compose로 실행할 때 Prometheus target은 컨테이너 이름을 바라본다.

```yaml
targets:
  - api-server:8080
  - worker-server:7749
```

### IntelliJ에서 앱만 직접 실행할 때

`server.address` 같은 `application.yml` 설정을 바꾼 뒤에는 IntelliJ에서 Spring Boot 앱을 완전히 Stop 후 다시 Run 해야 한다. Prometheus는 15초마다 다시 긁으므로 백엔드가 정상 재시작되면 별도 조작 없이 `UP`으로 바뀐다.

Spring Boot를 IntelliJ/로컬에서 실행하고 Prometheus만 Docker에서 실행한다면 Prometheus target은 아래처럼 host를 바라봐야 한다.

```yaml
targets:
  - host.docker.internal:8080
  - host.docker.internal:7749
```

### Prometheus target이 502 Bad Gateway로 DOWN일 때

Mac에서 Spring Boot를 IntelliJ로 직접 실행하면 Java가 IPv6 쪽으로만 listen해서 Docker 컨테이너의 `host.docker.internal:8080` 접근이 `502 Bad Gateway`로 실패할 수 있다.

이때는 IntelliJ 실행 설정에 아래 VM option을 추가한다.

```text
-Djava.net.preferIPv4Stack=true
```

IntelliJ 설정 위치:

1. `Run` -> `Edit Configurations...`
2. `ArcaneApplication` 선택
3. `Modify options` -> `Add VM options` 선택
4. VM options에 `-Djava.net.preferIPv4Stack=true` 입력
5. 실행 중인 Spring Boot 앱을 `Stop` 후 다시 `Run`

정상 적용되면 `http://127.0.0.1:8080/actuator/prometheus`와 Prometheus의 `host.docker.internal:8080` scrape가 모두 성공해야 한다.

## 3. 실행 명령

기본 관측성 스택 실행:

```bash
docker compose up -d
```

Worker Actuator 의존성을 바꾼 뒤에는 Worker 이미지를 다시 빌드해야 한다.

```bash
docker compose up -d --build worker-server prometheus grafana
```

상태 확인:

```bash
docker compose ps
```

중지:

```bash
docker compose down
```

주의: DB 데이터를 지우면 안 되므로 `docker compose down -v`는 사용하지 않는다.

## 4. 접속 주소

| 도구 | 주소 | 역할 |
| --- | --- | --- |
| Elasticsearch | `http://localhost:9200` | 로그 저장소 |
| Kibana | `http://localhost:5601` | Elasticsearch 로그 조회 UI |
| Prometheus | `http://localhost:9090` | Actuator 메트릭 수집/저장 |
| Grafana | `http://localhost:3001` | Prometheus 메트릭 대시보드 |
