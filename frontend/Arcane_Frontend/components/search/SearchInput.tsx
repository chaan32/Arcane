"use client";

import SearchButton from "../common/SearchButton";
import { DownArrow } from "../icons/DownArrow";
import { cn } from "@/lib/utils";

interface SearchInputProps {
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onFocus: () => void;
  onBlur: () => void;
  variant?: "hero" | "nav";
}

export default function SearchInput({
  value,
  onChange,
  onFocus,
  onBlur,
  variant = "hero",
}: SearchInputProps) {
  const isNav = variant === "nav";

  return (
    <div
      className={cn(
        "flex w-full items-center overflow-hidden rounded-full border border-[#ffd1e3] bg-white/92 text-[#69324b] shadow-[0_14px_36px_rgba(205,79,134,0.12)] backdrop-blur",
        isNav ? "h-11" : "h-[3.75rem] lg:h-[5.125rem]"
      )}
    >
      <div
        className={cn(
          "flex h-full flex-col items-start justify-center border-none",
          isNav
            ? "px-4 py-1"
            : "py-2 pl-5 pr-3 lg:py-2.5 lg:pl-12 lg:pr-5"
        )}
      >
        <span
          className={cn(
            "font-black text-[#a76886]",
            isNav ? "text-[10px]" : "text-m-body4 lg:text-d-nav1-def lg:mb-1"
          )}
        >
          서버
        </span>
        <div
          className={cn(
            "flex items-center",
            isNav ? "gap-2" : "gap-2 lg:gap-7"
          )}
        >
          <span
            className={cn(
              "font-black text-[#69324b]",
              isNav ? "text-sm" : "text-m-body2 lg:text-d-KR"
            )}
          >
            KR
          </span>
          <DownArrow
            className={cn(
              "text-[#a76886]",
              isNav ? "h-3.5 w-3.5" : "h-3 w-3 lg:h-6 lg:w-6"
            )}
          />
        </div>
      </div>

      <div
        className={cn(
          "flex h-full flex-1 items-center",
          isNav ? "py-1 pr-1.5" : "py-2 pr-2 lg:py-2.5 lg:pr-5"
        )}
      >
        <span
          className={cn(
            "w-px bg-[#ffd1e3]",
            isNav ? "mr-3 h-7" : "mr-3 h-8 lg:mr-5 lg:h-14"
          )}
        />
        <input
          type="text"
          value={value}
          onChange={onChange}
          onFocus={onFocus}
          onBlur={onBlur}
          placeholder="소환사명 #태그 를 입력해주세요"
          className={cn(
            "min-w-0 flex-1 bg-transparent font-black text-[#69324b] outline-none placeholder:text-[#bd7b98]",
            isNav ? "text-sm" : "text-m-body2 lg:text-d-KR"
          )}
        />
        <SearchButton
          type="submit"
          className={cn(
            isNav ? "h-9 w-9" : "h-12 w-12 p-3 lg:h-14 lg:w-14 lg:p-3.5"
          )}
        />
      </div>
    </div>
  );
}
