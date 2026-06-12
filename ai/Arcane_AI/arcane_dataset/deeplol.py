from __future__ import annotations

import re
import time
from dataclasses import dataclass, field
from typing import Any
from urllib.parse import quote

import requests
from playwright.sync_api import Page, sync_playwright


DEEPLOL_API_BASE = "https://b2c-api-cdn.deeplol.gg"
PLATFORM_ID = "KR"

QUEUE_BY_TEXT = {
    "솔로랭크": 420,
    "자유랭크": 440,
    "일반": 430,
    "칼바람": 450,
    "Ranked Solo": 420,
    "Ranked Flex": 440,
    "Normal": 430,
    "ARAM": 450,
}


@dataclass
class ScrapedParticipantScore:
    score: int
    rank: int | None = None
    game_name: str | None = None
    tag_line: str | None = None
    champion_name: str | None = None
    kills: int | None = None
    deaths: int | None = None
    assists: int | None = None
    participant_index: int | None = None


@dataclass
class ScrapedMatchCard:
    card_index: int
    page_url: str
    match_riot_id: str | None
    raw_text: str
    queue_id: int | None
    win: bool | None
    duration_seconds: int | None
    kills: int | None
    deaths: int | None
    assists: int | None
    owner_score: int | None
    owner_rank: int | None
    champion_names: list[str] = field(default_factory=list)
    participants: list[ScrapedParticipantScore] = field(default_factory=list)


def make_deeplol_url(game_name: str, tag_line: str, region: str = "kr") -> str:
    slug = f"{game_name.replace(' ', '')}-{tag_line}".lower()
    return f"https://www.deeplol.gg/summoner/{region}/{quote(slug)}"


def parse_kda(text: str) -> tuple[int | None, int | None, int | None]:
    match = re.search(r"(\d{1,2})\s*/\s*(\d{1,2})\s*/\s*(\d{1,2})", text)
    if not match:
        return None, None, None
    return int(match.group(1)), int(match.group(2)), int(match.group(3))


def parse_duration_seconds(text: str) -> int | None:
    match = re.search(r"(\d{1,2}):(\d{2})", text)
    if not match:
        return None
    return int(match.group(1)) * 60 + int(match.group(2))


def parse_score(text: str) -> int | None:
    match = re.search(r"AI-?\s*Score\s*(\d{1,3})", text, re.IGNORECASE)
    if match:
        return int(match.group(1))

    lines = [line.strip() for line in text.splitlines() if line.strip()]
    for index, line in enumerate(lines):
        if re.search(r"AI-?\s*Score", line, re.IGNORECASE):
            for candidate in lines[index + 1 : index + 4]:
                if re.fullmatch(r"\d{1,3}", candidate):
                    return int(candidate)
    return None


def parse_rank(text: str) -> int | None:
    match = re.search(r"(\d{1,2})\s*(?:등|st|nd|rd|th)", text)
    return int(match.group(1)) if match else None


def parse_queue_id(text: str) -> int | None:
    for label, queue_id in QUEUE_BY_TEXT.items():
        if label in text:
            return queue_id
    return None


def parse_win(text: str) -> bool | None:
    if "승리" in text or "Win" in text:
        return True
    if "패배" in text or "Lose" in text:
        return False
    return None


class DeepLolCrawler:
    def __init__(
        self,
        *,
        headless: bool = True,
        delay_seconds: float = 2.0,
        timeout_ms: int = 30_000,
    ) -> None:
        self.headless = headless
        self.delay_seconds = delay_seconds
        self.timeout_ms = timeout_ms

    def crawl_summoner(self, game_name: str, tag_line: str, max_cards: int = 20) -> list[ScrapedMatchCard]:
        url = make_deeplol_url(game_name, tag_line)

        try:
            cards = self._crawl_summoner_api(game_name, tag_line, url, max_cards)
            if cards:
                time.sleep(self.delay_seconds)
                return cards[:max_cards]
        except Exception:
            pass

        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(headless=self.headless)
            context = browser.new_context(
                viewport={"width": 1440, "height": 1200},
                user_agent=(
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/148.0.0.0 Safari/537.36"
                ),
            )
            page = context.new_page()
            page.goto(url, wait_until="domcontentloaded", timeout=self.timeout_ms)
            self._wait_for_match_area(page)
            self._scroll_until_cards_loaded(page, max_cards)
            cards = self._extract_cards(page, url)
            self._attach_detail_participants(page, cards, url, max_cards)
            browser.close()

        time.sleep(self.delay_seconds)
        return cards[:max_cards]

    def _crawl_summoner_api(
        self,
        game_name: str,
        tag_line: str,
        page_url: str,
        max_cards: int,
    ) -> list[ScrapedMatchCard]:
        session = requests.Session()
        session.headers.update(
            {
                "User-Agent": (
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/148.0.0.0 Safari/537.36"
                ),
                "Referer": page_url,
                "Accept": "application/json, text/plain, */*",
            }
        )

        puuid = self._fetch_deeplol_puuid(session, game_name, tag_line)
        if not puuid:
            return []

        match_ids = self._fetch_deeplol_match_ids(session, puuid, max_cards)
        cards: list[ScrapedMatchCard] = []

        for index, match_id in enumerate(match_ids[:max_cards]):
            response = session.get(
                f"{DEEPLOL_API_BASE}/match/match-cached",
                params={"match_id": match_id, "platform_id": PLATFORM_ID},
                timeout=self.timeout_ms / 1000,
            )
            response.raise_for_status()
            card = self._parse_api_match(index, page_url, puuid, response.json())
            if card is not None:
                card.match_riot_id = card.match_riot_id or match_id
                cards.append(card)

        return cards

    def crawl_match_ids(self, match_ids: list[str], page_url: str = "") -> list[ScrapedMatchCard]:
        session = requests.Session()
        session.headers.update(
            {
                "User-Agent": (
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/148.0.0.0 Safari/537.36"
                ),
                "Referer": page_url or "https://www.deeplol.gg/",
                "Accept": "application/json, text/plain, */*",
            }
        )

        cards: list[ScrapedMatchCard] = []
        for index, match_id in enumerate(match_ids):
            try:
                response = session.get(
                    f"{DEEPLOL_API_BASE}/match/match-cached",
                    params={"match_id": match_id, "platform_id": PLATFORM_ID},
                    timeout=self.timeout_ms / 1000,
                )
                response.raise_for_status()
                card = self._parse_api_match(index, page_url, None, response.json())
            except Exception:
                continue

            if card is not None:
                card.match_riot_id = card.match_riot_id or match_id
                cards.append(card)

        time.sleep(self.delay_seconds)
        return cards

    def _fetch_deeplol_puuid(self, session: requests.Session, game_name: str, tag_line: str) -> str | None:
        response = session.get(
            f"{DEEPLOL_API_BASE}/summoner/summoner",
            params={
                "riot_id_name": game_name.replace(" ", "").lower(),
                "riot_id_tag_line": tag_line.lower(),
                "platform_id": PLATFORM_ID,
            },
            timeout=self.timeout_ms / 1000,
        )
        response.raise_for_status()
        data = response.json()
        return data.get("summoner_basic_info_dict", {}).get("puu_id")

    def _fetch_deeplol_match_ids(
        self,
        session: requests.Session,
        puuid: str,
        max_cards: int,
    ) -> list[str]:
        match_ids: list[str] = []

        for offset in range(0, max_cards, 20):
            response = session.get(
                f"{DEEPLOL_API_BASE}/match/matches",
                params={
                    "puu_id": puuid,
                    "platform_id": PLATFORM_ID,
                    "offset": offset,
                    "count": 20,
                    "queue_type": "ALL",
                    "champion_id": 0,
                    "only_list": 1,
                    "last_updated_at": 0,
                },
                timeout=self.timeout_ms / 1000,
            )
            response.raise_for_status()
            page_ids = [
                row.get("match_id")
                for row in response.json().get("match_id_list", [])
                if row.get("match_id")
            ]
            match_ids.extend(page_ids)
            if len(page_ids) < 20:
                break

        return match_ids

    def _parse_api_match(
        self,
        card_index: int,
        page_url: str,
        owner_puuid: str | None,
        data: dict[str, Any],
    ) -> ScrapedMatchCard | None:
        match_basic = data.get("match_basic_dict", {})
        participants_json = data.get("participants_list", [])
        owner = None
        if owner_puuid:
            owner = next(
                (participant for participant in participants_json if participant.get("puu_id") == owner_puuid),
                None,
            )

        owner_stats = owner.get("final_stat_dict", {}) if owner else {}
        owner_win = self._api_participant_win(owner, match_basic) if owner else None
        participants = [
            self._parse_api_participant(index, participant)
            for index, participant in enumerate(participants_json)
        ]
        participants = [participant for participant in participants if participant is not None]

        return ScrapedMatchCard(
            card_index=card_index,
            page_url=page_url,
            match_riot_id=match_basic.get("match_id"),
            raw_text="",
            queue_id=self._to_int(match_basic.get("queue_id")),
            win=owner_win,
            duration_seconds=self._to_int(match_basic.get("game_duration")),
            kills=self._to_int(owner_stats.get("kills")),
            deaths=self._to_int(owner_stats.get("deaths")),
            assists=self._to_int(owner_stats.get("assists")),
            owner_score=self._display_score(owner_stats.get("ai_score")),
            owner_rank=self._to_int(owner_stats.get("ai_score_rank")),
            champion_names=[],
            participants=participants,
        )

    def _parse_api_participant(
        self,
        participant_index: int,
        participant: dict[str, Any],
    ) -> ScrapedParticipantScore | None:
        stats = participant.get("final_stat_dict", {})
        score = self._display_score(stats.get("ai_score"))
        if score is None:
            return None

        return ScrapedParticipantScore(
            score=score,
            rank=self._to_int(stats.get("ai_score_rank")),
            game_name=participant.get("riot_id_name"),
            tag_line=participant.get("riot_id_tag_line"),
            kills=self._to_int(stats.get("kills")),
            deaths=self._to_int(stats.get("deaths")),
            assists=self._to_int(stats.get("assists")),
            participant_index=participant_index,
        )

    def _api_participant_win(self, participant: dict[str, Any], match_basic: dict[str, Any]) -> bool | None:
        side = participant.get("side")
        blue_win = match_basic.get("blue_win")
        if side not in {"BLUE", "RED"} or blue_win is None:
            return None
        return bool(blue_win) if side == "BLUE" else not bool(blue_win)

    def _display_score(self, value: Any) -> int | None:
        if value is None:
            return None
        return int(round(float(value)))

    def _to_int(self, value: Any) -> int | None:
        if value is None:
            return None
        return int(value)

    def _wait_for_match_area(self, page: Page) -> None:
        try:
            page.get_by_text("AI-Score", exact=False).first.wait_for(timeout=self.timeout_ms)
        except Exception:
            # DeepLOL은 광고/SPA 로딩 영향이 커서 첫 대기에서 실패할 수 있다.
            # 아래 스크롤 단계에서 한 번 더 DOM을 확인한다.
            pass

    def _scroll_until_cards_loaded(self, page: Page, max_cards: int) -> None:
        last_count = 0
        stable_count = 0

        for _ in range(24):
            count = self._count_text_match_cards(page)
            if count >= max_cards:
                return

            if count == last_count:
                stable_count += 1
            else:
                stable_count = 0
                last_count = count

            if stable_count >= 4 and count > 0:
                return

            page.mouse.wheel(0, 900)
            page.wait_for_timeout(600)

    def _count_text_match_cards(self, page: Page) -> int:
        try:
            text = page.locator("body").inner_text(timeout=2_000)
        except Exception:
            return 0

        lines = [line.strip() for line in text.splitlines() if line.strip()]
        return len(self._find_match_starts(lines))

    def _open_detail_buttons(self, page: Page, max_cards: int) -> None:
        buttons = page.locator('button:has(img[alt="Detail"])').all()
        if not buttons:
            buttons = page.get_by_role("button", name=re.compile("자세히|상세|더보기|Detail")).all()

        for button in buttons[:max_cards]:
            try:
                button.scroll_into_view_if_needed(timeout=3_000)
                button.click(timeout=3_000)
                page.wait_for_timeout(250)
            except Exception:
                continue

    def _attach_detail_participants(
        self,
        page: Page,
        cards: list[ScrapedMatchCard],
        page_url: str,
        max_cards: int,
    ) -> None:
        detail_buttons = page.locator('button:has(img[alt="Detail"])')
        count = min(detail_buttons.count(), len(cards), max_cards)

        for index in range(count):
            try:
                button = detail_buttons.nth(index)
                button.scroll_into_view_if_needed(timeout=3_000)
                button.click(timeout=3_000)
                page.wait_for_timeout(600)

                text = page.locator("body").inner_text(timeout=5_000)
                detailed_cards = self._extract_cards_from_text(text, page_url)
                if index < len(detailed_cards) and detailed_cards[index].participants:
                    cards[index].participants = detailed_cards[index].participants
            except Exception:
                continue

    def _extract_cards(self, page: Page, page_url: str) -> list[ScrapedMatchCard]:
        try:
            text = page.locator("body").inner_text(timeout=5_000)
            text_cards = self._extract_cards_from_text(text, page_url)
            if text_cards:
                return text_cards
        except Exception:
            pass

        raw_cards: list[dict[str, Any]] = page.evaluate(
            """
            () => {
              const hasMatchShape = (text) =>
                text.includes('AI-Score') &&
                /\\d+\\s*\\/\\s*\\d+\\s*\\/\\s*\\d+/.test(text) &&
                /(승리|패배)/.test(text);

              const nodes = Array.from(document.querySelectorAll('*'))
                .filter((el) => hasMatchShape(el.innerText || ''));

              const cards = [];
              const seen = new Set();

              for (const node of nodes) {
                let current = node;
                for (let depth = 0; current && depth < 9; depth += 1, current = current.parentElement) {
                  const text = (current.innerText || '').trim();
                  const rect = current.getBoundingClientRect();

                  if (
                    hasMatchShape(text) &&
                    rect.width >= 420 &&
                    rect.height >= 120 &&
                    rect.top + window.scrollY >= 0
                  ) {
                    const signature = [
                      Math.round(rect.top + window.scrollY / 20),
                      Math.round(rect.left),
                      Math.round(rect.width),
                      text.slice(0, 120)
                    ].join('|');

                    if (!seen.has(signature)) {
                      seen.add(signature);
                      cards.push({
                        text,
                        top: rect.top + window.scrollY,
                        left: rect.left,
                        width: rect.width,
                        height: rect.height,
                        images: Array.from(current.querySelectorAll('img')).map((img) => ({
                          alt: img.alt || '',
                          src: img.currentSrc || img.src || ''
                        }))
                      });
                    }
                    break;
                  }
                }
              }

              return cards.sort((a, b) => a.top - b.top);
            }
            """
        )

        parsed_cards: list[ScrapedMatchCard] = []
        for index, raw in enumerate(raw_cards):
            text = raw.get("text", "")
            kills, deaths, assists = parse_kda(text)
            champion_names = [
                image.get("alt", "").strip()
                for image in raw.get("images", [])
                if image.get("alt", "").strip()
            ]

            parsed_cards.append(
                ScrapedMatchCard(
                    card_index=index,
                    page_url=page_url,
                    match_riot_id=None,
                    raw_text=text,
                    queue_id=parse_queue_id(text),
                    win=parse_win(text),
                    duration_seconds=parse_duration_seconds(text),
                    kills=kills,
                    deaths=deaths,
                    assists=assists,
                    owner_score=parse_score(text),
                    owner_rank=parse_rank(text),
                    champion_names=champion_names,
                    participants=self._parse_participant_scores(text),
                )
            )

        return self._dedupe_cards(parsed_cards)

    def _extract_cards_from_text(self, text: str, page_url: str) -> list[ScrapedMatchCard]:
        lines = [line.strip() for line in text.splitlines() if line.strip()]
        starts = self._find_match_starts(lines)
        cards: list[ScrapedMatchCard] = []

        for card_index, start in enumerate(starts):
            end = starts[card_index + 1] if card_index + 1 < len(starts) else len(lines)
            chunk = lines[start:end]
            raw_text = "\n".join(chunk)

            kills, deaths, assists = parse_kda(raw_text)
            score = parse_score(raw_text)
            if score is None:
                continue

            cards.append(
                ScrapedMatchCard(
                    card_index=card_index,
                    page_url=page_url,
                    match_riot_id=None,
                    raw_text=raw_text,
                    queue_id=parse_queue_id(chunk[0]),
                    win=parse_win(raw_text),
                    duration_seconds=parse_duration_seconds(raw_text),
                    kills=kills,
                    deaths=deaths,
                    assists=assists,
                    owner_score=score,
                    owner_rank=parse_rank(raw_text),
                    champion_names=[],
                    participants=self._parse_detail_participants(chunk),
                )
            )

        return self._dedupe_cards(cards)

    def _find_match_starts(self, lines: list[str]) -> list[int]:
        queue_labels = set(QUEUE_BY_TEXT.keys())
        starts: list[int] = []

        for index, line in enumerate(lines):
            if line not in queue_labels:
                continue

            lookahead = "\n".join(lines[index : index + 18])
            if "AI-Score" not in lookahead:
                continue
            if not re.search(r"\d{1,2}\s*/\s*\d{1,2}\s*/\s*\d{1,2}", lookahead):
                continue
            if not ("Win" in lookahead or "Lose" in lookahead or "승리" in lookahead or "패배" in lookahead):
                continue

            starts.append(index)

        return starts

    def _parse_detail_participants(self, lines: list[str]) -> list[ScrapedParticipantScore]:
        if "AI Analysis" not in lines:
            return []

        participants: list[ScrapedParticipantScore] = []

        for index, line in enumerate(lines):
            if "#" not in line:
                continue

            score_index = self._find_detail_score_index(lines, index)
            if score_index is None:
                continue

            score_line = lines[score_index]
            rank_line = lines[score_index + 1]
            kills, deaths, assists = self._parse_split_kda_lines(lines, score_index + 2)
            if kills is None:
                continue

            game_name, tag_line = self._split_riot_name(line)
            participants.append(
                ScrapedParticipantScore(
                    score=int(score_line),
                    rank=self._parse_compact_rank(rank_line),
                    game_name=game_name,
                    tag_line=tag_line,
                    kills=kills,
                    deaths=deaths,
                    assists=assists,
                    participant_index=len(participants),
                )
            )

        return participants

    def _find_detail_score_index(self, lines: list[str], name_index: int) -> int | None:
        # DeepLOL 상세 표는 소환사명 다음에 표시명이 있기도 하고 없기도 하다.
        # 그래서 고정 위치가 아니라 가까운 범위에서 "점수 + 등수" 패턴을 찾는다.
        for score_index in range(name_index + 1, min(name_index + 4, len(lines) - 1)):
            score_line = lines[score_index]
            rank_line = lines[score_index + 1]
            if re.fullmatch(r"\d{1,3}", score_line) and self._is_rank_line(rank_line):
                return score_index
        return None

    def _parse_split_kda_lines(
        self,
        lines: list[str],
        start: int,
    ) -> tuple[int | None, int | None, int | None]:
        if start + 4 >= len(lines):
            return None, None, None
        if lines[start + 1] != "/" or lines[start + 3] != "/":
            return None, None, None
        if not all(re.fullmatch(r"\d{1,2}", lines[start + offset]) for offset in (0, 2, 4)):
            return None, None, None
        return int(lines[start]), int(lines[start + 2]), int(lines[start + 4])

    def _split_riot_name(self, value: str) -> tuple[str, str | None]:
        game_name, separator, tag_line = value.rpartition("#")
        if not separator:
            return value, None
        return game_name, tag_line

    def _is_rank_line(self, value: str) -> bool:
        return bool(re.fullmatch(r"\d{1,2}(?:st|nd|rd|th|등)", value) or value == "ACE")

    def _parse_compact_rank(self, value: str) -> int | None:
        match = re.match(r"(\d{1,2})", value)
        return int(match.group(1)) if match else None

    def _parse_participant_scores(self, text: str) -> list[ScrapedParticipantScore]:
        # DeepLOL 상세 영역의 DOM 구조가 바뀌어도 버티기 위한 보수적인 파서다.
        # 참가자별 점수를 찾지 못하면 빈 리스트를 반환하고, collector가 검색 대상 소환사 1명 점수만 저장한다.
        blocks = re.split(r"\n{2,}", text)
        participants: list[ScrapedParticipantScore] = []

        for block in blocks:
            if "AI-Score" not in block:
                continue
            score = parse_score(block)
            if score is None:
                continue
            kills, deaths, assists = parse_kda(block)
            participants.append(
                ScrapedParticipantScore(
                    score=score,
                    rank=parse_rank(block),
                    kills=kills,
                    deaths=deaths,
                    assists=assists,
                    participant_index=len(participants),
                )
            )

        return participants

    def _dedupe_cards(self, cards: list[ScrapedMatchCard]) -> list[ScrapedMatchCard]:
        deduped: list[ScrapedMatchCard] = []
        seen: set[tuple] = set()

        for card in cards:
            key = (
                card.queue_id,
                card.win,
                card.duration_seconds,
                card.kills,
                card.deaths,
                card.assists,
                card.owner_score,
                card.owner_rank,
            )
            if key in seen:
                continue
            seen.add(key)
            deduped.append(card)

        return deduped
