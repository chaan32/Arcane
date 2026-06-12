"use client";

import React from "react";
import Image from "next/image";
import { getDataDragonItemIconUrl } from "@/services/dataDragonApi";
import type {
  ChampionDetailDto,
  ChampionOptionStatDto,
} from "@/types/championDetail";

interface ItemBuildProps {
  data: ChampionDetailDto[];
  laneIndex: number;
}

export function ItemBuild({ data, laneIndex }: ItemBuildProps) {
  const buildData = data[laneIndex]?.detailChampBuild;
  const version = data[laneIndex]?.detailChampInfo.version;
  const fallbackItems: ChampionOptionStatDto[] = [
    buildData?.item01,
    buildData?.item02,
    buildData?.item03,
  ]
    .filter((itemId): itemId is number => Number(itemId) > 0)
    .map((itemId) => ({ itemId }));
  const topItems: ChampionOptionStatDto[] =
    buildData?.topItems
      ?.filter((item) => Number(item.itemId) > 0)
      .slice(0, 3) ??
    fallbackItems;

  return (
    <div className="flex flex-col">
      <h3 className="mb-[0.75rem] text-d-body3 text-text-default">
        아이템 구매 TOP 3
      </h3>
      <div className="grid grid-cols-3 gap-[0.75rem]">
        {topItems.length === 0 ? (
          <div className="col-span-3 flex h-[5.25rem] items-center rounded-[0.875rem] border border-[#ffd1e3] bg-[#fff7fb] px-[1rem] text-d-body3-r text-[#a76886]">
            아이템 분석 데이터가 없습니다.
          </div>
        ) : (
          topItems.map((item, index) => (
            <div
              key={`${item.itemId}-${index}`}
              className="flex min-w-[7rem] flex-col items-center gap-[0.5rem] rounded-[0.875rem] border border-[#ffd1e3] bg-white/95 px-[0.9rem] py-[0.8rem] shadow-[0_12px_24px_rgba(244,114,182,0.10)]"
            >
              <span className="rounded-full bg-[#fff0f7] px-[0.55rem] py-[0.15rem] text-[0.75rem] font-black text-[#e75491]">
                TOP {index + 1}
              </span>
              <Image
                src={getDataDragonItemIconUrl(`${item.itemId}.png`, version)}
                className="h-[3.75rem] w-[3.75rem] rounded-[0.65rem] border border-[#ffd1e3] bg-[#fff0f7]"
                alt={`item-${item.itemId}`}
                width={64}
                height={64}
                sizes="64px"
              />
              {item.pickRate != null && (
                <span className="text-[0.82rem] font-black text-[#69324b]">
                  {Number(item.pickRate).toFixed(1)}%
                </span>
              )}
              {item.games != null && (
                <span className="text-[0.72rem] font-bold text-[#a76886]">
                  {item.games.toLocaleString()}게임
                </span>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
