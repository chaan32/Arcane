"use client";

import Link from "next/link";
import { Loader2, MessageCircle, PenLine, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { ExternalImage } from "@/components/common/ExternalImage";
import { useMockAuth } from "@/hooks/useMockAuth";
import type { GuidePost } from "@/types/community";
import LoginDialog from "@/components/auth/LoginDialog";
import { fetchGuidePosts, searchGuidePosts } from "@/services/communityApi";
import { getDataDragonChampionIconUrl } from "@/services/dataDragonApi";

const formatGuideDate = (value: string) =>
  new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(new Date(value));

export default function GuidesPage() {
  const { user } = useMockAuth();
  const [guides, setGuides] = useState<GuidePost[]>([]);
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [query, setQuery] = useState("");
  const [searchResults, setSearchResults] = useState<GuidePost[] | null>(null);

  useEffect(() => {
    let isMounted = true;

    const loadGuides = async () => {
      try {
        const nextGuides = await fetchGuidePosts();
        if (!isMounted) return;
        setGuides(nextGuides);
        setErrorMessage("");
      } catch (error) {
        if (!isMounted) return;
        setErrorMessage(error instanceof Error ? error.message : "공략 목록을 불러오지 못했습니다.");
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    void loadGuides();

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    const normalizedQuery = query.trim();

    if (!normalizedQuery) {
      setSearchResults(null);
      setIsSearching(false);
      return;
    }

    let isMounted = true;
    const timeoutId = window.setTimeout(async () => {
      setIsSearching(true);

      try {
        const elasticsearchResults = await searchGuidePosts(
          normalizedQuery,
          "elasticsearch",
          50
        );
        if (!isMounted) return;
        setSearchResults(elasticsearchResults);
      } catch {
        try {
          const databaseResults = await searchGuidePosts(
            normalizedQuery,
            "database",
            50
          );
          if (!isMounted) return;
          setSearchResults(databaseResults);
        } catch {
          if (!isMounted) return;
          setSearchResults(null);
        }
      } finally {
        if (isMounted) {
          setIsSearching(false);
        }
      }
    }, 260);

    return () => {
      isMounted = false;
      window.clearTimeout(timeoutId);
    };
  }, [query]);

  const locallyFilteredGuides = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    if (!normalizedQuery) return guides;

    return guides.filter(
      (guide) =>
        guide.title.toLowerCase().includes(normalizedQuery) ||
        guide.summary.toLowerCase().includes(normalizedQuery) ||
        guide.markdown.toLowerCase().includes(normalizedQuery) ||
        guide.champion.nameKo.toLowerCase().includes(normalizedQuery) ||
        guide.champion.nameEn.toLowerCase().includes(normalizedQuery)
    );
  }, [guides, query]);

  const visibleGuides = searchResults ?? locallyFilteredGuides;

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-8 text-[#69324b]">
      <div className="mx-auto flex w-full max-w-[90rem] flex-col gap-5">
        <section className="relative overflow-hidden rounded-[2.25rem] border border-white/80 bg-[linear-gradient(135deg,_#ffffff_0%,_#fff7fb_54%,_#ffe8f2_100%)] p-6 shadow-[0_28px_76px_rgba(205,79,134,0.18)] ring-1 ring-[#f8dce8]/70">
          <div className="absolute inset-x-0 top-0 h-1.5 bg-[linear-gradient(90deg,_#f45f9c_0%,_#ff9ac5_50%,_#ffd1e3_100%)]" />
          <div className="relative flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-white/82 px-4 py-2 text-xs font-black text-[#e75491] shadow-[inset_0_0_0_1px_rgba(255,209,227,0.86)]">
                <span className="h-2 w-2 rounded-full bg-[#f45f9c]" />
                Champion Guide Board
              </div>
              <h1 className="bg-[linear-gradient(90deg,_#69324b_0%,_#d94687_58%,_#f45f9c_100%)] bg-clip-text text-4xl font-black tracking-normal text-transparent lg:text-6xl">
                공략 게시판
              </h1>
              <p className="mt-3 max-w-2xl break-keep text-sm font-bold leading-6 text-[#a76886] lg:text-base">
                챔피언별 공략을 정리하고 댓글과 채팅으로 바로 이어서 대화합니다.
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <div className="hidden rounded-[1.25rem] bg-white/72 px-4 py-3 text-right shadow-[inset_0_0_0_1px_rgba(255,209,227,0.7)] sm:block">
                <div className="text-xl font-black text-[#e75491]">
                  {guides.length}
                </div>
                <div className="text-xs font-black text-[#a76886]">등록된 공략</div>
              </div>
              {user ? (
                <Link
                  href="/guides/new"
                  className="inline-flex h-12 items-center gap-2 rounded-full bg-[#f45f9c] px-5 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.24)] transition-all hover:-translate-y-0.5 hover:bg-[#e75491]"
                >
                  <PenLine className="h-4 w-4" />
                  공략 작성
                </Link>
              ) : (
                <button
                  type="button"
                  onClick={() => setIsLoginOpen(true)}
                  className="h-12 rounded-full bg-[#f45f9c] px-5 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.24)]"
                >
                  로그인하고 작성
                </button>
              )}
            </div>
          </div>
        </section>

        <section className="grid gap-4 lg:grid-cols-[20rem_minmax(0,1fr)]">
          <aside className="rounded-[1.75rem] border border-white/75 bg-white/92 p-4 shadow-[0_18px_46px_rgba(205,79,134,0.12)] ring-1 ring-[#f8dce8]/55">
            <div className="flex h-12 items-center gap-2 rounded-[1.25rem] border border-[#ffd1e3] bg-[#fff7fb] px-4">
              <Search className="h-4 w-4 text-[#e75491]" />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="공략 또는 챔피언 검색"
                className="min-w-0 flex-1 bg-transparent text-sm font-bold text-[#69324b] outline-none placeholder:text-[#bd7b98]"
              />
            </div>
            <div className="mt-4 rounded-[1.25rem] bg-[#fff7fb] p-4">
              <div className="text-sm font-black text-[#69324b]">게시판 현황</div>
              <div className="mt-3 grid grid-cols-2 gap-2 text-center">
                <div className="rounded-2xl bg-white px-3 py-4 shadow-[inset_0_0_0_1px_rgba(248,220,232,0.75)]">
                  <div className="text-2xl font-black text-[#e75491]">
                    {guides.length}
                  </div>
                  <div className="text-xs font-bold text-[#a76886]">공략</div>
                </div>
                <div className="rounded-2xl bg-white px-3 py-4 shadow-[inset_0_0_0_1px_rgba(248,220,232,0.75)]">
                  <div className="text-2xl font-black text-[#e75491]">
                    {guides.reduce((sum, guide) => sum + guide.commentCount, 0)}
                  </div>
                  <div className="text-xs font-bold text-[#a76886]">댓글</div>
                </div>
              </div>
            </div>
          </aside>

          <div className="grid gap-4 xl:grid-cols-2">
            {isLoading && (
              <div className="rounded-[1.75rem] border border-white/75 bg-white/94 p-10 text-center text-sm font-bold text-[#a76886] shadow-[0_18px_46px_rgba(205,79,134,0.12)]">
                <Loader2 className="mx-auto h-8 w-8 animate-spin text-[#e75491]" />
                <p className="mt-3">공략 목록을 불러오는 중입니다.</p>
              </div>
            )}

            {!isLoading && errorMessage && (
              <div className="rounded-[1.75rem] border border-white/75 bg-white/94 p-10 text-center text-sm font-bold text-[#a76886] shadow-[0_18px_46px_rgba(205,79,134,0.12)]">
                {errorMessage}
              </div>
            )}

            {!isLoading && !errorMessage && isSearching && (
              <div className="rounded-[1.75rem] border border-white/75 bg-white/94 p-10 text-center text-sm font-bold text-[#a76886] shadow-[0_18px_46px_rgba(205,79,134,0.12)]">
                <Loader2 className="mx-auto h-8 w-8 animate-spin text-[#e75491]" />
                <p className="mt-3">공략 내용을 검색하는 중입니다.</p>
              </div>
            )}

            {!isLoading && !errorMessage && !isSearching && visibleGuides.map((guide) => (
              <Link
                key={guide.id}
                href={`/guides/${guide.id}`}
                className="group overflow-hidden rounded-[1.75rem] border border-white/75 bg-white/94 shadow-[0_18px_46px_rgba(205,79,134,0.12)] ring-1 ring-[#f8dce8]/55 transition-all hover:-translate-y-1 hover:shadow-[0_26px_64px_rgba(205,79,134,0.18)]"
              >
                <div className="flex gap-4 p-4">
                  <div className="relative h-24 w-24 shrink-0 overflow-hidden rounded-[1.5rem] bg-[#fff0f7]">
                    <ExternalImage
                      src={
                        guide.coverImageUrl ||
                        getDataDragonChampionIconUrl(guide.champion.imageFull)
                      }
                      alt={guide.champion.nameKo}
                      width={96}
                      height={96}
                      sizes="96px"
                      fallback={<div className="h-full w-full bg-[#fff0f7]" />}
                      className="h-full w-full object-cover transition-transform group-hover:scale-105"
                    />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="rounded-full bg-[#fff0f7] px-2.5 py-1 text-xs font-black text-[#e75491]">
                        {guide.champion.nameKo}
                      </span>
                      <span className="text-xs font-bold text-[#a76886]">
                        {formatGuideDate(guide.updatedAt)}
                      </span>
                    </div>
                    <h2 className="mt-3 line-clamp-2 text-xl font-black leading-tight text-[#69324b]">
                      {guide.title}
                    </h2>
                    <p className="mt-2 line-clamp-2 text-sm font-bold leading-6 text-[#a76886]">
                      {guide.summary}
                    </p>
                    <div className="mt-4 flex items-center justify-between text-xs font-bold text-[#a76886]">
                      <span>{guide.author.name}</span>
                      <span className="inline-flex items-center gap-1">
                        <MessageCircle className="h-3.5 w-3.5" />
                        {guide.commentCount}
                      </span>
                    </div>
                  </div>
                </div>
              </Link>
            ))}

            {!isLoading && !errorMessage && !isSearching && visibleGuides.length === 0 && (
              <div className="rounded-[1.75rem] border border-white/75 bg-white/94 p-10 text-center text-sm font-bold text-[#a76886] shadow-[0_18px_46px_rgba(205,79,134,0.12)]">
                검색 결과가 없습니다.
              </div>
            )}
          </div>
        </section>
      </div>
      <LoginDialog isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
    </div>
  );
}
