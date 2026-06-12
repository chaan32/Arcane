from __future__ import annotations

import os
from dataclasses import dataclass

from dotenv import load_dotenv


@dataclass(frozen=True)
class DbConfig:
    host: str
    port: int
    database: str
    user: str
    password: str


@dataclass(frozen=True)
class CollectorConfig:
    db: DbConfig
    deeplol_delay_seconds: float


def load_config() -> CollectorConfig:
    load_dotenv()

    db = DbConfig(
        host=os.getenv("ARCANE_DB_HOST", "127.0.0.1"),
        port=int(os.getenv("ARCANE_DB_PORT", "3307")),
        database=os.getenv("ARCANE_DB_NAME", "arcane_db"),
        user=os.getenv("ARCANE_DB_USER", "root"),
        password=os.getenv("ARCANE_DB_PASSWORD", "arcane1234"),
    )

    return CollectorConfig(
        db=db,
        deeplol_delay_seconds=float(os.getenv("DEEPLOL_CRAWL_DELAY_SECONDS", "2.0")),
    )
