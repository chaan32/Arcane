"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import Image from "next/image";
import { getSummonerUrl } from "@/utils/navigation";
import { emptyRanking, rankingApi, type RankingTier } from "@/services/rankingApi";
import { getDataDragonProfileIconUrl } from "@/services/dataDragonApi";

// 티어 옵션 정의
const TIER_OPTIONS: readonly {
  value: RankingTier;
  label: string;
  color: string;
}[] = [
  { value: "all", label: "All Tiers", color: "#d98bad" },
  { value: "challenger", label: "Challenger", color: "#ff9ac5" },
  { value: "grandmaster", label: "Grandmaster", color: "#ff7aa2" },
  { value: "master", label: "Master", color: "#c084fc" },
] as const;

type TierValue = RankingTier;

// 티어 아이콘 컴포넌트
function TierIcon({ tier }: { tier: TierValue }) {
  const tierData = TIER_OPTIONS.find((t) => t.value === tier);
  const color = tierData?.color || "#bd7b98";

  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* 날개 모양 아이콘 */}
      <path
        d="M12 2L8 6L4 4L6 10L2 14L8 14L12 22L16 14L22 14L18 10L20 4L16 6L12 2Z"
        fill={color}
        stroke={color}
        strokeWidth="1"
        strokeLinejoin="round"
      />
      {/* 중앙 원 */}
      <circle cx="12" cy="12" r="3" fill="#fffafd" />
    </svg>
  );
}

export default function RankingPage() {
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedTier, setSelectedTier] = useState<TierValue>("all");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

  const rankingQuery = useQuery({
    queryKey: ["ranking", selectedTier, currentPage],
    queryFn: () => rankingApi.getRanking(selectedTier, currentPage),
    placeholderData: (previousData) => previousData,
    staleTime: 1000 * 60,
  });

  const rankings = rankingQuery.data ?? emptyRanking;
  const isLoading = rankingQuery.isLoading;

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // 티어 변경 시 페이지 리셋
  const handleTierChange = useCallback((tier: TierValue) => {
    setSelectedTier(tier);
    setCurrentPage(1);
    setIsDropdownOpen(false);
  }, []);

  // 페이지 이동 함수
  const goToPage = useCallback((page: number) => {
    if (page >= 1 && page <= rankings.totalPage) {
      setCurrentPage(page);
    }
  }, [rankings.totalPage]);

  // 페이지 버튼 렌더링 (10페이지씩 그룹)
  const pageButtons = useMemo(() => {
    const buttons = [];
    const pageGroup = Math.floor((currentPage - 1) / 10); // 현재 페이지가 속한 그룹 (0, 1, 2...)
    const startPage = pageGroup * 10 + 1; // 그룹의 시작 페이지
    const endPage = Math.min(startPage + 9, rankings.totalPage); // 그룹의 끝 페이지

    for (let i = startPage; i <= endPage; i++) {
      buttons.push(
        <button
          key={i}
          onClick={() => goToPage(i)}
          className={`w-8 h-8 rounded border flex items-center justify-center text-sm transition-colors ${
            currentPage === i
              ? "border-[#ff9ac5] text-white bg-[#ff7aae] shadow-[0_8px_18px_rgba(244,114,182,0.22)]"
              : "border-[#ffd1e3] text-[#a76886] bg-white/80 hover:bg-[#ffe6f1] hover:text-[#d94683]"
          }`}
        >
          {i}
        </button>
      );
    }
    return buttons;
  }, [currentPage, goToPage, rankings.totalPage]);

  return (
    <div className="min-h-screen text-[#69324b]">
      <div className="max-w-6xl mx-auto py-8 px-4">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold">랭킹</h1>

          {/* 티어 드롭다운 */}
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setIsDropdownOpen(!isDropdownOpen)}
            className="flex min-w-[180px] items-center gap-2 rounded-xl border border-[#ffd1e3] bg-white/90 px-4 py-2 shadow-[0_12px_28px_rgba(244,114,182,0.12)] transition-colors hover:border-[#ff9ac5]"
            >
              {/* 티어 아이콘 */}
              <TierIcon tier={selectedTier} />
              <span className="font-medium">
                {TIER_OPTIONS.find((t) => t.value === selectedTier)?.label}
              </span>
              <svg
                className={`w-4 h-4 ml-auto transition-transform ${
                  isDropdownOpen ? "rotate-180" : ""
                }`}
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M19 9l-7 7-7-7"
                />
              </svg>
            </button>

            {/* 드롭다운 메뉴 */}
            {isDropdownOpen && (
              <div className="absolute right-0 z-50 mt-2 w-full overflow-hidden rounded-xl border border-[#ffd1e3] bg-white/95 shadow-[0_18px_42px_rgba(244,114,182,0.18)] backdrop-blur">
                {TIER_OPTIONS.map((tier) => (
                  <button
                    key={tier.value}
                    onClick={() => handleTierChange(tier.value)}
                    className={`flex w-full items-center gap-3 px-4 py-3 transition-colors hover:bg-[#ffe6f1] ${
                      selectedTier === tier.value ? "bg-[#fff0f7]" : ""
                    }`}
                  >
                    <TierIcon tier={tier.value} />
                    <span className="font-medium">{tier.label}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* 테이블 헤더 */}
        <div className="rounded-t-[1.5rem] border border-b-0 border-[#ffd1e3] bg-white/90">
          <div className="grid grid-cols-12 gap-4 border-b border-[#ffd1e3] bg-[#fff0f7]/70 px-4 py-3 text-sm text-[#a76886]">
            <div className="col-span-1 text-center">순위</div>
            <div className="col-span-3">소환사명</div>
            <div className="col-span-2 text-center">티어</div>
            <div className="col-span-1 text-center">레벨</div>
            <div className="col-span-5 text-center">승률</div>
          </div>
        </div>

        {/* 테이블 바디 */}
        <div className="min-h-[600px] divide-y divide-[#ffe1ed] rounded-b-[1.5rem] border border-t-0 border-[#ffd1e3] bg-white/90 shadow-[0_18px_42px_rgba(244,114,182,0.14)]">
          {isLoading ? (
            <div className="py-12 text-center text-[#a76886]">
              랭킹 데이터를 불러오는 중...
            </div>
          ) : (
            rankings?.rankers?.map((player, index) => {
              const totalGames = player.wins + player.losses;
              const winPercentage =
                totalGames > 0 ? (player.wins / totalGames) * 100 : 0;
              // 실제 순위 계산 (페이지 * 100 + index + 1)
              const actualRank = (currentPage - 1) * 100 + index + 1;

              return (
                <div
                  key={player.puuid}
                  className="grid grid-cols-12 items-center gap-4 px-4 py-3 transition-colors hover:bg-[#ffe6f1]"
                >
                  {/* 순위 */}
                  <div className="col-span-1 text-center font-bold text-lg">
                    {actualRank}
                  </div>

                  {/* 소환사명 */}
                  <div
                    className="col-span-3 flex cursor-pointer items-center gap-3 transition-colors hover:text-[#d94683]"
                    onClick={() =>
                      router.push(
                        getSummonerUrl(`${player.gameName}#${player.tagLine}`)
                      )
                    }
                  >
                    {/* 프로필 아이콘 */}
                    <Image
                      src={getDataDragonProfileIconUrl(player.profileIconId)}
                      alt="profile"
                      width={40}
                      height={40}
                      sizes="40px"
                      className="w-10 h-10 rounded-full"
                    />
                    <div>
                      <span className="font-medium">{player.gameName}</span>
                      <span className="text-[#a76886]">#{player.tagLine}</span>
                    </div>
                  </div>

                  {/* 티어 */}
                  <div className="col-span-2 flex items-center justify-center gap-1">
                    <span className="rounded bg-[#fff0f7] px-2 py-0.5 text-sm font-medium text-[#e75491]">
                      C
                    </span>
                    <span className="text-[#69324b]">
                      {player.leaguePoints.toLocaleString()}
                    </span>
                    <span className="text-sm text-[#a76886]">LP</span>
                  </div>

                  {/* 레벨 */}
                  <div className="col-span-1 text-center text-[#a76886]">
                    {player.summonerLevel}
                  </div>

                  {/* 승률 */}
                  <div className="col-span-5 flex items-center gap-3">
                    {/* 승/패 바 */}
                    <div className="flex-1 flex h-5 rounded overflow-hidden text-xs font-medium">
                      <div
                        className="flex min-w-[40px] items-center justify-center bg-[#ff7aae] text-white"
                        style={{ width: `${winPercentage}%` }}
                      >
                        {player.wins}
                      </div>
                      <div
                        className="flex min-w-[40px] items-center justify-center bg-[#ff9aa8] text-white"
                        style={{ width: `${100 - winPercentage}%` }}
                      >
                        {player.losses}
                      </div>
                    </div>
                    {/* 승률 퍼센트 */}
                    <div className="w-16 text-right">
                      <span className="font-medium text-[#e75491]">
                        {player.winRate.toFixed(1)}
                      </span>
                      <span className="text-[#a76886]">%</span>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* 페이지네이션 */}
        <div className="flex justify-center items-center gap-1 mt-6">
          {/* 맨 처음 */}
          <button
            onClick={() => goToPage(1)}
            disabled={currentPage === 1}
            className="flex h-8 w-8 items-center justify-center rounded text-[#a76886] transition-colors hover:bg-[#ffe6f1] hover:text-[#d94683] disabled:cursor-not-allowed disabled:opacity-30"
          >
            «
          </button>
          {/* 이전 10페이지 그룹 */}
          <button
            onClick={() => {
              const pageGroup = Math.floor((currentPage - 1) / 10);
              const prevGroupStart = (pageGroup - 1) * 10 + 1;
              goToPage(Math.max(1, prevGroupStart));
            }}
            disabled={currentPage <= 10}
            className="flex h-8 w-8 items-center justify-center rounded text-[#a76886] transition-colors hover:bg-[#ffe6f1] hover:text-[#d94683] disabled:cursor-not-allowed disabled:opacity-30"
          >
            ‹
          </button>

          {/* 페이지 번호들 */}
          {pageButtons}

          {/* 다음 10페이지 그룹 */}
          <button
            onClick={() => {
              const pageGroup = Math.floor((currentPage - 1) / 10);
              const nextGroupStart = (pageGroup + 1) * 10 + 1;
              goToPage(Math.min(rankings.totalPage, nextGroupStart));
            }}
            disabled={
              Math.floor((currentPage - 1) / 10) >=
              Math.floor((rankings.totalPage - 1) / 10)
            }
            className="flex h-8 w-8 items-center justify-center rounded text-[#a76886] transition-colors hover:bg-[#ffe6f1] hover:text-[#d94683] disabled:cursor-not-allowed disabled:opacity-30"
          >
            ›
          </button>
          {/* 맨 끝 */}
          <button
            onClick={() => goToPage(rankings.totalPage)}
            disabled={currentPage === rankings.totalPage}
            className="flex h-8 w-8 items-center justify-center rounded text-[#a76886] transition-colors hover:bg-[#ffe6f1] hover:text-[#d94683] disabled:cursor-not-allowed disabled:opacity-30"
          >
            »
          </button>
        </div>
      </div>
    </div>
  );
}
