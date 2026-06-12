from __future__ import annotations

from dataclasses import dataclass

import pandas as pd


TARGET_COLUMN = "source_score"
GROUP_COLUMN = "match_riot_id"


IDENTIFIER_COLUMNS = [
    "label_source",
    "source_score",
    "source_rank",
    "source_page_url",
    "source_card_index",
    "source_participant_index",
    "match_participant_id",
    "match_db_id",
    "match_riot_id",
    "summoner_id",
    "puuid",
    "game_name",
    "tag_line",
    "champion_name_ko",
    "champion_name_en",
]


NUMERIC_FEATURES = [
    "game_duration",
    "game_end_timestamp",
    "queue_id",
    "champion_id",
    "win",
    "champ_level",
    "item0",
    "item1",
    "item2",
    "item3",
    "item4",
    "item5",
    "item6",
    "kda",
    "kills",
    "deaths",
    "assists",
    "total_damage_dealt_to_champions",
    "total_damage_taken",
    "total_minion_kills",
    "double_kills",
    "triple_kills",
    "quadra_kills",
    "penta_kills",
    "spell1casts",
    "spell2casts",
    "spell3casts",
    "spell4casts",
    "summoner1id",
    "summoner1casts",
    "summoner2id",
    "summoner2casts",
    "ward_killed",
    "ward_placed",
    "vision_wards_bought_in_game",
    "vision_score",
]


CATEGORICAL_FEATURES = [
    "game_mode",
    "team_position",
]


@dataclass(frozen=True)
class FeatureSpec:
    numeric_features: list[str]
    categorical_features: list[str]
    target_column: str = TARGET_COLUMN
    group_column: str = GROUP_COLUMN


def default_feature_spec() -> FeatureSpec:
    return FeatureSpec(
        numeric_features=list(NUMERIC_FEATURES),
        categorical_features=list(CATEGORICAL_FEATURES),
    )


def normalize_feature_frame(frame: pd.DataFrame, feature_spec: FeatureSpec) -> pd.DataFrame:
    normalized = frame.copy()
    for column in feature_spec.numeric_features:
        if column in normalized.columns:
            normalized[column] = normalize_numeric_feature(normalized[column])
    return normalized


def normalize_numeric_feature(series: pd.Series) -> pd.Series:
    normalized = series.replace(
        {
            True: 1,
            False: 0,
            "true": 1,
            "false": 0,
            "True": 1,
            "False": 0,
            "b'\\x01'": 1,
            "b'\\x00'": 0,
            'b"\\x01"': 1,
            'b"\\x00"': 0,
        }
    )
    return pd.to_numeric(normalized, errors="coerce")
