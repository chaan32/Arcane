import { ChampionProfile } from "./ChampionProfile";
import { ChampionStats } from "./ChampionStats";
import type { ChampionInfo } from "@/types/champion";
import type { ChampionDetailDto } from "@/types/championDetail";

interface ChampionDetailHeaderProps {
  data: ChampionDetailDto[];
  laneIndex: number;
  skill?: ChampionInfo;
  championDisplayName: string;
}

export function ChampionDetailHeader({
  data,
  laneIndex,
  skill,
  championDisplayName,
}: ChampionDetailHeaderProps) {
  const selectedInfo = data[laneIndex]?.detailChampInfo;
  const firstInfo = data[0]?.detailChampInfo;
  const championNameEn =
    firstInfo?.championNameEn || skill?.nameEn || championDisplayName;
  const championName =
    firstInfo?.championName || skill?.nameKo || championDisplayName;

  return (
    <div className="flex items-stretch justify-between gap-[2rem] rounded-[1.5rem] border border-[#ffd1e3] bg-white/90 p-[1.5rem] shadow-[0_22px_46px_rgba(244,114,182,0.16)] backdrop-blur">
      <ChampionProfile
        championNameEn={championNameEn}
        championName={championName}
        championDisplayName={championDisplayName}
        skill={skill}
      />

      <ChampionStats
        tier={selectedInfo?.tier}
        winRate={selectedInfo?.winRate}
        pickRate={selectedInfo?.pickRate}
        gameCount={selectedInfo?.gameCount}
      />
    </div>
  );
}
