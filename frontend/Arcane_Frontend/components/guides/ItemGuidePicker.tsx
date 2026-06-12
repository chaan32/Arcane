"use client";

import { PackageSearch, Search } from "lucide-react";
import Image from "next/image";
import { useEffect, useMemo, useState } from "react";
import {
  dataDragonApi,
  getDataDragonItemIconUrl,
  type DataDragonItem,
} from "@/services/dataDragonApi";

export type GuideItem = DataDragonItem;

interface ItemGuidePickerProps {
  onSelect: (item: GuideItem) => void;
}

export const getGuideItemMarkdown = (item: GuideItem) =>
  `![${item.name}](${getDataDragonItemIconUrl(item.imageFull)})`;

export default function ItemGuidePicker({ onSelect }: ItemGuidePickerProps) {
  const [items, setItems] = useState<GuideItem[]>([]);
  const [query, setQuery] = useState("");
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    let isCancelled = false;

    const fetchItems = async () => {
      const nextItems = await dataDragonApi.getItems();

      if (isCancelled) return;

      setItems(nextItems);
    };

    fetchItems().catch((error) => {
      if (!isCancelled) {
        console.error("아이템 목록 요청 실패:", error);
      }
    });

    return () => {
      isCancelled = true;
    };
  }, []);

  const filteredItems = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    if (!normalizedQuery) return items;

    return items.filter(
      (item) =>
        item.name.toLowerCase().includes(normalizedQuery) ||
        item.id.includes(normalizedQuery) ||
        item.tags.some((tag) => tag.toLowerCase().includes(normalizedQuery))
    );
  }, [items, query]);

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setIsOpen((current) => !current)}
        className="inline-flex h-12 items-center gap-2 rounded-full border border-[#ffd1e3] bg-[#fff0f7] px-5 text-sm font-black text-[#e75491] transition-colors hover:bg-[#ffe0ee]"
      >
        <PackageSearch className="h-4 w-4" />
        아이템 삽입
      </button>

      {isOpen && (
        <div className="absolute bottom-[calc(100%+0.75rem)] left-0 z-50 w-[min(36rem,calc(100vw-2rem))] overflow-hidden rounded-[1.5rem] border border-[#ffd1e3] bg-white shadow-[0_24px_58px_rgba(205,79,134,0.2)]">
          <div className="flex items-center gap-2 border-b border-[#ffe1ed] bg-[#fff7fb] px-4 py-3">
            <Search className="h-4 w-4 text-[#e75491]" />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="아이템 이름, 태그, ID 검색"
              className="min-w-0 flex-1 bg-transparent text-sm font-bold text-[#69324b] outline-none placeholder:text-[#bd7b98]"
            />
          </div>

          <div className="grid max-h-80 grid-cols-1 gap-2 overflow-y-auto p-2 sm:grid-cols-2">
            {filteredItems.length > 0 ? (
              filteredItems.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    onSelect(item);
                    setIsOpen(false);
                    setQuery("");
                  }}
                  className="flex min-w-0 items-center gap-3 rounded-2xl px-3 py-2 text-left transition-colors hover:bg-[#fff0f7]"
                >
                  <Image
                    src={getDataDragonItemIconUrl(item.imageFull)}
                    alt={item.name}
                    width={42}
                    height={42}
                    className="h-11 w-11 shrink-0 rounded-xl object-cover shadow-[0_8px_16px_rgba(205,79,134,0.12)]"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm font-black text-[#69324b]">
                      {item.name}
                    </div>
                    <div className="mt-0.5 flex items-center gap-2 text-xs font-bold text-[#a76886]">
                      <span>{item.totalGold.toLocaleString()}G</span>
                      {item.plaintext && (
                        <span className="truncate">{item.plaintext}</span>
                      )}
                    </div>
                  </div>
                </button>
              ))
            ) : (
              <div className="col-span-full px-4 py-10 text-center text-sm font-bold text-[#a76886]">
                검색된 아이템이 없습니다.
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
