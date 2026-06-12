"use client";

import { useState } from "react";
import { SummonerDropdownType } from "@/types/summoner";
import { Star } from "../icons/Star";
import { Close } from "../icons/Close";
import {
  getSaveRecentEnabled,
  isFavoriteSummoner,
  removeFavoriteSummoner,
  removeRecentSummoner,
  setSaveRecentEnabled,
  toggleFavoriteSummoner,
} from "@/lib/summonerSearchStorage";
import { ExternalImage } from "@/components/common/ExternalImage";

interface Props {
  favoriteSummoners: SummonerDropdownType[];
  query: string;
  recentSummoners: SummonerDropdownType[];
  results: SummonerDropdownType[];
  onSelect: (summoner: SummonerDropdownType) => void;
}

export default function SearchDropdown({
  favoriteSummoners,
  query,
  recentSummoners,
  results,
  onSelect,
}: Props) {
  const [activeTab, setActiveTab] = useState<"recent" | "favorite">("recent");
  const [saveEnabled, setSaveEnabled] = useState(() => getSaveRecentEnabled());
  const hasQuery = query.trim().length > 0;
  const list = hasQuery
    ? results
    : activeTab === "recent"
      ? recentSummoners
      : favoriteSummoners;

  const toggleSaveRecent = () => {
    const nextValue = !saveEnabled;
    setSaveEnabled(nextValue);
    setSaveRecentEnabled(nextValue);
  };

  const removeSummoner = (summoner: SummonerDropdownType) => {
    if (activeTab === "favorite") {
      removeFavoriteSummoner(summoner);
      return;
    }

    removeRecentSummoner(summoner);
  };

  return (
    <div
      className="absolute left-0 top-[calc(100%+0.6rem)] z-50 w-full min-w-[20rem] overflow-hidden rounded-[1.5rem] border border-[#ffd1e3] bg-white/98 text-[#69324b] shadow-[0_24px_64px_rgba(205,79,134,0.22)] backdrop-blur"
      onMouseDown={(e) => e.preventDefault()}
    >
      {!hasQuery && (
        <div className="flex items-center gap-2 border-b border-[#ffe1ed] bg-[#fff7fb] p-2">
          {[
            { key: "recent", label: "최근검색" },
            { key: "favorite", label: "즐겨찾기" },
          ].map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key as "recent" | "favorite")}
              className={`h-9 flex-1 rounded-full text-sm font-black transition-colors ${
                activeTab === tab.key
                  ? "bg-[#f45f9c] text-white shadow-[0_10px_20px_rgba(231,84,145,0.18)]"
                  : "bg-white text-[#a76886] hover:bg-[#fff0f7]"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      )}

      {!hasQuery && activeTab === "recent" && (
        <div className="flex items-center justify-between border-b border-[#ffe1ed] px-4 py-2 text-xs font-bold text-[#a76886]">
          <span>최근 검색 저장</span>
          <button
            type="button"
            onClick={toggleSaveRecent}
            className={`rounded-full px-3 py-1 font-black ${
              saveEnabled
                ? "bg-[#f45f9c] text-white"
                : "bg-[#fff0f7] text-[#bd7b98]"
            }`}
          >
            {saveEnabled ? "ON" : "OFF"}
          </button>
        </div>
      )}

      <div className="max-h-[22rem] overflow-y-auto p-2">
        {list.length > 0 ? (
          list.map((item) => {
            if (!item) return null;
            const favorite = isFavoriteSummoner(item);

            return (
              <div
                key={item.puuid || item.id}
                role="button"
                tabIndex={0}
                onClick={() => onSelect(item)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    onSelect(item);
                  }
                }}
                className="flex w-full items-center justify-between rounded-[1.1rem] p-2 text-left transition-colors hover:bg-[#fff0f7]"
              >
                <div className="flex min-w-0 items-center gap-3">
                  <div className="h-10 w-10 shrink-0 overflow-hidden rounded-[0.9rem] bg-[#fff0f7] ring-1 ring-[#ffd1e3]">
                    {item.profileUrl ? (
                      <ExternalImage
                        src={item.profileUrl}
                        alt="icon"
                        width={40}
                        height={40}
                        sizes="40px"
                        className="h-full w-full object-cover"
                        loading="lazy"
                        fallback={<div className="h-full w-full bg-[#fff0f7]" />}
                      />
                    ) : (
                      <div className="h-full w-full bg-[#fff0f7]" />
                    )}
                  </div>

                  <div className="min-w-0">
                    <div className="flex min-w-0 items-center">
                      <span className="truncate text-sm font-black text-[#69324b]">
                        {item.gameName}
                      </span>
                      <span className="ml-1 shrink-0 text-sm font-black text-[#a76886]">
                        #{item.tagLine}
                      </span>
                    </div>
                    <span className="text-xs font-bold text-[#bd7b98]">
                      Level {item.level ?? "none"}
                    </span>
                  </div>
                </div>

                <div className="flex shrink-0 items-center gap-1">
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleFavoriteSummoner(item);
                    }}
                    aria-label="즐겨찾기 토글"
                    className={`rounded-full p-1.5 ${
                      favorite ? "text-[#f45f9c]" : "text-[#bd7b98]"
                    } hover:bg-white`}
                  >
                    <Star className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    className="rounded-full p-1.5 text-[#bd7b98] hover:bg-white hover:text-[#e75491]"
                    onClick={(e) => {
                      e.stopPropagation();
                      removeSummoner(item);
                    }}
                    aria-label="검색 기록 삭제"
                  >
                    <Close className="h-4 w-4" />
                  </button>
                </div>
              </div>
            );
          })
        ) : (
          <div className="py-8 text-center text-sm font-black text-[#a76886]">
            {hasQuery
              ? "일치하는 소환사 정보가 없습니다."
              : activeTab === "recent"
                ? "최근 검색 내역이 없습니다."
                : "즐겨찾기한 소환사가 없습니다."}
          </div>
        )}
      </div>
    </div>
  );
}
