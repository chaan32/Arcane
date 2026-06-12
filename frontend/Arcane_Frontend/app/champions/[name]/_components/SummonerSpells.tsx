"use client";

import React from "react";
import Image from "next/image";
import { getDataDragonSpellIconUrl } from "@/services/dataDragonApi";
import type {
  ChampionDetailDto,
  ChampionOptionStatDto,
} from "@/types/championDetail";
import type { Spell } from "@/types/spell";

interface SummonerSpellsProps {
  data: ChampionDetailDto[];
  laneIndex: number;
  spell: Record<number, Spell>;
}

const spellImageClassName =
  "h-[3.4rem] w-[3.4rem] bg-[#fff0f7] rounded-[0.65rem] border border-[#ffd1e3]";

const getSpellImageUrl = (imageFull?: string | null, version?: string | null) => {
  if (!imageFull) return null;
  if (
    imageFull.startsWith("http://") ||
    imageFull.startsWith("https://") ||
    imageFull.startsWith("/")
  ) {
    return imageFull;
  }

  return getDataDragonSpellIconUrl(imageFull, version);
};

function SpellImage({ alt, src }: { alt: string; src?: string | null }) {
  if (!src) {
    return (
      <div
        aria-label={alt}
        className={spellImageClassName}
        role="img"
      />
    );
  }

  return (
    <Image
      src={src}
      className={spellImageClassName}
      alt={alt}
      width={60}
      height={60}
      sizes="60px"
    />
  );
}

export function SummonerSpells({
  data,
  laneIndex,
  spell,
}: SummonerSpellsProps) {
  const detailChampBuild = data[laneIndex]?.detailChampBuild;
  const version = data[laneIndex]?.detailChampInfo.version;
  const fallbackSpellOptions: ChampionOptionStatDto[] = [
    {
      spell1Id: detailChampBuild?.summoner1Id,
      spell2Id: detailChampBuild?.summoner2Id,
    },
    {
      spell1Id: detailChampBuild?.summoner3Id,
      spell2Id: detailChampBuild?.summoner4Id,
    },
  ].filter(
    (option) => Number(option.spell1Id) > 0 && Number(option.spell2Id) > 0
  );
  const spellOptions: ChampionOptionStatDto[] =
    detailChampBuild?.topSummonerSpells
      ?.filter((option) => Number(option.spell1Id) > 0 && Number(option.spell2Id) > 0)
      .slice(0, 2) ??
    fallbackSpellOptions;

  if (spellOptions.length === 0) {
    return (
      <div className="flex flex-col">
        <h3 className="mb-[0.75rem] text-d-body3 text-text-default">
          소환사 주문 사용 비율
        </h3>
        <div className="flex h-[5.25rem] items-center rounded-[0.875rem] border border-[#ffd1e3] bg-[#fff7fb] px-[1rem] text-d-body3-r text-[#a76886]">
          주문 분석 데이터가 없습니다.
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col">
      <h3 className="mb-[0.75rem] text-d-body3 text-text-default">
        소환사 주문 사용 비율
      </h3>
      <div className="flex gap-[0.75rem]">
        {spellOptions.map((option, idx) => {
          const firstSpellId = Number(option.spell1Id);
          const secondSpellId = Number(option.spell2Id);
          const firstSpellImage = getSpellImageUrl(spell[firstSpellId]?.imageFull, version);
          const secondSpellImage = getSpellImageUrl(spell[secondSpellId]?.imageFull, version);

          return (
            <div
              key={`${firstSpellId}-${secondSpellId}`}
              className="flex min-w-[9.25rem] flex-col gap-[0.55rem] rounded-[0.875rem] border border-[#ffd1e3] bg-white/95 px-[0.9rem] py-[0.8rem] shadow-[0_12px_24px_rgba(244,114,182,0.10)]"
            >
              <span className="w-fit rounded-full bg-[#fff0f7] px-[0.55rem] py-[0.15rem] text-[0.75rem] font-black text-[#e75491]">
                TOP {idx + 1}
              </span>
              <div className="flex gap-[0.5rem]">
                <SpellImage src={firstSpellImage} alt={`spell-${firstSpellId}`} />
                <SpellImage src={secondSpellImage} alt={`spell-${secondSpellId}`} />
              </div>
              {option.pickRate != null && (
                <span className="text-[0.82rem] font-black text-[#69324b]">
                  {Number(option.pickRate).toFixed(1)}%
                </span>
              )}
              {option.games != null && (
                <span className="text-[0.72rem] font-bold text-[#a76886]">
                  {option.games.toLocaleString()}게임
                </span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
