"use client";

import React from "react";
import { useChampionBaseStats } from "@/hooks/useChampionBaseStats";
import type { ChampionStats } from "@/types/champion";

interface ChampionBaseStatsProps {
  stats?: ChampionStats;
}

export function ChampionBaseStats({ stats }: ChampionBaseStatsProps) {
  const baseStatsList = useChampionBaseStats(stats);
  return (
    <div className="flex flex-col w-full lg:pt-[1.25rem] lg:pb-[1rem]">
      <div className="text-d-body3 text-text-default lg:mb-[0.75rem]">
        기본 능력치
      </div>

      <div className="border-t border-[#ffd1e3] lg:pt-[1rem]">
        {/* 목록 헤더 */}
        <div className="grid grid-cols-[minmax(0,1fr)_10.5rem_4rem_4rem] gap-[7.5rem] items-center text-d-body4-r text-text-default lg:pb-[0.75rem] lg:pr-[0.75rem]">
          {/* 위치를 맞추기 위한 빈칸 */}
          <span className="text-right"> </span>
          <span className="text-right">기본 능력치 (레벨 당 상승)</span>
          <span className="text-right">최종 수치</span>
          <span className="text-right">챔피언 순위</span>
        </div>

        {/* 실제 데이터 리스트 */}
        <div className="flex flex-col">
          {baseStatsList.map((stat, index) => (
            <div
              key={stat.id}
              className={`grid grid-cols-[minmax(0,1fr)_10.5rem_4rem_4rem] gap-[7.5rem] items-center lg:py-[0.5rem] lg:px-[0.75rem] rounded-[0.25rem] ${
                index % 2 === 0 ? "bg-[#fff0f7]" : "bg-transparent"
              }`}
            >
              {/* 왼쪽 영역 */}
              <div className="flex items-center gap-[0.5rem]">
                <span className="w-[1.5rem] h-[1.5rem] rounded-full bg-[#ffe0ee]" />
                <span className="text-d-body3 text-text-default">
                  {stat.name}
                </span>
              </div>
              {/* 오른쪽 영역 */}
              <span className="text-right text-d-body3 text-text-default">
                {stat.growth}
              </span>
              <span className="text-right text-d-body3 text-text-default">
                {stat.finalValue}
              </span>
              <span className="text-right text-d-body3 text-text-default">
                {stat.rank}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
