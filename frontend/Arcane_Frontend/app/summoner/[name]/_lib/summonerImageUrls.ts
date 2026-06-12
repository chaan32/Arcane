import {
  getDataDragonChampionIconUrl,
  getDataDragonItemIconUrl,
  getDataDragonSpellIconUrl,
} from "@/services/dataDragonApi";

const isResolvedImageSrc = (value: string) =>
  value.startsWith("http://") ||
  value.startsWith("https://") ||
  value.startsWith("/");

export const getChampionIconUrl = (
  championNameEn?: string | null,
  version?: string | null
): string =>
  championNameEn
    ? getDataDragonChampionIconUrl(`${championNameEn}.png`, version)
    : "/sad-summoner.svg";

export const getItemIconUrl = (
  itemId: number,
  version?: string | null
): string => getDataDragonItemIconUrl(`${itemId}.png`, version);

export const getSpellIconUrl = (
  imageFull?: string | null,
  version?: string | null
) => {
  if (!imageFull) return null;
  if (isResolvedImageSrc(imageFull)) return imageFull;

  return getDataDragonSpellIconUrl(imageFull, version);
};

export const getChampionSpellIconUrl = (
  imageFull?: string | null,
  version?: string | null
) => {
  if (!imageFull) return null;
  if (isResolvedImageSrc(imageFull)) return imageFull;

  return getDataDragonSpellIconUrl(imageFull, version);
};

export const getRuneIconUrl = (icon?: string | null) => {
  if (!icon) return null;
  if (isResolvedImageSrc(icon)) return icon;

  return `https://ddragon.leagueoflegends.com/cdn/img/${icon}`;
};
