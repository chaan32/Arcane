# Arcane TODO Priority

작성일: 2026-06-04

## 기준

우선순위는 다음 기준으로 정했다.

1. 보안상 위험한 것
2. 프로젝트 실행/시연을 막는 것
3. 포트폴리오에서 설명하기 좋은 핵심 개선
4. 나중에 해도 되는 품질 개선
5. 실험성 기능

## P0. 즉시 처리

### 1. 민감값 분리

대상:

- `backend/Arcane_Backend/src/main/resources/application.yml`
- `backend/Arcane_Backend/.env`

해야 할 일:

- Riot API key 제거
- OAuth client secret 제거
- JWT secret 제거
- 환경변수 참조로 변경
- 노출된 key 재발급
- Git tracked 상태 제거

완료 기준:

- 코드에 실제 secret이 없다.
- Git status에 secret 파일이 추적되지 않는다.
- 백엔드는 환경변수로 정상 실행된다.

### 2. AI `.gitignore` 정리

대상:

- `ai/Arcane_AI/.gitignore`

해야 할 일:

- `.env`
- `.venv/`
- `__pycache__/`
- `*.pyc`
- `data/`
- `models/`

완료 기준:

- 대용량 모델과 CSV가 Git untracked 목록에 계속 뜨지 않는다.

### 3. STOMP 암구호 검증 버그 수정

대상:

- `backend/Arcane_Backend/src/main/java/com/arcane/Arcane/Common/WebSocket/Stomp/StompJwtChannelInterceptor.java`

현재 문제:

```java
if (!passphrase.equals(passphrase)) {
```

해야 할 일:

- `passphrase`가 null인지 확인
- `"again"`과 비교

완료 기준:

- `dont` claim이 없거나 값이 다르면 WebSocket CONNECT 실패
- 정상 JWT는 연결 성공

## P1. 시연 안정화

### 4. 프론트 API URL 하드코딩 제거

대상:

- `frontend/Arcane_Frontend/app/summoner/[name]/page.tsx`
- `frontend/Arcane_Frontend/app/champions/page.tsx`
- `frontend/Arcane_Frontend/app/ranking/page.tsx`

해야 할 일:

- `http://localhost:8080` 직접 사용 제거
- `API_URL` import 후 공통 사용

완료 기준:

- `rg "http://localhost:8080" frontend/Arcane_Frontend/app frontend/Arcane_Frontend/services` 결과가 의도된 메시지 외에는 없다.

### 5. 실행 검증

해야 할 일:

- 백엔드 compile/test
- 프론트 build/lint
- AI py_compile
- 핵심 화면 브라우저 확인

완료 기준:

- 실행 방법 문서대로 개발 서버가 뜬다.
- 홈, 소환사, 챔피언, 공략, 패치노트, 로그인 콜백 화면이 깨지지 않는다.

### 6. 공략 이미지 저장 방식 결정

현재:

- 프론트에서 이미지 data URL을 Markdown 본문에 직접 삽입
- 백엔드 content는 LONGTEXT

선택지:

- 데모용으로 유지
- 이미지 업로드 기능은 제외
- DB에 base64 저장 제한
- 나중에 S3/로컬 파일 저장

추천:

- 데모에서는 이미지 기능을 과하게 강조하지 않는다.
- 포트폴리오에서는 Markdown 공략 중심으로 설명한다.

## P2. 백엔드 품질 개선

### 7. AI `/predict` 백엔드 연결

현재:

- `PythonService`는 `/random`만 호출
- AI 서버에는 `/predict` 존재

해야 할 일:

- `MatchParticipant` feature를 AI feature schema에 맞게 변환
- `/predict` 호출 DTO 생성
- 실패 시 fallback 정책 결정
- 기존 랜덤 점수 제거 또는 dev fallback으로 제한

완료 기준:

- 새 매치 저장 시 `ourScore`가 모델 예측값으로 들어간다.

### 8. RiotApiService null 반환 정리

현재:

- 일부 메서드가 오류 시 `null` 반환
- 호출부에서 NPE 가능

해야 할 일:

- 빈 배열/빈 리스트/명확한 예외로 통일
- 429는 `TooManyRequestFail`로 올리기
- 404는 `CannotFoundSummoner`

완료 기준:

- 외부 API 오류가 프론트에서 이해 가능한 메시지로 표시된다.

### 9. 미완성 API 정리

대상 예시:

- `/api/v1/summoner/most`
- `/api/v1/summoner/match/summary/{matchId}`
- tune 관련 실험 API

해야 할 일:

- 실제 사용 여부 확인
- 안 쓰면 숨기거나 주석 정리
- Swagger 설명 정리

완료 기준:

- 시연 중 "미완성" 주석이 노출되거나 설명을 방해하지 않는다.

## P3. 데이터/AI 방향

### 10. 4만 row 확보 전략 재설계

현재:

- clean rows 약 15,120
- 목표 40,000
- 부족분 약 24,880

추천 방향:

- DeepLOL label 대량 수집을 확장하지 않는다.
- Riot 공식 API로 match participant 원천 데이터 확보
- 자체 점수 공식 설계
- 자체 label로 모델 재학습

완료 기준:

- 포트폴리오에서 약관 리스크 없이 설명 가능한 AI 점수 전략이 된다.

### 11. 자체 점수 공식 초안

후보 feature:

- 승패
- KDA
- 킬 관여
- 챔피언에게 가한 피해량
- 받은 피해량
- CS
- 시야 점수
- 제어 와드 구매
- 포지션별 보정
- 게임 시간 보정

주의:

- win 비중이 너무 크면 "이기면 고득점" 모델이 된다.
- 포지션별 역할 차이를 반영해야 한다.

## P4. 포트폴리오 마무리

### 12. README 정리

필요 내용:

- 프로젝트 소개
- 기술 스택
- 아키텍처
- 핵심 기능
- 실행 방법
- 트러블슈팅
- 보안 주의
- 시연 흐름

### 13. 스크린샷/영상 준비

추천 화면:

- 홈 검색
- 소환사 상세
- 전적 갱신
- 챔피언 분석
- 공략 작성
- 채팅
- 패치노트

주의:

- API key나 secret이 보이는 화면 캡처 금지
- 터미널 로그에 secret이 찍히면 안 됨

### 14. 발표 수치 측정

측정하면 좋은 것:

- DB hit 시 Riot API 호출 수 0회
- refresh=true 시 호출 수
- 랭킹 Redis 조회 응답 시간
- 채팅 메시지 왕복 시간
- 전적 검색 평균 응답 시간

주의:

- 측정하지 않은 수치는 추정이라고 명확히 말한다.

## 버려도 되는 것

- `Arcane_ai` 초기 랜덤 서버
- `model_server/random_score_server.py`
- 오래된 프론트 backup 파일
- static 테스트 JSON 파일
- tune 실험 화면

버리기 전 주의:

- Git에서 추적 중인지 확인
- 실제 백엔드 코드가 참조하는지 `rg`로 확인
- 삭제는 별도 작업으로 분리
