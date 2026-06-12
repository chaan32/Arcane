# Arcane Codex Context

이 문서는 Codex 맥북 앱에서 진행하던 긴 작업 흐름을 Codex CLI로 넘기기 위한 인수인계 문서다.
CLI에서 새 세션을 열면 이 파일을 먼저 읽고, 사용자의 최신 요청과 함께 이어서 작업하면 된다.

작성일: 2026-06-04  
프로젝트 루트: `/Users/haechan/Desktop/Arcane`

## 1. 사용자의 작업 방식과 반드시 지켜야 할 점

사용자는 한국어로 소통한다. 답변도 한국어로 한다.

사용자는 코드 수정과 설명을 구분해서 요구한다.

- 프론트엔드 코드는 Codex가 직접 수정해도 된다.
- 백엔드 코드는 기본적으로 사용자가 직접 칠 수 있게 어느 파일, 어느 함수, 어느 코드가 문제인지 자세히 설명하는 방식을 선호한다.
- 다만 사용자가 명시적으로 "백엔드 코드도 직접 수정해"라고 하면 직접 수정해도 된다.
- 실행 명령은 사용자의 허락 없이 마음대로 실행하지 말라는 요구가 있었다. 필요한 명령 실행 전에는 목적을 짧게 말하고 진행한다.
- 코드 수정 전에는 어떤 파일에 어떤 변경을 할지 먼저 알려주는 편이 좋다.
- 설명할 때는 "문제 원인", "수정 위치", "수정 코드", "수정 후 흐름", "주의점"을 구체적으로 써야 한다.
- 단순히 "이렇게 바꿨어"가 아니라, 어떤 테이블/컬럼/메소드/요청 흐름 때문에 문제가 생겼고 왜 해당 수정이 맞는지 설명해야 한다.

사용자는 화가 나거나 답답할 때 강하게 표현할 수 있다. 그럴 때 방어적으로 반응하지 말고, 실제 문제를 빠르게 좁히고 정확히 정리한다.

## 2. 프로젝트 구조

전체 프로젝트는 대략 다음 구조다.

```text
/Users/haechan/Desktop/Arcane
├── frontend/Arcane_Frontend
├── backend/Arcane_Backend
└── ai/Arcane_AI
```

각 영역의 역할:

- `frontend/Arcane_Frontend`: Next.js 프론트엔드. 전적 검색, 공략, 채팅, 패치노트, 챔피언 분석 UI를 담당한다.
- `backend/Arcane_Backend`: Spring Boot 백엔드. Riot API 연동, DB 저장/조회, OAuth, JWT, 공략/댓글/채팅/STOMP, 패치노트 크롤링 등을 담당한다.
- `ai/Arcane_AI`: FastAPI 기반 AI 서버. 초기에 랜덤 점수 서버로 시작했고, 이후 DeepLOL 점수 기반 회귀 모델 학습/예측 코드가 추가됐다.

## 3. 로컬 실행과 인프라

프론트엔드:

```bash
cd /Users/haechan/Desktop/Arcane/frontend/Arcane_Frontend
npm install
npm run dev
```

Next.js 개발 서버는 보통 `http://localhost:3000`에서 열린다.

백엔드:

```bash
cd /Users/haechan/Desktop/Arcane/backend/Arcane_Backend
./gradlew bootRun
```

Spring Boot 서버는 보통 `http://localhost:8080`에서 열린다.

AI 서버:

```bash
cd /Users/haechan/Desktop/Arcane/ai/Arcane_AI
uvicorn main:app --host 0.0.0.0 --port 8864 --reload
```

백엔드는 예전에 `http://localhost:8864/random`을 호출해서 AI 점수나 랜덤 점수를 받았다.

Docker 컨테이너 이름:

- MySQL: `arcane-db`
- Redis: `arcane-redis`
- Spring app Docker 컨테이너: `arcane-app`

MySQL은 보통 호스트 포트 `3307`로 연결한다.

```bash
docker ps -a --filter "name=arcane"
```

컨테이너 내부에서 이미 `bash-5.1#` 같은 프롬프트에 들어가 있으면 `docker` 명령은 사용할 수 없다. 그때는 컨테이너 안이므로 바로:

```bash
mysql -uroot -p
```

호스트 터미널에서 MySQL 컨테이너에 접속할 때는:

```bash
docker exec -it arcane-db mysql -uroot -p
```

주의:

- `mysql -U root -p`가 아니라 MySQL CLI는 `-u` 소문자를 쓴다.
- `-U`는 PostgreSQL 스타일이라 MySQL에서는 맞지 않는다.

## 4. 초기에 있었던 프론트엔드 문제와 처리 흐름

### 4.1 메인 페이지 검색 버튼이 동작하지 않던 문제

처음에는 메인 페이지에서 소환사명을 입력하고 돋보기를 눌러도 아무 반응이 없었다.

검토 방향:

- 검색 버튼의 `onClick` 또는 `form onSubmit`이 실제 라우팅을 호출하는지 확인해야 했다.
- 입력값이 `gameName#tagLine` 형태일 때 `/summoner/{gameName}-{tagLine}` 형태로 이동해야 했다.
- 네비게이션 바 검색도 동일한 검색 로직을 써야 했다.

프론트에서는 검색 성공 후 검색창을 비우도록 수정했다.

주의:

- 메인 검색과 네비게이션 검색 모두 `refresh=false`가 기본이어야 한다.
- 전적 갱신 버튼을 눌렀을 때만 `refresh=true`를 붙여야 한다.

### 4.2 화면 디자인 문제

전적 페이지 `/summoner/Hide%20on%20bush-kr1`의 UX는 유지하되 전체 디자인을 여러 차례 바꿨다.

진행된 디자인 흐름:

1. 기존 어두운 남색 계열로 변경
2. 프로젝트 전체를 남색 계열로 통일
3. 이후 사용자가 밝고 귀여운 파스텔 핑크 계열을 요구
4. Toss 스타일처럼 패널 대비가 있는 핑크/화이트 디자인으로 변경
5. 경계선이 너무 선명해서 완성도가 떨어진다는 피드백 이후 경계선을 부드럽게 하고 그림자/배경 대비를 조정

현재 전체적인 방향:

- 밝은 파스텔 핑크 기반
- 흰색 패널
- 핑크 포인트 버튼
- 귀여운 이미지 활용
- 각 페이지의 제목/패널/버튼 스타일을 통일

주의:

- 사용자는 "토스 느낌"을 자주 말하지만 실제로는 너무 밋밋한 Toss보다 Arcane의 귀여운 핑크 톤과 섞인 느낌을 선호했다.
- 경계선이 과하면 싫어한다.
- 패널 간 대비는 있어야 하지만, 선이 너무 직접적으로 보이면 안 된다.
- UI가 깨지는 것, 공백이 과한 것, 긴 이름이 `...`으로 잘리는 것을 싫어한다.

### 4.3 소환사 페이지 프로필/랭크/포지션/요약 패널

전적 검색 페이지에서 상단 패널 구성 요구:

- 좌측: 프로필 아이콘, gameName, tagLine, 전적 갱신 버튼
- 중앙: 랭크 티어 패널
  - 솔로랭크/자유랭크 토글
  - 기본값은 솔로랭크
  - 티어 아이콘은 기존 티어 아이콘을 그대로 사용
  - 티어가 없을 때는 큰 이미지와 "아직 티어가 없어요" 같은 문구
- 우측: 포지션 분포
  - 원형 차트에서 막대 그래프로 변경
  - 탑/정글/미드/원딜/서폿 아이콘을 사용
  - 기타는 빼기
  - 퍼센트는 소수점 첫째 자리까지만 표시
  - 나중에는 게임 수를 빼고 퍼센트만 표시하도록 변경

요약 영역:

- "전체 게임 요약"과 "최근 많이 플레이한 챔피언"은 2:1 비율로 배치
- 포지션 분포는 상단으로 올렸기 때문에 요약 영역에서는 제거
- 전체 게임 요약의 파이차트는 크게 하고 `승 11 패 9`가 줄바꿈되지 않게 처리
- 파이차트 색상은 승/패 대비가 확실해야 함

전적 카드:

- 승리/패배 색상을 다르게 해서 한눈에 구분
- 요약된 게임 정보에는 tagLine까지 보여줄 필요 없음
- 상세보기 버튼을 누르면 그때 tagLine을 보여줌
- AI Score 옆의 등수는 실제 게임 내 참가자 기준 등수와 맞춰야 함

긴 이름 처리:

- gameName이 길면 `...` 처리하지 말고 두 줄로 표시
- tagLine은 항상 gameName 아래에 배치

복사 기능:

- 전적 검색 결과에서 gameName/tagLine 옆에 작은 복사 아이콘을 두고 누르면 `gameName#tagLine` 복사

### 4.4 이미지가 안 보이던 문제

챔피언 숙련도, 티어 이미지, 챔피언 이미지가 간헐적으로 안 보이는 문제가 있었다.

원인 후보:

- Data Dragon 버전 불일치
- champion id -> champion key/name 매핑 실패
- Next Image remote config 문제
- 이미지 URL에 잘못된 champion name이 들어감
- 한글 champion name을 그대로 이미지 파일명으로 쓰는 문제

아이템 이미지는 Data Dragon 버전을 `16.11.1`로 맞추는 작업이 있었다.

주의:

- Riot Data Dragon의 champion image 파일명은 한글명이 아니라 영문 champion id 기반이다.
- 예: 리신 -> `LeeSin.png`, 베인 -> `Vayne.png`
- championId 숫자만 있다면 champion metadata에서 key/id 매핑을 거쳐야 한다.
- 티어 이미지도 tier 문자열의 대소문자/파일명 매핑을 맞춰야 한다.

## 5. 전적 검색과 백엔드 Riot API 호출 최적화

### 5.1 기존 문제

전적 검색 시 프론트에서 `/profile`, `/tier`, `/matches`, `/mastery` 같은 API가 동시에 호출됐다.

초기에는 DB에 데이터가 있어도 Riot API를 자주 호출했다. 이로 인해:

- Riot API 429 Too Many Requests 발생
- match_info 중복 insert
- summoner 중복 insert
- DB deadlock
- lock wait timeout
- Hibernate SQL Error 1062 로그가 반복 발생

대표 로그:

```text
Duplicate entry 'KR_8237070180' for key 'match_info...'
Deadlock found when trying to get lock; try restarting transaction
Lock wait timeout exceeded; try restarting transaction
```

### 5.2 중복 저장 문제 원인

`match_info.match_id`, `summoner.puuid`는 unique다.

동시에 여러 요청이 들어오면 다음 순서가 발생했다.

```text
요청 A: DB에 match_id 없음 확인
요청 B: DB에 match_id 없음 확인
요청 A: insert 성공
요청 B: insert 시도 -> Duplicate entry
```

이전에는 DuplicateKeyException을 catch하려 했지만, Hibernate/MySQL이 이미 SQL Error 1062 로그를 찍은 뒤라 로그상 에러가 계속 남았다.

### 5.3 해결 방향

중복이 날 수 있는 insert는 DB 레벨에서 조용히 무시하도록 `INSERT IGNORE`를 사용했다.

예시:

```java
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query(value = """
        INSERT IGNORE INTO match_info (...)
        VALUES (...)
        """, nativeQuery = true)
int insertIgnore(@Param("matchEntity") Match match);
```

의미:

- 같은 `match_id`가 이미 있으면 insert를 하지 않는다.
- MySQL unique 충돌을 애플리케이션 예외로 터뜨리지 않는다.
- 반환값 `int`는 실제 insert된 row 수다.
  - `1`: 새로 insert됨
  - `0`: 이미 있어서 무시됨

중요:

- `INSERT IGNORE` 후에는 반드시 DB에서 다시 조회해야 한다.
- insert가 무시되면 현재 엔티티 객체는 DB 영속 상태의 기존 row가 아니기 때문이다.
- 그래서 `MatchWriteService`는 먼저 insert-ignore 하고, 이후 DB에서 다시 `Match`를 조회해서 participant를 붙이는 흐름으로 바꿨다.

관련 파일/역할:

- `SummonerRepository.java`: summoner 저장을 `INSERT IGNORE`로 변경
- `MatchRepository.java`: match_info 저장을 `INSERT IGNORE`로 변경
- `MatchWriteService.java`: match_info insert-ignore 후 다시 조회
- `SummonerWriteService.java`: summoner insert-ignore 후 다시 조회
- `RiotApiService.java`: `gameName#tagLine` 기준 60초 캐시 추가
- `SummonerService.java`: 이미 구한 puuid를 재사용해서 내부 중복 호출 제거

### 5.4 RiotAccountDto 로그가 두 번 찍히던 문제

원인:

- `/tier`, `/matches`, `/profile` 요청이 거의 동시에 들어왔다.
- 각 요청에서 `getSummonerInfo()` 또는 `findPuuid()`를 호출했다.
- `getSummonerMatches()` 내부에서도 PUUID 조회를 한 번 더 했다.

해결:

- `gameName#tagLine` 기준 짧은 캐시를 추가했다.
- `getSummonerMatches()` 내부에서 이미 구한 puuid를 재사용하도록 했다.

### 5.5 DB 우선 조회 흐름

새 방향:

- 일반 검색은 `refresh=false`
- DB에 summoner/profile/tier/match 정보가 있으면 DB에서 반환
- DB에 없을 때만 Riot API 호출
- 전적 갱신 버튼을 누르면 `refresh=true`
- `refresh=true`일 때는 강제로 Riot API를 호출하고 DB를 갱신

사용자가 최종적으로 이해한 API 구상:

- `/profile?refresh=true`
- `/matches?refresh=true`

이유:

- profile 응답에 티어 정보도 포함될 수 있기 때문에 `/tier`를 별도로 true 호출할 필요가 줄었다.
- 전적 갱신은 profile과 matches를 다시 요청하면 된다.

주의:

- 기본 검색에서는 refresh 파라미터가 빠지거나 false여야 한다.
- 전적 갱신 버튼을 누를 때만 true를 붙인다.
- 전적 갱신 중에는 전체 페이지 로딩이 아니라 "전적 갱신" 버튼만 돌아가는 게 좋다.

### 5.6 updateAt 사용

Summoner 테이블에 별도 lastRefreshAt을 만들지 않고 기존 `updateAt`을 전적 갱신 시간으로 쓰기로 했다.

요구:

- `/tier` API에서 `updateAt`을 보내준다.
- 프론트는 이를 바탕으로 "최근 갱신 1분 전", "최근 갱신 2시간 전" 같은 버튼/배지를 표시한다.
- 전적 갱신은 15분 이상 지난 경우에만 가능하게 한다.

추가 요구:

- 전적 갱신을 눌렀을 때 새 전적이 없어도 `updateAt`은 마지막에 갱신해야 한다.
- 그래야 "최근 갱신 1분 전"으로 바뀐다.

## 6. OAuth 로그인

### 6.1 OAuth 제공자

구글과 네이버 OAuth 로그인을 구현했다.

Google Console 설정:

- 승인된 JavaScript 원본:
  - `http://localhost:3000`
- 승인된 리디렉션 URI:
  - `http://localhost:8080/login/oauth2/code/google`

Naver Developer 설정:

- 서비스 URL:
  - `http://localhost:3000`
- Callback URL:
  - `http://localhost:8080/login/oauth2/code/naver`

주의:

- 백엔드는 로컬 `8080`으로 띄운다.
- OAuth redirect URI는 백엔드 Spring Security가 받는 URL이어야 한다.
- 프론트 `3000`이 아니다.

### 6.2 application.yml

사용자가 넣은 설정:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - email
              - profile
          naver:
            client-id: ${NAVER_CLIENT_ID}
            client-secret: ${NAVER_CLIENT_SECRET}
            scope:
              - email
              - profile
```

네이버는 provider 설정이 별도로 필요할 수 있다.

```yaml
provider:
  naver:
    authorization-uri: https://nid.naver.com/oauth2.0/authorize
    token-uri: https://nid.naver.com/oauth2.0/token
    user-info-uri: https://openapi.naver.com/v1/nid/me
    user-name-attribute: response
```

### 6.3 login_pw null 문제

OAuth 로그인 성공 후 DB insert에서 다음 에러가 발생했다.

```text
Column 'login_pw' cannot be null
```

원인:

- 기존 `users` 테이블이 일반 로그인용 `login_pw NOT NULL` 구조였다.
- OAuth 유저는 비밀번호가 없어서 null로 저장하려 했다.

해결 방향:

- OAuth 기반 로그인에서는 `login_pw`를 nullable로 바꾸거나,
- 더 좋은 방식은 User 엔티티를 OAuth 중심으로 재정의하고 password는 일반 로그인용 옵션 필드로 둔다.

주의:

- 사용자는 "User 테이블 아직 한번도 안 쓰고 했으니까 새롭게 필요한 것만 새로 정의하자"라고 했다.
- 따라서 불필요한 일반 로그인 필드는 제거하거나 nullable 처리하는 게 맞다.

### 6.4 소셜 계정 통합

요구:

- 구글 또는 네이버 중 하나로 최초 가입
- 나중에 다른 제공자로 로그인해도 같은 Arcane 계정으로 연결되게 하고 싶다.
- 이메일이 같으면 같은 사용자로 묶는 방식이 가능하다.
- 내 정보 페이지에서 다른 소셜 로그인을 추가 등록하면 연동되도록 하는 방향을 논의했다.

주의:

- 이메일만으로 자동 통합하면 보안/정책상 위험할 수 있다.
- 더 안전한 방식은 로그인된 상태에서 "소셜 계정 연결" 버튼을 눌러 provider를 추가 등록하는 방식이다.
- 사용자는 결국 내 정보 페이지에서 추가 소셜 인증을 하도록 하는 흐름에 관심이 있다.

### 6.5 최초 가입 정보 입력

요구:

- 최초 로그인 성공 시 바로 서비스 사용자가 되는 게 아니라 간단한 가입 정보 입력 화면으로 이동
- 공략/채팅에서 사용할 닉네임을 설정
- 닉네임은 중복 불가
- 추천 닉네임 3개 제공
- 직접 입력도 가능

닉네임 추천 방식:

- 형용사 100개
- 동물 100개
- 숫자 000~999
- 예: `귀여운다람쥐092`

주의:

- 실제 동물 단어가 프로젝트 스타일에 부적절할 수 있다는 시스템 지시가 있지만, 여기서는 사용자가 명시적으로 예시를 들었고 기능 도메인상 닉네임 생성이므로 사용할 수 있다.

### 6.6 내 정보 페이지

요구:

- 네비게이션 바에 로그인한 사용자의 닉네임 표시
- 닉네임 클릭 시 `/me` 같은 내 정보 페이지로 이동
- 내 정보 페이지에서:
  - 사용자 정보 확인
  - 닉네임 변경
  - 다른 소셜 로그인 연결
  - 로그아웃
- `/me`에서 로그아웃하면 홈으로 리다이렉트

## 7. 공략 게시판

### 7.1 요구사항

공략 기능 요구:

- 로그인한 유저만 공략 글 작성 가능
- 로그인한 유저만 댓글 작성 가능
- 공략 글은 markdown 형식
- 챔피언을 검색/선택해서 해당 챔피언에 대한 공략 작성
- 공략 글에는 이미지가 들어갈 수 있음
- 처음에는 S3 저장 이야기가 있었지만 최종적으로 DB 저장이 맞겠다고 변경
- 댓글 기능
- 공략 작성자에게 채팅 보내기
- 공략 목록/상세/작성 UI는 현재 프론트 디자인과 일체감 있게 구성

### 7.2 content 컬럼 길이 문제

공략 작성 시 다음 에러 발생:

```text
Data truncation: Data too long for column 'content' at row 1
```

원인:

- `guide.content` 컬럼 길이가 짧은 VARCHAR였거나 기본 length 제한이 걸려 있었다.
- Markdown 본문은 길 수 있으므로 VARCHAR로는 부족하다.

해결 방향:

엔티티에서:

```java
@Lob
@Column(columnDefinition = "LONGTEXT")
private String content;
```

DB에 이미 테이블이 있다면:

```sql
ALTER TABLE guide MODIFY content LONGTEXT NOT NULL;
```

주의:

- JPA ddl-auto가 update여도 컬럼 타입 변경은 기대대로 안 될 수 있다.
- 직접 ALTER를 적용해야 할 수 있다.

### 7.3 공략 목록에서 Failed to fetch

공략이 없어서 뜬 게 아니라 백엔드 API 연결 실패 또는 서버 다운/에러일 가능성이 높았다.

프론트는 공략이 없는 상태와 서버 연결 실패를 분리해야 한다.

- 공략이 없음: "아직 등록된 공략이 없습니다"
- 서버 실패: "백엔드 서버에 연결할 수 없습니다"

### 7.4 공략 댓글 위치

요구:

- 댓글은 공략글 아래로 배치
- 오른쪽 사이드 패널이 아니라 본문 하단이 더 자연스럽다.

## 8. 채팅 시스템

### 8.1 초기 채팅 요구

공략 작성자와 공략을 읽는 사용자가 채팅할 수 있어야 한다.

흐름:

- 공략 상세에서 "메시지 보내기" 클릭
- 공략 작성자와 현재 사용자 사이의 채팅방 생성/조회
- 채팅 목록 토글은 로그인 시 네비게이션 바에 표시
- 웹소켓 기반 실시간 채팅

### 8.2 현재 구현 방향

초기에는 REST/폴링 성격이 있었고, 이후 STOMP WebSocket 중심으로 개선했다.

백엔드 쪽 핵심 개념:

- `WebSocketConfig`
- STOMP endpoint
- `@MessageMapping`
- `@DestinationVariable`
- `Principal`
- `SimpMessagingTemplate`
- 채팅방 구독 `/sub/chat/rooms/{roomId}`
- 메시지 전송 `/pub/chat/rooms/{roomId}/messages`
- 읽음 처리 `/pub/chat/rooms/{roomId}/read`

프론트는 STOMP 클라이언트를 사용하도록 수정했다.

주의:

- 현재 Redis Pub/Sub을 쓰는 구조는 아니다.
- STOMP 브로커는 Spring 내부 simple broker 기반으로 이해하면 된다.
- Redis Pub/Sub이나 Kafka는 나중에 다중 인스턴스/이벤트 수집이 필요할 때 붙이는 후보로 논의했다.

### 8.3 WebSocketConfig 역할

일반적으로 정리하면:

- `registerStompEndpoints`
  - 브라우저가 WebSocket 연결을 시작할 endpoint 등록
  - 예: `/ws`
  - SockJS 사용 여부 설정 가능
- `configureMessageBroker`
  - 클라이언트가 구독할 prefix와 서버가 받을 prefix 설정
  - `/sub`: 클라이언트 구독용
  - `/pub`: 클라이언트 메시지 발행용
- `configureClientInboundChannel`
  - 클라이언트에서 들어오는 STOMP CONNECT/SEND/SUBSCRIBE 프레임을 가로채 검증
  - JWT 검증 또는 암구호 claims 검증 위치

### 8.4 Interceptor 암구호 요구

사용자가 추가 요구:

- Interceptor에서 검증 단계를 하나 더 둔다.
- JWT claims에서 `dont`라는 이름으로 `again`을 추출한다.
- 이 값이 없으면 에러를 띄운다.

의미:

- JWT가 서명만 맞는지 보는 것 외에, 우리가 발급한 특정 구조의 토큰인지 추가 검증하는 장치다.
- 실제 보안적으로 큰 의미가 있다기보다는 사용자가 학습 목적으로 요청한 검증 단계다.

주의:

- JWT를 발급할 때도 반드시 `dont: "again"` claim을 넣어야 한다.
- Interceptor에서만 검사하면 기존 토큰은 모두 실패한다.

### 8.5 Principal 역할

STOMP 컨트롤러에서 `Principal`을 쓰면 현재 WebSocket 연결의 인증 사용자를 알 수 있다.

예:

```java
@MessageMapping("/chat/rooms/{roomId}/read")
public void readRoom(@DestinationVariable Long roomId, Principal principal) {
    GuideChatDto.RoomResponse room =
            guideChatService.readRoom(roomId, principal.getName());

    messagingTemplate.convertAndSend("/sub/chat/rooms/" + roomId, room);
}
```

여기서:

- `roomId`: 클라이언트가 읽음 처리할 방
- `principal.getName()`: 현재 로그인한 사용자 식별자
- `readRoom`: 해당 사용자의 읽음 상태를 DB에 저장
- `convertAndSend`: 구독 중인 양쪽 사용자에게 방 상태가 바뀌었다고 방송

마지막 코드의 역할:

```java
messagingTemplate.convertAndSend("/sub/chat/rooms/" + roomId, room);
```

이는 `/sub/chat/rooms/{roomId}`를 구독 중인 클라이언트들에게 최신 room 상태를 보내는 코드다.
읽음 상태가 바뀌었으므로, 상대방 화면에서도 읽음 표시가 갱신되도록 한다.

### 8.6 @DestinationVariable을 쓰는 이유

STOMP 메시지 경로:

```java
@MessageMapping("/chat/rooms/{roomId}/messages")
```

여기서 `{roomId}`를 받으려면 `@DestinationVariable`을 쓴다.

REST Controller의 URL path variable에는 `@PathVariable`을 쓴다.

차이:

- `@PathVariable`: HTTP REST 요청 URL에서 추출
- `@DestinationVariable`: STOMP destination 경로에서 추출

따라서 STOMP 컨트롤러에서는 `@DestinationVariable`이 맞다.

### 8.7 채팅 관련 문제들

문제 1: 메시지 마지막 글자가 중복 전송됨

가능 원인:

- form submit과 button onClick이 동시에 실행
- Enter key handler와 submit handler가 둘 다 실행
- WebSocket optimistic append와 서버 echo append가 중복 처리
- input 상태 초기화 전에 같은 payload가 두 번 send

해결 방향:

- 전송 이벤트는 하나만 둔다.
- `onSubmit`에서만 전송하고 button은 `type="submit"`만 사용
- `onKeyDown`에서 직접 send하지 않는다.
- optimistic update를 쓴다면 서버 echo에서 같은 임시 id를 dedupe한다.

문제 2: 같은 사람과 대화하는데 방이 중복 생성됨

DB 확인 결과:

```sql
SHOW INDEX FROM guide_chat_room;
```

`uk_guide_chat_room_guide_reader` unique index가 이미 있었다.

```text
UNIQUE (guide_id, reader_id)
```

중복 row는 없었다.

```sql
SELECT guide_id, reader_id, COUNT(*) AS cnt
FROM guide_chat_room
GROUP BY guide_id, reader_id
HAVING COUNT(*) > 1;
```

결과는 Empty set.

따라서 실제 DB 중복이 아니라 프론트 상태에서 같은 방이 두 번 보이는 문제일 가능성이 높다.

가능 원인:

- WebSocket room event를 받을 때 `setRooms([nextRoom, ...currentRooms])`만 하고 id dedupe가 불안정
- REST로 불러온 방 목록 + socket으로 받은 방 목록을 병합하면서 중복
- room id가 문자열/숫자 타입이 섞여 필터가 실패

문제 3: 읽음 표시

사용자 요구:

- 카카오톡처럼 읽음 여부만 표시
- 언제 읽었는지는 필요 없음

따라서 메시지마다:

- 내가 보낸 메시지이고
- 상대가 읽지 않았으면 `1` 같은 unread 표시
- 상대가 읽으면 표시 제거

또는 단순히 `읽음`/미표시 방식도 가능하다.

### 8.8 Maximum update depth exceeded 문제

프론트 `components/chat/ChatDock.tsx`에서 발생:

```text
Maximum update depth exceeded
components/chat/ChatDock.tsx (119:7) @ ChatDock.useCallback[applySocketRoom]
```

원인:

- `useEffect` dependency에 매 렌더마다 바뀌는 함수/객체가 들어감
- `applySocketRoom`이 state를 바꾸고, state 변경이 다시 effect를 실행하는 순환

해결 방향:

- `applySocketRoom`은 stable dependency만 사용
- socket subscribe effect는 필요한 값만 dependency로 둔다.
- room update는 id 기준 dedupe 후 내용이 같으면 기존 state 반환

예시 방향:

```ts
setRooms((currentRooms) => {
  const exists = currentRooms.some((room) => room.id === nextRoom.id);
  const nextRooms = [
    nextRoom,
    ...currentRooms.filter((room) => room.id !== nextRoom.id),
  ];

  if (exists && JSON.stringify(currentRooms) === JSON.stringify(nextRooms)) {
    return currentRooms;
  }

  return nextRooms;
});
```

실제 코드는 JSON.stringify보다 더 가벼운 비교가 낫다.

### 8.9 알림 배지 요구

기존 문제:

- 채팅 서버가 열리자마자 채팅 알림이 오는 것처럼 보임
- 실제 새 메시지가 없는데 방이 열린 숫자가 배지로 표시됨

요구:

- 채팅 버튼 배지는 열린 채팅방 수가 아니라 읽지 않은 메시지 수만 표시
- 내가 보낸 메시지는 unread count에 포함하지 않음
- 실제 상대가 메시지를 보낸 순간 알림 수 증가

### 8.10 차단/삭제 요구

추가 요구:

- 채팅방에 차단하기 버튼 추가
- 한 명이 차단하면 양쪽 모두 더 이상 메시지를 보낼 수 없음
- 차단 확인 알림 필요
- 차단된 상대와 새 채팅방도 만들 수 없게 해야 함
- 대화 목록에서 방을 지우는 기능 추가

주의:

- "대화 목록 삭제"와 "차단"은 다르다.
- 삭제는 내 화면에서만 숨기는 soft delete가 자연스럽다.
- 차단은 양쪽 메시지 전송을 막는 상태여야 한다.
- DB에는 `blocked_by_id`, `blocked_at`, `deleted_by_author`, `deleted_by_reader` 같은 구조가 필요할 수 있다.

### 8.11 채팅 입력창 깨짐

문제:

- 채팅 메시지가 많아지면 아래 입력창이 화면 밖으로 밀리거나 깨짐
- 스크롤 영역과 입력 영역의 height 계산이 잘못됨

해결 방향:

- 채팅 dock 전체 높이 고정
- header 높이 고정
- message list는 `flex: 1; overflow-y: auto`
- input bar는 `flex-shrink: 0`
- message list에 padding-bottom 확보

## 9. JWT 만료 처리

요구:

- JWT 토큰이 만료되면 백엔드가 명확한 에러 메시지를 response로 반환
- 프론트는 해당 응답을 받으면 로그아웃 처리
- 이후 홈 또는 로그인 페이지로 이동

주의:

- WebSocket 연결에서도 토큰 만료 처리가 필요하다.
- HTTP fetch interceptor와 STOMP error handler 모두 처리해야 한다.

## 10. 패치노트 페이지

### 10.1 목표

League of Legends 한국어 공식 패치노트 사이트:

```text
https://www.leagueoflegends.com/ko-kr/news/tags/patch-notes/
```

2026년부터 패치노트를 크롤링해서:

- 전체 패치노트 보기
- 챔피언별 보기
- 챔피언을 검색하면 해당 챔피언이 언급된 패치노트 내용 보여주기
- 패치노트별 공식 링크 제공

초기에는 "스택처럼 카드로 보여주기"도 이야기했지만, 최종적으로는 내용을 보여주는 정도로 진행하기로 했다.

### 10.2 UI 요구

- 패치노트 nav 항목이 링크처럼 따로 보이지 말고 다른 nav 항목과 동일한 스타일이어야 한다.
- 챔피언 패치노트 페이지 하단의 챔피언 버튼 목록은 제거
- 대신:
  - 전체 패치노트 보기
  - 챔피언 별로 보기
- 챔피언 검색 기능에는 챔피언 이미지도 함께 넣으면 UX가 좋아진다.
- 공략 페이지와 패치노트 페이지 제목을 더 예쁘게 만들기

### 10.3 백엔드 크롤링 실패

백엔드에서 다음 에러가 반복됐다.

```text
official patchnote crawling failed
Connection reset
GET request for "https://www.leagueoflegends.com/ko-kr/news/tags/patch-notes/"
```

원인 후보:

- Riot/LoL 사이트가 Java RestTemplate 요청을 차단하거나 연결을 끊음
- User-Agent/headers 부족
- TLS/HTTP client 호환 문제
- 너무 자주 호출
- 서버 측 네트워크 문제

해결 방향:

- RestTemplate 대신 Jsoup/OkHttp 등으로 User-Agent를 명확히 넣기
- 서버 시작 시나 요청 때마다 실시간 크롤링하지 말고 캐시/DB 저장
- 실패 시 프론트에 "공식 패치노트 연결 실패" 상태를 명확히 보여주기
- 가능하면 패치노트 목록을 수동/배치로 저장하고 프론트는 DB만 조회

주의:

- 패치노트 페이지 클릭 시 공략으로 이동하는 라우팅 오류가 있었다.
- nav 링크 href를 반드시 확인해야 한다.

## 11. 챔피언 분석 페이지

요구:

- 챔피언 분석 페이지에도 챔피언 이미지를 넣기
- 공백을 줄이기
- 다른 페이지들과 UX/UI 톤 통일
- 챔피언 티어 리스트에서 탑 패널 왼쪽에 "전체"를 넣기
- 칼럼을 `랭킹`, `전체`, `탑`, `정글`, `미드`, `원딜`, `서폿` 느낌으로 몰아보기
- 챔피언 이미지 옆에 티어를 아이콘으로 표시
- 전체 패널에서는 티어 아이콘을 챔피언 아이콘 왼쪽에 둔다.
- 승률과 점수는 없애도 된다고 했다.
- OP와 1티어 같은 글자 수 차이 때문에 이미지 정렬이 안 맞던 문제를 고정 너비/아이콘 기반으로 해결해야 한다.

주의:

- 티어 텍스트 길이에 따라 레이아웃이 흔들리면 안 된다.
- OP, 1티어, 2티어 등은 같은 너비 박스 또는 아이콘으로 처리한다.

## 12. 즐겨찾기와 최근 검색

전적 검색 기능에 프론트 로컬 캐시 기능 추가 요구:

- 유저 즐겨찾기
  - 브라우저 localStorage 또는 웹 캐시로 처리
  - 서버 DB까지는 필요 없음
- 최근 전적 검색 내역
  - 프론트에서 처리
  - 검색 저장 기능 ON/OFF 제공

주의:

- 검색 저장 OFF면 새 검색 내역을 저장하지 않는다.
- 기존 저장 내역을 지울지 유지할지는 UI에서 명확히 해야 한다.

## 13. AI 서버와 학습 데이터

### 13.1 FastAPI 서버

초기 AI 서버 요구:

- `/Users/haechan/Desktop/Arcane/ai/Arcane_AI`에서 진행
- FastAPI로 서버 생성
- 가상환경/requirements 구성
- 백엔드가 `localhost:8864/random`을 호출할 수 있어야 함

현재 `ai/Arcane_AI`에는 FastAPI와 ML 코드가 있다.

### 13.2 생성/수정된 AI 파일

`/Users/haechan/Desktop/Arcane/ai/Arcane_AI/requirements.txt`

주요 패키지:

```text
fastapi==0.136.3
uvicorn[standard]==0.48.0
pymysql==1.1.2
python-dotenv==1.2.1
playwright==1.56.0
requests>=2.32
numpy>=1.26
pandas>=2.2
scikit-learn>=1.5
joblib>=1.4
```

`.env.example`:

```text
ARCANE_DB_HOST=127.0.0.1
ARCANE_DB_PORT=3307
ARCANE_DB_NAME=arcane_db
ARCANE_DB_USER=root
ARCANE_DB_PASSWORD=arcane1234
DEEPLOL_CRAWL_DELAY_SECONDS=2.0
```

`DATASET_CRAWLER.md`:

- 데이터 수집/학습 명령어와 흐름 정리

`arcane_dataset` 패키지:

- `config.py`
- `db.py`
- `deeplol.py`
- `matcher.py`
- `collect.py`

`arcane_model` 패키지:

- `features.py`
- `train.py`
- `predict.py`

`main.py`:

- FastAPI 서버
- `/health`
- `/random`
- `/predict`

### 13.3 DeepLOL 기반 데이터 수집

사용자의 목표:

- DeepLOL 같은 사이트는 참가자별 점수를 보여준다.
- Arcane도 여러 지표를 기반으로 점수를 산정하고 싶다.
- 다중회귀모델을 학습하기 위해:
  - 입력: Riot match participant 지표
  - 정답 label: DeepLOL 점수 같은 외부 사이트 점수
- 한 게임 10명 = 10개 데이터
- 20게임 = 200개 데이터
- 4만 데이터는 약 4,000게임 필요

구현된 크롤링 흐름:

1. Arcane DB에 저장된 match_participant 데이터를 가져온다.
2. match_id 기준으로 DeepLOL에서 해당 경기 상세를 조회한다.
3. DeepLOL participant의 `ai_score`, `ai_score_rank`를 가져온다.
4. DB participant와 DeepLOL participant를 champion/kda/name 등으로 매칭한다.
5. CSV에 저장한다.

DeepLOL 내부 API로 확인된 형태:

```text
https://b2c-api-cdn.deeplol.gg/summoner/summoner?...
https://b2c-api-cdn.deeplol.gg/match/matches?...
https://b2c-api-cdn.deeplol.gg/match/match-cached?match_id=...&platform_id=KR
```

주의:

- 이 API는 공식 API가 아니다.
- 대량 크롤링 및 학습 데이터 사용은 서비스 약관/저작권/윤리 문제가 있을 수 있다.
- 포트폴리오나 공개 서비스에는 "타 사이트 점수를 무단 수집해 학습"이라고 쓰면 위험하다.
- 더 안전한 방향은 Riot 공식 API로 원천 지표를 수집하고, Arcane 자체 점수 공식을 만들거나 허용된 label을 쓰는 것이다.

### 13.4 현재 수집 결과

CSV:

```text
/Users/haechan/Desktop/Arcane/ai/Arcane_AI/data/deeplol_training.csv
```

최종 상태:

- `wc -l`: 15183
- 실제 데이터 row: 15182
- 학습에 사용된 clean rows: 15120

DB 자체 후보:

- DB participant rows: 16380

따라서 현재 DB만으로는 4만 데이터를 만들 수 없다.

4만 데이터를 만들려면:

- Riot API로 더 많은 match를 DB에 저장해야 한다.
- 또는 seed summoner를 늘려야 한다.
- 4,000 unique matches가 필요하다.

### 13.5 모델 학습 결과

학습 명령:

```bash
cd /Users/haechan/Desktop/Arcane/ai/Arcane_AI
python -m arcane_model.train \
  --input data/deeplol_training.csv \
  --model-output models/deeplol_score_model.joblib \
  --metrics-output models/deeplol_score_model_metrics.json \
  --feature-importance-output models/deeplol_score_model_importance.csv \
  --min-rows 1000 \
  --n-estimators 300
```

최종 모델:

```text
/Users/haechan/Desktop/Arcane/ai/Arcane_AI/models/deeplol_score_model.joblib
```

크기:

- 약 213MB

이전 700-tree 모델은 약 494MB였고 너무 커서 300 trees로 줄였다.

최종 metrics:

```json
{
  "mae": 3.8229889911534953,
  "rmse": 5.238817394112949,
  "r2": 0.8825837006195366,
  "rows_total": 15120,
  "rows_train": 12114,
  "rows_test": 3006,
  "matches_train": 1211,
  "matches_test": 303,
  "model": "extra_trees",
  "n_estimators": 300
}
```

상위 feature importance:

```text
win                              약 0.3309
kda                              약 0.2117
kills                            약 0.1828
deaths                           약 0.1009
double_kills                     약 0.0219
total_damage_dealt_to_champions  약 0.0192
```

검증:

- `python -m py_compile` 통과
- 예측 로드 테스트에서 약 `64.89` 출력 확인

### 13.6 4만 데이터 확보 방향

사용자는 "웹 크롤링으로 다른 사람들 전적 그냥 막 가져와서 쓰면 안 되나?"라고 물었다.

정리:

- Riot 공식 API로 match data를 수집하는 것은 rate limit과 정책을 지키면 가능하다.
- 타 사이트 점수/라벨을 무단 대량 수집해 학습하는 것은 위험하다.
- DeepLOL 점수는 타 서비스의 독자적 산정 결과일 가능성이 높다.
- 따라서 안전한 포트폴리오 방향은:
  1. Riot API로 원천 데이터 수집
  2. Arcane 자체 점수 공식을 설계
  3. 그 점수를 label로 모델 학습
  4. 또는 사람이 검수한 일부 label 사용

공식 Riot API 기반 4만 row 생성 전략:

1. seed summoner 확보
   - 기존 DB summoner
   - Challenger/Grandmaster/Master ranking API
2. PUUID 확보
3. Match-V5 match ids 조회
   - `/lol/match/v5/matches/by-puuid/{puuid}/ids`
   - `count=100`
   - ranked/classic queue 필터 사용 가능
4. match_id 중복 제거
5. match detail 조회
   - `/lol/match/v5/matches/{matchId}`
6. match_info + match_participant 저장
7. 4,000 unique matches 확보 시 40,000 participant rows
8. 429 대응
   - Retry-After 헤더 준수
   - backoff
   - 진행 상태 저장

## 14. 성능 개선 논의

### 14.1 전적 검색 성능 개선 포트폴리오 스토리

기존 방식:

- 전적 검색 시 profile/tier/matches/mastery가 동시에 Riot API를 호출
- match list와 match detail도 반복 호출
- DB에 이미 있는 데이터도 외부 API 호출

개선 방식:

- DB 우선 조회
- refresh=false 기본값
- 데이터가 없을 때만 Riot API
- refresh=true일 때만 강제 갱신
- match_id/puuid unique insert는 insert-ignore로 중복 예외 방지
- 짧은 캐시로 같은 RiotAccountDto 중복 호출 방지

포트폴리오 표현:

- 외부 API 호출 수 감소
- 429 발생 가능성 감소
- 중복 insert 예외 제거
- deadlock/lock wait timeout 감소
- 응답 속도 개선

수치 예시:

- 기존: 한 소환사 검색 시 profile/tier/matches/mastery 등 최소 3~4개 외부 API 흐름 발생 가능
- matches 20개를 모두 외부에서 새로 가져오면 match detail 20회 + account/profile/tier 등 추가 호출
- 개선 후 DB hit 시 외부 Riot API 호출 0회 가능
- refresh=true일 때만 필요한 호출 수행

주의:

- 실제 포트폴리오 수치는 측정 기반으로 써야 한다.
- 추정 수치와 측정 수치를 구분해야 한다.

### 14.2 채팅 성능 개선 스토리

현재 채팅:

- STOMP WebSocket 기반
- Redis Pub/Sub은 아직 아님
- Kafka도 아직 아님

성능 개선 방향:

1. REST polling 제거
2. WebSocket/STOMP로 실시간 메시지 전송
3. 메시지 목록/읽음 상태는 필요한 방만 구독
4. unread count만 별도 가볍게 관리
5. 추후 다중 인스턴스면 Redis Pub/Sub 또는 외부 message broker

Kafka 논의:

- 채팅 자체 실시간 전달에는 Redis Pub/Sub 또는 STOMP broker relay가 더 단순하다.
- Kafka는 실시간 1:1 채팅 전달보다는 이벤트 수집/로그/모니터링/랭킹 변화 분석에 더 적합하다.
- 사용자는 Kafka를 데이터 수집 자동화/모니터링 쪽에 붙이는 게 어떠냐고 했고, 그 방향이 더 적절하다고 정리했다.

부하 테스트 준비:

- 동시 접속자 수
- 초당 메시지 수
- 방 수
- 구독 수
- 메시지 왕복 지연
- DB insert latency
- CPU/memory
- WebSocket 연결 유지율

도구 후보:

- k6 WebSocket
- JMeter WebSocket plugin
- Gatling
- Spring Actuator + Micrometer
- Prometheus/Grafana

## 15. 오류 로그와 의미

### 15.1 HikariPool connection closed

로그:

```text
HikariPool-1 - Failed to validate connection ...
No operations allowed after connection closed.
Possibly consider using a shorter maxLifetime value.
```

의미:

- DB 커넥션이 MySQL 쪽에서 먼저 닫혔는데 Hikari pool이 아직 살아있다고 생각한 상황
- 서버 재시작, DB 컨테이너 재시작, wait_timeout, maxLifetime 차이 등으로 발생 가능

해결 방향:

- 백엔드 재시작
- DB 컨테이너 상태 확인
- Hikari `maxLifetime`을 MySQL wait_timeout보다 짧게 설정

예:

```yaml
spring:
  datasource:
    hikari:
      max-lifetime: 1800000
      idle-timeout: 600000
      connection-timeout: 30000
```

### 15.2 Tomcat session deserialize 에러

로그:

```text
Cannot deserialize session attribute [SPRING_SECURITY_CONTEXT]
Exception loading sessions from persistent storage
java.io.StreamCorruptedException: invalid type code: 00
```

의미:

- Spring Boot DevTools/Tomcat이 이전 세션 파일을 복원하려다 실패
- SecurityContext 직렬화 데이터가 깨졌거나 클래스 구조가 바뀜

대부분 치명적이지 않다.

해결 방향:

- 서버 재시작
- Tomcat work/session 파일 삭제
- 세션 저장 비활성화

application.yml에:

```yaml
server:
  servlet:
    session:
      persistent: false
```

### 15.3 Next.js ENOENT page.js

로그:

```text
ENOENT: no such file or directory, open '.next/server/app/summoner/[name]/page.js'
```

의미:

- Next dev build cache가 깨졌거나 라우트 파일 변경 후 `.next`가 꼬임

해결:

```bash
rm -rf .next
npm run dev
```

주의:

- destructive 명령이라 Codex가 실행할 때는 사용자 승인 필요.

## 16. 현재 특히 주의해야 할 미완/불안정 영역

1. DeepLOL 대량 크롤링
   - 기술적으로는 어느 정도 구현됐지만 법적/약관 리스크가 있다.
   - 무단 대량 수집은 피하는 방향을 권장한다.

2. 패치노트 크롤링
   - Riot 공식 사이트가 connection reset을 일으킨다.
   - 실시간 크롤링보다 캐시/DB 저장 방식이 낫다.

3. 채팅 중복/읽음/알림
   - DB 중복은 unique index로 막혀 있지만 프론트 상태 중복 가능성이 있다.
   - unread count는 방 수가 아니라 읽지 않은 메시지 수 기준이어야 한다.

4. OAuth 계정 통합
   - 이메일 자동 통합은 가능하지만 보안적으로 신중해야 한다.
   - 내 정보 페이지에서 명시적 소셜 계정 연결 방식이 더 낫다.

5. 전적 갱신
   - `refresh=false` 기본 검색과 `refresh=true` 갱신을 명확히 구분해야 한다.
   - 새 전적이 없어도 updateAt은 갱신해야 한다.

6. 이미지 로딩
   - championId -> DDragon champion id 매핑이 항상 필요하다.
   - 한글명으로 이미지 URL을 만들면 실패한다.

7. 프론트 디자인
   - 현재 톤은 파스텔 핑크 + 귀여운 UI.
   - 갑자기 남색/검정으로 되돌리면 사용자 의도와 다르다.

## 17. CLI에서 이어서 작업할 때 추천 첫 명령

CLI 새 세션에서 사용자가 이어서 작업하려면 이렇게 말하면 된다.

```text
/Users/haechan/Desktop/Arcane/CODEX_CONTEXT.md 읽고 지금까지 흐름 이어서 작업해.
```

작업 전 상태 확인:

```bash
cd /Users/haechan/Desktop/Arcane
git status --short
```

프론트 파일 찾기:

```bash
cd /Users/haechan/Desktop/Arcane/frontend/Arcane_Frontend
rg "ChatDock|Guide|Patch|summoner|refresh|mastery"
```

백엔드 파일 찾기:

```bash
cd /Users/haechan/Desktop/Arcane/backend/Arcane_Backend
rg "GuideChat|WebSocketConfig|MessageMapping|JwtAuthenticationFilter|OAuth|PatchNote|SummonerService"
```

AI 파일 찾기:

```bash
cd /Users/haechan/Desktop/Arcane/ai/Arcane_AI
rg "DeepLol|ArcaneDatabase|ExtraTrees|predict|collect"
```

## 18. 마지막 대화의 핵심

가장 최근 주제는 AI 점수 모델 학습용 데이터 수집이었다.

사용자는 4만 개 데이터를 원했다.
현재 DB 기반으로는 약 1.5만 row까지만 확보했고, DB 자체 후보도 약 1.6만 participant row라 4만은 불가능했다.

사용자는 "웹 크롤링을 통해 다른 사람들 전적 그냥 막 가져와서 쓰면 안 되나?"라고 물었다.

정리된 답:

- Riot 공식 API로 다른 사람들의 공개 match data를 수집하는 것은 rate limit과 정책을 지키면 가능하다.
- 하지만 DeepLOL 같은 타 사이트의 점수 label을 무단으로 대량 수집해서 학습에 쓰는 것은 위험하다.
- 안전한 방향은 Riot API로 match 지표를 수집하고 Arcane 자체 점수 공식을 만들거나 허용된 label을 사용해서 학습하는 것이다.

다음에 이어질 가능성이 큰 작업:

1. Riot API 기반으로 4,000 match / 40,000 participant row 수집 파이프라인 설계 또는 구현
2. Arcane 자체 점수 공식 설계
3. 기존 DeepLOL model 대신 자체 label 기반 모델 재학습
4. AI 서버 `/predict`를 백엔드 전적 분석에 연결

