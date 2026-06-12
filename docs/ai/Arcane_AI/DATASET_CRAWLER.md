# DeepLOL 점수 데이터셋 수집

목표는 Arcane DB에 이미 저장된 `match_participant` 지표를 feature로 쓰고, DeepLOL 화면의 `AI-Score`를 label로 붙여서 학습용 CSV를 만드는 것이다.

## 1. 설치

```bash
cd /Users/haechan/Desktop/Arcane/ai/Arcane_AI
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python -m playwright install chromium
```

## 2. DB 접속 설정

```bash
cp .env.example .env
```

`.env`에서 MySQL 접속 정보를 현재 Docker DB와 맞춘다.

```env
ARCANE_DB_HOST=127.0.0.1
ARCANE_DB_PORT=3307
ARCANE_DB_NAME=arcane_db
ARCANE_DB_USER=root
ARCANE_DB_PASSWORD=arcane1234
DEEPLOL_CRAWL_DELAY_SECONDS=2.0
```

## 3. DB에 후보 데이터가 있는지 확인

```bash
python -m arcane_dataset.collect --dry-run
```

여기서 `DB participant rows`가 충분히 많아야 한다. 4만 row CSV를 만들려면 DB에도 최소 4만 개 이상의 `match_participant`가 있어야 한다.

## 4. 4만 row CSV 생성

```bash
python -m arcane_dataset.collect \
  --target-rows 40000 \
  --output data/deeplol_training.csv \
  --summoner-limit 3000 \
  --cards-per-summoner 20 \
  --headless
```

브라우저 동작을 눈으로 보면서 디버깅하고 싶으면:

```bash
python -m arcane_dataset.collect \
  --target-rows 40000 \
  --output data/deeplol_training.csv \
  --summoner-limit 3000 \
  --cards-per-summoner 20 \
  --no-headless
```

이미 저장된 `match_participant_id`는 다시 저장하지 않으므로, 중간에 끊겨도 같은 명령을 다시 실행하면 이어서 수집한다.

## 현재 파이프라인의 매칭 기준

1. DeepLOL 소환사 페이지를 연다.
2. 전적 카드에서 `AI-Score`, 등수, KDA, 승패, 큐 타입, 게임 길이를 읽는다.
3. Arcane DB의 같은 소환사 최근 경기들과 비교한다.
4. KDA, 승패, 큐 타입, 게임 길이, 챔피언명, 순서를 점수화해서 가장 그럴듯한 `match_participant`를 찾는다.
5. 상세 참가자 점수 파싱에 성공하면 한 경기에서 여러 참가자 row를 저장한다.
6. 상세 참가자 점수 파싱에 실패하면 검색 대상 소환사의 점수 1개라도 저장한다.

DeepLOL 화면 구조가 바뀌면 상세 참가자 10명 파싱은 깨질 수 있다. 그래도 검색 대상 소환사 기준 1게임 1라벨 수집은 유지되도록 설계했다.

## 5. CSV 확보 후 모델 학습

4만 row CSV가 확보되면 아래 명령으로 학습한다.

```bash
python -m arcane_model.train \
  --input data/deeplol_training.csv \
  --model-output models/deeplol_score_model.joblib \
  --metrics-output models/deeplol_score_metrics.json \
  --feature-importance-output models/deeplol_feature_importance.csv \
  --min-rows 40000
```

학습은 같은 게임의 10명 참가자가 train/test에 섞이지 않도록 `match_riot_id` 기준으로 그룹 분리한다.

생성되는 파일:

```text
models/deeplol_score_model.joblib
models/deeplol_score_metrics.json
models/deeplol_feature_importance.csv
```

`deeplol_score_model.joblib`가 최종 모델 파일이다. FastAPI 서버를 다시 띄우면 `/predict`에서 이 모델을 사용한다.
