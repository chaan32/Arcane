"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { spellApi } from "@/services/spellApi";
import type { Spell } from "@/types/spell";
import type { ChampionDetailDto } from "@/types/championDetail";

// 챔피언 빌드 탭에서 소환사 주문 정보를 가져오는 훅
export function useChampionSpells(
  data: ChampionDetailDto[],
  laneIndex: number
): Record<number, Spell> {
  const build = data[laneIndex]?.detailChampBuild;
  const spellIds = useMemo(() => spellApi.extractSpellIds(build), [build]);

  const spellQuery = useQuery({
    queryKey: ["championSpells", spellIds],
    queryFn: () => spellApi.fetchSpellsByIds(spellIds),
    enabled: spellIds.length > 0,
    staleTime: 1000 * 60 * 30,
  });

  return spellQuery.data ?? {};
}
