"use client";
import { useQuery } from "@tanstack/react-query";
import { summonerApi } from "@/services/summonerApi";
import { DDRAGON_VERSION } from "@/services/dataDragonApi";
import type { SummonerBase, SummonerDropdownType } from "@/types/summoner";
import useDebounce from "./useDebounce";

const getProfileIconUrl = (icon?: number) =>
  typeof icon === "number" && icon > 0
    ? `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}/img/profileicon/${icon}.png`
    : "";

export const useSummonerSearch = (searchQuery: string) => {
  // 디바운스 적용(0.5초)
  const debouncedQuery = useDebounce(searchQuery, 500);
  const trimmedQuery = debouncedQuery.trim();

  return useQuery({
    // 검색어에 따라 캐싱되도록
    queryKey: ["summonerSearch", trimmedQuery],

    queryFn: async (): Promise<SummonerDropdownType[]> => {
      if (!trimmedQuery) return [];

      const data: SummonerBase[] = await summonerApi.searchByKeyword(trimmedQuery);
      if (!Array.isArray(data) || data.length === 0) return [];

      // 데이터 4개로 잘라
      const limitedData = data.slice(0, 4);
      return limitedData.map((summoner) => ({
        id: summoner.id,
        puuid: summoner.puuid,
        gameName: summoner.gameName,
        tagLine: summoner.tagLine,
        profileUrl: getProfileIconUrl(summoner.icon),
        level: summoner.level || 0,
      }));
    },

    // 옵션
    enabled: trimmedQuery.length > 0, // 검색어가 있을 때만 실행
    staleTime: 1000 * 60 * 5, // 5분동안 캐시된 데이터 사용
    retry: false,
  });
};
