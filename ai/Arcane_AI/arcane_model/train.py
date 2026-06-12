from __future__ import annotations

import argparse
import json
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import ExtraTreesRegressor, RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import GroupShuffleSplit
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder

from arcane_model.features import FeatureSpec, default_feature_spec, normalize_feature_frame


def main() -> None:
    parser = argparse.ArgumentParser(description="Train Arcane score regression model from DeepLOL-labeled CSV.")
    parser.add_argument("--input", default="data/deeplol_training.csv")
    parser.add_argument("--model-output", default="models/deeplol_score_model.joblib")
    parser.add_argument("--metrics-output", default="models/deeplol_score_metrics.json")
    parser.add_argument("--feature-importance-output", default="models/deeplol_feature_importance.csv")
    parser.add_argument("--test-size", type=float, default=0.2)
    parser.add_argument("--random-state", type=int, default=42)
    parser.add_argument("--min-rows", type=int, default=40_000)
    parser.add_argument("--model", choices=["extra_trees", "random_forest"], default="extra_trees")
    parser.add_argument("--n-estimators", type=int, default=300)
    args = parser.parse_args()

    input_path = Path(args.input)
    if not input_path.exists():
        raise FileNotFoundError(f"CSV가 없습니다: {input_path}")

    frame = pd.read_csv(input_path)
    frame = clean_training_frame(frame)

    if len(frame) < args.min_rows:
        raise ValueError(
            f"학습 데이터가 부족합니다. 현재 {len(frame):,} rows, 필요 {args.min_rows:,} rows"
        )

    feature_spec = default_feature_spec()
    missing_columns = find_missing_columns(frame, feature_spec)
    if missing_columns:
        raise ValueError(f"CSV에 필요한 컬럼이 없습니다: {missing_columns}")

    train_frame, test_frame = split_by_match(frame, feature_spec, args.test_size, args.random_state)
    pipeline = build_pipeline(feature_spec, args.model, args.random_state, args.n_estimators)

    x_train = train_frame[feature_spec.numeric_features + feature_spec.categorical_features]
    y_train = train_frame[feature_spec.target_column]
    x_test = test_frame[feature_spec.numeric_features + feature_spec.categorical_features]
    y_test = test_frame[feature_spec.target_column]

    pipeline.fit(x_train, y_train)
    predictions = np.clip(pipeline.predict(x_test), 0, 100)

    metrics = evaluate(y_test, predictions)
    metrics.update(
        {
            "model": args.model,
            "n_estimators": int(args.n_estimators),
            "rows_total": int(len(frame)),
            "rows_train": int(len(train_frame)),
            "rows_test": int(len(test_frame)),
            "matches_train": int(train_frame[feature_spec.group_column].nunique()),
            "matches_test": int(test_frame[feature_spec.group_column].nunique()),
            "target": feature_spec.target_column,
            "numeric_features": feature_spec.numeric_features,
            "categorical_features": feature_spec.categorical_features,
        }
    )

    model_output = Path(args.model_output)
    metrics_output = Path(args.metrics_output)
    importance_output = Path(args.feature_importance_output)

    model_output.parent.mkdir(parents=True, exist_ok=True)
    metrics_output.parent.mkdir(parents=True, exist_ok=True)
    importance_output.parent.mkdir(parents=True, exist_ok=True)

    artifact = {
        "pipeline": pipeline,
        "feature_spec": feature_spec,
        "metrics": metrics,
    }
    joblib.dump(artifact, model_output)

    metrics_output.write_text(
        json.dumps(metrics, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    write_feature_importance(pipeline, feature_spec, importance_output)

    print(json.dumps(metrics, ensure_ascii=False, indent=2))
    print(f"saved model: {model_output}")
    print(f"saved metrics: {metrics_output}")
    print(f"saved feature importance: {importance_output}")


def clean_training_frame(frame: pd.DataFrame) -> pd.DataFrame:
    cleaned = frame.copy()
    cleaned = cleaned.drop_duplicates(subset=["match_participant_id"])
    cleaned = cleaned[cleaned["source_score"].notna()]
    cleaned["source_score"] = pd.to_numeric(cleaned["source_score"], errors="coerce")
    cleaned = cleaned[cleaned["source_score"].between(0, 100)]
    cleaned = cleaned[cleaned["match_riot_id"].notna()]
    feature_spec = default_feature_spec()
    cleaned = normalize_feature_frame(cleaned, feature_spec)
    return cleaned.reset_index(drop=True)


def find_missing_columns(frame: pd.DataFrame, feature_spec: FeatureSpec) -> list[str]:
    required = (
        [feature_spec.target_column, feature_spec.group_column]
        + feature_spec.numeric_features
        + feature_spec.categorical_features
    )
    return [column for column in required if column not in frame.columns]


def split_by_match(
    frame: pd.DataFrame,
    feature_spec: FeatureSpec,
    test_size: float,
    random_state: int,
) -> tuple[pd.DataFrame, pd.DataFrame]:
    splitter = GroupShuffleSplit(n_splits=1, test_size=test_size, random_state=random_state)
    train_index, test_index = next(
        splitter.split(frame, frame[feature_spec.target_column], groups=frame[feature_spec.group_column])
    )
    return frame.iloc[train_index].copy(), frame.iloc[test_index].copy()


def build_pipeline(
    feature_spec: FeatureSpec,
    model_name: str,
    random_state: int,
    n_estimators: int,
) -> Pipeline:
    numeric_pipeline = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median")),
        ]
    )

    categorical_pipeline = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="constant", fill_value="UNKNOWN")),
            ("one_hot", OneHotEncoder(handle_unknown="ignore")),
        ]
    )

    preprocessor = ColumnTransformer(
        transformers=[
            ("numeric", numeric_pipeline, feature_spec.numeric_features),
            ("categorical", categorical_pipeline, feature_spec.categorical_features),
        ]
    )

    if model_name == "random_forest":
        regressor = RandomForestRegressor(
            n_estimators=n_estimators,
            max_depth=None,
            min_samples_leaf=2,
            n_jobs=-1,
            random_state=random_state,
        )
    else:
        regressor = ExtraTreesRegressor(
            n_estimators=n_estimators,
            max_depth=None,
            min_samples_leaf=2,
            n_jobs=-1,
            random_state=random_state,
        )

    return Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("regressor", regressor),
        ]
    )


def evaluate(y_true: pd.Series, y_pred: np.ndarray) -> dict:
    return {
        "mae": float(mean_absolute_error(y_true, y_pred)),
        "rmse": float(np.sqrt(mean_squared_error(y_true, y_pred))),
        "r2": float(r2_score(y_true, y_pred)),
        "prediction_min": float(np.min(y_pred)),
        "prediction_max": float(np.max(y_pred)),
        "target_mean": float(np.mean(y_true)),
        "prediction_mean": float(np.mean(y_pred)),
    }


def write_feature_importance(pipeline: Pipeline, feature_spec: FeatureSpec, output_path: Path) -> None:
    regressor = pipeline.named_steps["regressor"]
    if not hasattr(regressor, "feature_importances_"):
        return

    feature_names = get_feature_names(pipeline, feature_spec)
    importances = regressor.feature_importances_

    importance_frame = pd.DataFrame(
        {
            "feature": feature_names,
            "importance": importances,
        }
    ).sort_values("importance", ascending=False)
    importance_frame.to_csv(output_path, index=False)


def get_feature_names(pipeline: Pipeline, feature_spec: FeatureSpec) -> list[str]:
    preprocessor = pipeline.named_steps["preprocessor"]
    names: list[str] = []

    names.extend(feature_spec.numeric_features)

    categorical_transformer = preprocessor.named_transformers_["categorical"]
    one_hot = categorical_transformer.named_steps["one_hot"]
    names.extend(one_hot.get_feature_names_out(feature_spec.categorical_features).tolist())

    return names


if __name__ == "__main__":
    main()
