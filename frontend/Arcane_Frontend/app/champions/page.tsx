"use client";

import { Loader2, Search, Sparkles } from "lucide-react";
import { memo, useCallback, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import Image from "next/image";
import {
  DDRAGON_VERSION,
  getChampionIconUrl,
} from "@/services/dataDragonApi";
import {
  createEmptyLaneTierData,
  statisticsApi,
  type ChampionSummary,
  type ChampionTier,
  type LanePositionValue,
} from "@/services/statisticsApi";

const lanePositions = [
  { name: "탑", value: "top" },
  { name: "정글", value: "jug" },
  { name: "미드", value: "mid" },
  { name: "원딜", value: "adc" },
  { name: "서폿", value: "sup" },
] as const;

const positions = [{ name: "전체", value: "all" }, ...lanePositions] as const;

type PositionValue = (typeof positions)[number]["value"];

const emptyLaneData = createEmptyLaneTierData();
const emptyChampionData: ChampionSummary[] = [];

const hangeul = [
  "ㄱ",
  "ㄴ",
  "ㄷ",
  "ㄹ",
  "ㅁ",
  "ㅂ",
  "ㅅ",
  "ㅇ",
  "ㅈ",
  "ㅊ",
  "ㅋ",
  "ㅌ",
  "ㅍ",
  "ㅎ",
];

// 한글 초성 추출 함수
const getChosung = (text: string): string => {
  const chosungList = [
    "ㄱ",
    "ㄲ",
    "ㄴ",
    "ㄷ",
    "ㄸ",
    "ㄹ",
    "ㅁ",
    "ㅂ",
    "ㅃ",
    "ㅅ",
    "ㅆ",
    "ㅇ",
    "ㅈ",
    "ㅉ",
    "ㅊ",
    "ㅋ",
    "ㅌ",
    "ㅍ",
    "ㅎ",
  ];

  const result = [];
  for (let i = 0; i < text.length; i++) {
    const char = text.charAt(i);
    const code = char.charCodeAt(0) - 44032;

    if (code >= 0 && code <= 11171) {
      const chosungIndex = Math.floor(code / 588);
      result.push(chosungList[chosungIndex]);
    } else {
      result.push(char);
    }
  }

  return result.join("");
};

const getChampionImageUrl = (
  champion: Pick<
    ChampionSummary,
    "championId" | "championImageFull" | "championNameEn" | "version"
  >
) => getChampionIconUrl(champion);

const getTierLabel = (tier: number) => {
  if (tier <= 1) return "OP";
  if (tier === 2) return "1티어";
  if (tier === 3) return "2티어";
  return `${tier}티어`;
};

const getTierBadgeClass = (tier: number) => {
  if (tier <= 1) return "bg-[#f45f9c] text-white shadow-[0_8px_18px_rgba(244,95,156,0.28)]";
  if (tier === 2) return "bg-[#ffe1ed] text-[#d94687]";
  if (tier === 3) return "bg-[#fff0f7] text-[#a76886]";
  return "bg-white text-[#a76886]";
};

function TierBadge({ tier }: { tier: number }) {
  return (
    <span
      className={`inline-flex h-7 w-12 shrink-0 items-center justify-center whitespace-nowrap rounded-full text-[0.68rem] font-black ring-1 ring-[#ffd1e3]/80 ${getTierBadgeClass(tier)}`}
    >
      {getTierLabel(tier)}
    </span>
  );
}

const ChampionLaneCell = memo(function ChampionLaneCell({
  champion,
  onSelect,
}: {
  champion?: ChampionTier;
  onSelect: (championNameEn: string) => void;
}) {
  if (!champion) {
    return <div className="text-sm font-bold text-[#d0a1b8]">-</div>;
  }

  return (
    <button
      type="button"
      onClick={() => onSelect(champion.championNameEn)}
      className="group grid w-full min-w-0 grid-cols-[3rem_2.5rem_minmax(0,1fr)] items-center gap-2 overflow-hidden rounded-[1.15rem] px-2 py-2 text-left transition-colors hover:bg-[#ffe6f1]"
    >
      <TierBadge tier={champion.tier} />
      <Image
        src={getChampionImageUrl(champion)}
        alt={champion.championName}
        width={42}
        height={42}
        sizes="42px"
        className="h-10 w-10 shrink-0 rounded-[1rem] object-cover shadow-[0_8px_18px_rgba(105,50,75,0.13)] transition-transform group-hover:scale-105"
      />
      <div className="min-w-0">
        <div className="truncate text-sm font-black text-[#69324b]">
          {champion.championName}
        </div>
      </div>
    </button>
  );
});

export default function ChampionsPage() {
  const [selectedPosition, setSelectedPosition] =
    useState<PositionValue>("all");
  const router = useRouter();
  const [selectedHangeul, setSelectedHangeul] = useState("");
  const [searchQuery, setSearchQuery] = useState("");

  const allPositionQuery = useQuery({
    queryKey: ["championTiers", "all"],
    queryFn: () =>
      statisticsApi.getAllLaneTiers(
        lanePositions.map((position) => position.value)
      ),
    staleTime: 1000 * 60 * 5,
  });

  const allChampionQuery = useQuery({
    queryKey: ["champions", "all"],
    queryFn: statisticsApi.getAllChampions,
    staleTime: 1000 * 60 * 30,
  });

  const allPositionData = allPositionQuery.data ?? emptyLaneData;
  const cachedLaneData =
    selectedPosition === "all" ? [] : allPositionData[selectedPosition];

  const laneTierQuery = useQuery({
    queryKey: ["championTiers", selectedPosition],
    queryFn: () =>
      statisticsApi.getChampionTier(selectedPosition as LanePositionValue),
    enabled: selectedPosition !== "all" && cachedLaneData.length === 0,
    staleTime: 1000 * 60 * 5,
  });

  const data =
    selectedPosition === "all"
      ? []
      : cachedLaneData.length > 0
        ? cachedLaneData
        : laneTierQuery.data ?? [];
  const allChampionData = allChampionQuery.data ?? emptyChampionData;
  const loading =
    selectedPosition === "all"
      ? allPositionQuery.isLoading
      : cachedLaneData.length === 0 && laneTierQuery.isLoading;

  const navigateToChampion = useCallback(
    (championNameEn: string) => {
      router.push(`/champions/${championNameEn}`);
    },
    [router]
  );

  const handlePositionClick = useCallback((position: PositionValue) => {
    setSelectedPosition(position);
  }, []);

  const handleHangeulClick = useCallback((hangeul: string) => {
    setSelectedHangeul(selectedHangeul === hangeul ? "" : hangeul);
  }, [selectedHangeul]);

  const filteredChampions = useMemo(() => {
    const normalizedSearchQuery = searchQuery.trim().toLowerCase();

    return allChampionData
      .filter((champion: ChampionSummary) => {
        const matchesSearch =
          normalizedSearchQuery === "" ||
          champion.championName
            .toLowerCase()
            .includes(normalizedSearchQuery) ||
          champion.championNameEn
            .toLowerCase()
            .includes(normalizedSearchQuery);

        const matchesChosung =
          !selectedHangeul ||
          getChosung(champion.championName).charAt(0) === selectedHangeul;

        return matchesSearch && matchesChosung;
      })
      .sort((a, b) => a.championName.localeCompare(b.championName));
  }, [allChampionData, searchQuery, selectedHangeul]);

  const selectedPositionName =
    positions.find((position) => position.value === selectedPosition)?.name ??
    "전체";

  const allTierRowCount = useMemo(
    () =>
      Math.max(
        ...lanePositions.map(
          (position) => allPositionData[position.value]?.length ?? 0
        ),
        0
      ),
    [allPositionData]
  );

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-6 text-[#69324b]">
      <div className="mx-auto flex w-full max-w-[90rem] flex-col gap-4">
        <section className="relative overflow-hidden rounded-[2.25rem] border border-white/80 bg-[linear-gradient(135deg,_#ffffff_0%,_#fff7fb_52%,_#ffe8f2_100%)] p-6 shadow-[0_28px_76px_rgba(205,79,134,0.18)] ring-1 ring-[#f8dce8]/70">
          <div className="absolute inset-x-0 top-0 h-1.5 bg-[linear-gradient(90deg,_#f45f9c_0%,_#ff9ac5_50%,_#ffd1e3_100%)]" />
          <div className="relative flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-white/82 px-4 py-2 text-xs font-black text-[#e75491] shadow-[inset_0_0_0_1px_rgba(255,209,227,0.86)]">
                <Sparkles className="h-3.5 w-3.5" />
                Champion Analytics
              </div>
              <h1 className="bg-[linear-gradient(90deg,_#69324b_0%,_#d94687_58%,_#f45f9c_100%)] bg-clip-text text-4xl font-black tracking-normal text-transparent lg:text-6xl">
                챔피언 분석
              </h1>
              <p className="mt-3 max-w-2xl break-keep text-sm font-bold leading-6 text-[#a76886] lg:text-base">
                라인별 티어와 챔피언 정보를 빠르게 훑어보고 상세 분석으로 이동합니다.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-2 sm:flex">
              <div className="rounded-[1.25rem] bg-white/72 px-4 py-3 text-right shadow-[inset_0_0_0_1px_rgba(255,209,227,0.7)]">
                <div className="text-xl font-black text-[#e75491]">
                  {allChampionData.length || "-"}
                </div>
                <div className="text-xs font-black text-[#a76886]">챔피언</div>
              </div>
              <div className="rounded-[1.25rem] bg-white/72 px-4 py-3 text-right shadow-[inset_0_0_0_1px_rgba(255,209,227,0.7)]">
                <div className="text-xl font-black text-[#e75491]">
                  {selectedPositionName}
                </div>
                <div className="text-xs font-black text-[#a76886]">선택 라인</div>
              </div>
              <div className="rounded-[1.25rem] bg-white/72 px-4 py-3 text-right shadow-[inset_0_0_0_1px_rgba(255,209,227,0.7)]">
                <div className="text-xl font-black text-[#e75491]">KR</div>
                <div className="text-xs font-black text-[#a76886]">Emerald+</div>
              </div>
            </div>
          </div>
        </section>

        <section className="grid gap-4 xl:grid-cols-[22rem_minmax(0,1fr)]">
          <aside className="rounded-[2rem] border border-white/75 bg-white/94 p-4 shadow-[0_22px_58px_rgba(205,79,134,0.13)] ring-1 ring-[#f8dce8]/55">
            <div className="flex h-12 items-center rounded-[1.25rem] border border-[#ffd1e3] bg-[#fff7fb] px-3 shadow-[inset_0_0_0_1px_rgba(255,255,255,0.65)]">
              <Search className="mr-2 h-4 w-4 text-[#e75491]" />
              <input
                className="min-w-0 flex-1 bg-transparent text-sm font-bold text-[#69324b] outline-none placeholder:text-[#bd7b98]"
                placeholder="챔피언 검색"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <ul className="mt-3 flex flex-wrap gap-1.5 rounded-[1.25rem] bg-[#fff7fb] p-2">
              {hangeul.map((hangeul) => (
                <li
                  className={`cursor-pointer rounded-full px-2.5 py-1 text-sm font-black transition-colors ${
                    selectedHangeul === hangeul
                      ? "bg-[#ff7aae] text-white shadow-[0_8px_18px_rgba(244,114,182,0.22)]"
                      : "text-[#a76886] hover:bg-[#ffe6f1] hover:text-[#d94683]"
                  }`}
                  key={hangeul}
                  onClick={() => handleHangeulClick(hangeul)}
                >
                  {hangeul}
                </li>
              ))}
            </ul>

            <div className="mt-3 max-h-[34rem] overflow-y-auto rounded-[1.5rem] bg-[#fffafd] p-2 shadow-[inset_0_0_0_1px_rgba(255,209,227,0.72)]">
              {filteredChampions.map((champion: ChampionSummary) => (
                <div
                  key={champion.championName}
                  onClick={() => navigateToChampion(champion.championNameEn)}
                  className="group flex cursor-pointer items-center gap-3 rounded-[1.1rem] px-3 py-2.5 text-[#69324b] transition-colors hover:bg-[#ffe6f1]"
                >
                  <Image
                    src={getChampionImageUrl(champion)}
                    alt={champion.championName}
                    width={42}
                    height={42}
                    sizes="42px"
                    className="h-10 w-10 rounded-2xl object-cover shadow-[0_8px_18px_rgba(105,50,75,0.12)] transition-transform group-hover:scale-105"
                  />
                  <div className="min-w-0">
                    <div className="truncate text-sm font-black">
                      {champion.championName}
                    </div>
                    <div className="truncate text-xs font-bold text-[#a76886]">
                      {champion.championNameEn}
                    </div>
                  </div>
                </div>
              ))}
              {filteredChampions.length === 0 && (
                <div className="px-4 py-10 text-center text-sm font-bold text-[#a76886]">
                  검색 결과가 없습니다.
                </div>
              )}
            </div>
          </aside>

          <div className="min-w-0 rounded-[2rem] border border-white/75 bg-white/94 p-4 shadow-[0_22px_58px_rgba(205,79,134,0.13)] ring-1 ring-[#f8dce8]/55">
            <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h2 className="text-2xl font-black">챔피언 티어리스트</h2>
                <p className="mt-1 text-sm font-bold text-[#a76886]">
                  {selectedPosition === "all"
                    ? "전체 포지션 몰아보기"
                    : `${selectedPositionName} 포지션 기준`}{" "}
                  · Emerald+ · KR · {DDRAGON_VERSION}
                </p>
              </div>
              <ul className="flex flex-wrap gap-2 rounded-[1.5rem] bg-[#fff0f7] p-1 shadow-[inset_0_0_0_1px_rgba(255,209,227,0.86)]">
                {positions.map((position) => (
                  <li
                    key={position.value}
                    className={`cursor-pointer rounded-[1.2rem] px-4 py-2 text-sm font-black transition-colors ${
                      selectedPosition === position.value
                        ? "bg-[#ff7aae] text-white shadow-[0_8px_18px_rgba(244,114,182,0.22)]"
                        : "text-[#a76886] hover:bg-[#ffe6f1]"
                    }`}
                    onClick={() => handlePositionClick(position.value)}
                  >
                    {position.name}
                  </li>
                ))}
              </ul>
            </div>

            <div className="overflow-x-auto overflow-y-hidden rounded-[1.5rem] bg-[#fffafd] shadow-[inset_0_0_0_1px_rgba(255,209,227,0.72)]">
              {loading ? (
                <div className="px-4 py-16 text-center text-sm font-bold text-[#a76886]">
                  <Loader2 className="mx-auto h-8 w-8 animate-spin text-[#e75491]" />
                  <p className="mt-3">챔피언 데이터를 불러오는 중입니다.</p>
                </div>
              ) : (
                selectedPosition === "all" ? (
                  <>
                    <div className="min-w-[62rem]">
                      <div className="grid grid-cols-[4rem_repeat(5,minmax(10.5rem,1fr))] border-b border-[#ffd1e3] bg-[#fff0f7]/78 px-4 py-3 text-sm font-black text-[#a76886]">
                        <div>랭킹</div>
                        {lanePositions.map((position) => (
                          <div key={position.value}>{position.name}</div>
                        ))}
                      </div>

                      {Array.from({ length: allTierRowCount }).map((_, index) => (
                        <div
                          key={`all-tier-row-${index}`}
                          className="grid grid-cols-[4rem_repeat(5,minmax(10.5rem,1fr))] items-center border-b border-[#ffe1ed] px-4 py-2 text-sm text-[#69324b]"
                        >
                          <div className="text-base font-black text-[#e75491]">
                            {index + 1}
                          </div>
                          {lanePositions.map((position) => (
                            <ChampionLaneCell
                              key={`${position.value}-${index}`}
                              champion={allPositionData[position.value]?.[index]}
                              onSelect={navigateToChampion}
                            />
                          ))}
                        </div>
                      ))}

                      {allTierRowCount === 0 && (
                        <div className="px-4 py-16 text-center text-sm font-bold text-[#a76886]">
                          표시할 챔피언 데이터가 없습니다.
                        </div>
                      )}
                    </div>
                  </>
                ) : (
                  <>
                  <div className="hidden grid-cols-[4rem_minmax(12rem,1fr)_7rem_7rem_7rem_7rem] border-b border-[#ffd1e3] bg-[#fff0f7]/78 px-4 py-3 text-sm font-black text-[#a76886] md:grid">
                    <div className="text-left">랭킹</div>
                    <div className="text-left">챔피언</div>
                    <div className="text-left">티어</div>
                    <div className="text-left">점수</div>
                    <div className="text-left">승률</div>
                    <div className="text-left">점수차</div>
                  </div>
                  {data.map((item: ChampionTier, index: number) => (
                    <div
                      key={item.championName}
                      className="grid cursor-pointer grid-cols-[3rem_minmax(0,1fr)_auto] items-center gap-3 border-b border-[#ffe1ed] px-4 py-3 text-sm text-[#69324b] transition-colors hover:bg-[#ffe6f1] md:grid-cols-[4rem_minmax(12rem,1fr)_7rem_7rem_7rem_7rem]"
                      onClick={() => navigateToChampion(item.championNameEn)}
                    >
                      <div className="text-left text-base font-black text-[#e75491]">
                        {index + 1}
                      </div>
                      <div className="flex min-w-0 items-center gap-3">
                        <TierBadge tier={item.tier} />
                        <Image
                          src={getChampionImageUrl(item)}
                          alt={item.championName}
                          width={48}
                          height={48}
                          sizes="48px"
                          className="h-12 w-12 rounded-[1.1rem] object-cover shadow-[0_8px_18px_rgba(105,50,75,0.13)]"
                        />
                        <div className="min-w-0">
                          <div className="truncate text-base font-black">
                            {item.championName}
                          </div>
                          <div className="truncate text-xs font-bold text-[#a76886]">
                            {item.championNameEn}
                          </div>
                        </div>
                      </div>
                      <div className="justify-self-end rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#e75491] md:justify-self-start">
                        {getTierLabel(item.tier)}
                      </div>
                      <div className="hidden font-black md:block">
                        {item.score}
                      </div>
                      <div className="hidden font-black md:block">
                        {Number(item.winRate).toFixed(1)}%
                      </div>
                      <div
                        className={`hidden font-black md:block ${
                          item.scoreDiff >= 0
                            ? "text-[#2f80ed]"
                            : "text-[#ff5470]"
                        }`}
                      >
                        {item.scoreDiff >= 0 ? "+" : ""}
                        {item.scoreDiff}
                      </div>
                    </div>
                  ))}
                  {data.length === 0 && (
                    <div className="px-4 py-16 text-center text-sm font-bold text-[#a76886]">
                      표시할 챔피언 데이터가 없습니다.
                    </div>
                  )}
                  </>
                )
              )}
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
