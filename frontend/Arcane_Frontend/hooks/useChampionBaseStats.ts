import { useMemo } from "react";
import type { ChampionStats } from "@/types/champion";

interface BaseStat {
  id: number;
  name: string;
  growth: string;
  finalValue: string;
  rank: string;
}

// 챔피언 상세 페이지의 기본 정보 탭에 표시할 기본 능력치 리스트를 계산하는 커스텀 훅
export function useChampionBaseStats(stats?: ChampionStats): BaseStat[] {
  return useMemo(() => {
    if (!stats) return [];

    // 최종 수치 계산 함수 (18레벨 기준)
    const calcFinalValue = (base: number, perLevel: number): string => {
      return (base + perLevel * 17).toFixed(2);
    };

    // 공격 속도 전용 최종 수치 (퍼센트 성장 반영)
    const calcAttackSpeedFinalValue = (
      base: number,
      percentPerLevel: number
    ): string => {
      const multiplier = percentPerLevel / 100;
      return (base * (1 + multiplier * 17)).toFixed(3);
    };

    // 성장률 포맷 함수
    const formatGrowth = (base: number, perLevel: number): string => {
      return `${base.toFixed(2)} (+${perLevel.toFixed(2)})`;
    };

    // 공격 속도 성장률 포맷 함수
    const formatAttackSpeedGrowth = (
      base: number,
      percentPerLevel: number
    ): string => {
      return `${base.toFixed(3)} (+${percentPerLevel.toFixed(2)}%)`;
    };

    return [
      {
        id: 1,
        name: "체력",
        growth: formatGrowth(stats.hp, stats.hpperlevel),
        finalValue: calcFinalValue(stats.hp, stats.hpperlevel),
        rank: "-",
      },
      {
        id: 2,
        name: "마나",
        growth: formatGrowth(stats.mp, stats.mpperlevel),
        finalValue: calcFinalValue(stats.mp, stats.mpperlevel),
        rank: "-",
      },
      {
        id: 3,
        name: "방어력",
        growth: formatGrowth(stats.armor, stats.armorperlevel),
        finalValue: calcFinalValue(stats.armor, stats.armorperlevel),
        rank: "-",
      },
      {
        id: 4,
        name: "마법 저항력",
        growth: formatGrowth(stats.spellblock, stats.spellblockperlevel),
        finalValue: calcFinalValue(stats.spellblock, stats.spellblockperlevel),
        rank: "-",
      },
      {
        id: 5,
        name: "공격력",
        growth: formatGrowth(stats.attackdamage, stats.attackdamageperlevel),
        finalValue: calcFinalValue(
          stats.attackdamage,
          stats.attackdamageperlevel
        ),
        rank: "-",
      },
      {
        id: 6,
        name: "공격 속도",
        growth: formatAttackSpeedGrowth(
          stats.attackspeed,
          stats.attackspeedperlevel
        ),
        // 공격 속도는 퍼센트 성장으로 계산
        finalValue: calcAttackSpeedFinalValue(
          stats.attackspeed,
          stats.attackspeedperlevel
        ),
        rank: "-",
      },
      {
        id: 7,
        name: "체력 재생",
        growth: formatGrowth(stats.hpregen, stats.hpregenperlevel),
        finalValue: calcFinalValue(stats.hpregen, stats.hpregenperlevel),
        rank: "-",
      },
      {
        id: 8,
        name: "마나 재생",
        growth: formatGrowth(stats.mpregen, stats.mpregenperlevel),
        finalValue: calcFinalValue(stats.mpregen, stats.mpregenperlevel),
        rank: "-",
      },
      {
        id: 9,
        name: "이동 속도",
        growth: `${stats.movespeed.toFixed(2)}`,
        finalValue: stats.movespeed.toFixed(2),
        rank: "-",
      },
      {
        id: 10,
        name: "공격 사거리",
        growth: `${stats.attackrange.toFixed(2)}`,
        finalValue: stats.attackrange.toFixed(2),
        rank: "-",
      },
    ];
  }, [stats]);
}
