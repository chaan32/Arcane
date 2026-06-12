"use client";

import { useQuery } from "@tanstack/react-query";
import { championDetailApi } from "@/services/championDetailApi";
import { championApi } from "@/services/championApi";

// 챔피언 상세 페이지에서 챔피언의 상세 정보와 스킬 정보를 동시에 가져오는 커스텀 훅
export function useChampionDetail(championName: string) {
  const championDetailQuery = useQuery({
    queryKey: ["championDetail", championName],
    queryFn: () => championDetailApi.getChampionDetail(championName),
    enabled: Boolean(championName),
    staleTime: 1000 * 60 * 5,
  });

  const championSkillQuery = useQuery({
    queryKey: ["championSkill", championName],
    queryFn: () => championApi.getChampionByName(championName),
    enabled: Boolean(championName),
    staleTime: 1000 * 60 * 30,
  });

  return {
    data: championDetailQuery.data ?? [],
    skill: championSkillQuery.data,
    isLoading: championDetailQuery.isLoading || championSkillQuery.isLoading,
    error:
      championDetailQuery.error instanceof Error
        ? championDetailQuery.error
        : championSkillQuery.error instanceof Error
          ? championSkillQuery.error
          : undefined,
  };
}
