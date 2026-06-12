interface LaneData {
  detailChampBuild?: { lane?: string };
  detailChampInfo?: { winRate?: number; percent?: number; gameCount?: number };
}

interface LaneSelectorProps {
  data: LaneData[];
  laneIndex: number;
  onLaneChange: (index: number) => void;
}

export function LaneSelector({
  data,
  laneIndex,
  onLaneChange,
}: LaneSelectorProps) {
  return (
    <div className="flex flex-wrap gap-[0.75rem] lg:mb-[1.5rem]">
      {data.map((laneData, idx) => (
        <button
          type="button"
          key={laneData.detailChampBuild?.lane ?? idx}
          onClick={() => onLaneChange(idx)}
          className={`group min-w-[8.5rem] rounded-[0.875rem] border px-[1rem] py-[0.75rem] text-left transition-all cursor-pointer ${
            laneIndex === idx
              ? "bg-[#ff7aae] text-white border-[#ff9ac5] shadow-[0_12px_26px_rgba(244,114,182,0.28)]"
              : "bg-white/90 text-[#8f5570] border-[#ffd1e3] hover:border-[#ff9ac5] hover:bg-[#fff0f7]"
          }`}
        >
          <span className="block text-[0.8rem] font-black uppercase tracking-[0.04em]">
            {laneData.detailChampBuild?.lane ?? "-"}
          </span>
          <span className="mt-[0.25rem] block text-[1.45rem] font-black leading-none">
            {Number(laneData.detailChampInfo?.percent ?? 0).toFixed(1)}%
          </span>
          <span
            className={`mt-[0.3rem] block text-[0.78rem] font-bold ${
              laneIndex === idx ? "text-white/90" : "text-[#b06c8b]"
            }`}
          >
            승률 {Number(laneData.detailChampInfo?.winRate ?? 0).toFixed(1)}%
          </span>
        </button>
      ))}
    </div>
  );
}
