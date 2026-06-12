"use client";

import { memo } from "react";

export type MatchFilterKey = "all" | "solo" | "flex" | "normal" | "other";

export const matchFilters: { key: MatchFilterKey; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "solo", label: "솔로랭크" },
  { key: "flex", label: "자유랭크" },
  { key: "normal", label: "일반" },
  { key: "other", label: "나머지" },
];

type MatchFilterTabsProps = {
  activeFilter: MatchFilterKey;
  counts: Record<MatchFilterKey, number>;
  onChange: (filter: MatchFilterKey) => void;
};

type MatchLoadMoreButtonProps = {
  hasMoreMatches: boolean;
  isDisabled: boolean;
  isLoading: boolean;
  onLoadMore: () => void;
};

export const MatchFilterTabs = memo(function MatchFilterTabs({
  activeFilter,
  counts,
  onChange,
}: MatchFilterTabsProps) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {matchFilters.map((filter) => {
        const isActive = activeFilter === filter.key;

        return (
          <button
            key={filter.key}
            onClick={() => onChange(filter.key)}
            className={`rounded-full px-4 py-2 text-sm font-bold transition-colors ${
              isActive
                ? "bg-[#f45f9c] text-white shadow-[0_10px_20px_rgba(231,84,145,0.22)]"
                : "border border-[#ffd1e3] bg-white/85 text-[#a76886] shadow-[inset_0_1px_0_rgba(255,255,255,0.9)] hover:bg-[#fff0f7] hover:text-[#e75491]"
            }`}
          >
            {filter.label}
            <span className="ml-1 text-xs opacity-75">
              {counts[filter.key]}
            </span>
          </button>
        );
      })}
    </div>
  );
});

export const MatchLoadMoreButton = memo(function MatchLoadMoreButton({
  hasMoreMatches,
  isDisabled,
  isLoading,
  onLoadMore,
}: MatchLoadMoreButtonProps) {
  return (
    <div className="flex justify-center py-3">
      {hasMoreMatches ? (
        <button
          type="button"
          onClick={onLoadMore}
          disabled={isDisabled}
          className="rounded-full bg-[#f45f9c] px-7 py-3 text-sm font-black text-white shadow-[0_14px_28px_rgba(205,79,134,0.22)] transition-colors hover:bg-[#d94683] disabled:cursor-not-allowed disabled:bg-[#f5a9c8] disabled:shadow-none"
        >
          {isLoading ? "불러오는 중..." : "전적 더 보기"}
        </button>
      ) : (
        <div className="rounded-full bg-white/75 px-5 py-2 text-sm font-bold text-[#a76886] shadow-[inset_0_0_0_1px_rgba(248,220,232,0.8)]">
          더 불러올 전적이 없습니다
        </div>
      )}
    </div>
  );
});
