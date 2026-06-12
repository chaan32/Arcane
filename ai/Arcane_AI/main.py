import json
import os
import threading
import time
from datetime import datetime
from pathlib import Path
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel

try:
    from dotenv import load_dotenv
except Exception:
    load_dotenv = None

MODEL_LOAD_ERROR = None
KAFKA_LOAD_ERROR = None

try:
    from arcane_model.predict import ArcaneScorePredictor
except Exception as exception:
    ArcaneScorePredictor = None
    MODEL_LOAD_ERROR = f"{exception.__class__.__name__}: {exception}"

try:
    from kafka import KafkaConsumer, KafkaProducer
except Exception as exception:
    KafkaConsumer = None
    KafkaProducer = None
    KAFKA_LOAD_ERROR = f"{exception.__class__.__name__}: {exception}"

app = FastAPI(title="Arcane AI Server")
BASE_DIR = Path(__file__).resolve().parent
if load_dotenv is not None:
    load_dotenv(BASE_DIR / ".env")

MODEL_PATH = BASE_DIR / "models" / "deeplol_score_model.joblib"
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_ENABLED = os.getenv("AI_KAFKA_ENABLED", "true").lower() == "true"
AI_SCORE_REQUEST_TOPIC = os.getenv("AI_SCORE_REQUEST_TOPIC", "arcane.ai.score.requested")
AI_SCORE_COMPLETED_TOPIC = os.getenv("AI_SCORE_COMPLETED_TOPIC", "arcane.ai.score.completed")
AI_SCORE_FAILED_TOPIC = os.getenv("AI_SCORE_FAILED_TOPIC", "arcane.ai.score.failed")
AI_SCORE_GROUP_ID = os.getenv("AI_SCORE_GROUP_ID", "arcane-ai-score-server")

predictor = None
kafka_started = False

if ArcaneScorePredictor is not None and MODEL_PATH.exists():
    try:
        predictor = ArcaneScorePredictor(MODEL_PATH)
    except Exception as exception:
        MODEL_LOAD_ERROR = f"{exception.__class__.__name__}: {exception}"


class ScorePredictRequest(BaseModel):
    features: dict[str, Any]


class ScorePredictItem(BaseModel):
    itemId: str
    features: dict[str, Any]


class ScorePredictBatchRequest(BaseModel):
    items: list[ScorePredictItem]


class ScorePredictResult(BaseModel):
    itemId: str
    score: float


@app.on_event("startup")
def start_kafka_consumer() -> None:
    global kafka_started
    if kafka_started or not KAFKA_ENABLED:
        return
    if KafkaConsumer is None or KafkaProducer is None:
        return

    thread = threading.Thread(target=consume_score_requests, name="ai-score-kafka-consumer", daemon=True)
    thread.start()
    kafka_started = True


@app.get("/health")
def health():
    return {
        "status": "ok",
        "model_loaded": predictor is not None,
        "model_path": str(MODEL_PATH),
        "model_load_error": MODEL_LOAD_ERROR,
        "kafka_enabled": KAFKA_ENABLED,
        "kafka_started": kafka_started,
        "kafka_bootstrap_servers": KAFKA_BOOTSTRAP_SERVERS,
        "kafka_load_error": KAFKA_LOAD_ERROR,
    }


@app.get("/random", response_model=int)
def random_score() -> int:
    return 50


@app.post("/predict", response_model=float)
def predict_score(request: ScorePredictRequest) -> float:
    return predict_feature(request.features)


@app.post("/predict/batch", response_model=list[ScorePredictResult])
def predict_scores(request: ScorePredictBatchRequest) -> list[ScorePredictResult]:
    scores = predict_features([item.features for item in request.items])
    return [
        ScorePredictResult(itemId=item.itemId, score=scores[index])
        for index, item in enumerate(request.items)
    ]


def consume_score_requests() -> None:
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        key_serializer=lambda value: str(value).encode("utf-8") if value is not None else None,
        value_serializer=lambda value: json.dumps(value, ensure_ascii=False).encode("utf-8"),
    )
    consumer = KafkaConsumer(
        AI_SCORE_REQUEST_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        group_id=AI_SCORE_GROUP_ID,
        auto_offset_reset="latest",
        enable_auto_commit=False,
        key_deserializer=lambda value: value.decode("utf-8") if value is not None else None,
        value_deserializer=lambda value: json.loads(value.decode("utf-8")),
    )

    for message in consumer:
        payload = message.value
        request_id = payload.get("requestId")
        trace_id = payload.get("traceId")
        started_at = time.perf_counter()
        try:
            items = payload.get("items") or []
            predicted_scores = predict_features([
                item.get("features") or {}
                for item in items
            ])
            scores = [
                {
                    "itemId": str(item.get("itemId")),
                    "score": predicted_scores[index],
                }
                for index, item in enumerate(items)
            ]
            elapsed_ms = int((time.perf_counter() - started_at) * 1000)
            producer.send(
                AI_SCORE_COMPLETED_TOPIC,
                key=request_id,
                value={
                    "requestId": request_id,
                    "traceId": trace_id,
                    "completedAt": datetime.now().isoformat(),
                    "elapsedMs": elapsed_ms,
                    "scores": scores,
                },
            )
            producer.flush()
            consumer.commit()
        except Exception as exception:
            producer.send(
                AI_SCORE_FAILED_TOPIC,
                key=request_id,
                value={
                    "requestId": request_id,
                    "traceId": trace_id,
                    "failedAt": datetime.now().isoformat(),
                    "reason": f"{exception.__class__.__name__}: {exception}",
                },
            )
            producer.flush()
            consumer.commit()


def predict_feature(features: dict[str, Any]) -> float:
    if predictor is None:
        return fallback_score(features)
    return predictor.predict_one(features)


def predict_features(features_list: list[dict[str, Any]]) -> list[float]:
    if predictor is None:
        return [fallback_score(features) for features in features_list]
    return predictor.predict_many(features_list)


def fallback_score(features: dict[str, Any]) -> float:
    kills = number(features.get("kills"))
    deaths = max(1.0, number(features.get("deaths")))
    assists = number(features.get("assists"))
    damage = number(features.get("total_damage_dealt_to_champions"))
    cs = number(features.get("total_minion_kills"))
    vision = number(features.get("vision_score"))
    win_bonus = 8.0 if bool(features.get("win")) else -4.0

    kda_score = min(28.0, ((kills + assists) / deaths) * 6.0)
    damage_score = min(22.0, damage / 1500.0)
    cs_score = min(18.0, cs / 12.0)
    vision_score = min(12.0, vision / 3.0)
    raw_score = 20.0 + kda_score + damage_score + cs_score + vision_score + win_bonus
    return max(0.0, min(100.0, round(raw_score, 1)))


def number(value: Any) -> float:
    if value is None:
        return 0.0
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0
