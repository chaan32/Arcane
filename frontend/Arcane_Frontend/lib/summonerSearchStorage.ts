import type { SummonerDropdownType } from "@/types/summoner";

const RECENT_SUMMONERS_KEY = "arcane.summoner.recent";
const FAVORITE_SUMMONERS_KEY = "arcane.summoner.favorite";
const SAVE_RECENT_ENABLED_KEY = "arcane.summoner.saveRecentEnabled";
export const SUMMONER_CACHE_CHANGE_EVENT = "arcane-summoner-cache-change";

const MAX_RECENT_SUMMONERS = 10;

const isBrowser = () => typeof window !== "undefined";

const getSummonerKey = (summoner: Pick<SummonerDropdownType, "gameName" | "tagLine">) =>
  `${summoner.gameName.trim().toLowerCase()}#${summoner.tagLine
    .trim()
    .toLowerCase()}`;

const readSummoners = (key: string): SummonerDropdownType[] => {
  if (!isBrowser()) return [];

  const rawValue = window.localStorage.getItem(key);
  if (!rawValue) return [];

  try {
    return JSON.parse(rawValue) as SummonerDropdownType[];
  } catch {
    window.localStorage.removeItem(key);
    return [];
  }
};

const writeSummoners = (key: string, summoners: SummonerDropdownType[]) => {
  if (!isBrowser()) return;
  window.localStorage.setItem(key, JSON.stringify(summoners));
  window.dispatchEvent(new Event(SUMMONER_CACHE_CHANGE_EVENT));
};

export const getRecentSummoners = () => readSummoners(RECENT_SUMMONERS_KEY);

export const getFavoriteSummoners = () =>
  readSummoners(FAVORITE_SUMMONERS_KEY);

export const getSaveRecentEnabled = () => {
  if (!isBrowser()) return true;

  return window.localStorage.getItem(SAVE_RECENT_ENABLED_KEY) !== "false";
};

export const setSaveRecentEnabled = (enabled: boolean) => {
  if (!isBrowser()) return;
  window.localStorage.setItem(SAVE_RECENT_ENABLED_KEY, String(enabled));
  window.dispatchEvent(new Event(SUMMONER_CACHE_CHANGE_EVENT));
};

export const saveRecentSummoner = (summoner: SummonerDropdownType) => {
  if (!getSaveRecentEnabled()) return;

  const summonerKey = getSummonerKey(summoner);
  const nextSummoners = [
    summoner,
    ...getRecentSummoners().filter(
      (item) => getSummonerKey(item) !== summonerKey
    ),
  ].slice(0, MAX_RECENT_SUMMONERS);

  writeSummoners(RECENT_SUMMONERS_KEY, nextSummoners);
};

export const removeRecentSummoner = (summoner: SummonerDropdownType) => {
  const summonerKey = getSummonerKey(summoner);

  writeSummoners(
    RECENT_SUMMONERS_KEY,
    getRecentSummoners().filter((item) => getSummonerKey(item) !== summonerKey)
  );
};

export const isFavoriteSummoner = (
  summoner: Pick<SummonerDropdownType, "gameName" | "tagLine">
) => {
  const summonerKey = getSummonerKey(summoner);

  return getFavoriteSummoners().some(
    (favorite) => getSummonerKey(favorite) === summonerKey
  );
};

export const toggleFavoriteSummoner = (summoner: SummonerDropdownType) => {
  const summonerKey = getSummonerKey(summoner);
  const favorites = getFavoriteSummoners();
  const exists = favorites.some((favorite) => getSummonerKey(favorite) === summonerKey);

  writeSummoners(
    FAVORITE_SUMMONERS_KEY,
    exists
      ? favorites.filter((favorite) => getSummonerKey(favorite) !== summonerKey)
      : [summoner, ...favorites]
  );
};

export const removeFavoriteSummoner = (summoner: SummonerDropdownType) => {
  const summonerKey = getSummonerKey(summoner);

  writeSummoners(
    FAVORITE_SUMMONERS_KEY,
    getFavoriteSummoners().filter(
      (favorite) => getSummonerKey(favorite) !== summonerKey
    )
  );
};

export const toStoredSummoner = ({
  gameName,
  tagLine,
  profileUrl = "",
  level = 0,
  puuid,
  id,
}: {
  gameName: string;
  tagLine: string;
  profileUrl?: string;
  level?: number;
  puuid?: string;
  id?: number;
}): SummonerDropdownType => ({
  id: id ?? Math.abs(`${gameName}#${tagLine}`.split("").reduce((sum, char) => sum + char.charCodeAt(0), 0)),
  puuid: puuid ?? `${gameName}#${tagLine}`,
  gameName,
  tagLine,
  profileUrl,
  level,
});
