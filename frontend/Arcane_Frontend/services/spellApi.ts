import { apiJson } from "@/services/apiClient";
import type { Spell } from "@/types/spell";

export const spellApi = {
  /**
   * 여러 소환사 주문 ID에 대한 데이터 가져오기
   */
  fetchSpellsByIds: async (
    spellIds: number[]
  ): Promise<Record<number, Spell>> => {
    const uniqueIds = [...new Set(spellIds.filter((id) => id !== undefined))];
    const spellData: Record<number, Spell> = {};

    await Promise.all(
      uniqueIds.map(async (spellId) => {
        try {
          const result = await apiJson<Spell | undefined>(
            `/api/v1/summoner-spell/${spellId}?id=${spellId}`,
            { method: "GET" }
          );
          if (result) {
            spellData[spellId] = result;
          }
        } catch (error) {
          console.error(`Error fetching spell ${spellId}:`, error);
        }
      })
    );

    return spellData;
  },

  /**
   * 빌드 데이터에서 소환사 주문 ID 추출
   */
  extractSpellIds: (
    build:
      | {
          summoner1Id?: number;
          summoner2Id?: number;
          summoner3Id?: number;
          summoner4Id?: number;
        }
      | undefined
  ): number[] => {
    if (!build) return [];

    return [
      build.summoner1Id,
      build.summoner2Id,
      build.summoner3Id,
      build.summoner4Id,
    ].filter((id): id is number => id !== undefined);
  },

  /**
   * 빌드 데이터로부터 소환사 주문 데이터 가져오기 (편의 함수)
   */
  fetchSpellsFromBuild: async (
    build:
      | {
          summoner1Id?: number;
          summoner2Id?: number;
          summoner3Id?: number;
          summoner4Id?: number;
        }
      | undefined
  ): Promise<Record<number, Spell>> => {
    const spellIds = spellApi.extractSpellIds(build);
    return spellApi.fetchSpellsByIds(spellIds);
  },
};
