"use client";

import { memo, useEffect, useMemo, useState } from "react";
import { ChevronRight, Loader2, PackageOpen } from "lucide-react";
import type { ChampionInfo } from "@/types/champion";
import type { MatchTimelineEvent } from "@/types/matchTimeline";
import type { Rune } from "@/types/rune";
import { championApi } from "@/services/championApi";
import { fetchMatchTimelineByParticipants } from "@/services/matchTimelineApi";
import { getParticipantLabel } from "../_lib/summonerFormatters";
import type { Match, MatchParticipant } from "../_types/summonerTypes";
import {
  getChampionIconUrl,
  getChampionSpellIconUrl,
  getItemIconUrl,
  getRuneIconUrl,
} from "../_lib/summonerImageUrls";
import { MatchImage } from "./MatchImage";

type MatchBuildPanelProps = {
  match: Match;
  rune: Record<number, Rune> | null;
};

type ItemBuildEntry = {
  action: "purchase" | "sell" | "destroy" | "undo";
  eventIndex: number;
  itemId: number;
  minute: number;
  timestamp: number;
};

type SkillSlot = 1 | 2 | 3 | 4;

type SkillBuildEntry = {
  isSkillMaxed: boolean;
  minute: number;
  order: number;
  rankInSkill: number;
  skillSlot: SkillSlot;
  timestamp: number;
};

type SkillMasterPriorityEntry = {
  count: number;
  firstOrder: number;
  isMaxed: boolean;
  lastOrder: number;
  maxedOrder: number | null;
  skillSlot: SkillSlot;
};

const SKILL_LABEL_BY_SLOT: Record<SkillSlot, "Q" | "W" | "E" | "R"> = {
  1: "Q",
  2: "W",
  3: "E",
  4: "R",
};

const BASIC_SKILL_SLOTS: SkillSlot[] = [1, 2, 3];

const MAX_SKILL_RANK_BY_SLOT: Record<SkillSlot, number> = {
  1: 5,
  2: 5,
  3: 5,
  4: 3,
};

const ITEM_ACTION_LABEL: Record<ItemBuildEntry["action"], string> = {
  purchase: "구매",
  sell: "판매",
  destroy: "소모",
  undo: "취소",
};

const ITEM_ACTION_BADGE: Record<ItemBuildEntry["action"], string> = {
  purchase: "+",
  sell: "-",
  destroy: "×",
  undo: "↺",
};

const ITEM_ACTION_CLASS: Record<ItemBuildEntry["action"], string> = {
  purchase: "bg-[#f45f9c] text-white",
  sell: "bg-[#f3a1bd] text-white",
  destroy: "bg-[#ff6b7f] text-white",
  undo: "bg-[#7d5368] text-white",
};

const MAX_SKILL_LEVEL = 18;

const STAT_RUNE_META: Record<
  number,
  {
    icon: string;
    label: string;
  }
> = {
  5001: {
    icon: "perk-images/StatMods/StatModsHealthScalingIcon.png",
    label: "성장 체력",
  },
  5002: {
    icon: "perk-images/StatMods/StatModsArmorIcon.png",
    label: "방어력",
  },
  5003: {
    icon: "perk-images/StatMods/StatModsMagicResIcon.png",
    label: "마법 저항",
  },
  5005: {
    icon: "perk-images/StatMods/StatModsAttackSpeedIcon.png",
    label: "공격 속도",
  },
  5007: {
    icon: "perk-images/StatMods/StatModsCDRScalingIcon.png",
    label: "스킬 가속",
  },
  5008: {
    icon: "perk-images/StatMods/StatModsAdaptiveForceIcon.png",
    label: "적응형",
  },
  5010: {
    icon: "perk-images/StatMods/StatModsMovementSpeedIcon.png",
    label: "이동 속도",
  },
  5011: {
    icon: "perk-images/StatMods/StatModsHealthPlusIcon.png",
    label: "체력",
  },
  5013: {
    icon: "perk-images/StatMods/StatModsTenacityIcon.png",
    label: "강인함",
  },
};

const championInfoCache = new Map<string, Promise<ChampionInfo>>();

const getCachedChampionInfo = (championNameEn: string) => {
  if (!championInfoCache.has(championNameEn)) {
    championInfoCache.set(
      championNameEn,
      championApi.getChampionByName(championNameEn).catch((error) => {
        championInfoCache.delete(championNameEn);
        throw error;
      })
    );
  }

  return championInfoCache.get(championNameEn)!;
};

const toMinute = (timestamp: number) =>
  Math.max(1, Math.floor(timestamp / 60000));

const getItemIdFromEvent = (event: MatchTimelineEvent) => {
  if (typeof event.itemId === "number" && event.itemId > 0) {
    return event.itemId;
  }

  if (typeof event.beforeId === "number" && event.beforeId > 0) {
    return event.beforeId;
  }

  if (typeof event.afterId === "number" && event.afterId > 0) {
    return event.afterId;
  }

  return 0;
};

const getItemActionFromEvent = (
  event: MatchTimelineEvent
): ItemBuildEntry["action"] | null => {
  if (event.type === "ITEM_PURCHASED") return "purchase";
  if (event.type === "ITEM_SOLD") return "sell";
  if (event.type === "ITEM_DESTROYED") return "destroy";
  if (event.type === "ITEM_UNDO") return "undo";

  return null;
};

const buildItemEntries = (events: MatchTimelineEvent[]) =>
  events
    .map((event, eventIndex): ItemBuildEntry | null => {
      const action = getItemActionFromEvent(event);
      const itemId = getItemIdFromEvent(event);

      if (!action || itemId <= 0) return null;

      return {
        action,
        eventIndex,
        itemId,
        minute: toMinute(event.timestamp),
        timestamp: event.timestamp,
      };
    })
    .filter((entry): entry is ItemBuildEntry => Boolean(entry))
    .sort((a, b) => a.timestamp - b.timestamp);

const buildSkillEntries = (events: MatchTimelineEvent[]) => {
  let order = 0;
  const rankBySkillSlot: Record<SkillSlot, number> = {
    1: 0,
    2: 0,
    3: 0,
    4: 0,
  };

  return events
    .filter(
      (event) =>
        event.type === "SKILL_LEVEL_UP" &&
        event.skillSlot !== undefined &&
        event.skillSlot >= 1 &&
        event.skillSlot <= 4
    )
    .sort((a, b) => a.timestamp - b.timestamp)
    .map((event) => {
      order += 1;
      const skillSlot = event.skillSlot as SkillSlot;
      rankBySkillSlot[skillSlot] += 1;
      const rankInSkill = rankBySkillSlot[skillSlot];

      return {
        isSkillMaxed: rankInSkill === MAX_SKILL_RANK_BY_SLOT[skillSlot],
        minute: toMinute(event.timestamp),
        order,
        rankInSkill,
        skillSlot,
        timestamp: event.timestamp,
      };
    });
};

const buildSkillMasterPriority = (
  entries: SkillBuildEntry[]
): SkillMasterPriorityEntry[] => {
  const summaryBySlot = new Map<SkillSlot, SkillMasterPriorityEntry>();

  BASIC_SKILL_SLOTS.forEach((skillSlot) => {
    summaryBySlot.set(skillSlot, {
      count: 0,
      firstOrder: Number.MAX_SAFE_INTEGER,
      isMaxed: false,
      lastOrder: 0,
      maxedOrder: null,
      skillSlot,
    });
  });

  entries.forEach((entry) => {
    if (!BASIC_SKILL_SLOTS.includes(entry.skillSlot)) return;

    const summary = summaryBySlot.get(entry.skillSlot);
    if (!summary) return;

    summary.count += 1;
    summary.firstOrder = Math.min(summary.firstOrder, entry.order);
    summary.lastOrder = Math.max(summary.lastOrder, entry.order);

    if (entry.rankInSkill === MAX_SKILL_RANK_BY_SLOT[entry.skillSlot]) {
      summary.isMaxed = true;
      summary.maxedOrder = entry.order;
    }
  });

  return Array.from(summaryBySlot.values())
    .filter((summary) => summary.count > 0)
    .sort((a, b) => {
      if (a.isMaxed && b.isMaxed) {
        return (a.maxedOrder ?? 0) - (b.maxedOrder ?? 0);
      }

      if (a.isMaxed !== b.isMaxed) {
        return a.isMaxed ? -1 : 1;
      }

      if (a.count !== b.count) {
        return b.count - a.count;
      }

      return a.firstOrder - b.firstOrder;
    });
};

const groupItemEntriesByMinute = (entries: ItemBuildEntry[]) => {
  const groups = new Map<number, ItemBuildEntry[]>();

  entries.forEach((entry) => {
    groups.set(entry.minute, [...(groups.get(entry.minute) ?? []), entry]);
  });

  return Array.from(groups.entries())
    .map(([minute, items]) => ({ minute, items }))
    .sort((a, b) => a.minute - b.minute);
};

const normalizeTimelineEventsByPuuid = (
  eventsByPuuid: Record<string, MatchTimelineEvent[]> | null | undefined
) =>
  Object.fromEntries(
    Object.entries(eventsByPuuid ?? {}).filter(([, events]) =>
      Array.isArray(events)
    )
  ) as Record<string, MatchTimelineEvent[]>;

const findTimelineKey = ({
  eventsByPuuid,
  fallbackPuuid,
  participants,
  selectedParticipant,
  selectedPuuid,
}: {
  eventsByPuuid: Record<string, MatchTimelineEvent[]>;
  fallbackPuuid: string;
  participants: MatchParticipant[];
  selectedParticipant?: MatchParticipant;
  selectedPuuid: string;
}) => {
  const timelineKeys = Object.keys(eventsByPuuid);
  const candidates = [
    selectedPuuid,
    selectedParticipant?.puuid,
    fallbackPuuid,
  ].filter((puuid): puuid is string => Boolean(puuid));

  for (const candidate of candidates) {
    if (eventsByPuuid[candidate]) return candidate;
  }

  const timelineKeyByLowerPuuid = new Map(
    timelineKeys.map((timelineKey) => [timelineKey.toLowerCase(), timelineKey])
  );

  for (const candidate of candidates) {
    const timelineKey = timelineKeyByLowerPuuid.get(candidate.toLowerCase());
    if (timelineKey) return timelineKey;
  }

  if (selectedParticipant) {
    const participantIndex = participants.findIndex(
      (participant) => participant.puuid === selectedParticipant.puuid
    );

    if (participantIndex >= 0 && timelineKeys[participantIndex]) {
      return timelineKeys[participantIndex];
    }
  }

  return null;
};

export const MatchBuildPanel = memo(function MatchBuildPanel({
  match,
  rune,
}: MatchBuildPanelProps) {
  const participants = useMemo(
    () => Object.values(match.participants),
    [match.participants]
  );
  const [selectedPuuid, setSelectedPuuid] = useState(match.myData.puuid);
  const [timelineEventsByPuuid, setTimelineEventsByPuuid] = useState<
    Record<string, MatchTimelineEvent[]>
  >({});
  const [championInfo, setChampionInfo] = useState<ChampionInfo | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const selectedParticipant =
    participants.find((participant) => participant.puuid === selectedPuuid) ??
    participants.find((participant) => participant.puuid === match.myData.puuid) ??
    participants[0];
  const gameVersion = match.metaData.gameVersion;
  const resolvedTimelineKey = useMemo(
    () =>
      findTimelineKey({
        eventsByPuuid: timelineEventsByPuuid,
        fallbackPuuid: match.myData.puuid,
        participants,
        selectedParticipant,
        selectedPuuid,
      }),
    [
      match.myData.puuid,
      participants,
      selectedParticipant,
      selectedPuuid,
      timelineEventsByPuuid,
    ]
  );
  const timelineEvents = useMemo(
    () =>
      resolvedTimelineKey
        ? timelineEventsByPuuid[resolvedTimelineKey] ?? []
        : [],
    [resolvedTimelineKey, timelineEventsByPuuid]
  );

  const itemEntries = useMemo(
    () => buildItemEntries(timelineEvents),
    [timelineEvents]
  );
  const itemGroups = useMemo(
    () => groupItemEntriesByMinute(itemEntries),
    [itemEntries]
  );
  const skillEntries = useMemo(
    () => buildSkillEntries(timelineEvents),
    [timelineEvents]
  );
  const skillByOrder = useMemo(
    () =>
      skillEntries.reduce<Record<number, SkillBuildEntry>>((acc, entry) => {
        acc[entry.order] = entry;
        return acc;
      }, {}),
    [skillEntries]
  );
  const skillMasterPriority = useMemo(
    () => buildSkillMasterPriority(skillEntries),
    [skillEntries]
  );
  const skillIconBySlot = useMemo(() => {
    const spells = championInfo?.spells ?? [];

    return {
      1: getChampionSpellIconUrl(spells[0]?.imageFull, gameVersion),
      2: getChampionSpellIconUrl(spells[1]?.imageFull, gameVersion),
      3: getChampionSpellIconUrl(spells[2]?.imageFull, gameVersion),
      4: getChampionSpellIconUrl(spells[3]?.imageFull, gameVersion),
    } satisfies Record<SkillSlot, string | null>;
  }, [championInfo, gameVersion]);

  useEffect(() => {
    setSelectedPuuid(match.myData.puuid || participants[0]?.puuid || "");
  }, [match.metaData.matchId, match.myData.puuid, participants]);

  useEffect(() => {
    const controller = new AbortController();

    setIsLoading(true);
    setErrorMessage(null);
    setTimelineEventsByPuuid({});

    fetchMatchTimelineByParticipants({
      matchId: match.metaData.matchId,
      signal: controller.signal,
    })
      .then((eventsByPuuid) => {
        setTimelineEventsByPuuid(normalizeTimelineEventsByPuuid(eventsByPuuid));
      })
      .catch((error) => {
        if (error?.name === "AbortError") return;

        setTimelineEventsByPuuid({});
        setErrorMessage("타임라인 빌드 정보를 가져오지 못했습니다.");
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      });

    return () => controller.abort();
  }, [match.metaData.matchId]);

  useEffect(() => {
    let ignore = false;
    const championNameEn = selectedParticipant?.championNameEn;

    setChampionInfo(null);

    if (!championNameEn) return;

    getCachedChampionInfo(championNameEn)
      .then((info) => {
        if (!ignore) setChampionInfo(info);
      })
      .catch(() => {
        if (!ignore) setChampionInfo(null);
      });

    return () => {
      ignore = true;
    };
  }, [selectedParticipant?.championNameEn]);

  if (!selectedParticipant) {
    return (
      <div className="p-6 text-center text-sm font-bold text-[#a76886]">
        참가자 정보를 찾을 수 없습니다.
      </div>
    );
  }

  return (
    <div className="space-y-4 bg-[#fffafd] p-4">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-[1.25rem] border border-[#f8dce8]/80 bg-white/85 px-4 py-3 shadow-[0_16px_36px_rgba(98,56,77,0.08)]">
        <div className="flex items-center gap-3">
          <MatchImage
            src={getChampionIconUrl(
              selectedParticipant.championNameEn,
              gameVersion
            )}
            alt={selectedParticipant.championNameKo}
            width={42}
            height={42}
            className="rounded-2xl"
          />
          <div>
            <p className="text-sm font-black text-[#69324b]">
              {getParticipantLabel(selectedParticipant)}
            </p>
            <p className="text-xs font-bold text-[#a76886]">
              {selectedParticipant.championNameKo} 빌드 분석
            </p>
          </div>
        </div>

        <select
          value={selectedPuuid}
          onChange={(event) => setSelectedPuuid(event.target.value)}
          className="min-w-64 rounded-full border border-[#ffc9dd] bg-[#fff7fb] px-4 py-2 text-sm font-black text-[#69324b] outline-none transition-colors hover:bg-white focus:border-[#f45f9c]"
        >
          {participants.map((participant) => (
            <option key={participant.puuid} value={participant.puuid}>
              {participant.championNameKo} · {getParticipantLabel(participant)}
            </option>
          ))}
        </select>
      </div>

      {isLoading ? (
        <div className="flex min-h-64 flex-col items-center justify-center gap-3 rounded-[1.25rem] border border-[#f8dce8]/80 bg-white/80 text-[#a76886]">
          <Loader2 className="h-8 w-8 animate-spin text-[#f45f9c]" />
          <p className="text-sm font-black">타임라인 빌드를 불러오는 중입니다.</p>
        </div>
      ) : errorMessage ? (
        <div className="flex min-h-64 flex-col items-center justify-center gap-3 rounded-[1.25rem] border border-[#f8dce8]/80 bg-white/80 text-[#a76886]">
          <PackageOpen className="h-8 w-8 text-[#f45f9c]" />
          <p className="text-sm font-black">{errorMessage}</p>
        </div>
      ) : (
        <>
          <BuildSection title="아이템 빌드">
            {itemGroups.length > 0 ? (
              <div className="flex flex-wrap items-start gap-x-4 gap-y-5">
                {itemGroups.map((group, groupIndex) => (
                  <div
                    key={group.minute}
                    className="flex items-center gap-3"
                  >
                    <div className="flex flex-col items-center gap-2">
                      <div className="flex min-h-12 items-center gap-1 rounded-2xl bg-[#fff0f7] px-2 py-2">
                        {group.items.map((item) => (
                          <div
                            key={`${item.timestamp}-${item.itemId}-${item.action}-${item.eventIndex}`}
                            className="relative"
                            title={`${ITEM_ACTION_LABEL[item.action]} · ${group.minute}분`}
                          >
                            <MatchImage
                              src={getItemIconUrl(item.itemId, gameVersion)}
                              alt={`${item.itemId}`}
                              width={34}
                              height={34}
                              className="rounded-xl border border-white/80 shadow-[0_8px_18px_rgba(98,56,77,0.14)]"
                            />
                            <span
                              className={`absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full text-[10px] font-black ${ITEM_ACTION_CLASS[item.action]}`}
                            >
                              {ITEM_ACTION_BADGE[item.action]}
                            </span>
                          </div>
                        ))}
                      </div>
                      <span className="text-sm font-black text-[#69324b]">
                        {group.minute}분
                      </span>
                    </div>

                    {groupIndex < itemGroups.length - 1 && (
                      <ChevronRight className="mt-4 h-5 w-5 text-[#c88ca8]" />
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <EmptyBuildMessage message="아이템 구매 이벤트가 없습니다." />
            )}
          </BuildSection>

          <BuildSection title="스킬 빌드">
            {skillEntries.length > 0 ? (
              <div className="grid gap-5 xl:grid-cols-[19rem_1fr]">
                <div className="rounded-[1.25rem] bg-[#fff7fb] p-4">
                  <p className="mb-3 text-xs font-black text-[#a76886]">
                    초반 스킬 순서
                  </p>
                  <div className="flex items-center gap-3">
                    {skillEntries.slice(0, 3).map((entry, index) => (
                      <div
                        key={`${entry.order}-${entry.skillSlot}`}
                        className="flex items-center gap-3"
                      >
                        <SkillIcon
                          iconUrl={skillIconBySlot[entry.skillSlot]}
                          skillSlot={entry.skillSlot}
                          size={48}
                        />
                        {index < Math.min(3, skillEntries.length) - 1 && (
                          <ChevronRight className="h-5 w-5 text-[#c88ca8]" />
                        )}
                      </div>
                    ))}
                  </div>

                  <div className="mt-5 border-t border-[#f8dce8]/80 pt-4">
                    <p className="mb-3 text-xs font-black text-[#a76886]">
                      스킬 마스터 순서
                    </p>
                    {skillMasterPriority.length > 0 ? (
                      <div className="space-y-2">
                        {skillMasterPriority.map((entry, index) => (
                          <div
                            key={`skill-master-${entry.skillSlot}`}
                            className="flex items-center justify-between gap-3 rounded-2xl bg-white/80 px-3 py-2 shadow-[0_10px_20px_rgba(98,56,77,0.06)]"
                            title={
                              entry.isMaxed
                                ? `${SKILL_LABEL_BY_SLOT[entry.skillSlot]} ${entry.maxedOrder}레벨에 마스터 완료`
                                : `${SKILL_LABEL_BY_SLOT[entry.skillSlot]} ${entry.count}번 투자`
                            }
                          >
                            <div className="flex items-center gap-2">
                              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[#f45f9c] text-xs font-black text-white">
                                {index + 1}
                              </span>
                              <SkillIcon
                                iconUrl={skillIconBySlot[entry.skillSlot]}
                                skillSlot={entry.skillSlot}
                                size={34}
                              />
                              <div className="leading-tight">
                                <p className="text-sm font-black text-[#69324b]">
                                  {SKILL_LABEL_BY_SLOT[entry.skillSlot]}
                                </p>
                                <p className="text-[11px] font-bold text-[#a76886]">
                                  {entry.count}/5 포인트
                                </p>
                              </div>
                            </div>
                            <span
                              className={`rounded-full px-2.5 py-1 text-[11px] font-black ${
                                entry.isMaxed
                                  ? "bg-[#69324b] text-white"
                                  : "bg-[#fff0f7] text-[#a76886]"
                              }`}
                            >
                              {entry.isMaxed ? "완성" : "진행"}
                            </span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="rounded-2xl bg-white/80 px-3 py-4 text-center text-xs font-bold text-[#a76886]">
                        스킬 투자 정보가 없습니다.
                      </p>
                    )}
                  </div>
                </div>

                <div className="overflow-x-auto rounded-[1.25rem] bg-[#fff7fb] p-3">
                  <div
                    className="grid min-w-[48rem] overflow-hidden rounded-2xl border border-[#f8dce8]/80 bg-white/80"
                    style={{
                      gridTemplateColumns: `3rem repeat(${MAX_SKILL_LEVEL}, minmax(2.1rem, 1fr))`,
                    }}
                  >
                    <div className="bg-[#f8e8f0]" />
                    {Array.from({ length: MAX_SKILL_LEVEL }, (_, index) => (
                      <div
                        key={`skill-level-head-${index + 1}`}
                        className="border-l border-[#f8dce8]/80 bg-[#f8e8f0] py-2 text-center text-xs font-black text-[#a76886]"
                      >
                        {index + 1}
                      </div>
                    ))}

                    {([1, 2, 3, 4] as SkillSlot[]).map((slot) => (
                      <SkillMatrixRow
                        key={slot}
                        skillByOrder={skillByOrder}
                        skillIconUrl={skillIconBySlot[slot]}
                        skillSlot={slot}
                      />
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <EmptyBuildMessage message="스킬 레벨업 이벤트가 없습니다." />
            )}
          </BuildSection>

          <BuildSection title="룬">
            <RuneSummary participant={selectedParticipant} rune={rune} />
          </BuildSection>
        </>
      )}
    </div>
  );
});

function BuildSection({
  children,
  title,
}: {
  children: React.ReactNode;
  title: string;
}) {
  return (
    <section className="overflow-hidden rounded-[1.25rem] border border-[#f8dce8]/80 bg-white/90 shadow-[0_16px_36px_rgba(98,56,77,0.08)]">
      <div className="border-b border-[#f8dce8]/80 bg-[#fff7fb] px-4 py-3">
        <h4 className="text-lg font-black text-[#69324b]">{title}</h4>
      </div>
      <div className="p-4">{children}</div>
    </section>
  );
}

function EmptyBuildMessage({ message }: { message: string }) {
  return (
    <div className="flex min-h-28 items-center justify-center rounded-[1.25rem] bg-[#fff7fb] text-sm font-black text-[#a76886]">
      {message}
    </div>
  );
}

function SkillIcon({
  iconUrl,
  skillSlot,
  size,
}: {
  iconUrl?: string | null;
  skillSlot: SkillSlot;
  size: number;
}) {
  return (
    <div className="relative">
      <MatchImage
        src={iconUrl}
        alt={SKILL_LABEL_BY_SLOT[skillSlot]}
        width={size}
        height={size}
        className="rounded-2xl border border-white/80 shadow-[0_10px_22px_rgba(98,56,77,0.14)]"
        placeholderClassName="rounded-2xl bg-[#f8e8f0]/80"
      />
      <span className="absolute -bottom-1 -right-1 flex h-6 min-w-6 items-center justify-center rounded-full bg-[#69324b] px-1 text-xs font-black text-white">
        {SKILL_LABEL_BY_SLOT[skillSlot]}
      </span>
    </div>
  );
}

function SkillMatrixRow({
  skillByOrder,
  skillIconUrl,
  skillSlot,
}: {
  skillByOrder: Record<number, SkillBuildEntry>;
  skillIconUrl?: string | null;
  skillSlot: SkillSlot;
}) {
  return (
    <>
      <div className="flex items-center justify-center border-t border-[#f8dce8]/80 bg-[#f8e8f0]">
        <SkillIcon iconUrl={skillIconUrl} skillSlot={skillSlot} size={30} />
      </div>
      {Array.from({ length: MAX_SKILL_LEVEL }, (_, index) => {
        const order = index + 1;
        const entry = skillByOrder[order];
        const isCurrentSkill = entry?.skillSlot === skillSlot;

        return (
          <div
            key={`${skillSlot}-${order}`}
            className="flex h-11 items-center justify-center border-l border-t border-[#f8dce8]/80 bg-white/70"
            title={
              isCurrentSkill
                ? `${order}레벨 · ${entry.minute}분 · ${SKILL_LABEL_BY_SLOT[skillSlot]} ${entry.rankInSkill}레벨${entry.isSkillMaxed ? " · 마스터 완료" : ""}`
                : undefined
            }
          >
            {isCurrentSkill && (
              <span
                className={`flex h-full w-full items-center justify-center bg-[#f45f9c] text-base font-black text-white ${
                  entry.isSkillMaxed
                    ? "ring-4 ring-inset ring-[#69324b] shadow-[inset_0_0_0_2px_rgba(255,255,255,0.92)]"
                    : ""
                }`}
              >
                {order}
              </span>
            )}
          </div>
        );
      })}
    </>
  );
}

function RuneSummary({
  participant,
  rune,
}: {
  participant: MatchParticipant;
  rune: Record<number, Rune> | null;
}) {
  const mainRune = participant.rune?.mainRune;
  const subRune = participant.rune?.subRune;
  const statRune = participant.rune?.statRune;
  const mainStyleId = mainRune?.typeId;
  const subStyleId = subRune?.styleId;
  const mainSelections = compactRuneIds([
    mainRune?.mainRune?.id,
    mainRune?.rune1?.id,
    mainRune?.rune2?.id,
    mainRune?.rune3?.id,
  ]);
  const subSelections = compactRuneIds([
    subRune?.mainRune?.id,
    subRune?.rune1?.id,
    subRune?.rune2?.id,
  ]);
  const statSelections = compactRuneIds([
    statRune?.offense,
    statRune?.flex,
    statRune?.defense,
  ]);

  return (
    <div className="grid gap-4 xl:grid-cols-[1.1fr_1fr_0.85fr]">
      <RuneTreeColumn
        accentClassName="from-[#ff5f7e] to-[#f45f9c]"
        rune={rune}
        runeIds={mainSelections}
        styleId={mainStyleId}
        title={mainRune?.typeDesc || "주 룬"}
        typeLabel="주 룬"
      />
      <RuneTreeColumn
        accentClassName="from-[#49c6bd] to-[#8f7bff]"
        rune={rune}
        runeIds={subSelections}
        styleId={subStyleId}
        title={rune?.[subStyleId ?? -1]?.name ?? "보조 룬"}
        typeLabel="보조 룬"
      />
      <RuneShardColumn runeIds={statSelections} />
    </div>
  );
}

const compactRuneIds = (runeIds: Array<number | null | undefined>) =>
  runeIds.filter(
    (runeId): runeId is number =>
      typeof runeId === "number" && Number.isFinite(runeId) && runeId > 0
  );

function RuneTreeColumn({
  accentClassName,
  rune,
  runeIds,
  styleId,
  title,
  typeLabel,
}: {
  accentClassName: string;
  rune: Record<number, Rune> | null;
  runeIds: number[];
  styleId?: number | null;
  title: string;
  typeLabel: string;
}) {
  const [primaryRuneId, ...secondaryRuneIds] = runeIds;
  const styleRune = styleId ? rune?.[styleId] : null;

  return (
    <div className="relative overflow-hidden rounded-[1.35rem] bg-[#fff7fb] p-4 shadow-[inset_0_0_0_1px_rgba(248,220,232,0.88)]">
      <div className="pointer-events-none absolute inset-x-4 top-14 h-px bg-[#f8dce8]/90" />
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-black text-[#a76886]">{typeLabel}</p>
          <h5 className="text-xl font-black text-[#69324b]">{title}</h5>
        </div>
        <RuneIconFrame
          accentClassName={accentClassName}
          icon={styleRune?.icon}
          label={styleRune?.name ?? title}
          size={38}
          variant="style"
        />
      </div>

      {primaryRuneId ? (
        <div className="grid gap-4 lg:grid-cols-[6rem_minmax(0,1fr)]">
          <div className="flex flex-col items-center justify-center rounded-[1.25rem] bg-white/82 px-3 py-4 shadow-[0_14px_30px_rgba(98,56,77,0.08)]">
            <RuneIconFrame
              accentClassName={accentClassName}
              icon={rune?.[primaryRuneId]?.icon}
              label={rune?.[primaryRuneId]?.name ?? `${primaryRuneId}`}
              size={58}
              variant="primary"
            />
            <p className="mt-3 max-w-full truncate text-center text-xs font-black text-[#69324b]">
              {rune?.[primaryRuneId]?.name ?? "핵심 룬"}
            </p>
          </div>

          <div className="grid grid-cols-3 gap-3">
            {secondaryRuneIds.map((runeId) => (
              <RuneSelectionTile
                key={runeId}
                accentClassName={accentClassName}
                rune={rune?.[runeId]}
                runeId={runeId}
              />
            ))}
          </div>
        </div>
      ) : (
        <RuneEmptyState />
      )}
    </div>
  );
}

function RuneSelectionTile({
  accentClassName,
  rune,
  runeId,
}: {
  accentClassName: string;
  rune?: Rune;
  runeId: number;
}) {
  return (
    <div className="flex min-h-28 flex-col items-center justify-center rounded-[1.15rem] bg-white/82 px-2 py-3 text-center shadow-[0_12px_24px_rgba(98,56,77,0.07)]">
      <RuneIconFrame
        accentClassName={accentClassName}
        icon={rune?.icon}
        label={rune?.name ?? `${runeId}`}
        size={46}
        variant="selected"
      />
      <p className="mt-2 line-clamp-2 text-[11px] font-black leading-tight text-[#69324b]">
        {rune?.name ?? "선택 룬"}
      </p>
    </div>
  );
}

function RuneShardColumn({ runeIds }: { runeIds: number[] }) {
  return (
    <div className="rounded-[1.35rem] bg-[#fff7fb] p-4 shadow-[inset_0_0_0_1px_rgba(248,220,232,0.88)]">
      <div className="mb-4">
        <p className="text-xs font-black text-[#a76886]">능력치</p>
        <h5 className="text-xl font-black text-[#69324b]">룬 파편</h5>
      </div>
      {runeIds.length > 0 ? (
        <div className="grid gap-3">
          {runeIds.map((runeId, index) => {
            const meta = STAT_RUNE_META[runeId];

            return (
              <div
                key={`${index}-${runeId}`}
                className="flex items-center gap-3 rounded-[1.15rem] bg-white/82 px-3 py-3 shadow-[0_12px_24px_rgba(98,56,77,0.07)]"
              >
                <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[#fff0f7] text-xs font-black text-[#f45f9c]">
                  {index + 1}
                </span>
                <RuneIconFrame
                  accentClassName="from-[#31cfc2] to-[#8f7bff]"
                  icon={meta?.icon}
                  label={meta?.label ?? `${runeId}`}
                  size={40}
                  variant="selected"
                />
                <div className="min-w-0">
                  <p className="text-sm font-black text-[#69324b]">
                    {meta?.label ?? "룬 파편"}
                  </p>
                  <p className="text-[11px] font-bold text-[#a76886]">
                    {index === 0 ? "공격" : index === 1 ? "유연" : "방어"}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <RuneEmptyState />
      )}
    </div>
  );
}

function RuneIconFrame({
  accentClassName,
  icon,
  label,
  size,
  variant,
}: {
  accentClassName: string;
  icon?: string | null;
  label: string;
  size: number;
  variant: "primary" | "selected" | "style";
}) {
  const ringSize =
    variant === "primary"
      ? "p-1.5"
      : variant === "style"
        ? "p-1"
        : "p-1";

  return (
    <div
      className={`relative flex shrink-0 items-center justify-center rounded-full bg-gradient-to-br ${accentClassName} ${ringSize} shadow-[0_12px_28px_rgba(98,56,77,0.16)]`}
    >
      <MatchImage
        src={getRuneIconUrl(icon)}
        alt={label}
        width={size}
        height={size}
        className="rounded-full bg-[#332331] p-1"
        placeholderClassName="rounded-full bg-[#f8e8f0]/80"
      />
    </div>
  );
}

function RuneEmptyState() {
  return (
    <div className="flex min-h-28 items-center justify-center rounded-[1.15rem] bg-white/80 text-xs font-black text-[#a76886]">
      룬 정보 없음
    </div>
  );
}
