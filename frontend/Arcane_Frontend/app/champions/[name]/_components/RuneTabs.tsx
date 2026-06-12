"use client";

import Image from "next/image";
import type { RuneBuild } from "@/hooks/useChampionRunes";
import type { StatPerksDto } from "@/types/championDetail";
import type { RuneTreePath, RuneTreeRune } from "@/types/rune";

interface RuneTabsProps {
  builds: RuneBuild[];
  runeTree: RuneTreePath[];
  statPerks?: StatPerksDto;
}

const STAT_RUNE_META: Record<number, { icon: string; label: string }> = {
  5001: {
    icon: "perk-images/StatMods/StatModsHealthScalingIcon.png",
    label: "성장 체력",
  },
  5002: {
    icon: "perk-images/StatMods/StatModsArmorIcon.png",
    label: "방어력",
  },
  5003: {
    icon: "perk-images/StatMods/StatModsMagicResIcon.png",
    label: "마법 저항",
  },
  5005: {
    icon: "perk-images/StatMods/StatModsAttackSpeedIcon.png",
    label: "공격 속도",
  },
  5007: {
    icon: "perk-images/StatMods/StatModsCDRScalingIcon.png",
    label: "스킬 가속",
  },
  5008: {
    icon: "perk-images/StatMods/StatModsAdaptiveForceIcon.png",
    label: "적응형",
  },
  5010: {
    icon: "perk-images/StatMods/StatModsMovementSpeedIcon.png",
    label: "이동 속도",
  },
  5011: {
    icon: "perk-images/StatMods/StatModsHealthPlusIcon.png",
    label: "체력",
  },
  5013: {
    icon: "perk-images/StatMods/StatModsTenacityIcon.png",
    label: "강인함",
  },
};

const getRuneIconUrl = (icon?: string | null) => {
  if (!icon) return null;
  if (
    icon.startsWith("http://") ||
    icon.startsWith("https://") ||
    icon.startsWith("/")
  ) {
    return icon;
  }

  return `https://ddragon.leagueoflegends.com/cdn/img/${icon}`;
};

const compactRuneIds = (runeIds: Array<number | null | undefined>) =>
  runeIds.filter(
    (runeId): runeId is number =>
      typeof runeId === "number" && Number.isFinite(runeId) && runeId > 0
  );

const getStyleLabel = (build: RuneBuild, index: number) => {
  if (build.description === "primaryStyle") return "주 룬";
  if (build.description === "subStyle") return "보조 룬";
  return index === 0 ? "주 룬" : "보조 룬";
};

export function RuneTabs({ builds, runeTree, statPerks }: RuneTabsProps) {
  const selectedStyleIds = new Set(builds.map((build) => build.style));
  const selectedShardIds = compactRuneIds([
    statPerks?.offense,
    statPerks?.flex,
    statPerks?.defense,
  ]);

  if (builds.length === 0) {
    return (
      <div className="flex min-h-[8rem] items-center justify-center rounded-[0.75rem] border border-[#ffd1e3] bg-[#fff7fb] text-d-body3-r text-[#a76886]">
        룬 분석 데이터가 없습니다.
      </div>
    );
  }

  if (runeTree.length === 0) {
    return (
      <div className="flex min-h-[8rem] items-center justify-center rounded-[0.75rem] border border-[#ffd1e3] bg-[#fff7fb] text-d-body3-r text-[#a76886]">
        룬 트리 데이터를 불러오는 중입니다.
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-[1.25rem]">
      <div className="flex items-center justify-between gap-[0.75rem] rounded-[0.75rem] border border-[#ffd1e3] bg-[#fff0f7] px-[1rem] py-[0.75rem]">
        {runeTree.map((path) => {
          const selected = selectedStyleIds.has(path.id);

          return (
            <div
              key={path.id}
              className={`flex min-w-[5.5rem] flex-1 items-center justify-center gap-[0.5rem] rounded-[0.65rem] px-[0.75rem] py-[0.5rem] transition ${
                selected
                  ? "bg-white text-[#69324b] shadow-[0_10px_22px_rgba(244,114,182,0.16)]"
                  : "text-[#a76886]"
              }`}
            >
              <RuneImage
                alt={path.name}
                className={`h-[2rem] w-[2rem] rounded-full ${
                  selected ? "opacity-100 grayscale-0" : "opacity-35 grayscale"
                }`}
                icon={path.icon}
                size={32}
              />
              <span className="truncate text-d-body4">{path.name}</span>
            </div>
          );
        })}
      </div>

      <div className="grid gap-[1rem] xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_16rem]">
        {builds.map((build, index) => {
          const path = runeTree.find((candidate) => candidate.id === build.style);
          const selectedRuneIds = new Set(
            compactRuneIds(build.selections?.map((selection) => selection.perk) ?? [])
          );

          return (
            <RunePathPanel
              key={`${build.style}-${index}`}
              label={getStyleLabel(build, index)}
              path={path}
              selectedRuneIds={selectedRuneIds}
            />
          );
        })}

        <RuneShardPanel selectedShardIds={selectedShardIds} />
      </div>
    </div>
  );
}

function RunePathPanel({
  label,
  path,
  selectedRuneIds,
}: {
  label: string;
  path?: RuneTreePath;
  selectedRuneIds: Set<number>;
}) {
  if (!path) {
    return (
      <div className="flex min-h-[20rem] items-center justify-center rounded-[1rem] border border-[#ffd1e3] bg-[#fff7fb] text-d-body3-r text-[#a76886]">
        룬 경로 데이터가 없습니다.
      </div>
    );
  }

  return (
    <div className="rounded-[1rem] border border-[#ffd1e3] bg-[#fff7fb] p-[1rem] shadow-[0_16px_32px_rgba(244,114,182,0.1)]">
      <div className="mb-[1rem] flex items-center justify-between">
        <div>
          <p className="text-d-cap1 text-[#a76886]">{label}</p>
          <h4 className="text-d-body2 text-[#69324b]">{path.name}</h4>
        </div>
        <RuneImage
          alt={path.name}
          className="h-[2.5rem] w-[2.5rem] rounded-full"
          icon={path.icon}
          size={40}
        />
      </div>

      <div className="flex flex-col gap-[0.85rem]">
        {path.slots.map((slot) => (
          <div
            key={slot.id}
            className="grid grid-cols-4 items-center gap-[0.75rem] rounded-[0.75rem] bg-white/80 px-[0.75rem] py-[0.65rem]"
          >
            {slot.runes.map((rune) => (
              <RuneChoice
                key={rune.id}
                rune={rune}
                selected={selectedRuneIds.has(rune.id)}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

function RuneChoice({
  rune,
  selected,
}: {
  rune: RuneTreeRune;
  selected: boolean;
}) {
  return (
    <div className="flex min-w-0 flex-col items-center gap-[0.35rem] text-center">
      <div
        className={`rounded-full p-[0.2rem] transition ${
          selected
            ? "bg-[#ff7aae] shadow-[0_0_0_3px_rgba(255,122,174,0.22),0_10px_18px_rgba(244,114,182,0.2)]"
            : "bg-[#f1d6e2]"
        }`}
      >
        <RuneImage
          alt={rune.name}
          className={`h-[2.6rem] w-[2.6rem] rounded-full bg-[#2d2130] p-[0.15rem] transition ${
            selected ? "opacity-100 grayscale-0" : "opacity-35 grayscale"
          }`}
          icon={rune.icon}
          size={42}
        />
      </div>
      <span
        className={`w-full truncate text-[0.7rem] font-bold ${
          selected ? "text-[#69324b]" : "text-[#b88aa0]"
        }`}
      >
        {rune.name}
      </span>
    </div>
  );
}

function RuneShardPanel({ selectedShardIds }: { selectedShardIds: number[] }) {
  const shardRows = [
    [5008, 5005, 5007],
    [5008, 5010, 5011],
    [5001, 5013, 5011],
  ];

  return (
    <div className="rounded-[1rem] border border-[#ffd1e3] bg-[#fff7fb] p-[1rem] shadow-[0_16px_32px_rgba(244,114,182,0.1)]">
      <div className="mb-[1rem]">
        <p className="text-d-cap1 text-[#a76886]">능력치</p>
        <h4 className="text-d-body2 text-[#69324b]">룬 파편</h4>
      </div>

      <div className="flex flex-col gap-[0.85rem]">
        {shardRows.map((row, rowIndex) => (
          <div
            key={rowIndex}
            className="grid grid-cols-3 gap-[0.6rem] rounded-[0.75rem] bg-white/80 px-[0.75rem] py-[0.65rem]"
          >
            {row.map((runeId) => {
              const meta = STAT_RUNE_META[runeId];
              const selected = selectedShardIds[rowIndex] === runeId;

              return (
                <div key={`${rowIndex}-${runeId}`} className="flex flex-col items-center gap-[0.35rem]">
                  <div
                    className={`rounded-full p-[0.2rem] transition ${
                      selected ? "bg-[#36cfc0]" : "bg-[#f1d6e2]"
                    }`}
                  >
                    <RuneImage
                      alt={meta.label}
                      className={`h-[2.35rem] w-[2.35rem] rounded-full bg-[#2d2130] p-[0.2rem] transition ${
                        selected ? "opacity-100 grayscale-0" : "opacity-35 grayscale"
                      }`}
                      icon={meta.icon}
                      size={38}
                    />
                  </div>
                  <span
                    className={`w-full truncate text-center text-[0.68rem] font-bold ${
                      selected ? "text-[#69324b]" : "text-[#b88aa0]"
                    }`}
                  >
                    {meta.label}
                  </span>
                </div>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}

function RuneImage({
  alt,
  className,
  icon,
  size,
}: {
  alt: string;
  className: string;
  icon?: string | null;
  size: number;
}) {
  const src = getRuneIconUrl(icon);

  if (!src) {
    return <div aria-label={alt} className={className} role="img" />;
  }

  return (
    <Image
      alt={alt}
      className={className}
      height={size}
      sizes={`${size}px`}
      src={src}
      width={size}
    />
  );
}
