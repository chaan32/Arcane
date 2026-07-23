"use client";

import { memo, useMemo } from "react";
import { ChevronDown } from "lucide-react";
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
  title: string;
  onSummonerClick: (gameName: string, tagLine: string) => void;
};

const TEAM_ONE_PLAYERS = [0, 1, 2, 3, 4] as const;
const TEAM_TWO_PLAYERS = [5, 6, 7, 8, 9] as const;

const getMatchItems = (match: Match): number[] => [
  match.myData.item0,
  match.myData.item1,
  match.myData.item2,
  match.myData.item6,
  match.myData.item3,
  match.myData.item4,
  match.myData.item5,
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
        chip: "border-[#bdd8fb] bg-white/75 text-[#4e79ad]",
        resultChip: "border-[#9dc8fb] bg-[#e8f3ff] text-[#2f80ed]",
        expand: "bg-[#e8f2ff] text-[#2f80ed] hover:bg-[#dbeaff]",
      }
    : {
        card: "border-[#ffd5de]/80 bg-white/95 ring-[#ffd5de]/80",
        summary: "border-l-[#ff5f7e] bg-[#fff4f6] hover:bg-[#ffedf2]",
        label: "text-[#ff4f73]",
        muted: "text-[#9b6a7a]",
        scoreBox: "bg-[#fff0f4]",
        scoreText: "text-[#ff4f73]",
        chip: "border-[#ffc5d2] bg-white/75 text-[#9b6a7a]",
        resultChip: "border-[#ffaac0] bg-[#ffe8ee] text-[#ff4f73]",
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
    (index) =>
      getParticipant(match.participants, index).puuid === match.myData.puuid
  );
  const team1Lost = isMyTeamOne ? !match.myData.win : match.myData.win;
  const myAiScoreRank = getAiScoreRank(allParticipants, match.myData.puuid);
  const matchTone = getMatchTone(match.myData.win);
  const gameVersion = match.metaData.gameVersion;
  const hasLegendaryAiScore = match.myData.ourScore >= 95;

  return (
    <div
      className={`flex flex-col overflow-hidden rounded-[1.5rem] border shadow-[0_18px_46px_rgba(98,56,77,0.1)] ring-1 ${matchTone.card}`}
    >
      <div
        className={`relative grid cursor-pointer grid-cols-1 gap-4 border-l-[0.375rem] p-4 transition-colors lg:grid-cols-2 xl:grid-cols-[minmax(39rem,1fr)_13.5rem_13.5rem_2.75rem] xl:items-stretch xl:gap-3 ${matchTone.summary}`}
      >
        <div className="grid min-w-0 grid-cols-1 gap-4 pr-14 sm:grid-cols-[minmax(18rem,1fr)_minmax(9rem,auto)_6.75rem] sm:items-center lg:col-span-2 xl:col-span-1 xl:h-full xl:grid-rows-[auto_1fr] xl:border-r xl:border-[#dbe8f7]/80 xl:pr-4">
          <div className="flex flex-wrap items-center gap-1.5 sm:col-span-3">
            <span
              className={`rounded-full border px-3 py-1.5 text-xs font-black shadow-[0_4px_10px_rgba(98,56,77,0.05)] ${matchTone.chip}`}
            >
              {queueName}
            </span>
            <span
              className={`rounded-full border px-3 py-1.5 text-xs font-bold shadow-[0_4px_10px_rgba(98,56,77,0.05)] ${matchTone.chip}`}
            >
              {getTimeAgo(match.metaData.gameEndTimestamp)}
            </span>
            <span
              className={`rounded-full border px-3 py-1.5 text-xs font-black shadow-[0_4px_10px_rgba(98,56,77,0.05)] ${matchTone.resultChip}`}
            >
              {match.myData.win ? "승리" : "패배"}
              <span className="mx-1 opacity-50">·</span>
              {formatGameDuration(match.metaData.gameDuration)}
            </span>
          </div>

          <div className="flex min-w-0 items-center gap-4">
            <div className="flex items-center gap-2">
              <div className="relative shrink-0">
                <MatchImage
                  src={getChampionIconUrl(
                    match.myData.championNameEn,
                    gameVersion
                  )}
                  alt={`${match.myData.championNameEn} 챔피언`}
                  width={86}
                  height={86}
                  className="h-[86px] w-[86px] rounded-[1.4rem] border-2 border-white object-cover shadow-[0_10px_24px_rgba(98,56,77,0.16)]"
                />
                <span className="absolute -bottom-1 -right-1 flex h-7 min-w-7 items-center justify-center rounded-full border-2 border-white bg-[#69324b] px-1 text-[11px] font-black text-white shadow-md">
                  {match.myData.champLevel}
                </span>
              </div>
              <div className="flex items-center gap-1">
                <div className="flex flex-col gap-1">
                  <MatchImage
                    src={getSpellIconUrl(
                      spell?.[match.myData.summoner1Id]?.imageFull,
                      gameVersion
                    )}
                    alt="소환사 주문 1"
                    width={30}
                    height={30}
                    className="rounded-[0.6rem]"
                    placeholderClassName="rounded-lg bg-[#f8e8f0]/80"
                  />
                  <MatchImage
                    src={getSpellIconUrl(
                      spell?.[match.myData.summoner2Id]?.imageFull,
                      gameVersion
                    )}
                    alt="소환사 주문 2"
                    width={30}
                    height={30}
                    className="rounded-[0.6rem]"
                    placeholderClassName="rounded-lg bg-[#f8e8f0]/80"
                  />
                </div>
                <div className="flex flex-col gap-1">
                  <MatchImage
                    src={getRuneIconUrl(
                      rune?.[match.myData.primaryStyle]?.icon
                    )}
                    alt="주 룬"
                    width={30}
                    height={30}
                    className="rounded-[0.6rem]"
                    placeholderClassName="rounded-lg bg-[#f8e8f0]/80"
                  />
                  <MatchImage
                    src={getRuneIconUrl(rune?.[match.myData.subStyle]?.icon)}
                    alt="보조 룬"
                    width={30}
                    height={30}
                    className="rounded-[0.6rem]"
                    placeholderClassName="rounded-lg bg-[#f8e8f0]/80"
                  />
                </div>
              </div>
            </div>

            <div className="flex min-w-0 flex-col justify-center">
              <div className="flex gap-1 text-[1.65rem] font-black leading-none text-[#69324b]">
                <span>{match.myData.kills}</span>
                <span className="opacity-45">/</span>
                <span className="text-[#ff4f73]">{match.myData.deaths}</span>
                <span className="opacity-45">/</span>
                <span>{match.myData.assists}</span>
              </div>
              <p className={`mt-2 text-sm font-bold ${matchTone.muted}`}>
                {match.myData.kda ? match.myData.kda.toFixed(2) : ""}
                KDA
              </p>
              <div className={`mt-1 min-h-4 text-xs font-bold ${matchTone.muted}`}>
                {getMultiKillLabel(match)}
              </div>
            </div>
          </div>

          <div className="grid min-w-0 grid-cols-4 gap-1.5 justify-self-start sm:justify-self-center">
            {getMatchItems(match).map((itemId, index) =>
              itemId > 0 ? (
                <MatchImage
                  key={`${match.metaData.matchId}-item-${index}`}
                  src={getItemIconUrl(itemId, gameVersion)}
                  alt={`item ${index + 1}`}
                  width={34}
                  height={34}
                  className="h-[34px] w-[34px] rounded-[0.7rem] border border-white/80 bg-white shadow-[0_5px_12px_rgba(98,56,77,0.11)]"
                />
              ) : (
                <div
                  key={`${match.metaData.matchId}-empty-item-${index}`}
                  className="h-[34px] w-[34px] rounded-[0.7rem] border border-white/80 bg-[#f8e8f0]/80 shadow-inner"
                  aria-label={`empty item ${index + 1}`}
                />
              )
            )}
          </div>

          <div
            className={`w-[6.75rem] shrink-0 justify-self-start rounded-[1.1rem] sm:justify-self-end ${
              hasLegendaryAiScore ? "ai-score-legendary p-[2px]" : ""
            }`}
          >
            <div
              className={`relative z-10 flex min-h-[5.5rem] flex-col items-center justify-center rounded-[1rem] px-3 py-2.5 text-center shadow-[inset_0_1px_0_rgba(255,255,255,0.9)] ${matchTone.scoreBox}`}
            >
              <h5
                className={`whitespace-nowrap text-[11px] font-black leading-none ${matchTone.muted}`}
              >
                AI-SCORE
              </h5>
              <div
                className={`mt-1 text-[2.1rem] font-black leading-none ${matchTone.scoreText} ${
                  hasLegendaryAiScore ? "ai-score-legendary-text" : ""
                }`}
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
        </div>

        <MatchParticipantSummaryList
          currentPuuid={match.myData.puuid}
          gameVersion={gameVersion}
          playerIndexes={TEAM_ONE_PLAYERS}
          players={match.participants}
          title={isMyTeamOne ? "우리 팀" : "상대 팀"}
          onSummonerClick={onSummonerClick}
        />
        <MatchParticipantSummaryList
          currentPuuid={match.myData.puuid}
          gameVersion={gameVersion}
          playerIndexes={TEAM_TWO_PLAYERS}
          players={match.participants}
          title={isMyTeamOne ? "상대 팀" : "우리 팀"}
          onSummonerClick={onSummonerClick}
        />

        <button
          type="button"
          onClick={() => onToggleExpand(match.metaData.matchId)}
          className={`absolute right-4 top-4 flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl transition-colors xl:static xl:self-center ${matchTone.expand}`}
          aria-label={isExpanded ? "전적 상세 닫기" : "전적 상세 보기"}
        >
          <ChevronDown
            className={`h-5 w-5 transition-transform ${
              isExpanded ? "rotate-180" : ""
            }`}
          />
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
  title,
  onSummonerClick,
}: MatchParticipantSummaryListProps) {
  return (
    <div className="flex w-full min-w-0 flex-col gap-1 text-xs text-[#7d5368]">
      <div className="flex h-5 items-center gap-1.5 px-1 text-[10px] font-black tracking-normal text-[#7890ad]">
        <span className="h-1.5 w-1.5 rounded-full bg-[#82b8f4]" />
        {title}
      </div>
      {playerIndexes.map((playerIndex) => {
        const player: MatchParticipant = getParticipant(players, playerIndex);
        const participantSummaryLabel = getParticipantSummaryLabel(player);
        const isCurrentSummoner = player.puuid === currentPuuid;

        return (
          <button
            key={playerIndex}
            type="button"
            className={`group flex min-w-0 items-center gap-2 rounded-[0.75rem] border px-2 py-1 text-left shadow-[0_4px_12px_rgba(98,56,77,0.05)] transition-all hover:-translate-y-0.5 hover:border-[#f5a9c8] hover:bg-white hover:shadow-[0_7px_16px_rgba(205,79,134,0.11)] ${
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
                width={26}
                height={26}
                className="h-[26px] w-[26px] rounded-[0.55rem]"
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
