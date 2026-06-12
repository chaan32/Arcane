import { apiJson } from "@/services/apiClient";

export type LanePositionValue = "top" | "jug" | "mid" | "adc" | "sup";

export interface ChampionTier {
  championId?: number;
  championNameEn: string;
  championName: string;
  championImageFull?: string;
  version?: string | null;
  tier: number;
  score: number;
  winRate: number;
  scoreDiff: number;
  pickRate?: number;
  counterChampions?: CounterChampion[];
}

export interface CounterChampion {
  championId?: number;
  championNameEn: string;
  championImageFull?: string;
  championImgUrl: string;
}

export interface ChampionSummary {
  championId?: number;
  championNameEn: string;
  championName: string;
  championImageFull?: string;
  version?: string | null;
}

export type LaneTierData = Record<LanePositionValue, ChampionTier[]>;

export const createEmptyLaneTierData = (): LaneTierData => ({
  top: [],
  jug: [],
  mid: [],
  adc: [],
  sup: [],
});

export const statisticsApi = {
  getChampionTier: (position: LanePositionValue): Promise<ChampionTier[]> =>
    apiJson<ChampionTier[]>(`/api/v1/analysis/tier/${position}`, {
      method: "GET",
    }),

  getAllLaneTiers: async (
    positions: readonly LanePositionValue[]
  ): Promise<LaneTierData> => {
    const entries = await Promise.all(
      positions.map(async (position) => {
        const result = await statisticsApi.getChampionTier(position);
        return [position, result] as const;
      })
    );

    return entries.reduce<LaneTierData>((acc, [position, result]) => {
      acc[position] = result;
      return acc;
    }, createEmptyLaneTierData());
  },

  getAllChampions: (): Promise<ChampionSummary[]> =>
    apiJson<ChampionSummary[]>("/api/v1/analysis/champions/all", {
      method: "GET",
    }),
};
