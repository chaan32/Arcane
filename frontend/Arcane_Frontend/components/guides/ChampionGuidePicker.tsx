"use client";

import { Search } from "lucide-react";
import Image from "next/image";
import { useEffect, useMemo, useState } from "react";
import {
  dataDragonApi,
  getDataDragonChampionIconUrl,
} from "@/services/dataDragonApi";
import type { GuideChampion } from "@/types/community";

interface ChampionGuidePickerProps {
  selectedChampion: GuideChampion | null;
  onSelect: (champion: GuideChampion) => void;
}

export default function ChampionGuidePicker({
  selectedChampion,
  onSelect,
}: ChampionGuidePickerProps) {
  const [champions, setChampions] = useState<GuideChampion[]>([]);
  const [query, setQuery] = useState("");
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    let isCancelled = false;

    const fetchChampions = async () => {
      const nextChampions = await dataDragonApi.getChampions();

      if (isCancelled) return;

      setChampions(nextChampions);
    };

    fetchChampions().catch((error) => {
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

    if (!normalizedQuery) return champions;

    return champions.filter(
      (champion) =>
        champion.nameKo.toLowerCase().includes(normalizedQuery) ||
        champion.nameEn.toLowerCase().includes(normalizedQuery)
    );
  }, [champions, query]);

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setIsOpen((current) => !current)}
        className="flex w-full items-center gap-3 rounded-[1.5rem] border border-[#ffd1e3] bg-white/92 px-4 py-3 text-left shadow-[0_16px_34px_rgba(205,79,134,0.1)] transition-colors hover:bg-[#fff7fb]"
      >
        {selectedChampion ? (
          <Image
            src={getDataDragonChampionIconUrl(selectedChampion.imageFull)}
            alt={selectedChampion.nameKo}
            width={44}
            height={44}
            sizes="44px"
            className="h-11 w-11 rounded-2xl object-cover"
          />
        ) : (
          <div className="h-11 w-11 rounded-2xl bg-[#fff0f7]" />
        )}
        <div className="min-w-0">
          <div className="text-xs font-black text-[#a76886]">챔피언 선택</div>
          <div className="truncate text-base font-black text-[#69324b]">
            {selectedChampion
              ? selectedChampion.nameKo
              : "공략할 챔피언을 골라주세요"}
          </div>
        </div>
      </button>

      {isOpen && (
        <div className="absolute left-0 top-[calc(100%+0.75rem)] z-40 w-full overflow-hidden rounded-[1.5rem] border border-[#ffd1e3] bg-white shadow-[0_24px_58px_rgba(205,79,134,0.2)]">
          <div className="flex items-center gap-2 border-b border-[#ffe1ed] bg-[#fff7fb] px-4 py-3">
            <Search className="h-4 w-4 text-[#e75491]" />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="챔피언 검색"
              className="min-w-0 flex-1 bg-transparent text-sm font-bold text-[#69324b] outline-none placeholder:text-[#bd7b98]"
            />
          </div>
          <div className="max-h-72 overflow-y-auto p-2">
            {filteredChampions.map((champion) => (
              <button
                key={champion.id}
                type="button"
                onClick={() => {
                  onSelect(champion);
                  setIsOpen(false);
                  setQuery("");
                }}
                className="flex w-full items-center gap-3 rounded-2xl px-3 py-2 text-left transition-colors hover:bg-[#fff0f7]"
              >
                <Image
                  src={getDataDragonChampionIconUrl(champion.imageFull)}
                  alt={champion.nameKo}
                  width={36}
                  height={36}
                  sizes="36px"
                  className="h-9 w-9 rounded-xl object-cover"
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
        </div>
      )}
    </div>
  );
}
