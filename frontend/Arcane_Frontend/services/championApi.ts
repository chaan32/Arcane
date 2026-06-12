import { apiJson } from "@/services/apiClient";
import type { ChampionInfo } from "@/types/champion";

export const championApi = {
  // 챔피언 기본 정보 조회 (스킬, 스탯 등)
  getChampionByName: async (championName: string): Promise<ChampionInfo> => {
    const encodedName = encodeURIComponent(championName);
    return apiJson<ChampionInfo>(`/api/v1/champion/name/${encodedName}`, {
      method: "GET",
    });
  },
};
