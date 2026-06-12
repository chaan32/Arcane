import { apiJson } from "@/services/apiClient";

export interface Ranking {
  rankers: Ranker[];
  totalPage: number;
  currentPage: number;
}

export interface Ranker {
  puuid: string;
  gameName: string;
  tagLine: string;
  leaguePoints: number;
  wins: number;
  losses: number;
  rank: string;
  winRate: number;
  veteran: boolean;
  inactive: boolean;
  freshBlood: boolean;
  hotStreak: boolean;
  profileIconId: number;
  summonerLevel: number;
}

export type RankingTier = "all" | "challenger" | "grandmaster" | "master";

export const emptyRanking: Ranking = {
  rankers: [],
  totalPage: 0,
  currentPage: 0,
};

export const rankingApi = {
  getRanking: (tier: RankingTier, page: number): Promise<Ranking> =>
    apiJson<Ranking>(`/api/v1/ranker/${tier}/${page}`, {
      method: "GET",
    }),
};
