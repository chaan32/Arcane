# Arcane Project Status

작성일: 2026-06-04

## 한 줄 요약

Arcane은 League of Legends 전적/챔피언 분석/공략/채팅/AI 점수 실험을 포함한 풀스택 프로젝트다. 프론트, 백엔드, AI 서버는 모두 기능 단위 구현이 상당히 진행됐지만, 보안값 분리, Git 정리, 하드코딩 제거, 모델-백엔드 연결, 빌드 검증이 아직 남아 있다.

## 저장소 구조

현재 루트 `/Users/haechan/Desktop/Arcane` 자체는 Git 저장소가 아니다. 하위 프로젝트별로 Git 저장소가 있다.

- `frontend/Arcane_Frontend`: Next.js 프론트엔드
- `backend/Arcane_Backend`: Spring Boot 백엔드
- `ai/Arcane_AI`: FastAPI + ML 학습/예측 코드
- `Arcane_ai`: 초기 FastAPI 랜덤 점수 서버로 보이는 별도 폴더
- `model_server`: 단순 랜덤 점수 HTTP 서버

실제 최신 AI 작업은 `ai/Arcane_AI`가 기준이다. `Arcane_ai`와 `model_server`는 과거/보조 실험 코드로 분류하는 편이 맞다.

## 프론트엔드 상태

경로: `frontend/Arcane_Frontend`

기술 스택:

- Next.js 15
- React 19
- TypeScript
- Tailwind CSS
- React Query
- STOMP WebSocket client

구현된 주요 기능:

- 홈/네비게이션 소환사 검색
- `gameName#tagLine` 검색 후 `/summoner/{gameName}-{tagLine}` 이동
- 최근 검색, 즐겨찾기, 검색 저장 ON/OFF localStorage 처리
- 소환사 상세 페이지
- 전적 갱신 버튼과 `refresh=true`
- `updateAt` 기반 15분 갱신 쿨다운 표시
- 소환사명 복사 버튼
- 랭크 토글, 포지션 분포, 요약, 최근 많이 한 챔피언
- 챔피언 분석 목록/상세
- 패치노트 페이지
- 공략 목록/상세/작성
- Markdown 미리보기
- 댓글
- OAuth 콜백/온보딩/내 정보
- STOMP 채팅 dock
- unread badge, 읽음 처리, 차단, 대화 목록 삭제

남은 정리:

- 일부 파일에 `http://localhost:8080` 하드코딩이 남아 있다.
- 공통 `API_URL`으로 통일해야 한다.
- 프론트 전체 build/lint 검증은 아직 별도 확인이 필요하다.
- 채팅 UI는 구현되어 있지만 실제 서버와 장시간 사용 시 중복/읽음 상태 검증이 필요하다.
- 가이드 작성에서 이미지가 data URL로 Markdown에 들어가므로 DB 저장 용량과 UX를 확인해야 한다.

## 백엔드 상태

경로: `backend/Arcane_Backend`

기술 스택:

- Java 21
- Spring Boot 3.4.1
- JPA/Hibernate
- MySQL
- Redis
- Spring Security
- OAuth2 Client
- JWT
- STOMP WebSocket
- Swagger
- Jsoup/Java HttpClient

구현된 주요 기능:

- Riot API 소환사 검색
- DB 우선 조회
- `refresh=false` 기본 검색
- `refresh=true` 강제 갱신
- `summoner.updateAt` 기반 최근 갱신 시간 반환
- `summoner.puuid`, `match_info.match_id` 중복 저장 완화
- `INSERT IGNORE` 후 재조회 패턴
- 매치/참가자 저장
- 챔피언/룬/소환사 주문 데이터
- 랭커 Redis 캐시
- OAuth Google/Naver 로그인
- 최초 온보딩 닉네임 설정
- 내 정보 조회/닉네임 변경
- 소셜 계정 연동
- JWT 만료 응답 JSON 처리
- 공략/댓글
- STOMP 채팅
- 채팅 읽음/차단/목록 삭제
- 패치노트 크롤링, 캐시, fallback
- Python 랜덤 점수 서버 호출

남은 정리:

- `application.yml`에 민감값이 하드코딩되어 있고 Git 추적 중이다.
- `StompJwtChannelInterceptor`의 `dont` claim 검증이 잘못되어 있다.
- AI 모델 `/predict`가 백엔드에 아직 연결되지 않았다.
- 현재 백엔드 점수는 `/random` 호출 기반이다.
- `RiotApiService` 일부 메서드는 오류 시 `null`을 반환해 NPE 가능성이 있다.
- 일부 컨트롤러 주석에 미완성/안 쓰는 API가 남아 있다.
- 전체 Gradle test/build 검증이 필요하다.

## AI 상태

경로: `ai/Arcane_AI`

기술 스택:

- FastAPI
- pandas
- scikit-learn
- joblib
- pymysql
- requests
- Playwright

구현된 기능:

- `/health`
- `/random`
- `/predict`
- DeepLOL 기반 label 수집 파이프라인
- Arcane DB `match_participant` feature 추출
- CSV 생성
- ExtraTreesRegressor 학습
- 모델 저장/로드

현재 데이터/모델:

- CSV: `data/deeplol_training.csv`
- CSV row: 헤더 포함 15,183줄
- 학습 clean rows: 15,120
- 모델: `models/deeplol_score_model.joblib`
- 모델 크기: 약 224MB
- MAE: 약 3.82
- RMSE: 약 5.24
- R2: 약 0.883

남은 정리:

- 목표 40,000 row 대비 약 24,880 row 부족
- DeepLOL label 대량 수집은 약관/윤리 리스크가 있다.
- 안전한 방향은 Riot 공식 API 데이터 기반 자체 점수 공식 또는 허용된 label로 전환하는 것이다.
- AI `.gitignore`가 너무 약하다. `data/`, `models/`, `.env`, `__pycache__/` 등을 정리해야 한다.
- 백엔드에서 `/predict`를 호출하도록 연결해야 실제 서비스 점수가 된다.

## 현재 가장 큰 리스크

1. 민감값 노출
2. Git 작업 상태가 너무 큼
3. 백엔드/프론트 일부 하드코딩
4. AI 모델과 백엔드 미연결
5. DeepLOL label 사용 리스크
6. 실제 빌드/테스트 검증 미완료
7. 데이터 4만 row 미달

## 현재 가장 좋은 다음 방향

1. 보안값 분리와 키 재발급
2. `.gitignore`와 추적 파일 정리
3. 프론트 API URL 하드코딩 제거
4. STOMP JWT claim 검증 버그 수정
5. 백엔드 `/predict` 연결
6. 실행/빌드 검증
7. 포트폴리오 문서 정리
