interface ChampionStatsProps {
  tier?: number | string;
  winRate?: number;
  pickRate?: number;
  gameCount?: number;
}

export function ChampionStats({
  tier,
  winRate,
  pickRate,
  gameCount,
}: ChampionStatsProps) {
  const tierLabel = (() => {
    if (tier == null || tier === "") return "-";
    if (tier === 1 || tier === "1") return "OP";
    if (tier === 2 || tier === "2") return "1티어";
    if (tier === 3 || tier === "3") return "2티어";
    if (tier === 4 || tier === "4") return "3티어";
    if (tier === 5 || tier === "5") return "4티어";
    return String(tier);
  })();

  const stats = [
    {
      label: "티어",
      value: tierLabel,
      color: "text-[#e75491]",
      variant: "badge",
    },
    {
      label: "승률",
      value: winRate == null ? "-" : `${Number(winRate).toFixed(2)}%`,
    },
    {
      label: "픽률",
      value: pickRate == null ? "-" : `${Number(pickRate).toFixed(2)}%`,
    },
    {
      label: "게임수",
      value: gameCount == null ? "-" : gameCount.toLocaleString(),
    },
  ];

  return (
    <div className="flex min-w-[31rem] items-center rounded-[1.25rem] bg-[#fff7fb] px-[1rem] py-[1rem]">
      <div className="grid w-full grid-cols-4 gap-[0.75rem]">
        {stats.map((stat, i) => (
          <div
            key={i}
            className="flex min-h-[5.5rem] flex-col items-center justify-center rounded-[0.875rem] border border-[#ffd1e3] bg-white/95 px-[0.75rem] shadow-[0_12px_24px_rgba(244,114,182,0.10)]"
          >
            <span className="mb-[0.65rem] text-[0.85rem] font-black text-[#9d5c79]">
              {stat.label}
            </span>
            <span
              className={`text-[1.35rem] font-black leading-none ${stat.color || "text-[#69324b]"} ${
                stat.variant === "badge"
                  ? "min-w-[3.75rem] h-[2.15rem] px-[0.6rem] flex rounded-[0.625rem] bg-[#fff0f7] items-center justify-center whitespace-nowrap"
                  : ""
              }`}
            >
              {stat.value}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
