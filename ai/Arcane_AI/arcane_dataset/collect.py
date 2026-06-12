from __future__ import annotations

import argparse
import csv
from pathlib import Path

from arcane_dataset.config import load_config
from arcane_dataset.db import CSV_COLUMNS, ArcaneDatabase, pick_csv_columns
from arcane_dataset.deeplol import DeepLolCrawler, make_deeplol_url
from arcane_dataset.matcher import match_detail_participant, match_owner_card


def main() -> None:
    parser = argparse.ArgumentParser(description="Collect DeepLOL labels and export Arcane training CSV.")
    parser.add_argument("--target-rows", type=int, default=40_000)
    parser.add_argument("--output", default="data/deeplol_training.csv")
    parser.add_argument("--summoner-limit", type=int, default=3_000)
    parser.add_argument("--min-matches", type=int, default=10)
    parser.add_argument("--cards-per-summoner", type=int, default=20)
    parser.add_argument("--batch-size", type=int, default=100)
    parser.add_argument("--direct-unlabeled", action="store_true")
    parser.add_argument("--headless", action=argparse.BooleanOptionalAction, default=True)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    config = load_config()
    database = ArcaneDatabase(config.db)
    crawler = DeepLolCrawler(headless=args.headless, delay_seconds=config.deeplol_delay_seconds)

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    existing_ids = _load_existing_ids(output_path)
    written = len(existing_ids)

    if args.dry_run:
        total_candidates = database.count_candidate_participants()
        targets = database.fetch_summoner_targets(limit=10, min_matches=args.min_matches)
        print(f"DB participant rows: {total_candidates}")
        print(f"Existing CSV rows: {written}")
        print("Sample targets:")
        for target in targets:
            print(f"- {target.game_name}#{target.tag_line} matches={target.match_count}")
        return

    if args.direct_unlabeled:
        _collect_direct_unlabeled(
            database=database,
            crawler=crawler,
            output_path=output_path,
            existing_ids=existing_ids,
            written=written,
            target_rows=args.target_rows,
            batch_size=args.batch_size,
        )
        return

    targets = database.fetch_summoner_targets(limit=args.summoner_limit, min_matches=args.min_matches)
    print(f"target summoners={len(targets)}, existing csv rows={written}, target rows={args.target_rows}")

    with output_path.open("a", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_COLUMNS)
        if output_path.stat().st_size == 0:
            writer.writeheader()

        for target_index, target in enumerate(targets, start=1):
            if written >= args.target_rows:
                break

            print(
                f"[{target_index}/{len(targets)}] crawl {target.game_name}#{target.tag_line} "
                f"matches={target.match_count}"
            )

            owner_candidates = database.fetch_owner_match_candidates(
                target.summoner_id,
                limit=max(args.cards_per_summoner * 3, 80),
            )
            owner_candidate_ids = {
                int(candidate["match_participant_id"])
                for candidate in owner_candidates
                if candidate.get("match_participant_id")
            }
            if owner_candidate_ids and owner_candidate_ids.issubset(existing_ids):
                print("  skip: all owner matches already labeled")
                continue

            match_ids = _pick_unlabeled_match_ids(owner_candidates, existing_ids, args.cards_per_summoner)
            if not match_ids:
                print("  skip: no unlabeled DB matches")
                continue

            try:
                cards = crawler.crawl_match_ids(
                    match_ids,
                    page_url=make_deeplol_url(target.game_name, target.tag_line),
                )
            except Exception as exc:
                print(f"  skip: DeepLOL crawl failed: {exc}")
                continue

            print(f"  fetched DB match cards={len(cards)}/{len(match_ids)}")

            for card in cards:
                owner = match_owner_card(card, owner_candidates, card.card_index)
                if owner is None:
                    print(f"  unmatched card index={card.card_index} kda={card.kills}/{card.deaths}/{card.assists}")
                    continue

                rows = _build_rows_for_card(database, card, owner)
                for row in rows:
                    participant_id = int(row["match_participant_id"])
                    if participant_id in existing_ids:
                        continue

                    writer.writerow(pick_csv_columns(row))
                    existing_ids.add(participant_id)
                    written += 1

                    if written % 100 == 0:
                        file.flush()
                        print(f"  csv rows={written}")

                    if written >= args.target_rows:
                        break

                if written >= args.target_rows:
                    break

    print(f"done: {output_path} rows={written}")


def _collect_direct_unlabeled(
    *,
    database: ArcaneDatabase,
    crawler: DeepLolCrawler,
    output_path: Path,
    existing_ids: set[int],
    written: int,
    target_rows: int,
    batch_size: int,
) -> None:
    all_participants = database.fetch_all_match_participants()
    participants_by_match_id: dict[str, list[dict]] = {}
    match_order: list[str] = []

    for participant in all_participants:
        match_id = participant.get("match_riot_id")
        if not match_id:
            continue
        if match_id not in participants_by_match_id:
            participants_by_match_id[match_id] = []
            match_order.append(match_id)
        participants_by_match_id[match_id].append(participant)

    unlabeled_match_ids = [
        match_id
        for match_id in match_order
        if any(int(row["match_participant_id"]) not in existing_ids for row in participants_by_match_id[match_id])
    ]

    print(
        f"direct unlabeled matches={len(unlabeled_match_ids)}, "
        f"existing csv rows={written}, target rows={target_rows}"
    )

    with output_path.open("a", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_COLUMNS)
        if output_path.stat().st_size == 0:
            writer.writeheader()

        for batch_start in range(0, len(unlabeled_match_ids), batch_size):
            if written >= target_rows:
                break

            batch = unlabeled_match_ids[batch_start : batch_start + batch_size]
            cards = crawler.crawl_match_ids(batch)
            print(
                f"  batch {batch_start // batch_size + 1}: "
                f"fetched cards={len(cards)}/{len(batch)}"
            )

            for card in cards:
                if not card.match_riot_id:
                    continue

                match_participants = participants_by_match_id.get(card.match_riot_id, [])
                if not match_participants:
                    continue

                for detail in card.participants:
                    matched = match_detail_participant(detail, match_participants)
                    if matched is None:
                        continue

                    participant_id = int(matched["match_participant_id"])
                    if participant_id in existing_ids:
                        continue

                    writer.writerow(
                        pick_csv_columns(
                            _merge_label(
                                card,
                                matched,
                                detail.score,
                                detail.rank,
                                detail.participant_index,
                            )
                        )
                    )
                    existing_ids.add(participant_id)
                    written += 1

                    if written % 100 == 0:
                        print(f"  csv rows={written}")

                    if written >= target_rows:
                        break

                if written >= target_rows:
                    break

            file.flush()

    print(f"done: {output_path} rows={written}")


def _build_rows_for_card(database: ArcaneDatabase, card, owner: dict) -> list[dict]:
    rows: list[dict] = []

    # 상세 참가자 점수를 안정적으로 뽑은 경우에는 한 경기에서 최대 10명 라벨을 저장한다.
    if card.participants:
        match_participants = database.fetch_match_participants(int(owner["match_db_id"]))
        for detail in card.participants:
            matched = match_detail_participant(detail, match_participants)
            if matched is None:
                continue
            rows.append(_merge_label(card, matched, detail.score, detail.rank, detail.participant_index))

    # 상세 파싱에 실패하면 검색 대상 소환사의 카드 점수라도 저장한다.
    if not rows and card.owner_score is not None:
        rows.append(_merge_label(card, owner, card.owner_score, card.owner_rank, 0))

    return rows


def _pick_unlabeled_match_ids(owner_candidates: list[dict], existing_ids: set[int], limit: int) -> list[str]:
    match_ids: list[str] = []
    seen: set[str] = set()

    for candidate in owner_candidates:
        participant_id = int(candidate["match_participant_id"])
        match_id = candidate.get("match_riot_id")
        if participant_id in existing_ids or not match_id or match_id in seen:
            continue

        seen.add(match_id)
        match_ids.append(match_id)
        if len(match_ids) >= limit:
            break

    return match_ids


def _merge_label(card, feature_row: dict, score: int, rank: int | None, participant_index: int | None) -> dict:
    merged = dict(feature_row)
    merged.update(
        {
            "label_source": "DEEPLOL",
            "source_score": score,
            "source_rank": rank,
            "source_page_url": card.page_url,
            "source_card_index": card.card_index,
            "source_participant_index": participant_index,
        }
    )
    return merged


def _load_existing_ids(output_path: Path) -> set[int]:
    if not output_path.exists():
        return set()

    with output_path.open("r", newline="", encoding="utf-8") as file:
        reader = csv.DictReader(file)
        return {
            int(row["match_participant_id"])
            for row in reader
            if row.get("match_participant_id")
        }


if __name__ == "__main__":
    main()
