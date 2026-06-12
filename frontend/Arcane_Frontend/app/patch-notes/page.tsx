"use client";

import { ExternalLink, Loader2, Search } from "lucide-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import Image from "next/image";
import { ChampionPatchNotes } from "@/app/champions/[name]/_components/ChampionPatchNotes";
import {
  dataDragonApi,
  getDataDragonChampionIconUrl,
  type DataDragonChampion,
} from "@/services/dataDragonApi";
import { riotPatchNoteApi } from "@/services/riotPatchNoteApi";
import type { RiotPatchNoteSummary } from "@/types/riotPatchNotes";

type PatchNoteView = "all" | "champion";

type ChampionCandidate = DataDragonChampion;

const DEFAULT_CHAMPION: ChampionCandidate = {
  id: 67,
  nameKo: "베인",
  nameEn: "Vayne",
  imageFull: "Vayne.png",
};

const formatPatchDate = (value: string) => {
  if (!value) return "날짜 정보 없음";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(date);
};

export default function PatchNotesPage() {
  const [activeView, setActiveView] = useState<PatchNoteView>("all");
  const [patches, setPatches] = useState<RiotPatchNoteSummary[]>([]);
  const [isPatchLoading, setIsPatchLoading] = useState(true);
  const [patchErrorMessage, setPatchErrorMessage] = useState("");
  const [champions, setChampions] = useState<ChampionCandidate[]>([]);
  const [query, setQuery] = useState("");
  const [selectedChampion, setSelectedChampion] =
    useState<ChampionCandidate>(DEFAULT_CHAMPION);
  const [isPickerOpen, setIsPickerOpen] = useState(false);

  useEffect(() => {
    let isCancelled = false;

    const loadPatchNotes = async () => {
      try {
        const result = await riotPatchNoteApi.getPatchNotes();
        if (isCancelled) return;

        setPatches(result.patches);
        setPatchErrorMessage("");
      } catch (error) {
        if (isCancelled) return;

        setPatchErrorMessage(
          error instanceof Error
            ? error.message
            : "전체 패치노트를 불러오지 못했습니다."
        );
      } finally {
        if (!isCancelled) {
          setIsPatchLoading(false);
        }
      }
    };

    void loadPatchNotes();

    return () => {
      isCancelled = true;
    };
  }, []);

  useEffect(() => {
    let isCancelled = false;

    const loadChampions = async () => {
      const nextChampions = await dataDragonApi.getChampions();

      if (isCancelled) return;

      setChampions(nextChampions);
    };

    loadChampions().catch((error) => {
      if (!isCancelled) {
        console.error("챔피언 목록 요청 실패:", error);
      }
    });

    return () => {
      isCancelled = true;
    };
  }, []);

  const filteredChampions = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    const nextChampions = normalizedQuery
      ? champions.filter(
          (champion) =>
            champion.nameKo.toLowerCase().includes(normalizedQuery) ||
            champion.nameEn.toLowerCase().includes(normalizedQuery)
        )
      : champions;

    return nextChampions.slice(0, 12);
  }, [champions, query]);

  const selectChampion = (champion: ChampionCandidate) => {
    setSelectedChampion(champion);
    setQuery(champion.nameKo);
    setIsPickerOpen(false);
  };

  const handleChampionSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const normalizedQuery = query.trim();
    if (!normalizedQuery) return;

    const exactChampion = champions.find(
      (champion) =>
        champion.nameKo.toLowerCase() === normalizedQuery.toLowerCase() ||
        champion.nameEn.toLowerCase() === normalizedQuery.toLowerCase()
    );

    if (exactChampion) {
      selectChampion(exactChampion);
      return;
    }

    if (filteredChampions[0]) {
      selectChampion(filteredChampions[0]);
      return;
    }

    setSelectedChampion({
      id: 0,
      nameKo: normalizedQuery,
      nameEn: normalizedQuery,
      imageFull: "",
    });
    setIsPickerOpen(false);
  };

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-8 text-[#69324b]">
      <div className="mx-auto flex w-full max-w-[90rem] flex-col gap-5">
        <section className="relative overflow-visible rounded-[2.25rem] border border-white/80 bg-[linear-gradient(135deg,_#ffffff_0%,_#fff7fb_52%,_#ffe8f2_100%)] p-6 shadow-[0_28px_76px_rgba(205,79,134,0.18)] ring-1 ring-[#f8dce8]/70">
          <div className="absolute inset-x-0 top-0 h-1.5 rounded-t-[2.25rem] bg-[linear-gradient(90deg,_#f45f9c_0%,_#ff9ac5_48%,_#ffd1e3_100%)]" />
          <div className="relative flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-white/82 px-4 py-2 text-xs font-black text-[#e75491] shadow-[inset_0_0_0_1px_rgba(255,209,227,0.86)]">
                <span className="h-2 w-2 rounded-full bg-[#f45f9c]" />
                Official Patch Notes · 2026
              </div>
              <h1 className="bg-[linear-gradient(90deg,_#69324b_0%,_#d94687_58%,_#f45f9c_100%)] bg-clip-text text-4xl font-black tracking-normal text-transparent lg:text-6xl">
                패치노트 아카이브
              </h1>
              <p className="mt-3 max-w-2xl break-keep text-sm font-bold leading-6 text-[#a76886] lg:text-base">
                공식 패치노트를 전체 흐름으로 보거나 챔피언별 변경 이력만 골라서 확인합니다.
              </p>
            </div>

            <div className="grid rounded-[1.5rem] bg-white/72 p-1 shadow-[inset_0_0_0_1px_rgba(255,209,227,0.86)] sm:grid-cols-2">
              {[
                { id: "all", label: "전체 패치노트 보기" },
                { id: "champion", label: "챔피언 별로 보기" },
              ].map((view) => (
                <button
                  key={view.id}
                  type="button"
                  onClick={() => setActiveView(view.id as PatchNoteView)}
                  className={`h-11 rounded-[1.25rem] px-5 text-sm font-black transition-all ${
                    activeView === view.id
                      ? "bg-[#f45f9c] text-white shadow-[0_12px_24px_rgba(231,84,145,0.22)]"
                      : "text-[#a76886] hover:bg-white/72"
                  }`}
                >
                  {view.label}
                </button>
              ))}
            </div>
          </div>
        </section>

        {activeView === "all" && (
          <section className="rounded-[2rem] border border-white/75 bg-white/94 p-5 shadow-[0_22px_58px_rgba(205,79,134,0.13)] ring-1 ring-[#f8dce8]/55">
            <div className="mb-4 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <h2 className="text-2xl font-black">전체 패치노트</h2>
                <p className="mt-1 text-sm font-bold text-[#a76886]">
                  Riot 공식 패치노트 원문으로 이동할 수 있습니다.
                </p>
              </div>
              <span className="rounded-full bg-[#fff0f7] px-4 py-2 text-sm font-black text-[#a76886]">
                2026년 이후
              </span>
            </div>

            {isPatchLoading && (
              <div className="rounded-[1.5rem] bg-[#fff7fb] px-5 py-12 text-center text-sm font-bold text-[#a76886]">
                <Loader2 className="mx-auto h-8 w-8 animate-spin text-[#e75491]" />
                <p className="mt-3">공식 패치노트 목록을 불러오는 중입니다.</p>
              </div>
            )}

            {!isPatchLoading && patchErrorMessage && (
              <div className="rounded-[1.5rem] bg-[#fff7fb] px-5 py-12 text-center text-sm font-bold text-[#a76886]">
                {patchErrorMessage}
              </div>
            )}

            {!isPatchLoading && !patchErrorMessage && (
              <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {patches.map((patch) => (
                  <a
                    key={patch.url}
                    href={patch.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="group rounded-[1.5rem] border border-[#f8dce8]/80 bg-[#fff7fb] p-4 shadow-[0_14px_34px_rgba(205,79,134,0.08)] transition-all hover:-translate-y-0.5 hover:bg-white hover:shadow-[0_20px_42px_rgba(205,79,134,0.14)]"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <span className="rounded-full bg-[#f45f9c] px-3 py-1 text-xs font-black text-white">
                        {patch.patchVersion}
                      </span>
                      <ExternalLink className="h-4 w-4 shrink-0 text-[#e75491] transition-transform group-hover:translate-x-0.5" />
                    </div>
                    <h3 className="mt-4 line-clamp-2 text-lg font-black leading-tight">
                      {patch.title}
                    </h3>
                    <p className="mt-3 text-sm font-bold text-[#a76886]">
                      {formatPatchDate(patch.publishedAt)}
                    </p>
                  </a>
                ))}
              </div>
            )}
          </section>
        )}

        {activeView === "champion" && (
          <>
            <section className="rounded-[2rem] border border-white/75 bg-white/94 p-5 shadow-[0_22px_58px_rgba(205,79,134,0.13)] ring-1 ring-[#f8dce8]/55">
              <div className="mb-4">
                <h2 className="text-2xl font-black">챔피언 별로 보기</h2>
                <p className="mt-1 text-sm font-bold text-[#a76886]">
                  챔피언을 검색하면 해당 챔피언이 언급된 공식 패치노트만 모아서 보여줍니다.
                </p>
              </div>

              <form
                onSubmit={handleChampionSubmit}
                className="relative flex w-full max-w-[42rem] items-center gap-2 rounded-[1.5rem] border border-[#ffd1e3] bg-[#fff7fb] p-2 shadow-[inset_0_0_0_1px_rgba(255,255,255,0.65)]"
              >
                <div className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-[1.15rem] bg-[#fff0f7] ring-1 ring-[#ffd1e3]">
                  {selectedChampion.imageFull ? (
                    <Image
                      src={getDataDragonChampionIconUrl(selectedChampion.imageFull)}
                      alt={selectedChampion.nameKo}
                      width={48}
                      height={48}
                      sizes="48px"
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <Search className="h-5 w-5 text-[#e75491]" />
                  )}
                </div>
                <div className="flex min-w-0 flex-1 items-center gap-2 px-2">
                  <Search className="h-4 w-4 shrink-0 text-[#e75491]" />
                  <input
                    value={query}
                    onFocus={() => setIsPickerOpen(true)}
                    onChange={(event) => {
                      setQuery(event.target.value);
                      setIsPickerOpen(true);
                    }}
                    placeholder="챔피언 이름 입력"
                    className="min-w-0 flex-1 bg-transparent text-sm font-black text-[#69324b] outline-none placeholder:text-[#bd7b98]"
                  />
                </div>
                <button
                  type="submit"
                  className="h-11 shrink-0 rounded-full bg-[#f45f9c] px-5 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.22)] transition-all hover:-translate-y-0.5 hover:bg-[#e75491]"
                >
                  조회
                </button>

                {isPickerOpen && filteredChampions.length > 0 && (
                  <div className="absolute left-0 top-[calc(100%+0.75rem)] z-30 max-h-80 w-full overflow-y-auto rounded-[1.5rem] border border-[#ffd1e3] bg-white p-2 shadow-[0_24px_58px_rgba(205,79,134,0.2)]">
                    {filteredChampions.map((champion) => (
                      <button
                        key={champion.id}
                        type="button"
                        onMouseDown={(event) => event.preventDefault()}
                        onClick={() => selectChampion(champion)}
                        className="flex w-full items-center gap-3 rounded-2xl px-3 py-2 text-left transition-colors hover:bg-[#fff0f7]"
                      >
                        <Image
                          src={getDataDragonChampionIconUrl(champion.imageFull)}
                          alt={champion.nameKo}
                          width={40}
                          height={40}
                          sizes="40px"
                          className="h-10 w-10 rounded-xl object-cover"
                        />
                        <span className="font-black text-[#69324b]">
                          {champion.nameKo}
                        </span>
                        <span className="ml-auto text-xs font-bold text-[#a76886]">
                          {champion.nameEn}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </form>
            </section>

            <ChampionPatchNotes championName={selectedChampion.nameKo} />
          </>
        )}
      </div>
    </main>
  );
}
