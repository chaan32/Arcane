import { apiJson } from "@/services/apiClient";
import type { ChampionDetailDto } from "@/types/championDetail";

export const championDetailApi = {
  // 챔피언 상세 분석 정보 조회
  // 가져오는 정보-챔피언 상세 정보(라인 다르게) + 빌드 + 룬 + 관련 챔피언
  getChampionDetail: async (
    championName: string
  ): Promise<ChampionDetailDto[]> => {
    return apiJson<ChampionDetailDto[]>(
      `/api/v1/analysis/championDetail/${encodeURIComponent(championName)}`,
      { method: "GET" }
    );
  },
};
