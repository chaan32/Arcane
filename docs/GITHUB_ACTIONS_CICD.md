# GitHub Actions CI/CD

Arcane의 배포 기준은 다음과 같다.

- Frontend: Vercel Git 연동으로 배포
- API Server / Worker Server / AI Server: GitHub Actions에서 Docker image build 후 GHCR push
- EC2: GHCR image pull 후 `docker-compose.ec2.yml`로 재기동

이 workflow는 `/backend`, `/worker`, `/ai`, `/frontend`가 하나의 GitHub repository 안에 있는 monorepo 구조를 기준으로 한다.

## Workflow

### 1. `Arcane CI`

파일: `.github/workflows/ci.yml`

실행 시점:

- `main`, `develop` push
- `main`, `develop` 대상 pull request

검증 내용:

- Backend `compileJava`
- Worker `compileJava`
- Frontend `npm ci`, `tsc --noEmit`, `next build`
- AI Server dependency install, Python compile, FastAPI app import
- EC2 Docker Compose config 검증

### 2. `Build Images and Deploy EC2`

파일: `.github/workflows/deploy-ec2.yml`

실행 시점:

- `main` push
- GitHub Actions 화면에서 수동 실행

처리 흐름:

1. API / Worker / AI Docker image build
2. GHCR에 image push
3. EC2에 SSH 접속
4. EC2 프로젝트 디렉터리에서 `git pull`
5. GHCR image pull
6. `docker compose up -d --no-build`로 컨테이너 교체

## GitHub Secrets

Repository Settings -> Secrets and variables -> Actions에 아래 값을 등록한다.

| Secret | 설명 | 예시 |
| --- | --- | --- |
| `EC2_HOST` | EC2 public IP 또는 도메인 | `13.125.10.20` |
| `EC2_USER` | SSH 사용자 | `ubuntu` |
| `EC2_SSH_KEY` | EC2 접속 private key 전체 내용 | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `EC2_PROJECT_DIR` | EC2 안의 프로젝트 경로 | `/home/ubuntu/Arcane` |
| `GHCR_USERNAME` | GHCR 로그인 사용자 | GitHub username |
| `GHCR_TOKEN` | GHCR read 권한 token | fine-grained PAT 또는 classic PAT |

`GHCR_TOKEN`은 private package를 EC2에서 pull해야 할 때 필요하다. 최소 권한은 `read:packages`이다.

## EC2 준비

EC2에는 아래가 준비되어 있어야 한다.

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-plugin git
sudo usermod -aG docker $USER
```

프로젝트를 clone한다.

```bash
git clone https://github.com/<owner>/<repo>.git /home/ubuntu/Arcane
cd /home/ubuntu/Arcane
cp .env.ec2.example .env.ec2
```

`.env.ec2`의 실제 운영 값을 채운다.

## Vercel 준비

Vercel에는 아래 환경변수를 넣는다.

```properties
NEXT_PUBLIC_API_URL=https://api.your-domain.com
NEXT_PUBLIC_DDRAGON_VERSION=latest
```

EC2 API를 IP로 직접 열어둔 경우:

```properties
NEXT_PUBLIC_API_URL=http://your-ec2-public-ip:8080
```

## OAuth 주의사항

Vercel callback과 OAuth provider callback은 다르다.

- Vercel callback: `https://your-frontend.vercel.app/oauth/callback`
- Google callback: `https://api.your-domain.com/login/oauth2/code/google`
- Naver callback: `https://api.your-domain.com/login/oauth2/code/naver`

EC2 `.env.ec2`에는 Vercel callback을 넣는다.

```properties
OAUTH2_SUCCESS_REDIRECT_URI=https://your-frontend.vercel.app/oauth/callback
OAUTH2_FAILURE_REDIRECT_URI=https://your-frontend.vercel.app/oauth/callback
APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://your-frontend.vercel.app
```

Google/Naver 개발자 콘솔에는 API server callback을 넣는다.

## Rollback

GHCR에 push된 이전 image tag를 `.env.ec2` 또는 배포 명령의 image 변수로 지정한 뒤 다시 실행한다.

```bash
ARCANE_API_IMAGE=ghcr.io/<owner>/<repo>/arcane-api:<old-tag> \
ARCANE_WORKER_IMAGE=ghcr.io/<owner>/<repo>/arcane-worker:<old-tag> \
ARCANE_AI_IMAGE=ghcr.io/<owner>/<repo>/arcane-ai:<old-tag> \
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml up -d --no-build
```
