# Arcane Project Progress Summary

작성일: 2026-05-29

## 1. 현재 프로젝트 전환 상황

기존 organization 레포에 있던 Nexushub 코드를 개인 레포 기준으로 분리해 Arcane 프로젝트로 정리하는 중이다.

현재 로컬 기준 주요 경로는 다음과 같다.

- 백엔드 후보 경로: `/Users/haechan/Desktop/Arcane/backend/Arcane_Backend`
- 프론트엔드 후보 경로: `/Users/haechan/Desktop/Arcane/frontend/Arcane_Frontend`
- 추가로 `/Users/haechan/Desktop/Arcane/Arcane_Backend` 폴더도 존재하므로, 최종적으로 어느 백엔드 폴더를 사용할지 정리 필요

## 2. 실행 방식 정리

개발 중에는 전체를 Docker로 띄우기보다 아래 방식이 더 적합하다고 판단했다.

- MySQL, Redis: Docker로 실행
- Spring Boot 백엔드: IntelliJ에서 로컬 실행
- Next.js 프론트엔드: 로컬에서 `npm run dev`
- 최종 시연/배포 시점: Docker Compose로 한 번에 묶는 방식 검토

권장 실행 흐름은 다음과 같다.

```bash
cd /Users/haechan/Desktop/Arcane/backend/Arcane_Backend
docker compose up -d arcane-db redis
```

그 다음 IntelliJ에서 백엔드 실행:

```txt
ArcaneApplication 실행
```

프론트엔드 실행:

```bash
cd /Users/haechan/Desktop/Arcane/frontend/Arcane_Frontend
npm install
npm run dev
```

## 3. Docker 실행 중 발견한 문제

백엔드를 Docker로 실행했을 때 Spring Boot가 `application.yml` 파싱 단계에서 실패했다.

원인은 `application.yml` 안에 YAML 문법에 맞지 않는 임의 문자열이 들어가 있었기 때문이다.

```txt
ㅁㄴㅇㅁ
```

이 줄은 제거해야 한다.

또한 로컬 백엔드 실행 기준으로는 설정값이 다음처럼 맞아야 한다.

- MySQL: `localhost:3307`
- Redis: `localhost:6379`
- Python 모델 서버가 있다면: `localhost:8864`

Docker 내부에서 백엔드를 실행할 때는 MySQL 주소가 `arcane-db:3306`이어야 한다.

## 4. 프론트엔드 404 문제

프론트에서 다음과 같은 요청이 발생했다.

```txt
GET /undefined/api/v1/summoner/contain/...
```

원인은 `NEXT_PUBLIC_API_URL` 환경변수가 없어서 API base URL이 `undefined`가 되었기 때문이다.

프론트에서는 다음 방식으로 기본값을 두는 것이 좋다.

```ts
export const API_URL =
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
```

또한 검색어, `gameName`, `tagLine`은 URL 인코딩을 적용해야 한다.

## 5. Git 정리 상황

GitHub에 다음과 같은 로컬/빌드 산출물이 올라간 상태를 확인했다.

- `.env`
- `.DS_Store`
- `.gradle/`
- `.idea/`
- `build/`

이 파일들은 Git에서 추적하면 안 된다.

현재 브랜치에서 제거하려면 다음 명령을 사용한다.

```bash
git rm --cached .env
git rm --cached .DS_Store
git rm -r --cached build
git rm -r --cached .gradle
git rm -r --cached .idea
git add .gitignore
git commit -m "Remove ignored local files from repository"
git push
```

주의할 점:

- `.gitignore`는 앞으로 새로 추적하지 않게 하는 규칙이다.
- 이미 Git에 올라간 파일은 `git rm --cached`로 추적을 제거해야 한다.
- `.env`에 있던 비밀값은 GitHub 히스토리에 남을 수 있으므로 재발급이 필요하다.

## 6. 보안값 정리

`.env`가 GitHub에 올라간 적이 있으므로 JWT Secret은 변경하기로 했다.

새 JWT Secret 생성:

```bash
openssl rand -base64 64
```

`.env`에는 새 값만 로컬에서 보관한다.

```env
JWT_SECRET=새로_생성한_값
```

백엔드 설정은 하드코딩 대신 환경변수를 바라보게 정리하는 것이 좋다.

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:3600000}
```

DB 비밀번호는 로컬 개발용이라 현재는 변경하지 않기로 했다.

## 7. 프로젝트 이름 변경 관련 정리

`settings.gradle`의 아래 값은 Gradle 프로젝트 이름만 바꾼다.

```gradle
rootProject.name = 'Arcane'
```

하지만 이것만으로 Java 패키지명이나 클래스명이 바뀌지는 않는다.

현재 남아 있는 이름들의 의미는 다음과 같다.

- `rootProject.name`: Gradle 프로젝트 이름
- `com.arcane.Arcane`: Java package 경로
- `ArcaneApplication`: Spring Boot 실행 클래스 이름
- IntelliJ 상단 `Arcane`: IDE 프로젝트 표시명 또는 캐시

패키지명까지 더 단순화하려면 IntelliJ Refactor 기능으로 다음을 처리해야 한다.

- 실행 클래스명은 `ArcaneApplication`으로 통일됨
- `com.arcane.Arcane` -> 원하는 패키지명, 예: `com.arcane`
- import/package 선언 자동 변경 확인

단순 폴더명 변경으로 처리하면 package/import가 깨질 수 있으므로 직접 파일 이동보다 IntelliJ Refactor 사용이 안전하다.

## 8. 프론트엔드 폴더 이동 및 ignore 파일 정리

기존 프론트엔드에서 `.gitignore` 때문에 Git에 올라가지 않은 주요 파일은 다음이었다.

- `node_modules/`
- `.next/`
- `next-env.d.ts`
- `.idea/workspace.xml`

이 파일들은 새 폴더로 복사할 필요가 없다.

- `node_modules/`: `npm install`로 재생성
- `.next/`: `npm run dev` 또는 `npm run build`로 재생성
- `next-env.d.ts`: Next.js가 자동 생성
- `.idea/workspace.xml`: 개인 IDE 상태 파일

현재 확인 기준으로 기존 프론트에는 `.env`, `.env.local`이 없었으므로 새 프론트 폴더로 수동 복사할 환경 파일은 없었다.

## 9. 이후 해야 할 일

1. 실제 사용할 백엔드 폴더를 하나로 확정한다.
2. `.env`, `build/`, `.gradle/`, `.idea/`, `.DS_Store`를 Git 추적에서 제거한다.
3. JWT Secret을 새로 생성하고 로컬 `.env`에만 저장한다.
4. 백엔드 `application.yml`이 환경변수 기반으로 되어 있는지 확인한다.
5. DB/Redis Docker + 백엔드 로컬 + 프론트 로컬 실행 흐름을 안정화한다.
6. 백엔드가 정상 실행되면 실제 API 응답과 성능 병목을 다시 측정한다.
7. 프론트는 API URL 하드코딩을 제거하고 공통 API client로 정리한다.

