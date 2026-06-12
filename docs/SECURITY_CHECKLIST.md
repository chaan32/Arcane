# Arcane Security Checklist

작성일: 2026-06-04

## 결론

현재 가장 급한 보안 문제는 백엔드 설정 파일에 민감값이 하드코딩되어 있고, 해당 설정 파일이 Git에서 이미 추적 중이라는 점이다. `.gitignore`에 추가하는 것만으로는 이미 추적된 파일이 보호되지 않는다.

## 바로 조치해야 할 항목

### 1. Riot API Key 재발급

대상:

- Riot API key

이유:

- 백엔드 설정 파일에 직접 들어가 있다.
- GitHub 또는 외부 저장소에 올라간 적이 있으면 기존 키는 노출된 것으로 간주해야 한다.

조치:

- Riot Developer Portal에서 새 API key 발급
- 기존 key 폐기 또는 더 이상 사용하지 않기
- 로컬 `.env`에만 새 key 저장
- `application.yml`에서는 `${RIOT_API_KEY}`만 참조

### 2. OAuth Client Secret 재발급

대상:

- Google OAuth client secret
- Naver OAuth client secret

이유:

- OAuth client secret은 서버 비밀값이다.
- 코드나 Git에 들어가면 안 된다.

조치:

- Google Cloud Console에서 OAuth client secret 재발급
- Naver Developers에서 client secret 재발급
- 기존 secret 폐기
- 로컬 `.env`에만 저장
- `application.yml`에서는 환경변수로 참조

### 3. JWT Secret 재발급

대상:

- JWT signing secret

이유:

- JWT secret이 노출되면 공격자가 유효한 토큰을 직접 만들 수 있다.

조치:

- 새 secret 생성
- 최소 256-bit 이상, HS256 기준 충분히 긴 랜덤 문자열 사용
- 로컬 `.env`에만 저장
- 기존 토큰은 모두 무효화된다고 보고 다시 로그인

예시:

```bash
openssl rand -base64 64
```

### 4. Git 추적 파일 정리

대상:

- `backend/Arcane_Backend/src/main/resources/application.yml`
- `backend/Arcane_Backend/.env`
- `.DS_Store`
- `.gradle/`
- `build/`
- `.idea/`
- AI `data/`
- AI `models/`

중요:

- `.gitignore`는 새 파일 추적만 막는다.
- 이미 Git에 올라간 파일은 `git rm --cached`로 추적을 제거해야 한다.

권장 방향:

- `application.yml`은 민감값 없는 기본 템플릿만 유지하거나, `application-local.yml`을 로컬 전용으로 둔다.
- 실제 비밀값은 `.env` 또는 IDE run configuration 환경변수에 둔다.
- AI 모델 파일은 용량이 크므로 일반 Git에 넣지 않는다.

## 권장 환경변수

백엔드:

```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/arcane_db?serverTimezone=Asia/Seoul
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=...
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
RIOT_API_KEY=...
RIOT_PATCH_VERSION=16.11.1
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

프론트:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
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

## `.gitignore` 권장 보강

백엔드:

```gitignore
.env
application-local.yml
.DS_Store
.gradle/
build/
.idea/
```

AI:

```gitignore
.env
.venv/
__pycache__/
*.pyc
data/
models/
```

프론트:

현재 프론트 `.gitignore`는 대체로 적절하다.

## 히스토리 리스크

이미 GitHub에 민감값이 올라간 적이 있다면 파일을 삭제해도 히스토리에는 남을 수 있다.

현실적인 조치 순서:

1. 키 재발급
2. 현재 브랜치에서 추적 제거
3. 새 비밀값은 로컬에만 저장
4. GitHub 저장소가 공개라면 secret scanning 경고 확인
5. 필요하면 Git 히스토리 정리 검토

## 우선순위

1. Riot/OAuth/JWT secret 재발급
2. `application.yml` 환경변수화
3. Git tracked secret 제거
4. AI 대용량 산출물 Git 제외
5. README에 민감값 없이 실행 방법만 문서화
