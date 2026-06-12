from __future__ import annotations

from pathlib import Path
from typing import Any

import joblib
import pandas as pd

from arcane_model.features import normalize_feature_frame


class ArcaneScorePredictor:
    def __init__(self, model_path: str | Path = "models/deeplol_score_model.joblib") -> None:
        self.model_path = Path(model_path)
        self.artifact = joblib.load(self.model_path)
        self.pipeline = self.artifact["pipeline"]
        self.feature_spec = self.artifact["feature_spec"]

    def predict_one(self, payload: dict[str, Any]) -> float:
        frame = pd.DataFrame([payload])
        frame = normalize_feature_frame(frame, self.feature_spec)
        features = self.feature_spec.numeric_features + self.feature_spec.categorical_features
        prediction = float(self.pipeline.predict(frame[features])[0])
        return max(0.0, min(100.0, prediction))

    def predict_many(self, payloads: list[dict[str, Any]]) -> list[float]:
        if not payloads:
            return []
        frame = pd.DataFrame(payloads)
        frame = normalize_feature_frame(frame, self.feature_spec)
        features = self.feature_spec.numeric_features + self.feature_spec.categorical_features
        predictions = self.pipeline.predict(frame[features])
        return [max(0.0, min(100.0, float(prediction))) for prediction in predictions]
