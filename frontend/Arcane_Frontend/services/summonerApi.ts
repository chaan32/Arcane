import { apiFetch } from "@/services/apiClient";

export const summonerApi = {
  // 키워드 포함 검색 (사용자 이름만 작성시)
  searchByKeyword: async (keyword: string) => {
    const res = await apiFetch(
      `/api/v1/summoner/contain/${encodeURIComponent(keyword)}`
    );
    return res.ok ? await res.json() : [];
  },

  // 사용자 정보 조회 (사용자 이름+ 태그 작성시)
  searchByTag: async (gameName: string, tagLine: string) => {
    const params = new URLSearchParams({
      gameName,
      tagLine,
      refresh: "false",
    });
    const res = await apiFetch(`/api/v1/summoner/tier?${params.toString()}`);
    return res.ok ? await res.json() : null;
  },

  // 사용자 아이콘 url+레벨 조회
  getProfile: async (gameName: string, tagLine: string) => {
    const params = new URLSearchParams({
      gameName,
      tagLine,
      refresh: "false",
    });
    const res = await apiFetch(`/api/v1/summoner/profile?${params.toString()}`);
    return res.ok ? await res.json() : null;
  },
};
