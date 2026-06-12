"use client";

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { Check, Clock3, Copy, RefreshCw, Star } from "lucide-react";
import { spellApi } from "@/services/spellApi";
import type { Spell } from "@/types/spell";
import { runeApi } from "@/services/runeApi";
import type { Rune } from "@/types/rune";
import { apiJson, ApiRequestError } from "@/services/apiClient";
import {
  dataDragonApi,
  getDataDragonChampionIconUrl,
  getDataDragonProfileIconUrl,
} from "@/services/dataDragonApi";
import {
  isFavoriteSummoner,
  saveRecentSummoner,
  toStoredSummoner,
  toggleFavoriteSummoner,
} from "@/lib/summonerSearchStorage";
import {
  SummonerLoadingView,
  SummonerNotFoundView,
  SummonerRateLimitView,
} from "./_components/SummonerStatusViews";
import {
  MatchFilterTabs,
  MatchLoadMoreButton,
  matchFilters,
  type MatchFilterKey,
} from "./_components/MatchListControls";
import { MatchCard } from "./_components/MatchCard";
import {
  extractTierName,
  getElapsedLabel,
  getMatchDateKey,
  getMatchDateLabel,
  getRemainingRefreshLabel,
  normalizeTierName,
} from "./_lib/summonerFormatters";
import { getChampionIconUrl } from "./_lib/summonerImageUrls";
import type { Mastery, Match, Profile, Summoner } from "./_types/summonerTypes";

const REFRESH_COOLDOWN_MS = 15 * 60 * 1000;

const parseApiDateTime = (value?: string | null): number | null => {
  if (!value) return null;

  const normalizedValue = value.replace(/(\.\d{3})\d+/, "$1");
  const timestamp = new Date(normalizedValue).getTime();

  return Number.isNaN(timestamp) ? null : timestamp;
};

const RATE_LIMIT_MESSAGE =
  "서버가 Riot API 요청 제한에 걸렸습니다. 잠시 후 다시 시도해 주세요.";

const getRateLimitErrorMessage = (error: unknown): string | null => {
  if (error instanceof ApiRequestError && error.status === 429) {
    return error.message && !error.message.startsWith("HTTP error!")
      ? error.message
      : RATE_LIMIT_MESSAGE;
  }

  return null;
};

// const loadingFrames = Array.from(
//   { length: 8 },
//   (_, index) =>
//     `/loading/summoner-loading-${String(index + 1).padStart(2, "0")}.svg`
// );
const loadingFrames = Array.from(
  { length: 6 },
  (_, index) =>
    `/loading/loading_teemo_${String(index + 1).padStart(2, "0")}.png`
);

type LoadingState = {
  tier: boolean;
  matches: boolean;
  profile: boolean;
};

const createInitialLoadingState = (): LoadingState => ({
  tier: true,
  matches: true,
  profile: true,
});

const mergeUniqueMatches = (
  currentMatches: Match[],
  nextMatches: Match[]
): Match[] => {
  const matchIds = new Set(
    currentMatches.map((match) => match.metaData.matchId)
  );
  const uniqueNextMatches = nextMatches.filter(
    (match) => !matchIds.has(match.metaData.matchId)
  );

  return [...currentMatches, ...uniqueNextMatches];
};

const getParticipantFromMatch = (match: Match, index: number) =>
  match.participants[`player${index}` as keyof Match["participants"]];

const getMissingIds = <T,>(
  ids: number[],
  currentData: Record<number, T> | null
) => {
  const currentIds = currentData ?? {};

  return [...new Set(ids)].filter((id) => !currentIds[id]);
};

const collectSpellIdsFromMatches = (matches: Match[]) => {
  const ids: number[] = [];

  matches.forEach((match) => {
    if (match.myData.summoner1Id) ids.push(match.myData.summoner1Id);
    if (match.myData.summoner2Id) ids.push(match.myData.summoner2Id);

    for (let i = 0; i < 10; i++) {
      const player = getParticipantFromMatch(match, i);
      if (player?.summoner1Id) ids.push(player.summoner1Id);
      if (player?.summoner2Id) ids.push(player.summoner2Id);
    }
  });

  return ids;
};

const collectRuneIdsFromMatches = (matches: Match[]) => {
  const ids: number[] = [];

  matches.forEach((match) => {
    if (match.myData.primaryStyle) ids.push(match.myData.primaryStyle);
    if (match.myData.subStyle) ids.push(match.myData.subStyle);

    for (let i = 0; i < 10; i++) {
      const player = getParticipantFromMatch(match, i);
      if (player?.rune?.mainRune?.typeId) ids.push(player.rune.mainRune.typeId);
      if (player?.rune?.subRune?.styleId) ids.push(player.rune.subRune.styleId);
      if (player?.rune?.mainRune?.mainRune?.id) {
        ids.push(player.rune.mainRune.mainRune.id);
      }
    }
  });

  return ids;
};

type RankDetail = Summoner["soloRank"];
type RankQueueKey = "solo" | "flex";

const getProfileIconUrl = (profile: Profile | null): string => {
  if (profile?.profileIconId) {
    return getDataDragonProfileIconUrl(profile.profileIconId);
  }

  return profile?.profileUrl || "";
};

const getTierIconUrl = (rankTier?: string): string => {
  const tierName = extractTierName(rankTier || "").toLowerCase();

  return tierName
    ? `https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-shared-components/global/default/${tierName}.png`
    : "";
};

const getLegacyTierIconUrl = (rankTier?: string): string => {
  const tierName = extractTierName(rankTier || "").toLowerCase();

  return tierName
    ? `https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-static-assets/global/default/images/ranked-emblem/emblem-${tierName}.png`
    : "";
};

const getDDragonChampionIconUrl = (imageFull?: string): string =>
  imageFull ? getDataDragonChampionIconUrl(imageFull) : "/sad-summoner.svg";

const getChampionMasteryIconUrl = (imageFull?: string): string =>
  imageFull ? getDDragonChampionIconUrl(imageFull) : "/sad-summoner.svg";

const getChampionMasteryFallbackIconUrl = (championId?: number): string =>
  championId
    ? `https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons/${championId}.png`
    : "/sad-summoner.svg";

const isAllowedImageSrc = (src: string) =>
  src.startsWith("/") ||
  src.startsWith("https://ddragon.leagueoflegends.com/") ||
  src.startsWith("https://raw.communitydragon.org/");

type FallbackImageProps = {
  alt: string;
  className?: string;
  height: number;
  placeholderClassName?: string;
  sizes?: string;
  sources: Array<string | null | undefined>;
  width: number;
};

function FallbackImage({
  alt,
  className,
  height,
  placeholderClassName,
  sizes,
  sources,
  width,
}: FallbackImageProps) {
  const resolvedSources = useMemo(
    () =>
      Array.from(
        new Set(
          sources.filter(
            (src): src is string =>
              typeof src === "string" && isAllowedImageSrc(src)
          )
        )
      ),
    [sources]
  );
  const sourceKey = resolvedSources.join("|");
  const [sourceIndex, setSourceIndex] = useState(0);

  useEffect(() => {
    setSourceIndex(0);
  }, [sourceKey]);

  const src = resolvedSources[sourceIndex];

  if (!src) {
    return (
      <div
        aria-label={alt}
        className={placeholderClassName ?? className}
        role="img"
        style={{ width, height }}
      />
    );
  }

  return (
    <Image
      alt={alt}
      className={className}
      height={height}
      onError={() => setSourceIndex((currentIndex) => currentIndex + 1)}
      sizes={sizes ?? `${width}px`}
      src={src}
      width={width}
    />
  );
}

const hasRankInfo = (rank?: RankDetail | null): boolean =>
  Boolean(rank?.rankTier && normalizeTierName(rank.rankTier) !== "UNRANKED");

const getRankWinRate = (rank?: RankDetail | null): number => {
  const wins = rank?.rankWin ?? 0;
  const losses = rank?.rankDefeat ?? 0;
  const games = wins + losses;

  return games > 0 ? (wins / games) * 100 : 0;
};

const queue = [
  { id: 420, name: "솔로랭크" },
  { id: 440, name: "자유랭크" },
  { id: 400, name: "일반" },
  { id: 430, name: "일반" },
  { id: 450, name: "칼바람 나락" },
  { id: 700, name: "격전" },
  { id: 900, name: "URF" },
  { id: 1020, name: "단일 챔피언" },
  { id: 1400, name: "궁극기 주문서" },
  { id: 2000, name: "튜토리얼" },
  { id: 1700, name: "아레나" },
];

const RECENT_CHAMPION_GAME_LIMIT = 10;

const normalQueueIds = new Set([400, 430]);

const getQueueName = (queueId: number): string =>
  queue.find((item) => item.id === queueId)?.name || "기타";

const getMatchFilterKey = (queueId: number): MatchFilterKey => {
  if (queueId === 420) return "solo";
  if (queueId === 440) return "flex";
  if (normalQueueIds.has(queueId)) return "normal";
  return "other";
};

const getPositionLabel = (position?: string): string => {
  const normalized = (position || "").toUpperCase();
  const labels: Record<string, string> = {
    TOP: "탑",
    JUNGLE: "정글",
    MIDDLE: "미드",
    MID: "미드",
    BOTTOM: "원딜",
    ADC: "원딜",
    UTILITY: "서폿",
    SUPPORT: "서폿",
  };

  return labels[normalized] || "기타";
};

const formatPercent = (value: number): string => `${value.toFixed(1)}%`;

const positionRoleConfigs = [
  { label: "탑", icon: "/positions/top.svg" },
  { label: "정글", icon: "/positions/jungle.svg" },
  { label: "미드", icon: "/positions/mid.svg" },
  { label: "원딜", icon: "/positions/bottom.svg" },
  { label: "서폿", icon: "/positions/support.svg" },
];

const buildWinLossPieStyle = (
  wins: number,
  losses: number
): React.CSSProperties => {
  const games = wins + losses;

  if (games === 0) {
    return { background: "#fff0f7" };
  }

  const winPercent = (wins / games) * 100;

  return {
    background: `conic-gradient(#2f80ed 0% ${winPercent}%, #ff5f7e ${winPercent}% 100%)`,
  };
};

const buildMatchSummary = (matches: Match[]) => {
  const championMap = new Map<
    number,
    {
      championId: number;
      championNameEn: string;
      championNameKo: string;
      games: number;
      wins: number;
      kills: number;
      deaths: number;
      assists: number;
    }
  >();
  const positionMap = new Map<string, number>();
  const queueMap = new Map<string, number>();

  let wins = 0;
  let kills = 0;
  let deaths = 0;
  let assists = 0;
  let aiScoreSum = 0;

  matches.forEach((match) => {
    const current = championMap.get(match.myData.championId) || {
      championId: match.myData.championId,
      championNameEn: match.myData.championNameEn,
      championNameKo: match.myData.championNameKo,
      games: 0,
      wins: 0,
      kills: 0,
      deaths: 0,
      assists: 0,
    };

    current.games += 1;
    current.wins += match.myData.win ? 1 : 0;
    current.kills += match.myData.kills;
    current.deaths += match.myData.deaths;
    current.assists += match.myData.assists;
    championMap.set(match.myData.championId, current);

    wins += match.myData.win ? 1 : 0;
    kills += match.myData.kills;
    deaths += match.myData.deaths;
    assists += match.myData.assists;
    aiScoreSum += match.myData.ourScore || 0;

    const position = getPositionLabel(match.myData.teamPosition);
    positionMap.set(position, (positionMap.get(position) || 0) + 1);

    const queueName = getQueueName(match.metaData.queueId);
    queueMap.set(queueName, (queueMap.get(queueName) || 0) + 1);
  });

  const games = matches.length;
  const losses = games - wins;
  const winRate = games > 0 ? (wins / games) * 100 : 0;
  const kda = deaths > 0 ? (kills + assists) / deaths : kills + assists;
  const averageAiScore = games > 0 ? aiScoreSum / games : 0;

  const champions = Array.from(championMap.values())
    .sort((a, b) => b.games - a.games || b.wins - a.wins || b.kills - a.kills)
    .map((champion) => ({
      ...champion,
      winRate: champion.games > 0 ? (champion.wins / champion.games) * 100 : 0,
      kda:
        champion.deaths > 0
          ? (champion.kills + champion.assists) / champion.deaths
          : champion.kills + champion.assists,
    }));
  const topChampions = champions.slice(0, 3);

  const positionGames = positionRoleConfigs.reduce(
    (sum, position) => sum + (positionMap.get(position.label) || 0),
    0
  );

  const positionDistribution = positionRoleConfigs.map((position) => {
    const count = positionMap.get(position.label) || 0;

    return {
      ...position,
      count,
      percent: positionGames > 0 ? (count / positionGames) * 100 : 0,
    };
  });

  const queueDistribution = Array.from(queueMap.entries())
    .map(([label, count]) => ({
      label,
      count,
      percent: games > 0 ? (count / games) * 100 : 0,
    }))
    .sort((a, b) => b.count - a.count);

  return {
    games,
    wins,
    losses,
    winRate,
    kills,
    deaths,
    assists,
    kda,
    averageAiScore,
    champions,
    topChampions,
    positionGames,
    positionDistribution,
    queueDistribution,
  };
};

export default function SummonerPage({
  params,
}: {
  params: Promise<{ name: string }>;
}) {
  const resolvedParams = React.use(params);
  const decodedName = decodeURIComponent(resolvedParams.name);
  const router = useRouter();
  const [gameName, tagLine] = decodedName.includes("-")
    ? decodedName.split("-")
    : [decodedName, "KR1"]; // 기본 tagLine 설정

  const [data, setData] = useState<Summoner | null>(null);
  const [matchData, setMatchData] = useState<Match[]>([]);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [masteryData, setMasteryData] = useState<Mastery[]>([]);
  const [championImageById, setChampionImageById] = useState<
    Record<number, string>
  >({});
  const [spell, setSpell] = useState<Record<number, Spell> | null>(null);
  const [rune, setRune] = useState<Record<number, Rune> | null>(null);
  const [notFoundMessage, setNotFoundMessage] = useState<string | null>(null);
  const [rateLimitMessage, setRateLimitMessage] = useState<string | null>(null);
  const [loadingState, setLoadingState] = useState<LoadingState>(
    createInitialLoadingState
  );
  const [loadingFrameIndex, setLoadingFrameIndex] = useState(0);
  const [now, setNow] = useState(() => Date.now());
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [matchPage, setMatchPage] = useState(1);
  const [hasMoreMatches, setHasMoreMatches] = useState(true);
  const [isLoadingMoreMatches, setIsLoadingMoreMatches] = useState(false);
  const [isFavorite, setIsFavorite] = useState(false);
  const [copiedSummoner, setCopiedSummoner] = useState(false);
  const [activeMatchFilter, setActiveMatchFilter] =
    useState<MatchFilterKey>("all");
  const [activeRankQueue, setActiveRankQueue] =
    useState<RankQueueKey>("solo");
  const [expandedMatches, setExpandedMatches] = useState<
    Record<string, boolean>
  >({});
  const requestVersionRef = useRef(0);
  const summonerQuery = useMemo(
    () => new URLSearchParams({ gameName, tagLine }).toString(),
    [gameName, tagLine]
  );
  const summonerQueryWithRefresh = useCallback(
    (refresh: boolean) => `${summonerQuery}&refresh=${refresh}`,
    [summonerQuery]
  );
  const currentStoredSummoner = useMemo(
    () =>
      data
        ? toStoredSummoner({
            id: data.id,
            puuid: data.puuid,
            gameName: data.gameName,
            tagLine: data.tagLine,
            profileUrl: getProfileIconUrl(profile),
            level: profile?.summonerLevel ?? 0,
          })
        : null,
    [data, profile]
  );

  const filterCounts = useMemo(() => {
    const counts = matchFilters.reduce(
      (acc, filter) => ({ ...acc, [filter.key]: 0 }),
      {} as Record<MatchFilterKey, number>
    );

    matchData.forEach((match) => {
      counts.all += 1;
      counts[getMatchFilterKey(match.metaData.queueId)] += 1;
    });

    return counts;
  }, [matchData]);

  const filteredMatches = useMemo(() => {
    if (activeMatchFilter === "all") return matchData;

    return matchData.filter(
      (match) => getMatchFilterKey(match.metaData.queueId) === activeMatchFilter
    );
  }, [activeMatchFilter, matchData]);

  const matchSummary = useMemo(
    () => buildMatchSummary(filteredMatches),
    [filteredMatches]
  );
  const recentChampionSummary = useMemo(
    () => buildMatchSummary(filteredMatches.slice(0, RECENT_CHAMPION_GAME_LIMIT)),
    [filteredMatches]
  );
  const hiddenRecentChampions = recentChampionSummary.champions.slice(3);
  const matchDateStats = useMemo(() => {
    const stats = new Map<
      string,
      {
        wins: number;
        losses: number;
      }
    >();

    filteredMatches.forEach((match) => {
      const key = getMatchDateKey(match.metaData.gameEndTimestamp);
      const current = stats.get(key) || { wins: 0, losses: 0 };

      if (match.myData.win) {
        current.wins += 1;
      } else {
        current.losses += 1;
      }

      stats.set(key, current);
    });

    return stats;
  }, [filteredMatches]);
  const isInitialLoading =
    !notFoundMessage &&
    !rateLimitMessage &&
    (loadingState.tier || loadingState.matches || loadingState.profile);
  const positionMaxCount = Math.max(
    ...matchSummary.positionDistribution.map((position) => position.count),
    1
  );

  const activeRank = activeRankQueue === "solo" ? data?.soloRank : data?.flexRank;
  const activeRankLabel = activeRankQueue === "solo" ? "솔로랭크" : "자유랭크";
  const activeRankHasRank = hasRankInfo(activeRank);
  const activeRankWinRate = getRankWinRate(activeRank);
  const refreshStatus = useMemo(() => {
    const updatedAt = parseApiDateTime(data?.updateAt);

    if (!updatedAt) {
      return {
        canRefresh: true,
        lastUpdatedLabel: "갱신 기록 없음",
        availabilityLabel: "갱신 가능",
      };
    }

    const elapsedMs = Math.max(0, now - updatedAt);
    const remainingMs = REFRESH_COOLDOWN_MS - elapsedMs;
    const canRefresh = remainingMs <= 0;

    return {
      canRefresh,
      lastUpdatedLabel: `최근 갱신 ${getElapsedLabel(updatedAt, now)}`,
      availabilityLabel: canRefresh
        ? "갱신 가능"
        : getRemainingRefreshLabel(remainingMs),
    };
  }, [data?.updateAt, now]);
  const topMasteries = useMemo(
    () =>
      [...masteryData]
        .sort((a, b) => (b.championPoints ?? 0) - (a.championPoints ?? 0))
        .slice(0, 3),
    [masteryData]
  );

  useEffect(() => {
    if (!currentStoredSummoner) return;

    saveRecentSummoner(currentStoredSummoner);
    setIsFavorite(isFavoriteSummoner(currentStoredSummoner));
  }, [currentStoredSummoner]);

  const handleToggleFavorite = () => {
    if (!currentStoredSummoner) return;

    toggleFavoriteSummoner(currentStoredSummoner);
    setIsFavorite(isFavoriteSummoner(currentStoredSummoner));
  };

  const handleCopySummoner = async () => {
    if (!data) return;

    const summonerName = `${data.gameName}#${data.tagLine}`;

    try {
      await navigator.clipboard.writeText(summonerName);
      setCopiedSummoner(true);
      window.setTimeout(() => setCopiedSummoner(false), 1400);
    } catch (error) {
      console.error("소환사 이름 복사 실패:", error);
    }
  };

  const handleSummonerClick = useCallback(
    (nextGameName: string, nextTagLine: string) => {
      router.push(`/summoner/${nextGameName}-${nextTagLine}`);
    },
    [router]
  );

  const toggleMatchExpand = useCallback((matchId: string) => {
    setExpandedMatches((prev) => ({
      ...prev,
      [matchId]: !prev[matchId],
    }));
  }, []);

  useEffect(() => {
    requestVersionRef.current += 1;
    setNotFoundMessage(null);
    setRateLimitMessage(null);
    setData(null);
    setMatchData([]);
    setProfile(null);
    setMasteryData([]);
    setSpell(null);
    setRune(null);
    setLoadingState(createInitialLoadingState());
    setLoadingFrameIndex(0);
    setIsRefreshing(false);
    setMatchPage(1);
    setHasMoreMatches(true);
    setIsLoadingMoreMatches(false);
    setActiveRankQueue("solo");
    setExpandedMatches({});
  }, [gameName, tagLine]);

  useEffect(() => {
    if (!isInitialLoading) return;

    const intervalId = window.setInterval(() => {
      setLoadingFrameIndex((current) => (current + 1) % loadingFrames.length);
    }, 160);

    return () => window.clearInterval(intervalId);
  }, [isInitialLoading]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setNow(Date.now());
    }, 60 * 1000);

    return () => window.clearInterval(intervalId);
  }, []);

  useEffect(() => {
    let isCancelled = false;

    const fetchChampionImageMap = async () => {
      try {
        const nextChampionImageById =
          await dataDragonApi.getChampionImageByIdMap();

        if (isCancelled) return;

        setChampionImageById(nextChampionImageById);
      } catch (error) {
        if (!isCancelled) {
          console.error("❌ 챔피언 이미지 매핑 요청 실패:", error);
        }
      }
    };

    fetchChampionImageMap();

    return () => {
      isCancelled = true;
    };
  }, []);

  useEffect(() => {
    let isCancelled = false;
    const requestVersion = requestVersionRef.current;

    const fetchSummonerData = async () => {
      try {
        const result = await apiJson<Summoner>(
          `/api/v1/summoner/tier?${summonerQueryWithRefresh(
            false
          )}`,
          { method: "GET" }
        );

        if (isCancelled || requestVersion !== requestVersionRef.current) return;
        setData(result);
      } catch (error) {
        if (isCancelled || requestVersion !== requestVersionRef.current) return;

        const nextRateLimitMessage = getRateLimitErrorMessage(error);
        if (nextRateLimitMessage) {
          setNotFoundMessage(null);
          setRateLimitMessage(nextRateLimitMessage);
          return;
        }

        if (error instanceof ApiRequestError && error.status === 404) {
          setNotFoundMessage(error.message);
          return;
        }

        console.error("❌ API 요청 실패:", error);
        setData(null);
      } finally {
        if (!isCancelled && requestVersion === requestVersionRef.current) {
          setLoadingState((current) => ({ ...current, tier: false }));
        }
      }
    };

    fetchSummonerData();

    return () => {
      isCancelled = true;
    };
  }, [summonerQueryWithRefresh]);

  useEffect(() => {
    let isCancelled = false;
    const requestVersion = requestVersionRef.current;

    const fetchMatchData = async () => {
      try {
        const result = await apiJson<Match[]>(
          `/api/v1/summoner/matches/1?${summonerQueryWithRefresh(
            false
          )}`,
          { method: "GET" }
        );

        if (isCancelled || requestVersion !== requestVersionRef.current) return;
        const initialMatches = Array.isArray(result) ? result : [];
        setMatchData(initialMatches);
        setMatchPage(1);
        setHasMoreMatches(initialMatches.length >= 20);
      } catch (error) {
        if (isCancelled || requestVersion !== requestVersionRef.current) return;

        const nextRateLimitMessage = getRateLimitErrorMessage(error);
        if (nextRateLimitMessage) {
          setNotFoundMessage(null);
          setRateLimitMessage(nextRateLimitMessage);
          setHasMoreMatches(false);
          return;
        }

        if (error instanceof ApiRequestError && error.status === 404) {
          setNotFoundMessage(error.message);
          return;
        }

        console.error("❌ 전적 API 요청 실패:", error);
        setMatchData([]);
        setHasMoreMatches(false);
      } finally {
        if (!isCancelled && requestVersion === requestVersionRef.current) {
          setLoadingState((current) => ({ ...current, matches: false }));
        }
      }
    };

    fetchMatchData();

    return () => {
      isCancelled = true;
    };
  }, [summonerQueryWithRefresh]);

  useEffect(() => {
    let isCancelled = false;
    const requestVersion = requestVersionRef.current;

    const fetchProfileData = async () => {
      try {
        const result = await apiJson<Profile>(
          `/api/v1/summoner/profile?${summonerQueryWithRefresh(
            false
          )}`
        );

        if (isCancelled || requestVersion !== requestVersionRef.current) return;
        setProfile(result);
      } catch (error) {
        if (isCancelled || requestVersion !== requestVersionRef.current) return;

        const nextRateLimitMessage = getRateLimitErrorMessage(error);
        if (nextRateLimitMessage) {
          setNotFoundMessage(null);
          setRateLimitMessage(nextRateLimitMessage);
          return;
        }

        if (error instanceof ApiRequestError && error.status === 404) {
          setNotFoundMessage(error.message);
          return;
        }

        console.error("❌ 프로필 API 요청 실패:", error);
        setProfile(null);
      } finally {
        if (!isCancelled && requestVersion === requestVersionRef.current) {
          setLoadingState((current) => ({ ...current, profile: false }));
        }
      }
    };

    fetchProfileData();

    return () => {
      isCancelled = true;
    };
  }, [summonerQueryWithRefresh]);

  useEffect(() => {
    let isCancelled = false;

    const fetchMasteryData = async () => {
      try {
        const result = await apiJson<Mastery[]>(
          `/api/v1/summoner/mastery?${summonerQuery}`
        );

        if (isCancelled) return;
        setMasteryData(result);
      } catch (error) {
        if (isCancelled) return;

        const nextRateLimitMessage = getRateLimitErrorMessage(error);
        if (nextRateLimitMessage) {
          setNotFoundMessage(null);
          setRateLimitMessage(nextRateLimitMessage);
          return;
        }

        console.error("❌ 숙련도 API 요청 실패:", error);
        setMasteryData([]);
      }
    };

    fetchMasteryData();

    return () => {
      isCancelled = true;
    };
  }, [summonerQuery]);

  const handleRefresh = async () => {
    if (
      !data ||
      isInitialLoading ||
      !refreshStatus.canRefresh ||
      isRefreshing ||
      isLoadingMoreMatches
    ) {
      return;
    }

    const requestVersion = requestVersionRef.current + 1;
    requestVersionRef.current = requestVersion;
    setIsRefreshing(true);
    setIsLoadingMoreMatches(false);
    setRateLimitMessage(null);
    setNotFoundMessage(null);

    try {
      const [tierResult, profileResult, matchResult] = await Promise.all([
        apiJson<Summoner>(
          `/api/v1/summoner/tier?${summonerQueryWithRefresh(
            true
          )}`
        ),
        apiJson<Profile>(
          `/api/v1/summoner/profile?${summonerQueryWithRefresh(
            true
          )}`
        ),
        apiJson<Match[]>(
          `/api/v1/summoner/matches/1?${summonerQueryWithRefresh(
            true
          )}`
        ),
      ]);
      const refreshedMatches = Array.isArray(matchResult) ? matchResult : [];

      if (requestVersion !== requestVersionRef.current) return;

      setData(tierResult);
      setProfile(profileResult);
      setMatchData(refreshedMatches);
      setMatchPage(1);
      setHasMoreMatches(refreshedMatches.length >= 20);
      setExpandedMatches({});
      setNow(Date.now());
    } catch (error) {
      if (requestVersion !== requestVersionRef.current) return;

      const nextRateLimitMessage = getRateLimitErrorMessage(error);
      if (nextRateLimitMessage) {
        setRateLimitMessage(nextRateLimitMessage);
        return;
      }

      if (error instanceof ApiRequestError && error.status === 404) {
        setNotFoundMessage(error.message);
        return;
      }

      console.error("❌ 전적 갱신 실패:", error);
    } finally {
      if (requestVersion === requestVersionRef.current) {
        setIsRefreshing(false);
      }
    }
  };

  const loadMoreMatches = useCallback(async () => {
    if (isLoadingMoreMatches || isRefreshing || !hasMoreMatches) {
      return;
    }

    const nextPage = matchPage + 1;
    const requestVersion = requestVersionRef.current;
    setIsLoadingMoreMatches(true);

    try {
      const result = await apiJson<Match[]>(
        `/api/v1/summoner/matches/${nextPage}?${summonerQueryWithRefresh(
          false
        )}`
      );
      const nextMatches = Array.isArray(result) ? result : [];

      if (requestVersion !== requestVersionRef.current) return;

      setMatchData((currentMatches) =>
        mergeUniqueMatches(currentMatches, nextMatches)
      );
      setMatchPage(nextPage);
      setHasMoreMatches(nextMatches.length >= 20);
    } catch (error) {
      if (requestVersion !== requestVersionRef.current) return;

      const nextRateLimitMessage = getRateLimitErrorMessage(error);
      if (nextRateLimitMessage) {
        setNotFoundMessage(null);
        setRateLimitMessage(nextRateLimitMessage);
        setHasMoreMatches(false);
        return;
      }

      console.error("❌ 추가 전적 API 요청 실패:", error);
    } finally {
      if (requestVersion === requestVersionRef.current) {
        setIsLoadingMoreMatches(false);
      }
    }
  }, [
    hasMoreMatches,
    isLoadingMoreMatches,
    isRefreshing,
    matchPage,
    summonerQueryWithRefresh,
  ]);

  // 소환사 주문 데이터 가져오기 (모든 매치의 모든 플레이어에서 스펠 ID 수집)
  const fetchSpellData = useCallback(async () => {
    if (matchData.length === 0) return;

    const missingSpellIds = getMissingIds(
      collectSpellIdsFromMatches(matchData),
      spell
    );

    if (missingSpellIds.length === 0) return;

    const spellData = await spellApi.fetchSpellsByIds(missingSpellIds);
    setSpell((currentSpell) => ({ ...(currentSpell ?? {}), ...spellData }));
  }, [matchData, spell]);

  // 룬 데이터 가져오기 (모든 매치의 모든 플레이어에서 룬 스타일 ID 수집)
  const fetchRuneData = useCallback(async () => {
    if (matchData.length === 0) return;

    const missingRuneIds = getMissingIds(
      collectRuneIdsFromMatches(matchData),
      rune
    );

    if (missingRuneIds.length === 0) return;

    const runeData = await runeApi.getRunesByIds(missingRuneIds);
    setRune((currentRune) => ({ ...(currentRune ?? {}), ...runeData }));
  }, [matchData, rune]);

  // data가 로드된 후 스펠/룬 데이터 가져오기
  useEffect(() => {
    fetchSpellData();
    fetchRuneData();
  }, [fetchRuneData, fetchSpellData]);

  if (rateLimitMessage) {
    return (
      <SummonerRateLimitView
        message={rateLimitMessage}
        onRetry={() => router.push("/")}
      />
    );
  }

  if (notFoundMessage) {
    return (
      <SummonerNotFoundView
        message={notFoundMessage}
        onRetry={() => router.push("/")}
      />
    );
  }

  if (isInitialLoading) {
    return (
      <SummonerLoadingView frameSrc={loadingFrames[loadingFrameIndex]} />
    );
  }

  return (
    <div
      data-summoner-page
      className="flex min-h-screen justify-center bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_48%,_#fffafd_100%)] text-[#69324b]"
    >
      <div className="flex w-full max-w-[90rem] flex-col px-4 pb-8 pt-4 lg:pb-10 lg:pt-5">
        <div className="w-full">
          <div className="toss-profile-card grid gap-4 rounded-[2rem] border border-white/70 bg-white/95 p-5 shadow-[0_26px_72px_rgba(205,79,134,0.16)] ring-1 ring-[#f8dce8]/60 lg:grid-cols-[minmax(20rem,1.05fr)_minmax(22rem,0.95fr)_minmax(24rem,1.05fr)]">
            <section className="flex min-w-0 items-center rounded-[1.5rem] bg-[#fff7fb] p-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.9),0_12px_34px_rgba(205,79,134,0.08)]">
              <div className="relative shrink-0">
                {getProfileIconUrl(profile) ? (
                  <FallbackImage
                    sources={[getProfileIconUrl(profile), profile?.profileUrl]}
                    alt="profile"
                    width={96}
                    height={96}
                    className="h-24 w-24 rounded-[1.5rem] border border-white/80 bg-[#fff0f7] object-cover shadow-[0_16px_34px_rgba(205,79,134,0.18)]"
                    placeholderClassName="h-24 w-24 rounded-[1.5rem] border border-white/80 bg-[#fff0f7] shadow-[0_16px_34px_rgba(205,79,134,0.18)]"
                  />
                ) : (
                  <div className="h-24 w-24 rounded-[1.5rem] border border-white/80 bg-[#fff0f7] shadow-[0_16px_34px_rgba(205,79,134,0.18)]" />
                )}
                <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 rounded-full bg-[#f45f9c] px-2.5 py-0.5 text-xs font-black text-white shadow-[0_8px_16px_rgba(205,79,134,0.28)]">
                  {profile?.summonerLevel}
                </div>
              </div>
              <div className="ml-5 flex min-w-0 flex-col">
                <div className="min-w-0">
                  <div className="max-w-full break-words text-3xl font-black leading-tight tracking-normal text-[#69324b] [overflow-wrap:anywhere]">
                    {data?.gameName}
                  </div>
                  <div className="mt-1 flex flex-wrap items-center gap-2">
                    <span className="text-2xl font-bold leading-tight text-[#a76886]">
                      #{data?.tagLine}
                    </span>
                    <button
                      type="button"
                      onClick={handleCopySummoner}
                      className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-white text-[#e75491] shadow-[inset_0_0_0_1px_rgba(248,220,232,0.9)] transition-colors hover:bg-[#fff0f7]"
                      title="소환사 이름 복사"
                    >
                      {copiedSummoner ? (
                        <Check className="h-3.5 w-3.5" />
                      ) : (
                        <Copy className="h-3.5 w-3.5" />
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={handleToggleFavorite}
                      className={`inline-flex h-7 w-7 items-center justify-center rounded-full bg-white shadow-[inset_0_0_0_1px_rgba(248,220,232,0.9)] transition-colors hover:bg-[#fff0f7] ${
                        isFavorite ? "text-[#f45f9c]" : "text-[#a76886]"
                      }`}
                      title="즐겨찾기"
                    >
                      <Star
                        className="h-3.5 w-3.5"
                        fill={isFavorite ? "currentColor" : "none"}
                      />
                    </button>
                  </div>
                </div>
                <div className="mt-5 flex flex-col items-start gap-2.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      type="button"
                      onClick={handleRefresh}
                      disabled={
                        !data ||
                        isInitialLoading ||
                        !refreshStatus.canRefresh ||
                        isRefreshing ||
                        isLoadingMoreMatches
                      }
                      title={
                        isRefreshing
                          ? "전적 갱신 중"
                          : refreshStatus.canRefresh
                            ? "전적 갱신 가능"
                            : refreshStatus.availabilityLabel
                      }
                      className="group inline-flex h-11 items-center gap-2 rounded-full bg-[#f45f9c] pl-3 pr-5 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.24)] transition-all hover:-translate-y-0.5 hover:bg-[#e75491] hover:shadow-[0_18px_36px_rgba(231,84,145,0.28)] disabled:translate-y-0 disabled:cursor-not-allowed disabled:bg-[#f4b6cf] disabled:text-white/90 disabled:shadow-none"
                    >
                      <span className="flex h-7 w-7 items-center justify-center rounded-full bg-white/22 transition-colors group-hover:bg-white/28">
                        <RefreshCw
                          className={`h-4 w-4 ${
                            isRefreshing ? "animate-spin" : ""
                          }`}
                          aria-hidden="true"
                        />
                      </span>
                      <span>{isRefreshing ? "갱신 중" : "전적 갱신"}</span>
                    </button>
                    <span
                      className={`inline-flex h-8 items-center rounded-full px-3 text-xs font-black ${
                        refreshStatus.canRefresh && !isRefreshing
                          ? "bg-[#fff0f7] text-[#e75491] ring-1 ring-[#ffd1e3]"
                          : "bg-[#f3f7fb] text-[#7f95b2] ring-1 ring-[#dfe9f3]"
                      }`}
                    >
                      {isRefreshing
                        ? "갱신 중"
                        : refreshStatus.availabilityLabel}
                    </span>
                  </div>
                  <div className="inline-flex max-w-full items-center gap-1.5 rounded-full bg-white/70 px-3 py-1.5 text-xs font-bold text-[#a76886] shadow-[inset_0_0_0_1px_rgba(248,220,232,0.8)]">
                    <Clock3
                      className="h-3.5 w-3.5 shrink-0 text-[#e75491]"
                      aria-hidden="true"
                    />
                    <span className="truncate">
                      {refreshStatus.lastUpdatedLabel}
                    </span>
                  </div>
                </div>
              </div>
            </section>

            <section className="flex min-h-[14.25rem] min-w-0 flex-col rounded-[1.5rem] border border-white/75 bg-[#fffafd] p-4 shadow-[0_16px_40px_rgba(205,79,134,0.11)] ring-1 ring-[#f8dce8]/55">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-black text-[#69324b]">랭크 티어</h2>
                <div className="flex rounded-full bg-[#ffe4ef] p-1">
                  {[
                    { key: "solo" as const, label: "솔로" },
                    { key: "flex" as const, label: "자유" },
                  ].map((rank) => (
                    <button
                      key={rank.key}
                      type="button"
                      onClick={() => setActiveRankQueue(rank.key)}
                      className={`rounded-full px-3 py-1 text-xs font-black transition-colors ${
                        activeRankQueue === rank.key
                          ? "bg-[#f45f9c] text-white shadow-[0_8px_18px_rgba(205,79,134,0.22)]"
                          : "text-[#a76886] hover:bg-white"
                      }`}
                    >
                      {rank.label}
                    </button>
                  ))}
                </div>
              </div>

              {activeRankHasRank ? (
                <div className="mt-4 grid min-h-[9.5rem] flex-1 grid-cols-[8.75rem_minmax(0,1fr)] items-center gap-4">
                  <div className="flex h-[8.75rem] w-[8.75rem] shrink-0 items-center justify-center overflow-visible">
                    <FallbackImage
                      sources={[
                        getTierIconUrl(activeRank?.rankTier),
                        getLegacyTierIconUrl(activeRank?.rankTier),
                        "/sad-summoner.svg",
                      ]}
                      alt={activeRank?.rankTier || "rank tier"}
                      width={168}
                      height={168}
                      className="h-[9.5rem] w-[9.5rem] max-w-none object-contain drop-shadow-[0_20px_30px_rgba(98,56,77,0.24)]"
                      placeholderClassName="h-[9.5rem] w-[9.5rem] rounded-2xl bg-[#fff0f7]"
                    />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="text-xs font-black text-[#a76886]">
                      {activeRankLabel}
                    </div>
                    <div className="mt-1 break-words text-[1.65rem] font-black leading-tight text-[#69324b] [overflow-wrap:anywhere]">
                      {activeRank?.rankTier}
                    </div>
                    <div className="mt-1 text-lg font-black text-[#e75491]">
                      {activeRank?.rankLP.toLocaleString()} LP
                    </div>
                    <div className="mt-3 grid grid-cols-2 gap-2 text-sm font-bold">
                      <div className="rounded-2xl bg-[#fff0f7] px-3 py-2 text-[#a76886]">
                        {activeRank?.rankWin}승 {activeRank?.rankDefeat}패
                      </div>
                      <div className="rounded-2xl bg-[#fff0f7] px-3 py-2 text-[#a76886]">
                        승률 {activeRankWinRate.toFixed(1)}%
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="mt-4 flex min-h-[9.5rem] flex-1 flex-col items-center justify-center rounded-[1.25rem] bg-[#fff0f7] px-4 py-4 text-center">
                  <Image
                    src="/sad_mumu_final.png"
                    alt="언랭크 안내 이미지"
                    width={128}
                    height={96}
                    className="h-24 w-32 shrink-0 object-contain"
                  />
                  <div className="mt-2 text-xs font-black text-[#a76886]">
                    {activeRankLabel}
                  </div>
                  <div className="mt-1 text-xl font-black text-[#69324b]">
                    아직 티어가 없어요
                  </div>
                </div>
              )}
            </section>

            <section className="flex min-w-0 flex-col rounded-[1.5rem] border border-white/75 bg-[#fffafd] p-4 shadow-[0_16px_40px_rgba(205,79,134,0.11)] ring-1 ring-[#f8dce8]/55">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-black text-[#69324b]">포지션 분포</h2>
                <span className="rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#a76886]">
                  {matchSummary.positionGames}게임
                </span>
              </div>
              {matchSummary.positionGames > 0 ? (
                <div className="mt-4 flex flex-1 flex-col justify-end rounded-[1.25rem] bg-[#fff7fb] p-3 shadow-[inset_0_1px_0_rgba(255,255,255,0.9),0_12px_28px_rgba(205,79,134,0.08)]">
                  <div className="grid h-28 grid-cols-5 items-end gap-3 rounded-[1rem] bg-white/70 px-3 pt-3 shadow-inner shadow-[#ffd1e3]/45">
                    {matchSummary.positionDistribution.map((position) => {
                      const positionPercentLabel = formatPercent(
                        position.percent
                      );
                      const heightPercent =
                        position.count > 0
                          ? Math.max(
                              16,
                              (position.count / positionMaxCount) * 100
                            )
                          : 0;
                      const isTopPosition =
                        position.count > 0 &&
                        position.count === positionMaxCount;

                      return (
                        <div
                          key={position.label}
                          className="flex h-full min-w-0 flex-col items-center justify-end gap-1"
                          title={`${position.label} ${positionPercentLabel}`}
                        >
                          <div
                            className={`w-3 rounded-t-full transition-all ${
                              isTopPosition
                                ? "bg-gradient-to-t from-[#e75491] to-[#ff7aae] shadow-[0_0_18px_rgba(231,84,145,0.28)]"
                                : "bg-[#f6bdd4]"
                            }`}
                            style={{ height: `${heightPercent}%` }}
                          />
                        </div>
                      );
                    })}
                  </div>
                  <div className="mt-3 grid grid-cols-5 gap-2">
                    {matchSummary.positionDistribution.map((position) => {
                      const positionPercentLabel = formatPercent(
                        position.percent
                      );
                      const isTopPosition =
                        position.count > 0 &&
                        position.count === positionMaxCount;

                      return (
                        <div
                          key={position.label}
                          className="flex min-w-0 flex-col items-center gap-1"
                        >
                          <Image
                            src={position.icon}
                            alt={position.label}
                            width={28}
                            height={28}
                            className={`h-7 w-7 object-contain ${
                              position.count > 0 ? "opacity-100" : "opacity-35"
                            }`}
                          />
                          <div
                            className={`whitespace-nowrap text-[11px] font-black ${
                              isTopPosition ? "text-[#e75491]" : "text-[#a76886]"
                            }`}
                          >
                            {position.label}
                          </div>
                          <div className="whitespace-nowrap text-[10px] font-bold text-[#b15b82]">
                            {positionPercentLabel}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ) : (
                <div className="mt-4 flex flex-1 items-center justify-center rounded-[1.25rem] bg-[#fff0f7] px-4 py-6 text-center text-sm font-bold text-[#a76886]">
                  선택한 구간의 포지션 기록이 없습니다.
                </div>
              )}
            </section>
          </div>
        </div>
        <div className="mt-3 flex flex-col gap-3 md:flex-row">
          <div className="toss-sidebar flex shrink-0 flex-col gap-3 md:w-[20rem]">
            <div className="flex flex-col overflow-hidden rounded-[1.5rem] border border-white/75 bg-white/95 shadow-[0_18px_46px_rgba(205,79,134,0.12)] ring-1 ring-[#f8dce8]/55">
              <div className="border-b border-[#f8dce8] px-5 py-3">
                <div className="text-[15px] font-black text-[#69324b]">
                  챔피언 숙련도
                </div>
              </div>
              <ul className="flex flex-col">
                {topMasteries.length > 0 ? (
                  topMasteries.map((mastery, index) => (
                    <li
                      key={mastery.championId}
                      className="flex items-center border-b border-[#f9e5ee] px-5 py-3 last:border-b-0"
                    >
                      <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[#fff0f7] text-xs font-black text-[#e75491]">
                        {index + 1}
                      </div>
                      <FallbackImage
                        sources={[
                          championImageById[mastery.championId]
                            ? getChampionMasteryIconUrl(
                                championImageById[mastery.championId]
                              )
                            : null,
                          getChampionMasteryFallbackIconUrl(mastery.championId),
                          "/sad-summoner.svg",
                        ]}
                        alt={mastery.championName}
                        width={40}
                        height={40}
                        className="ml-3 h-12 w-12 shrink-0 rounded-xl border border-[#ffd1e3] bg-[#fff0f7] object-cover"
                        placeholderClassName="ml-3 h-12 w-12 shrink-0 rounded-xl border border-[#ffd1e3] bg-[#fff0f7]"
                      />
                      <div className="ml-2.5 min-w-0 flex-1">
                        <div className="truncate font-black text-[#69324b]">
                          {mastery.championName || "챔피언"}
                        </div>
                        <div className="text-xs font-bold text-[#a76886]">
                          Lv.{mastery.championLevel} ·{" "}
                          {mastery.championPoints?.toLocaleString()}점
                        </div>
                      </div>
                    </li>
                  ))
                ) : (
                  <li className="flex flex-col items-center px-5 py-7 text-center">
                    <Image
                      src="/sad-summoner.svg"
                      alt="숙련도 없음"
                      width={96}
                      height={80}
                      className="h-20 w-24 object-contain"
                    />
                    <div className="mt-2 text-sm font-black text-[#69324b]">
                      숙련도 정보가 없어요
                    </div>
                    <div className="mt-1 text-xs font-bold text-[#a76886]">
                      API 응답을 받으면 여기에 표시됩니다.
                    </div>
                  </li>
                )}
              </ul>
            </div>
            <div className="flex flex-col rounded-[1.5rem] border border-white/75 bg-white/95 shadow-[0_18px_46px_rgba(205,79,134,0.12)] ring-1 ring-[#f8dce8]/55">
              <div className="border-b border-[#f8dce8] px-5 py-3">
                <div className="text-[15px] font-black text-[#69324b]">
                  선택 구간 모스트 챔피언
                </div>
                <div className="mt-1 text-xs text-[#64748b]">
                  {matchSummary.games}게임 기준
                </div>
              </div>
              <ul className="flex flex-col">
                {matchSummary.topChampions.length > 0 ? (
                  matchSummary.topChampions.map((champion) => (
                    <li
                      key={champion.championId}
                      className="flex items-center border-b border-[#f9e5ee] px-5 py-3 last:border-b-0"
                    >
                      <Image
                        src={getChampionIconUrl(champion.championNameEn)}
                        alt={champion.championNameKo}
                        width={38}
                        height={38}
                        className="rounded-xl"
                      />
                      <div className="ml-2.5 flex min-w-0 flex-col py-0.5">
                        <div className="truncate font-semibold">
                          {champion.championNameKo}
                        </div>
                        <div className="text-sm text-[#9fb2cc]">
                          {champion.kda.toFixed(2)} KDA
                        </div>
                      </div>
                      <div className="ml-auto flex flex-col text-right text-sm">
                        <div>{formatPercent(champion.winRate)}</div>
                        <div className="text-[#9fb2cc]">
                          {champion.games}게임
                        </div>
                      </div>
                    </li>
                  ))
                ) : (
                  <li className="px-5 py-6 text-sm text-[#9fb2cc]">
                    선택한 구간의 전적이 없습니다.
                  </li>
                )}
              </ul>
            </div>
          </div>
          <div className="flex min-w-0 flex-1 flex-col gap-3 md:ml-0 xl:max-w-[82.5rem]">
            <MatchFilterTabs
              activeFilter={activeMatchFilter}
              counts={filterCounts}
              onChange={setActiveMatchFilter}
            />
            <div className="grid auto-rows-fr items-stretch gap-3 xl:grid-cols-[2fr_1fr]">
              <div className="flex h-full min-h-[18rem] overflow-hidden rounded-[1.75rem] border border-white/75 bg-white/95 p-5 shadow-[0_22px_58px_rgba(205,79,134,0.14)] ring-1 ring-[#f8dce8]/55">
                <div className="grid h-full w-full grid-cols-[11.75rem_minmax(0,1fr)] gap-5">
                  <div className="flex min-w-0 flex-col items-center justify-center gap-3 rounded-[1.5rem] bg-[#fff7fb] p-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.9),0_12px_28px_rgba(205,79,134,0.08)]">
                    <div
                      className="relative h-36 w-36 rounded-full p-[10px] shadow-[0_16px_34px_rgba(47,128,237,0.16),0_16px_34px_rgba(255,95,126,0.13)]"
                      style={buildWinLossPieStyle(
                        matchSummary.wins,
                        matchSummary.losses
                      )}
                    >
                      <div className="flex h-full w-full flex-col items-center justify-center rounded-full bg-white shadow-inner shadow-[#ffd1e3]/70">
                        <div className="text-[11px] font-black uppercase tracking-wide text-[#a76886]">
                          Win Rate
                        </div>
                        <div className="mt-1 whitespace-nowrap text-3xl font-black leading-none text-[#69324b]">
                          {formatPercent(matchSummary.winRate)}
                        </div>
                      </div>
                    </div>
                    <div className="grid w-full grid-cols-2 gap-2 text-sm font-black">
                      <div className="flex items-center justify-center gap-1.5 whitespace-nowrap rounded-2xl bg-[#eef6ff] px-3 py-2 text-[#2f80ed]">
                        <span className="h-2.5 w-2.5 shrink-0 rounded-full bg-[#2f80ed]" />
                        승 {matchSummary.wins}
                      </div>
                      <div className="flex items-center justify-center gap-1.5 whitespace-nowrap rounded-2xl bg-[#fff0f4] px-3 py-2 text-[#ff4f73]">
                        <span className="h-2.5 w-2.5 shrink-0 rounded-full bg-[#ff5f7e]" />
                        패 {matchSummary.losses}
                      </div>
                    </div>
                  </div>

                  <div className="flex min-w-0 flex-col justify-between gap-3">
                    <div>
                      <div className="text-lg font-black text-[#69324b]">
                        {matchFilters.find(
                          (filter) => filter.key === activeMatchFilter
                        )?.label || "전체"}{" "}
                        게임 요약
                      </div>
                      <div className="mt-2 text-[1.8rem] font-black leading-tight tracking-normal text-[#69324b]">
                        {matchSummary.games}게임 {matchSummary.wins}승{" "}
                        {matchSummary.losses}패
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <div className="min-w-0 rounded-[1.25rem] bg-[#fff7fb] px-4 py-4 shadow-[inset_0_0_0_1px_rgba(248,220,232,0.7)]">
                        <div className="whitespace-nowrap text-sm font-black text-[#a76886]">
                          평균 KDA
                        </div>
                        <div className="mt-2 whitespace-nowrap text-3xl font-black leading-none text-[#69324b]">
                          {matchSummary.kda.toFixed(2)}
                        </div>
                        <div className="mt-2 truncate text-sm font-bold text-[#a76886]">
                          {matchSummary.kills}/{matchSummary.deaths}/
                          {matchSummary.assists}
                        </div>
                      </div>

                      <div className="min-w-0 rounded-[1.25rem] bg-[#fff7fb] px-4 py-4 shadow-[inset_0_0_0_1px_rgba(248,220,232,0.7)]">
                        <div className="whitespace-nowrap text-sm font-black text-[#a76886]">
                          평균 AI Score
                        </div>
                        <div className="mt-2 whitespace-nowrap text-3xl font-black leading-none text-[#69324b]">
                          {matchSummary.averageAiScore.toFixed(1)}
                        </div>
                        <div className="mt-2 truncate text-sm font-bold text-[#a76886]">
                          평균 점수
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="relative flex h-full min-h-[18rem] flex-col rounded-[1.5rem] border border-white/75 bg-white/95 p-5 shadow-[0_22px_58px_rgba(205,79,134,0.13)] ring-1 ring-[#f8dce8]/55">
                <div className="flex items-center justify-between gap-3">
                  <div className="whitespace-nowrap text-lg font-black text-[#69324b]">
                    최근 많이 플레이한 챔피언
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <div className="rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#a76886]">
                      최근 {recentChampionSummary.games}게임
                    </div>
                    {hiddenRecentChampions.length > 0 && (
                      <div className="group relative">
                        <button
                          type="button"
                          className="rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#a76886] outline-none transition-colors hover:bg-[#ffe4ef] focus-visible:ring-2 focus-visible:ring-[#f45f9c]/35"
                        >
                          +{hiddenRecentChampions.length}
                        </button>
                        <div className="pointer-events-none absolute right-0 top-full z-30 mt-2 w-[20rem] rounded-[1.25rem] border border-white/80 bg-white/95 p-3 opacity-0 shadow-[0_22px_54px_rgba(105,50,75,0.18)] ring-1 ring-[#f8dce8]/75 transition-opacity group-hover:opacity-100 group-focus-within:opacity-100">
                          <div className="mb-2 text-xs font-black text-[#a76886]">
                            나머지 챔피언
                          </div>
                          <div className="flex max-h-64 flex-col gap-2 overflow-y-auto pr-1">
                            {hiddenRecentChampions.map((champion, index) => (
                              <div
                                key={champion.championId}
                                className="grid grid-cols-[1.5rem_2.25rem_minmax(0,1fr)_4rem] items-center gap-2 rounded-2xl bg-[#fff7fb] p-2 shadow-[inset_0_0_0_1px_rgba(248,220,232,0.7)]"
                              >
                                <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-white text-[11px] font-black text-[#e75491]">
                                  {index + 4}
                                </div>
                                <Image
                                  src={getChampionIconUrl(
                                    champion.championNameEn
                                  )}
                                  width={36}
                                  height={36}
                                  alt={champion.championNameKo}
                                  className="rounded-xl shadow-[0_6px_14px_rgba(98,56,77,0.12)]"
                                />
                                <div className="min-w-0">
                                  <div className="truncate text-sm font-black text-[#69324b]">
                                    {champion.championNameKo}
                                  </div>
                                  <div className="truncate text-xs font-bold text-[#a76886]">
                                    {champion.games}게임 · 승률{" "}
                                    {formatPercent(champion.winRate)}
                                  </div>
                                </div>
                                <div className="whitespace-nowrap text-right text-xs font-black text-[#b15b82]">
                                  {champion.kda.toFixed(2)}
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
                <div className="mt-4 flex flex-1 flex-col justify-between gap-2.5">
                  {recentChampionSummary.topChampions.length > 0 ? (
                    recentChampionSummary.topChampions.map((champion, index) => (
                      <div
                        key={champion.championId}
                        className="grid grid-cols-[1.5rem_2.625rem_minmax(0,1fr)_4.5rem] items-center gap-2.5 rounded-2xl bg-[#fff7fb] p-2.5 shadow-[inset_0_0_0_1px_rgba(248,220,232,0.7)] transition-colors hover:bg-[#fff0f7]"
                      >
                        <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-white text-xs font-black text-[#e75491]">
                          {index + 1}
                        </div>
                        <Image
                          src={getChampionIconUrl(champion.championNameEn)}
                          width={42}
                          height={42}
                          alt={champion.championNameKo}
                          className="rounded-xl shadow-[0_6px_14px_rgba(98,56,77,0.12)]"
                        />
                        <div className="min-w-0 flex-1">
                          <div className="truncate whitespace-nowrap text-sm font-black text-[#69324b]">
                            {champion.championNameKo}
                          </div>
                          <div className="whitespace-nowrap text-xs font-bold text-[#a76886]">
                            {champion.games}게임 · 승률{" "}
                            {formatPercent(champion.winRate)}
                          </div>
                        </div>
                        <div className="whitespace-nowrap text-right text-xs font-black text-[#b15b82]">
                          {champion.kda.toFixed(2)} KDA
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="flex flex-1 items-center justify-center rounded-2xl bg-[#fff0f7] px-4 py-8 text-center text-sm font-bold text-[#a76886]">
                      최근 10게임의 챔피언 기록이 없습니다.
                    </div>
                  )}
                </div>
              </div>
            </div>
            <div className="flex flex-col gap-3">
              {filteredMatches.map((match, index) => {
                const isExpanded = expandedMatches[match.metaData.matchId];
                const previousMatch = filteredMatches[index - 1];
                const matchDateKey = getMatchDateKey(
                  match.metaData.gameEndTimestamp
                );
                const dateStats = matchDateStats.get(matchDateKey);
                const shouldShowDateSeparator =
                  !previousMatch ||
                  getMatchDateKey(previousMatch.metaData.gameEndTimestamp) !==
                    matchDateKey;

                return (
                  <React.Fragment key={match.metaData.matchId}>
                    {shouldShowDateSeparator && (
                      <div className="flex items-center gap-3 px-2 py-1">
                        <div className="h-px flex-1 bg-[#f8dce8]" />
                        <div className="group relative">
                          <div
                            tabIndex={0}
                            className="cursor-default rounded-full bg-white/85 px-4 py-1.5 text-xs font-black text-[#a76886] shadow-[inset_0_0_0_1px_rgba(248,220,232,0.85),0_8px_18px_rgba(205,79,134,0.08)] outline-none transition-colors focus-visible:bg-[#fff0f7] focus-visible:ring-2 focus-visible:ring-[#f45f9c]/35"
                          >
                            {getMatchDateLabel(match.metaData.gameEndTimestamp)}
                          </div>
                          <div className="pointer-events-none absolute left-1/2 top-full z-20 mt-2 -translate-x-1/2 whitespace-nowrap rounded-2xl bg-[#69324b] px-4 py-2 text-xs font-black text-white opacity-0 shadow-[0_14px_30px_rgba(105,50,75,0.22)] transition-opacity group-hover:opacity-100 group-focus-within:opacity-100">
                            {dateStats
                              ? `${dateStats.wins}승 ${dateStats.losses}패`
                              : "전적 없음"}
                          </div>
                        </div>
                        <div className="h-px flex-1 bg-[#f8dce8]" />
                      </div>
                    )}
                    <MatchCard
                      isExpanded={isExpanded}
                      match={match}
                      queueName={getQueueName(match.metaData.queueId)}
                      rune={rune}
                      spell={spell}
                      onSummonerClick={handleSummonerClick}
                      onToggleExpand={toggleMatchExpand}
                    />
                  </React.Fragment>
                );
              })}
              <MatchLoadMoreButton
                hasMoreMatches={hasMoreMatches}
                isDisabled={!data?.puuid || isLoadingMoreMatches || isRefreshing}
                isLoading={isLoadingMoreMatches || isRefreshing}
                onLoadMore={loadMoreMatches}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
