"use client";

import React from "react";
import type { ChampionDetailDto } from "@/types/championDetail";
import { RuneTabs } from "./RuneTabs";
import { ItemBuild } from "./ItemBuild";
import { SummonerSpells } from "./SummonerSpells";
import { useChampionRunes } from "@/hooks/useChampionRunes";
import { useChampionSpells } from "@/hooks/useChampionSpells";

interface ChampionBuildTabProps {
  data: ChampionDetailDto[];
  laneIndex: number;
  isLoading?: boolean;
  errorMessage?: string;
}

function AnalysisEmptyState({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="flex min-h-[16rem] flex-col items-center justify-center gap-[0.5rem] text-center">
      <p className="text-d-body2 text-[#69324b]">{title}</p>
      <p className="text-d-body3-r text-[#a76886]">{description}</p>
    </div>
  );
}

// [챔피언명 빌드] 탭 내용 컴포넌트
export function ChampionBuildTab({
  data,
  laneIndex,
  isLoading,
  errorMessage,
}: ChampionBuildTabProps) {
  const { builds, runeTree } = useChampionRunes(data, laneIndex);
  const spell = useChampionSpells(data, laneIndex);
  const selectedData = data[laneIndex];
  const statPerks = selectedData?.detailChampBuild?.perks?.statPerks;

  if (isLoading) {
    return (
      <AnalysisEmptyState
        title="챔피언 분석 데이터를 불러오는 중입니다."
        description="수집된 매치 데이터를 기반으로 빌드 정보를 준비하고 있습니다."
      />
    );
  }

  if (errorMessage) {
    return (
      <AnalysisEmptyState
        title="챔피언 분석 데이터를 불러오지 못했습니다."
        description={errorMessage}
      />
    );
  }

  if (!selectedData) {
    return (
      <AnalysisEmptyState
        title="아직 이 챔피언의 분석 데이터가 없습니다."
        description="수집된 매치 분석이 완료되면 실제 빌드 데이터가 표시됩니다."
      />
    );
  }

  return (
    <div className="flex flex-col w-full lg:pt-[1.25rem] lg:pb-[1.75rem]">
      {/* 룬 빌드 글자 */}
      <span className="text-d-body3 text-text-default lg:mb-[0.75rem]">
        룬 빌드
      </span>
      {/* 탭 + 룬 컴포넌트 영역  */}
      <RuneTabs builds={builds} runeTree={runeTree} statPerks={statPerks} />

      {/* 아이템 빌드 영역 */}
      <div className="mt-[3.75rem] grid grid-cols-[minmax(0,1fr)_auto] gap-[2rem]">
        <ItemBuild data={data} laneIndex={laneIndex} />
        <SummonerSpells data={data} laneIndex={laneIndex} spell={spell} />
      </div>
    </div>
  );
}
