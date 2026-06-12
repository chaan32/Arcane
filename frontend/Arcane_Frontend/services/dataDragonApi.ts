import { apiJson } from "@/services/apiClient";

export const DDRAGON_VERSION =
  process.env.NEXT_PUBLIC_DDRAGON_VERSION || "16.11.1";

export interface DataDragonChampion {
  id: number;
  nameKo: string;
  nameEn: string;
  imageFull: string;
}

export interface DataDragonItem {
  id: string;
  name: string;
  plaintext: string;
  imageFull: string;
  totalGold: number;
  tags: string[];
}

type DDragonChampionResponse = {
  data?: Record<
    string,
    {
      key: string;
      id: string;
      name: string;
      image: {
        full: string;
      };
    }
  >;
};

type DDragonItemResponse = {
  data?: Record<
    string,
    {
      name?: string;
      plaintext?: string;
      image?: {
        full?: string;
      };
      gold?: {
        purchasable?: boolean;
        total?: number;
      };
      maps?: Record<string, boolean>;
      tags?: string[];
    }
  >;
};

let championListPromise: Promise<DataDragonChampion[]> | null = null;
let championImageByIdPromise: Promise<Record<number, string>> | null = null;
let itemListPromise: Promise<DataDragonItem[]> | null = null;

export const normalizeDataDragonVersion = (version?: string | null) => {
  const trimmedVersion = version?.trim();

  if (!trimmedVersion) {
    return DDRAGON_VERSION;
  }

  const versionParts = trimmedVersion.split(".");

  if (versionParts.length >= 4 && versionParts[0] && versionParts[1]) {
    return `${versionParts[0]}.${versionParts[1]}.1`;
  }

  if (versionParts.length === 2 && versionParts[0] && versionParts[1]) {
    return `${versionParts[0]}.${versionParts[1]}.1`;
  }

  return trimmedVersion;
};

const resolveDataDragonCdnUrl = (version?: string | null) =>
  `https://ddragon.leagueoflegends.com/cdn/${normalizeDataDragonVersion(version)}`;

const championListUrl = `${resolveDataDragonCdnUrl()}/data/ko_KR/champion.json`;
const itemListUrl = `${resolveDataDragonCdnUrl()}/data/ko_KR/item.json`;

export const getDataDragonChampionIconUrl = (imageFull: string, version?: string | null) =>
  `${resolveDataDragonCdnUrl(version)}/img/champion/${imageFull}`;

export const getDataDragonItemIconUrl = (imageFull: string, version?: string | null) =>
  `${resolveDataDragonCdnUrl(version)}/img/item/${imageFull}`;

export const getDataDragonSpellIconUrl = (imageFull: string, version?: string | null) =>
  `${resolveDataDragonCdnUrl(version)}/img/spell/${imageFull}`;

export const getDataDragonPassiveIconUrl = (imageFull: string, version?: string | null) =>
  `${resolveDataDragonCdnUrl(version)}/img/passive/${imageFull}`;

export const getDataDragonProfileIconUrl = (profileIconId: number | string, version?: string | null) =>
  `${resolveDataDragonCdnUrl(version)}/img/profileicon/${profileIconId}.png`;

export const getCommunityDragonChampionIconUrl = (
  championId: number | string
) =>
  `https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons/${championId}.png`;

export const getChampionIconUrl = ({
  championId,
  championImageFull,
  championNameEn,
  imageFull,
  version,
}: {
  championId?: number | string | null;
  championImageFull?: string | null;
  championNameEn?: string | null;
  imageFull?: string | null;
  version?: string | null;
}) => {
  const resolvedImageFull = championImageFull || imageFull;
  if (resolvedImageFull) {
    return getDataDragonChampionIconUrl(resolvedImageFull, version);
  }

  if (championNameEn) {
    return getDataDragonChampionIconUrl(`${championNameEn}.png`, version);
  }

  if (championId) {
    return getCommunityDragonChampionIconUrl(championId);
  }

  return "/sad-summoner.svg";
};

const fetchChampionList = async () => {
  const result = await apiJson<DDragonChampionResponse>(championListUrl);

  return Object.values(result.data ?? {})
    .map((champion) => ({
      id: Number(champion.key),
      nameKo: champion.name,
      nameEn: champion.id,
      imageFull: champion.image.full,
    }))
    .sort((a, b) => a.nameKo.localeCompare(b.nameKo));
};

const fetchItemList = async () => {
  const result = await apiJson<DDragonItemResponse>(itemListUrl);

  return Object.entries(result.data ?? {})
    .filter(([, item]) => {
      const isSummonersRiftItem = item.maps?.["11"] !== false;
      const isPurchasable = item.gold?.purchasable === true;

      return Boolean(
        item.name && item.image?.full && isSummonersRiftItem && isPurchasable
      );
    })
    .map(([id, item]) => ({
      id,
      name: item.name ?? "아이템",
      plaintext: item.plaintext ?? "",
      imageFull: item.image?.full ?? "",
      totalGold: item.gold?.total ?? 0,
      tags: item.tags ?? [],
    }))
    .sort((a, b) => a.name.localeCompare(b.name));
};

export const dataDragonApi = {
  getChampions: () => {
    championListPromise ??= fetchChampionList();
    return championListPromise;
  },

  getChampionImageByIdMap: () => {
    championImageByIdPromise ??= dataDragonApi.getChampions().then((champions) =>
      champions.reduce<Record<number, string>>((acc, champion) => {
        if (!Number.isNaN(champion.id) && champion.imageFull) {
          acc[champion.id] = champion.imageFull;
        }

        return acc;
      }, {})
    );

    return championImageByIdPromise;
  },

  getItems: () => {
    itemListPromise ??= fetchItemList();
    return itemListPromise;
  },
};
