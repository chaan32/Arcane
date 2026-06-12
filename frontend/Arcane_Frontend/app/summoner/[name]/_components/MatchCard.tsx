"use client";

import { memo, useMemo } from "react";
import type { Spell } from "@/types/spell";
import type { Rune } from "@/types/rune";
import {
  formatAiScoreRank,
  formatGameDuration,
  getAiScoreRank,
  getParticipantSummaryLabel,
  getParticipantTierBadgeClassName,
  getParticipantTierShortLabel,
  getTimeAgo,
} from "../_lib/summonerFormatters";
import { MatchDetailPanel } from "./MatchDetailPanel";
import type {
  Match,
  MatchParticipant,
  MatchPlayerIndex,
} from "../_types/summonerTypes";
import {
  getChampionIconUrl,
  getItemIconUrl,
  getRuneIconUrl,
  getSpellIconUrl,
} from "../_lib/summonerImageUrls";
import { MatchImage } from "./MatchImage";

type MatchCardProps = {
  isExpanded: boolean;
  match: Match;
  queueName: string;
  rune: Record<number, Rune> | null;
  spell: Record<number, Spell> | null;
  onSummonerClick: (gameName: string, tagLine: string) => void;
  onToggleExpand: (matchId: string) => void;
};

type MatchParticipantSummaryListProps = {
  currentPuuid: string;
  gameVersion?: string | null;
  playerIndexes: readonly MatchPlayerIndex[];
  players: Match["participants"];
  onSummonerClick: (gameName: string, tagLine: string) => void;
};

const TEAM_ONE_PLAYERS = [0, 1, 2, 3, 4] as const;
const TEAM_TWO_PLAYERS = [5, 6, 7, 8, 9] as const;

const getMatchItems = (match: Match): number[] => [
  match.myData.item0,
  match.myData.item1,
  match.myData.item2,
  match.myData.item3,
  match.myData.item4,
  match.myData.item5,
  match.myData.item6,
];

const getParticipant = (
  players: Match["participants"],
  index: MatchPlayerIndex
) => players[`player${index}`];

const getMultiKillLabel = (match: Match) => {
  if (match.myData.pentaKills > 0) return "펜타 킬";
  if (match.myData.quadraKills > 0) return "쿼드라 킬";
  if (match.myData.tripleKills > 0) return "트리플 킬";
  if (match.myData.doubleKills > 0) return "더블 킬";

  return "";
};

const getMatchTone = (isWin: boolean) =>
  isWin
    ? {
        card: "border-[#d7e8ff]/80 bg-white/95 ring-[#d7e8ff]/80",
        summary: "border-l-[#2f80ed] bg-[#f4f9ff] hover:bg-[#edf5ff]",
        label: "text-[#2f80ed]",
        muted: "text-[#6f8aad]",
        scoreBox: "bg-[#edf5ff]",
        scoreText: "text-[#2f80ed]",
        expand: "bg-[#e8f2ff] text-[#2f80ed] hover:bg-[#dbeaff]",
      }
    : {
        card: "border-[#ffd5de]/80 bg-white/95 ring-[#ffd5de]/80",
        summary: "border-l-[#ff5f7e] bg-[#fff4f6] hover:bg-[#ffedf2]",
        label: "text-[#ff4f73]",
        muted: "text-[#9b6a7a]",
        scoreBox: "bg-[#fff0f4]",
        scoreText: "text-[#ff4f73]",
        expand: "bg-[#ffe8ee] text-[#ff4f73] hover:bg-[#ffdce6]",
      };

export const MatchCard = memo(function MatchCard({
  isExpanded,
  match,
  queueName,
  rune,
  spell,
  onSummonerClick,
  onToggleExpand,
}: MatchCardProps) {
  const allParticipants = useMemo(
    () => Object.values(match.participants),
    [match.participants]
  );
  const isMyTeamOne = TEAM_ONE_PLAYERS.some(
    (index) => getParticipant(match.participants, index).puuid === match.myData.puuid
  );
  const team1Lost = isMyTeamOne ? !match.myData.win : match.myData.win;
  const myAiScoreRank = getAiScoreRank(allParticipants, match.myData.puuid);
  const matchTone = getMatchTone(match.myData.win);
  const gameVersion = match.metaData.gameVersion;

  return (
    <div
      className={`flex flex-col overflow-hidden rounded-[1.5rem] border shadow-[0_18px_46px_rgba(98,56,77,0.1)] ring-1 ${matchTone.card}`}
    >
      <div
        className={`flex cursor-pointer items-center justify-between gap-4 border-l-[0.375rem] p-4 transition-colors ${matchTone.summary}`}
      >
        <div className="flex w-24 shrink-0 flex-col items-start justify-center gap-1">
          <div className={`text-sm font-bold ${matchTone.label}`}>
            {queueName}
          </div>
          <div className={`text-xs font-medium ${matchTone.muted}`}>
            {getTimeAgo(match.metaData.gameEndTimestamp)}
          </div>
          <div className="flex gap-1 text-xs">
            <div className={`font-semibold ${matchTone.label}`}>
              {match.myData.win ? "승리" : "패배"}
            </div>
            <div className={matchTone.muted}>
              {formatGameDuration(match.metaData.gameDuration)}
            </div>
          </div>
        </div>
        <div className="flex min-w-0 flex-1 items-center justify-between gap-5">
          <div className="flex flex-col">
            <div className="flex gap-3">
              <div className="flex items-center gap-1">
                <div>
                  <MatchImage
                    src={getChampionIconUrl(
                      match.myData.championNameEn,
                      gameVersion
                    )}
                    alt="profile"
                    width={52}
                    height={52}
                    className="rounded-2xl"
                  />
                </div>
                <div className="flex flex-col gap-1">
                  <MatchImage
                    src={getSpellIconUrl(
                      spell?.[match.myData.summoner1Id]?.imageFull,
                      gameVersion
                    )}
                    alt="summoner spell 1"
                    width={24}
                    height={24}
                    className="rounded-lg"
                    placeholderClassName="rounded-lg bg-[#f8e8f0]/80"
                  />
                  <MatchImage
                    src={getSpellIconUrl(
                      spell?.[match.myData.summoner2Id]?.imageFull,
                      gameVersion
                    )}
                    alt="summoner spell 2"
                    width={24}
                    height={24}
                    className="rounded-lg"
                    placeholderClassName="rounded-lg bg-[#f8e8f0]/80"
                  />
                </div>
                <div className="flex flex-col gap-1">
                  <MatchImage
                    src={getRuneIconUrl(rune?.[match.myData.primaryStyle]?.icon)}
                    alt="primary rune"
                    width={24}
                    height={24}
                    className="rounded-lg"
                    placeholderClassName="rounded-lg bg-[#f8e8f0]/80"
                  />
                  <MatchImage
                    src={getRuneIconUrl(rune?.[match.myData.subStyle]?.icon)}
                    alt="sub rune"
                    width={24}
                    height={24}
                    className="rounded-lg"
                    placeholderClassName="rounded-lg bg-[#f8e8f0]/80"
                  />
                </div>
              </div>

              <div className="flex flex-col justify-center">
                <div className="flex gap-1 text-lg font-bold">
                  <span>{match.myData.kills}</span>
                  <span>/</span>
                  <span className="text-[#ff4f73]">
                    {match.myData.deaths}
                  </span>
                  <span>/</span>
                  <span>{match.myData.assists}</span>
                </div>
                <p className={`text-xs font-semibold ${matchTone.muted}`}>
                  {match.myData.kda ? match.myData.kda.toFixed(2) : ""}
                  KDA
                </p>
                <div className={`mt-1 text-xs ${matchTone.muted}`}>
                  {getMultiKillLabel(match)}
                </div>
              </div>
            </div>
            <div className="mt-2 flex gap-1">
              {getMatchItems(match).map((itemId, index) =>
                itemId > 0 ? (
                  <MatchImage
                    key={`${match.metaData.matchId}-item-${index}`}
                    src={getItemIconUrl(itemId, gameVersion)}
                    alt={`item ${index + 1}`}
                    width={22}
                    height={22}
                    className="h-[22px] w-[22px] rounded-md border border-white/80 bg-white shadow-[0_4px_10px_rgba(98,56,77,0.1)]"
                  />
                ) : (
                  <div
                    key={`${match.metaData.matchId}-empty-item-${index}`}
                    className="h-[22px] w-[22px] rounded-md border border-white/80 bg-[#f8e8f0]/80 shadow-inner"
                    aria-label={`empty item ${index + 1}`}
                  />
                )
              )}
            </div>
          </div>
          <div
            className={`flex w-[6.5rem] shrink-0 flex-col items-center justify-center rounded-2xl px-3 py-3 text-center shadow-[inset_0_1px_0_rgba(255,255,255,0.9)] ${matchTone.scoreBox}`}
          >
            <h5
              className={`whitespace-nowrap text-sm font-bold leading-none ${matchTone.muted}`}
            >
              AI-Score
            </h5>
            <div
              className={`mt-1 text-3xl font-black leading-none ${matchTone.scoreText}`}
            >
              {match.myData.ourScore}
            </div>
            <div
              className={`mt-1 whitespace-nowrap text-xs font-semibold ${matchTone.muted}`}
            >
              {formatAiScoreRank(myAiScoreRank)}
            </div>
          </div>
        </div>

        <MatchParticipantSummaryList
          currentPuuid={match.myData.puuid}
          gameVersion={gameVersion}
          playerIndexes={TEAM_ONE_PLAYERS}
          players={match.participants}
          onSummonerClick={onSummonerClick}
        />
        <MatchParticipantSummaryList
          currentPuuid={match.myData.puuid}
          gameVersion={gameVersion}
          playerIndexes={TEAM_TWO_PLAYERS}
          players={match.participants}
          onSummonerClick={onSummonerClick}
        />

        <button
          type="button"
          onClick={() => onToggleExpand(match.metaData.matchId)}
          className={`flex h-12 w-12 items-center justify-center rounded-2xl transition-colors ${matchTone.expand}`}
        >
          <svg
            className={`h-4 w-4 transition-transform ${
              isExpanded ? "rotate-180" : ""
            }`}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </button>
      </div>

      {isExpanded && (
        <MatchDetailPanel
          match={match}
          rune={rune}
          spell={spell}
          team1Lost={team1Lost}
          onSummonerClick={onSummonerClick}
        />
      )}
    </div>
  );
});

const MatchParticipantSummaryList = memo(function MatchParticipantSummaryList({
  currentPuuid,
  gameVersion,
  playerIndexes,
  players,
  onSummonerClick,
}: MatchParticipantSummaryListProps) {
  return (
    <div className="flex w-60 flex-col gap-1 text-xs text-[#7d5368]">
      {playerIndexes.map((playerIndex) => {
        const player: MatchParticipant = getParticipant(players, playerIndex);
        const participantSummaryLabel = getParticipantSummaryLabel(player);
        const isCurrentSummoner = player.puuid === currentPuuid;

        return (
          <button
            key={playerIndex}
            type="button"
            className={`group flex min-w-0 items-center gap-2 rounded-xl border px-2 py-1 text-left shadow-[0_5px_14px_rgba(98,56,77,0.06)] transition-all hover:-translate-y-0.5 hover:border-[#f5a9c8] hover:bg-white hover:shadow-[0_8px_18px_rgba(205,79,134,0.12)] ${
              isCurrentSummoner
                ? "border-[#f5a9c8] bg-[#fff0f7]"
                : "border-white/80 bg-white/70"
            }`}
            title={`${player.gameName}#${player.tagLine}`}
            onClick={(event) => {
              event.stopPropagation();
              onSummonerClick(player.gameName, player.tagLine);
            }}
          >
            <div className="shrink-0">
              <MatchImage
                src={getChampionIconUrl(player.championNameEn, gameVersion)}
                alt="profile"
                width={22}
                height={22}
                className="rounded-lg"
              />
            </div>
            <span className={getParticipantTierBadgeClassName(player)}>
              {getParticipantTierShortLabel(player)}
            </span>
            <span className="min-w-0 flex-1">
              <span
                className={`block truncate text-[13px] font-black leading-tight tracking-normal ${
                  isCurrentSummoner
                    ? "text-[#e75491]"
                    : "text-[#69324b] group-hover:text-[#e75491]"
                }`}
              >
                {participantSummaryLabel}
              </span>
              <span className="block truncate text-[10px] font-extrabold leading-tight text-[#a76886]">
                #{player.tagLine}
              </span>
            </span>
          </button>
        );
      })}
    </div>
  );
});
