from __future__ import annotations

from contextlib import contextmanager
from dataclasses import dataclass
from typing import Iterable

import pymysql
from pymysql.cursors import DictCursor

from arcane_dataset.config import DbConfig


@dataclass(frozen=True)
class SummonerTarget:
    summoner_id: int
    game_name: str
    tag_line: str
    puuid: str
    match_count: int


FEATURE_COLUMNS = [
    "match_participant_id",
    "match_db_id",
    "match_riot_id",
    "game_creation",
    "game_duration",
    "game_end_timestamp",
    "game_mode",
    "queue_id",
    "summoner_id",
    "puuid",
    "game_name",
    "tag_line",
    "champion_id",
    "champion_name_ko",
    "champion_name_en",
    "win",
    "champ_level",
    "team_position",
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


LABEL_COLUMNS = [
    "label_source",
    "source_score",
    "source_rank",
    "source_page_url",
    "source_card_index",
    "source_participant_index",
]


CSV_COLUMNS = LABEL_COLUMNS + FEATURE_COLUMNS


class ArcaneDatabase:
    def __init__(self, config: DbConfig) -> None:
        self.config = config

    @contextmanager
    def connect(self):
        connection = pymysql.connect(
            host=self.config.host,
            port=self.config.port,
            user=self.config.user,
            password=self.config.password,
            database=self.config.database,
            charset="utf8mb4",
            cursorclass=DictCursor,
            autocommit=True,
        )
        try:
            yield connection
        finally:
            connection.close()

    def fetch_summoner_targets(self, limit: int, min_matches: int) -> list[SummonerTarget]:
        query = """
            SELECT
                s.id AS summoner_id,
                s.game_name,
                s.tag_line,
                s.puuid,
                COUNT(DISTINCT mi.id) AS match_count
            FROM summoner s
            JOIN match_participant mp ON mp.summoner_id = s.id
            JOIN match_info mi ON mi.id = mp.match_id
            WHERE s.game_name IS NOT NULL
              AND s.tag_line IS NOT NULL
              AND s.puuid IS NOT NULL
            GROUP BY s.id, s.game_name, s.tag_line, s.puuid
            HAVING match_count >= %s
            ORDER BY match_count DESC, s.update_at DESC
            LIMIT %s
        """

        with self.connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(query, (min_matches, limit))
                rows = cursor.fetchall()

        return [
            SummonerTarget(
                summoner_id=int(row["summoner_id"]),
                game_name=row["game_name"],
                tag_line=row["tag_line"],
                puuid=row["puuid"],
                match_count=int(row["match_count"]),
            )
            for row in rows
        ]

    def fetch_owner_match_candidates(self, summoner_id: int, limit: int = 80) -> list[dict]:
        query = """
            SELECT
                mp.id AS match_participant_id,
                mi.id AS match_db_id,
                mi.match_id AS match_riot_id,
                mi.game_creation,
                mi.game_duration,
                mi.game_end_timestamp,
                mi.game_mode,
                mi.queue_id,
                s.id AS summoner_id,
                s.puuid,
                s.game_name,
                s.tag_line,
                mp.champion_id,
                c.name_ko AS champion_name_ko,
                c.name_en AS champion_name_en,
                mp.win,
                mp.champ_level,
                mp.team_position,
                mp.item0,
                mp.item1,
                mp.item2,
                mp.item3,
                mp.item4,
                mp.item5,
                mp.item6,
                mp.kda,
                mp.kills,
                mp.deaths,
                mp.assists,
                mp.total_damage_dealt_to_champions,
                mp.total_damage_taken,
                mp.total_minion_kills,
                mp.double_kills,
                mp.triple_kills,
                mp.quadra_kills,
                mp.penta_kills,
                mp.spell1casts,
                mp.spell2casts,
                mp.spell3casts,
                mp.spell4casts,
                mp.summoner1id,
                mp.summoner1casts,
                mp.summoner2id,
                mp.summoner2casts,
                mp.ward_killed,
                mp.ward_placed,
                mp.vision_wards_bought_in_game,
                mp.vision_score
            FROM match_participant mp
            JOIN match_info mi ON mi.id = mp.match_id
            JOIN summoner s ON s.id = mp.summoner_id
            LEFT JOIN champion c ON c.id = mp.champion_id
            WHERE mp.summoner_id = %s
            ORDER BY mi.game_end_timestamp DESC
            LIMIT %s
        """

        with self.connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(query, (summoner_id, limit))
                return list(cursor.fetchall())

    def fetch_match_participants(self, match_db_id: int) -> list[dict]:
        query = """
            SELECT
                mp.id AS match_participant_id,
                mi.id AS match_db_id,
                mi.match_id AS match_riot_id,
                mi.game_creation,
                mi.game_duration,
                mi.game_end_timestamp,
                mi.game_mode,
                mi.queue_id,
                s.id AS summoner_id,
                s.puuid,
                s.game_name,
                s.tag_line,
                mp.champion_id,
                c.name_ko AS champion_name_ko,
                c.name_en AS champion_name_en,
                mp.win,
                mp.champ_level,
                mp.team_position,
                mp.item0,
                mp.item1,
                mp.item2,
                mp.item3,
                mp.item4,
                mp.item5,
                mp.item6,
                mp.kda,
                mp.kills,
                mp.deaths,
                mp.assists,
                mp.total_damage_dealt_to_champions,
                mp.total_damage_taken,
                mp.total_minion_kills,
                mp.double_kills,
                mp.triple_kills,
                mp.quadra_kills,
                mp.penta_kills,
                mp.spell1casts,
                mp.spell2casts,
                mp.spell3casts,
                mp.spell4casts,
                mp.summoner1id,
                mp.summoner1casts,
                mp.summoner2id,
                mp.summoner2casts,
                mp.ward_killed,
                mp.ward_placed,
                mp.vision_wards_bought_in_game,
                mp.vision_score
            FROM match_participant mp
            JOIN match_info mi ON mi.id = mp.match_id
            JOIN summoner s ON s.id = mp.summoner_id
            LEFT JOIN champion c ON c.id = mp.champion_id
            WHERE mi.id = %s
            ORDER BY mp.id ASC
        """

        with self.connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(query, (match_db_id,))
                return list(cursor.fetchall())

    def fetch_all_match_participants(self) -> list[dict]:
        query = """
            SELECT
                mp.id AS match_participant_id,
                mi.id AS match_db_id,
                mi.match_id AS match_riot_id,
                mi.game_creation,
                mi.game_duration,
                mi.game_end_timestamp,
                mi.game_mode,
                mi.queue_id,
                s.id AS summoner_id,
                s.puuid,
                s.game_name,
                s.tag_line,
                mp.champion_id,
                c.name_ko AS champion_name_ko,
                c.name_en AS champion_name_en,
                mp.win,
                mp.champ_level,
                mp.team_position,
                mp.item0,
                mp.item1,
                mp.item2,
                mp.item3,
                mp.item4,
                mp.item5,
                mp.item6,
                mp.kda,
                mp.kills,
                mp.deaths,
                mp.assists,
                mp.total_damage_dealt_to_champions,
                mp.total_damage_taken,
                mp.total_minion_kills,
                mp.double_kills,
                mp.triple_kills,
                mp.quadra_kills,
                mp.penta_kills,
                mp.spell1casts,
                mp.spell2casts,
                mp.spell3casts,
                mp.spell4casts,
                mp.summoner1id,
                mp.summoner1casts,
                mp.summoner2id,
                mp.summoner2casts,
                mp.ward_killed,
                mp.ward_placed,
                mp.vision_wards_bought_in_game,
                mp.vision_score
            FROM match_participant mp
            JOIN match_info mi ON mi.id = mp.match_id
            JOIN summoner s ON s.id = mp.summoner_id
            LEFT JOIN champion c ON c.id = mp.champion_id
            ORDER BY mi.game_end_timestamp DESC, mp.id ASC
        """

        with self.connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(query)
                return list(cursor.fetchall())

    def count_candidate_participants(self) -> int:
        query = "SELECT COUNT(*) AS cnt FROM match_participant"
        with self.connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(query)
                return int(cursor.fetchone()["cnt"])


def normalize_name(value: str | None) -> str:
    return (value or "").replace(" ", "").replace("#", "").lower()


def pick_csv_columns(row: dict) -> dict:
    return {column: row.get(column) for column in CSV_COLUMNS}


def existing_participant_ids(rows: Iterable[dict]) -> set[int]:
    ids: set[int] = set()
    for row in rows:
        value = row.get("match_participant_id")
        if value not in (None, ""):
            ids.add(int(value))
    return ids
