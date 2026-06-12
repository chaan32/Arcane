"use client";

import { memo, useMemo, useState } from "react";
import type { Spell } from "@/types/spell";
import type { Rune } from "@/types/rune";
import {
  formatAiScoreRank,
  getAiScoreRank,
  getAiScoreValue,
  getParticipantLabel,
  getParticipantTierBadgeClassName,
  getParticipantTierShortLabel,
  getTierMetaLabel,
} from "../_lib/summonerFormatters";
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
import { MatchBuildPanel } from "./MatchBuildPanel";
import { MatchImage } from "./MatchImage";

type MatchDetailPanelProps = {
  match: Match;
  rune: Record<number, Rune> | null;
  spell: Record<number, Spell> | null;
  team1Lost: boolean;
  onSummonerClick: (gameName: string, tagLine: string) => void;
};

type MatchTeamTableProps = {
  currentPuuid: string;
  gameDuration: number;
  gameVersion?: string | null;
  isLost: boolean;
  playerIndexes: readonly MatchPlayerIndex[];
  players: Match["participants"];
  rune: Record<number, Rune> | null;
  spell: Record<number, Spell> | null;
  teamLabel: string;
  allParticipants: MatchParticipant[];
  onSummonerClick: (gameName: string, tagLine: string) => void;
};

type MatchParticipantRowProps = {
  allParticipants: MatchParticipant[];
  currentPuuid: string;
  gameDuration: number;
  gameVersion?: string | null;
  player: MatchParticipant;
  rune: Record<number, Rune> | null;
  spell: Record<number, Spell> | null;
  onSummonerClick: (gameName: string, tagLine: string) => void;
};

const TEAM_ONE_PLAYERS = [0, 1, 2, 3, 4] as const;
const TEAM_TWO_PLAYERS = [5, 6, 7, 8, 9] as const;
const DETAIL_TABS = [
  { id: "summary", label: "AI 기본 분석" },
  { id: "timeline", label: "시간대별 분석" },
  { id: "build", label: "빌드" },
] as const;

type DetailTabId = (typeof DETAIL_TABS)[number]["id"];

const getParticipant = (
  players: Match["participants"],
  index: MatchPlayerIndex
) => players[`player${index}`];

const getParticipantItems = (player: MatchParticipant) => [
  player.item0,
  player.item1,
  player.item2,
  player.item3,
  player.item4,
  player.item5,
  player.item6,
];

export const MatchDetailPanel = memo(function MatchDetailPanel({
  match,
  rune,
  spell,
  team1Lost,
  onSummonerClick,
}: MatchDetailPanelProps) {
  const [activeTab, setActiveTab] = useState<DetailTabId>("summary");
  const allParticipants = useMemo(
    () => Object.values(match.participants),
    [match.participants]
  );

  return (
    <div className="overflow-hidden border-t border-[#f8dce8] bg-[#fffafd] text-[#69324b]">
      <div className="flex gap-2 border-b border-[#ffe1ed] bg-[#fff7fb] px-4 py-3">
        {DETAIL_TABS.map((tab) => {
          const isActive = activeTab === tab.id;

          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id)}
              className={
                isActive
                  ? "rounded-full bg-[#f45f9c] px-4 py-2 text-sm font-black text-white shadow-[0_10px_20px_rgba(231,84,145,0.18)]"
                  : "rounded-full px-4 py-2 text-sm font-black text-[#a76886] transition-colors hover:bg-[#fff0f7] hover:text-[#e75491]"
              }
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {activeTab === "summary" && (
        <>
          <MatchTeamTable
            allParticipants={allParticipants}
            currentPuuid={match.myData.puuid}
            gameDuration={match.metaData.gameDuration}
            gameVersion={match.metaData.gameVersion}
            isLost={team1Lost}
            playerIndexes={TEAM_ONE_PLAYERS}
            players={match.participants}
            rune={rune}
            spell={spell}
            teamLabel="블루팀"
            onSummonerClick={onSummonerClick}
          />

          <MatchTeamTable
            allParticipants={allParticipants}
            currentPuuid={match.myData.puuid}
            gameDuration={match.metaData.gameDuration}
            gameVersion={match.metaData.gameVersion}
            isLost={!team1Lost}
            playerIndexes={TEAM_TWO_PLAYERS}
            players={match.participants}
            rune={rune}
            spell={spell}
            teamLabel="레드팀"
            onSummonerClick={onSummonerClick}
          />
        </>
      )}

      {activeTab === "timeline" && (
        <div className="p-4">
          <div className="rounded-[1.25rem] border border-[#f8dce8]/80 bg-white/90 p-6 text-sm font-bold text-[#a76886] shadow-[0_16px_36px_rgba(98,56,77,0.08)]">
            시간대별 분석은 아직 준비 중입니다. 빌드 탭에서 타임라인 기반
            아이템/스킬 빌드를 먼저 확인할 수 있습니다.
          </div>
        </div>
      )}

      {activeTab === "build" && <MatchBuildPanel match={match} rune={rune} />}
    </div>
  );
});

const MatchTeamTable = memo(function MatchTeamTable({
  allParticipants,
  currentPuuid,
  gameDuration,
  gameVersion,
  isLost,
  playerIndexes,
  players,
  rune,
  spell,
  teamLabel,
  onSummonerClick,
}: MatchTeamTableProps) {
  return (
    <div className={isLost ? "bg-[#fff4f6]" : "bg-[#f4f9ff]"}>
      <div className="flex items-center gap-2 border-b border-[#f8dce8] px-4 py-3">
        <span
          className={`font-black ${isLost ? "text-[#ff4f73]" : "text-[#2f80ed]"}`}
        >
          {isLost ? "패배" : "승리"}
        </span>
        <span className="text-sm font-bold text-[#a76886]">({teamLabel})</span>
      </div>
      <table className="w-full text-sm text-[#69324b]">
        <thead>
          <tr className="border-b border-[#f8dce8] text-sm font-black text-[#a76886]">
            <th className="w-80 px-5 py-3 text-left"></th>
            <th className="px-3 py-3">AI-Score</th>
            <th className="px-3 py-3">KDA</th>
            <th className="px-3 py-3">피해량</th>
            <th className="px-3 py-3">CS</th>
            <th className="px-3 py-3">와드</th>
            <th className="px-3 py-3">아이템</th>
          </tr>
        </thead>
        <tbody>
          {playerIndexes.map((index) => (
            <MatchParticipantRow
              key={index}
              allParticipants={allParticipants}
              currentPuuid={currentPuuid}
              gameDuration={gameDuration}
              gameVersion={gameVersion}
              player={getParticipant(players, index)}
              rune={rune}
              spell={spell}
              onSummonerClick={onSummonerClick}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
});

const MatchParticipantRow = memo(function MatchParticipantRow({
  allParticipants,
  currentPuuid,
  gameDuration,
  gameVersion,
  player,
  rune,
  spell,
  onSummonerClick,
}: MatchParticipantRowProps) {
  const gameDurationMinutes = gameDuration / 60;
  const csPerMin =
    gameDurationMinutes > 0
      ? (player.totalMinionKills / gameDurationMinutes).toFixed(1)
      : "0.0";
  const primaryRuneId = player.rune?.mainRune?.mainRune?.id;
  const subRuneStyleId = player.rune?.subRune?.styleId;
  const isCurrentSummoner = player.puuid === currentPuuid;

  return (
    <tr
      className={`border-b transition-colors ${
        isCurrentSummoner
          ? "border-[#f45f9c]/65 bg-[#fff0f7] shadow-[inset_0_0_0_2px_rgba(244,95,156,0.34)] hover:bg-[#ffe8f2]"
          : "border-[#f8dce8] hover:bg-white/70"
      }`}
    >
      <td className="px-5 py-3">
        <div className="flex items-center gap-3">
          <div className="relative">
            <MatchImage
              src={getChampionIconUrl(player.championNameEn, gameVersion)}
              alt={player.championNameEn}
              width={44}
              height={44}
              className="rounded-2xl shadow-[0_10px_20px_rgba(98,56,77,0.12)]"
            />
            <span className="absolute -bottom-1.5 -left-1.5 rounded-md bg-white px-1.5 text-[11px] font-black text-[#69324b] shadow-[inset_0_0_0_1px_rgba(248,220,232,0.9),0_6px_14px_rgba(98,56,77,0.12)]">
              {player.champLevel}
            </span>
          </div>
          <div className="flex flex-col gap-1">
            <MatchImage
              src={getSpellIconUrl(
                spell?.[player.summoner1Id]?.imageFull,
                gameVersion
              )}
              alt="spell"
              width={18}
              height={18}
              className="rounded-md"
              placeholderClassName="rounded-md bg-[#f8e8f0]/80"
            />
            <MatchImage
              src={getSpellIconUrl(
                spell?.[player.summoner2Id]?.imageFull,
                gameVersion
              )}
              alt="spell"
              width={18}
              height={18}
              className="rounded-md"
              placeholderClassName="rounded-md bg-[#f8e8f0]/80"
            />
          </div>
          <div className="flex flex-col gap-1">
            <MatchImage
              src={getRuneIconUrl(
                primaryRuneId ? rune?.[primaryRuneId]?.icon : undefined
              )}
              alt="rune"
              width={18}
              height={18}
              className="rounded-md"
              placeholderClassName="rounded-md bg-[#f8e8f0]/80"
            />
            <MatchImage
              src={getRuneIconUrl(
                subRuneStyleId ? rune?.[subRuneStyleId]?.icon : undefined
              )}
              alt="rune"
              width={18}
              height={18}
              className="rounded-md"
              placeholderClassName="rounded-md bg-[#f8e8f0]/80"
            />
          </div>
          <button
            type="button"
            className="group flex w-56 min-w-0 flex-col items-start rounded-xl px-2 py-1 text-left transition-colors hover:bg-[#fff0f7]"
            title={getParticipantLabel(player)}
            onClick={() => onSummonerClick(player.gameName, player.tagLine)}
          >
            <span className="max-w-full truncate text-base font-black leading-tight text-[#69324b] group-hover:text-[#e75491]">
              {player.gameName}
            </span>
            <span className="max-w-full truncate text-xs font-extrabold leading-tight text-[#a76886]">
              #{player.tagLine}
            </span>
            <span className="mt-0.5 flex max-w-full items-center gap-1 text-[11px] font-bold text-[#a76886]">
              <span className={getParticipantTierBadgeClassName(player)}>
                {getParticipantTierShortLabel(player)}
              </span>
              <span className="truncate">{getTierMetaLabel(player)}</span>
            </span>
          </button>
        </div>
      </td>
      <td className="px-3 py-3 text-center">
        <div className="flex flex-col items-center">
          <span className="text-xl font-black text-[#f45f9c]">
            {getAiScoreValue(player)}
          </span>
          <span className="text-xs font-bold text-[#a76886]">
            {formatAiScoreRank(getAiScoreRank(allParticipants, player.puuid))}
          </span>
        </div>
      </td>
      <td className="px-3 py-3 text-center">
        <div className="flex flex-col items-center">
          <span className="text-base font-extrabold">
            {player.kills} /{" "}
            <span className="font-black text-[#ff4f73]">{player.deaths}</span> /{" "}
            {player.assists}
          </span>
          <span className="whitespace-nowrap text-sm font-bold text-[#a76886]">
            {player.kda?.toFixed(2) || "Perfect"}
          </span>
        </div>
      </td>
      <td className="px-3 py-3 text-center">
        <div className="flex flex-col items-center gap-1">
          <span className="text-base font-extrabold">
            {player.totalDamageDealtToChampions?.toLocaleString()}
          </span>
          <div className="h-1.5 w-20 overflow-hidden rounded-full bg-[#ffe1ed]">
            <div
              className="h-full bg-[#ff5f7e]"
              style={{
                width: `${Math.min(
                  100,
                  (player.totalDamageDealtToChampions / 50000) * 100
                )}%`,
              }}
            ></div>
          </div>
        </div>
      </td>
      <td className="px-3 py-3 text-center">
        <div className="flex flex-col items-center">
          <span className="text-base font-extrabold">
            {player.totalMinionKills}
          </span>
          <span className="text-xs font-bold text-[#a76886]">({csPerMin}/분)</span>
        </div>
      </td>
      <td className="px-3 py-3 text-center">
        <div className="flex flex-col items-center">
          <span className="text-base font-black text-[#a76886]">
            {player.visionWardsBoughtInGame}
          </span>
          <span className="text-base font-black text-[#a76886]">
            {player.wardPlaced} / {player.wardKilled}
          </span>
        </div>
      </td>
      <td className="px-3 py-3">
        <div className="flex justify-center gap-1">
          {getParticipantItems(player).map((itemId, index) =>
            itemId > 0 ? (
              <MatchImage
                key={index}
                src={getItemIconUrl(itemId, gameVersion)}
                alt="item"
                width={30}
                height={30}
                className="rounded-xl shadow-[0_8px_16px_rgba(98,56,77,0.12)]"
              />
            ) : (
              <div
                key={index}
                className="h-[30px] w-[30px] rounded-xl bg-[#f8e8f0]/80 shadow-inner"
              ></div>
            )
          )}
        </div>
      </td>
    </tr>
  );
});
