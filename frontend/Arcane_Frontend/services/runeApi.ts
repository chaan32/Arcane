import { apiJson } from "@/services/apiClient";
import type { Rune, RuneTreePath } from "@/types/rune";

export const runeApi = {
  // 룬 ID 배열로 룬 정보 가져오기
  getRunesByIds: async (runeIds: number[]): Promise<Record<number, Rune>> => {
    const uniqueIds = [
      ...new Set(runeIds.filter((id): id is number => id !== undefined)),
    ];

    const entries = await Promise.all(
      uniqueIds.map(async (runeId) => {
        try {
          const data = await apiJson<Rune | undefined>(`/api/v1/rune/${runeId}`);
          if (!data) return undefined;

          return [runeId, data] as const;
        } catch (error) {
          console.error(`Error fetching rune ${runeId}:`, error);
          return undefined;
        }
      })
    );

    const successfulEntries = entries.filter(
      (entry): entry is readonly [number, Rune] => Array.isArray(entry)
    );

    return Object.fromEntries(successfulEntries);
  },

  getRuneTree: async (): Promise<RuneTreePath[]> => {
    return apiJson<RuneTreePath[]>("/api/v1/rune/tree");
  },
};
