"use client";

import { useEffect, useMemo, useState } from "react";
import { ExternalLink, RefreshCw } from "lucide-react";
import { riotPatchNoteApi } from "@/services/riotPatchNoteApi";
import type { RiotChampionPatchNote } from "@/types/riotPatchNotes";

interface ChampionPatchNotesProps {
  championName: string;
}

const formatPatchDate = (publishedAt: string) => {
  if (!publishedAt) {
    return "";
  }

  const date = new Date(publishedAt);
  if (Number.isNaN(date.getTime())) {
    return publishedAt;
  }

  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
};

export function ChampionPatchNotes({ championName }: ChampionPatchNotesProps) {
  const [patches, setPatches] = useState<RiotChampionPatchNote[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const normalizedChampionName = useMemo(
    () => championName?.trim() ?? "",
    [championName]
  );

  useEffect(() => {
    if (!normalizedChampionName) {
      setPatches([]);
      return;
    }

    let isMounted = true;

    const fetchPatchNotes = async () => {
      setIsLoading(true);
      setErrorMessage("");

      try {
        const result = await riotPatchNoteApi.getChampionPatchNotes(
          normalizedChampionName
        );

        if (isMounted) {
          setPatches(result.patches);
        }
      } catch {
        if (isMounted) {
          setPatches([]);
          setErrorMessage("공식 패치노트를 불러오지 못했습니다.");
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    fetchPatchNotes();

    return () => {
      isMounted = false;
    };
  }, [normalizedChampionName]);

  return (
    <div className="flex w-full flex-col lg:px-[1.25rem] lg:py-[1.5rem]">
      <div className="mb-[1.5rem] flex items-center justify-between gap-[1rem]">
        <div>
          <p className="text-d-body3-r font-semibold text-[#d94683]">
            2026년 이후 공식 패치노트
          </p>
          <h3 className="mt-[0.25rem] text-d-title3 text-[#69324b]">
            {normalizedChampionName} 변경 내역
          </h3>
        </div>
        <a
          href="https://www.leagueoflegends.com/ko-kr/news/tags/patch-notes/"
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center gap-[0.35rem] rounded-full border border-[#ffc9dd] bg-white px-[0.9rem] py-[0.55rem] text-d-body3-r font-semibold text-[#b45f86] shadow-[0_10px_24px_rgba(244,114,182,0.12)] transition hover:border-[#ff8fbd] hover:text-[#e94f95]"
        >
          원문 목록
          <ExternalLink size={15} />
        </a>
      </div>

      {isLoading && (
        <div className="flex min-h-[16rem] flex-col items-center justify-center rounded-[1.5rem] bg-[#fff7fb] text-[#b45f86] shadow-inner shadow-[#ffe3ef]">
          <RefreshCw className="mb-[0.9rem] animate-spin text-[#f45b9b]" />
          <p className="text-d-body2 font-semibold">
            공식 패치노트를 읽는 중입니다.
          </p>
        </div>
      )}

      {!isLoading && errorMessage && (
        <div className="flex min-h-[14rem] items-center justify-center rounded-[1.5rem] bg-[#fff7fb] text-d-body2 font-semibold text-[#b45f86] shadow-inner shadow-[#ffe3ef]">
          {errorMessage}
        </div>
      )}

      {!isLoading && !errorMessage && patches.length === 0 && (
        <div className="flex min-h-[14rem] flex-col items-center justify-center rounded-[1.5rem] bg-[#fff7fb] px-[2rem] text-center text-[#b45f86] shadow-inner shadow-[#ffe3ef]">
          <p className="text-d-title4 text-[#69324b]">변경 내역이 없습니다.</p>
          <p className="mt-[0.5rem] text-d-body3-r">
            2026년 이후 공식 패치노트에서 이 챔피언 항목을 찾지 못했습니다.
          </p>
        </div>
      )}

      {!isLoading && !errorMessage && patches.length > 0 && (
        <div className="flex flex-col gap-[1rem]">
          {patches.map((note) => (
            <article
              key={`${note.patchVersion}-${note.url}`}
              className="overflow-hidden rounded-[1.5rem] bg-white shadow-[0_18px_44px_rgba(244,114,182,0.13)] ring-1 ring-[#ffe1ec]"
            >
              <div className="flex items-start justify-between gap-[1rem] border-b border-[#ffe3ef] bg-[#fff8fb] px-[1.25rem] py-[1rem]">
                <div>
                  <div className="mb-[0.5rem] inline-flex rounded-full bg-[#f45b9b] px-[0.75rem] py-[0.25rem] text-d-body3-r font-bold text-white shadow-[0_8px_18px_rgba(244,91,155,0.24)]">
                    {note.patchVersion}
                  </div>
                  <h4 className="text-d-body1 font-bold text-[#69324b]">
                    {note.title}
                  </h4>
                  <p className="mt-[0.25rem] text-d-body3-r font-semibold text-[#b45f86]">
                    {formatPatchDate(note.publishedAt)}
                  </p>
                </div>
                <a
                  href={note.url}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex shrink-0 items-center gap-[0.35rem] rounded-full bg-white px-[0.75rem] py-[0.5rem] text-d-body3-r font-semibold text-[#d94683] ring-1 ring-[#ffc9dd] transition hover:bg-[#fff0f7]"
                >
                  원문
                  <ExternalLink size={14} />
                </a>
              </div>

              <div className="flex flex-col gap-[0.85rem] px-[1.25rem] py-[1rem]">
                {note.changes.map((change, changeIndex) => (
                  <section
                    key={`${note.patchVersion}-${change.sectionTitle}-${changeIndex}`}
                    className="rounded-[1rem] bg-[#fff7fb] px-[1rem] py-[0.9rem]"
                  >
                    <h5 className="mb-[0.55rem] text-d-body2 font-bold text-[#8a3d61]">
                      {change.sectionTitle}
                    </h5>
                    <ul className="flex flex-col gap-[0.45rem]">
                      {change.items.map((item, itemIndex) => (
                        <li
                          key={`${item}-${itemIndex}`}
                          className="flex gap-[0.5rem] text-d-body3-r leading-relaxed text-[#7c4760]"
                        >
                          <span className="mt-[0.5rem] h-[0.35rem] w-[0.35rem] shrink-0 rounded-full bg-[#f45b9b]" />
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </section>
                ))}
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
