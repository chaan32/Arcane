"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { ChampionDetailDto, StyleDto } from "@/types/championDetail";
import { runeApi } from "@/services/runeApi";
import type { Rune, RuneTreePath } from "@/types/rune";

// StyleDto에 Rune 정보를 합친 타입
export interface RuneBuild extends StyleDto {
  runeInfo?: Rune;
}

export interface ChampionRunesResult {
  builds: RuneBuild[];
  runeTree: RuneTreePath[];
}

// 현재 챔피언 라인에 맞는 룬 ID를 모아서 룬 상세를 가져오는 훅
// 현재는 핵심 룬과 보조 룬만 가져오도록 구현 + 추후에 필요시 세부 룬 추가하면 될 듯
// 핵심 룬이랑 보조룬 구분 되도록 (styleInfo 추가)
export function useChampionRunes(
  data: ChampionDetailDto[],
  laneIndex: number
): ChampionRunesResult {
  const styles = data[laneIndex]?.detailChampBuild?.perks?.styles;
  const styleIds = useMemo(() => styles?.map((style) => style.style) ?? [], [styles]);

  const runeQuery = useQuery({
    queryKey: ["championRunes", styleIds],
    queryFn: () => runeApi.getRunesByIds(styleIds),
    enabled: styleIds.length > 0,
    staleTime: 1000 * 60 * 30,
  });

  const runeTreeQuery = useQuery({
    queryKey: ["runeTree"],
    queryFn: runeApi.getRuneTree,
    enabled: Boolean(styles),
    staleTime: 1000 * 60 * 60,
  });

  const builds = useMemo(() => {
    if (!styles) return [];

    const runeData = runeQuery.data ?? {};

    return styles.map((style) => ({
      ...style,
      runeInfo: runeData[style.style],
    }));
  }, [runeQuery.data, styles]);

  return {
    builds,
    runeTree: runeTreeQuery.data ?? [],
  };
}
