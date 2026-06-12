"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import SearchInput from "@/components/search/SearchInput";
import SearchDropdown from "@/components/search/SearchDropdown";
import { useSummonerSearch } from "@/hooks/useSummonerSearch";
import { getSummonerUrl } from "@/utils/navigation";
import {
  getFavoriteSummoners,
  getRecentSummoners,
  saveRecentSummoner,
  SUMMONER_CACHE_CHANGE_EVENT,
  toStoredSummoner,
} from "@/lib/summonerSearchStorage";
import type { SummonerDropdownType } from "@/types/summoner";

interface SearchBarProps {
  className?: string;
  variant?: "hero" | "nav";
}

const parseSearchQuery = (query: string) => {
  const trimmedQuery = query.trim();
  if (!trimmedQuery) return null;

  if (trimmedQuery.includes("#")) {
    const [gameName, tagLine] = trimmedQuery.split("#").map((value) => value.trim());
    return gameName && tagLine ? { gameName, tagLine } : null;
  }

  const lastDashIndex = trimmedQuery.lastIndexOf("-");
  if (lastDashIndex > 0 && lastDashIndex < trimmedQuery.length - 1) {
    return {
      gameName: trimmedQuery.slice(0, lastDashIndex).trim(),
      tagLine: trimmedQuery.slice(lastDashIndex + 1).trim(),
    };
  }

  return null;
};

export default function SearchBar({ className, variant = "hero" }: SearchBarProps) {
  const router = useRouter();
  const [isFocused, setIsFocused] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const { data: results = [] } = useSummonerSearch(searchQuery);
  const [recentSummoners, setRecentSummoners] = useState<SummonerDropdownType[]>([]);
  const [favoriteSummoners, setFavoriteSummoners] = useState<SummonerDropdownType[]>([]);

  const isNav = variant === "nav";
  const dropdownResults = useMemo(() => results.filter(Boolean), [results]);

  useEffect(() => {
    const syncCache = () => {
      setRecentSummoners(getRecentSummoners());
      setFavoriteSummoners(getFavoriteSummoners());
    };

    syncCache();
    window.addEventListener(SUMMONER_CACHE_CHANGE_EVENT, syncCache);
    window.addEventListener("storage", syncCache);

    return () => {
      window.removeEventListener(SUMMONER_CACHE_CHANGE_EVENT, syncCache);
      window.removeEventListener("storage", syncCache);
    };
  }, []);

  const goToSummoner = (summoner: SummonerDropdownType) => {
    saveRecentSummoner(summoner);
    setSearchQuery("");
    setIsFocused(false);
    router.push(getSummonerUrl(`${summoner.gameName}#${summoner.tagLine}`));
  };

  const handleSearch = (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();

    const parsedQuery = parseSearchQuery(searchQuery);
    if (parsedQuery) {
      const storedSummoner = toStoredSummoner(parsedQuery);
      goToSummoner(storedSummoner);
      return;
    }

    if (dropdownResults[0]) {
      goToSummoner(dropdownResults[0]);
    }
  };

  return (
    <form
      onSubmit={handleSearch}
      className={
        className ??
        (isNav
          ? "relative hidden w-[31rem] max-w-[38vw] lg:block"
          : "relative mx-auto w-[20.9375rem] lg:w-[58.125rem]")
      }
    >
      <SearchInput
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
        variant={variant}
      />

      {isFocused && (
        <SearchDropdown
          favoriteSummoners={favoriteSummoners}
          query={searchQuery}
          recentSummoners={recentSummoners}
          results={dropdownResults}
          onSelect={goToSummoner}
        />
      )}
    </form>
  );
}
