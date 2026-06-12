from __future__ import annotations

from arcane_dataset.db import normalize_name
from arcane_dataset.deeplol import ScrapedMatchCard, ScrapedParticipantScore


def match_owner_card(card: ScrapedMatchCard, candidates: list[dict], card_index: int) -> dict | None:
    if card.match_riot_id:
        for candidate in candidates:
            if candidate.get("match_riot_id") == card.match_riot_id:
                return candidate

    best: dict | None = None
    best_score = -1

    for index, candidate in enumerate(candidates):
        score = 0

        if _same_number(candidate.get("kills"), card.kills):
            score += 8
        if _same_number(candidate.get("deaths"), card.deaths):
            score += 8
        if _same_number(candidate.get("assists"), card.assists):
            score += 8

        if card.win is not None and bool(candidate.get("win")) == card.win:
            score += 5

        if card.queue_id is not None and int(candidate.get("queue_id") or -1) == card.queue_id:
            score += 5

        if card.duration_seconds is not None:
            diff = abs(int(candidate.get("game_duration") or 0) - card.duration_seconds)
            if diff <= 30:
                score += 8
            elif diff <= 90:
                score += 4

        if _champion_matches(card, candidate):
            score += 5

        order_gap = abs(index - card_index)
        if order_gap == 0:
            score += 5
        elif order_gap <= 2:
            score += 2

        if score > best_score:
            best_score = score
            best = candidate

    # KDA 3개 + 승패 정도는 맞아야 잘못된 라벨을 붙일 가능성이 낮다.
    return best if best_score >= 25 else None


def match_detail_participant(detail: ScrapedParticipantScore, participants: list[dict]) -> dict | None:
    best: dict | None = None
    best_score = -1

    for candidate in participants:
        score = 0

        if detail.game_name and normalize_name(detail.game_name) == normalize_name(candidate.get("game_name")):
            score += 10
        if detail.tag_line and normalize_name(detail.tag_line) == normalize_name(candidate.get("tag_line")):
            score += 5

        if _same_number(candidate.get("kills"), detail.kills):
            score += 6
        if _same_number(candidate.get("deaths"), detail.deaths):
            score += 6
        if _same_number(candidate.get("assists"), detail.assists):
            score += 6

        if detail.champion_name:
            candidate_names = {
                normalize_name(candidate.get("champion_name_ko")),
                normalize_name(candidate.get("champion_name_en")),
            }
            if normalize_name(detail.champion_name) in candidate_names:
                score += 5

        if score > best_score:
            best_score = score
            best = candidate

    return best if best_score >= 18 else None


def _same_number(left, right) -> bool:
    return left is not None and right is not None and int(left) == int(right)


def _champion_matches(card: ScrapedMatchCard, candidate: dict) -> bool:
    if not card.champion_names:
        return False

    candidate_names = {
        normalize_name(candidate.get("champion_name_ko")),
        normalize_name(candidate.get("champion_name_en")),
    }
    return any(normalize_name(name) in candidate_names for name in card.champion_names)
