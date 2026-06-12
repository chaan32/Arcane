export type RankedParticipant = {
  gameName: string;
  tagLine: string;
  soloRankTier?: string | null;
  soloRankLP?: number | null;
};

export type AiScoreParticipant = {
  puuid: string;
  ourScore?: number | null;
  teamLuckScore?: number | null;
};

const matchDateLabelFormatter = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "long",
  day: "numeric",
});

export const getTimeAgo = (timestamp: number): string => {
  const now = Date.now();
  const diff = now - timestamp;

  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  const months = Math.floor(days / 30);
  const years = Math.floor(days / 365);

  if (years > 0) return `${years}년 전`;
  if (months > 0) return `${months}개월 전`;
  if (days > 0) return `${days}일 전`;
  if (hours > 0) return `${hours}시간 전`;
  if (minutes > 0) return `${minutes}분 전`;

  return `${seconds}초 전`;
};

export const getElapsedLabel = (timestamp: number, now: number): string => {
  const diff = Math.max(0, now - timestamp);
  const minutes = Math.floor(diff / (60 * 1000));
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  const months = Math.floor(days / 30);
  const years = Math.floor(days / 365);

  if (years > 0) return `${years}년 전`;
  if (months > 0) return `${months}개월 전`;
  if (days > 0) return `${days}일 전`;
  if (hours > 0) return `${hours}시간 전`;
  if (minutes > 0) return `${minutes}분 전`;

  return "방금 전";
};

export const getRemainingRefreshLabel = (remainingMs: number): string => {
  const remainingMinutes = Math.max(1, Math.ceil(remainingMs / (60 * 1000)));

  return `${remainingMinutes}분 후 가능`;
};

export const getMatchDateKey = (timestamp: number): string => {
  const date = new Date(timestamp);

  if (Number.isNaN(date.getTime())) return "unknown";

  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
};

export const getMatchDateLabel = (timestamp: number): string => {
  const date = new Date(timestamp);

  return Number.isNaN(date.getTime())
    ? "날짜 정보 없음"
    : matchDateLabelFormatter.format(date);
};

export const formatGameDuration = (durationInSeconds: number): string => {
  const minutes = Math.floor(durationInSeconds / 60);
  const seconds = durationInSeconds % 60;

  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
};

export const extractTierName = (fullTier: string): string => {
  if (!fullTier) return "";

  return fullTier.replace(/\s+[IVX]+$/, "");
};

export const getParticipantLabel = (player: RankedParticipant) =>
  `${player.gameName}#${player.tagLine}`;

export const getParticipantSummaryLabel = (player: RankedParticipant) =>
  player.gameName || getParticipantLabel(player);

export const normalizeTierName = (tier?: string | null) => {
  if (!tier) return "UNRANKED";

  return extractTierName(tier).replace(/[_-]/g, " ").trim().toUpperCase();
};

export const getTierShortLabel = (tier?: string | null) => {
  const normalizedTier = normalizeTierName(tier);

  if (normalizedTier.includes("GRAND")) return "GM";
  if (normalizedTier === "UNRANKED") return "U";

  return normalizedTier.charAt(0);
};

export const getTierBadgeClassName = (tier?: string | null) => {
  const normalizedTier = normalizeTierName(tier);
  const base =
    "inline-flex h-5 min-w-5 shrink-0 items-center justify-center rounded-md border px-1.5 text-[10px] font-black leading-none tracking-wide";

  if (normalizedTier.includes("CHALLENGER")) {
    return `${base} border-[#ffc658]/60 bg-[#fff3bf] text-[#b7791f]`;
  }
  if (normalizedTier.includes("GRAND")) {
    return `${base} border-[#ff9aa8]/50 bg-[#ffe2e8] text-[#d64b66]`;
  }
  if (normalizedTier.includes("MASTER")) {
    return `${base} border-[#d8b4fe]/50 bg-[#f3e8ff] text-[#8b5cf6]`;
  }
  if (normalizedTier.includes("DIAMOND")) {
    return `${base} border-[#c4b5fd]/50 bg-[#ede9fe] text-[#7c3aed]`;
  }
  if (normalizedTier.includes("EMERALD")) {
    return `${base} border-[#86efac]/50 bg-[#dcfce7] text-[#15803d]`;
  }
  if (normalizedTier.includes("PLATINUM")) {
    return `${base} border-[#99f6e4]/50 bg-[#ccfbf1] text-[#0f766e]`;
  }
  if (normalizedTier.includes("GOLD")) {
    return `${base} border-[#f7d36a]/50 bg-[#fef3c7] text-[#b7791f]`;
  }
  if (normalizedTier.includes("SILVER")) {
    return `${base} border-[#d6c7d1]/50 bg-[#fff0f7] text-[#a76886]`;
  }
  if (normalizedTier.includes("BRONZE")) {
    return `${base} border-[#f1a67a]/50 bg-[#ffeadb] text-[#b45309]`;
  }
  if (normalizedTier.includes("IRON")) {
    return `${base} border-[#d6c7d1]/40 bg-[#fff0f7] text-[#a76886]`;
  }

  return `${base} border-[#ffd1e3] bg-[#fff0f7] text-[#a76886]`;
};

export const getParticipantTierBadgeClassName = (player: RankedParticipant) =>
  getTierBadgeClassName(player.soloRankTier);

export const getParticipantTierShortLabel = (player: RankedParticipant) =>
  getTierShortLabel(player.soloRankTier);

export const getTierMetaLabel = (player: RankedParticipant) => {
  const normalizedTier = normalizeTierName(player.soloRankTier);

  if (normalizedTier === "UNRANKED") return "Unranked";

  return typeof player.soloRankLP === "number" ? `${player.soloRankLP} LP` : "";
};

export const getAiScoreValue = (player: AiScoreParticipant): number =>
  player.ourScore ?? player.teamLuckScore ?? 0;

export const getAiScoreRank = (
  players: AiScoreParticipant[],
  puuid: string
): number | null => {
  const currentPlayer = players.find((player) => player.puuid === puuid);

  if (!currentPlayer) return null;

  const currentScore = getAiScoreValue(currentPlayer);

  return (
    players.filter((player) => getAiScoreValue(player) > currentScore).length +
    1
  );
};

export const formatAiScoreRank = (rank: number | null): string =>
  rank ? `${rank}등` : "-";
