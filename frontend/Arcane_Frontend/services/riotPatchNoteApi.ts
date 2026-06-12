import { apiJson } from "@/services/apiClient";
import type {
  RiotChampionPatchResponse,
  RiotPatchNoteListResponse,
} from "@/types/riotPatchNotes";

export const riotPatchNoteApi = {
  getPatchNotes: async (): Promise<RiotPatchNoteListResponse> => {
    return apiJson<RiotPatchNoteListResponse>("/api/v1/patchnote/riot?fromYear=2026", {
      method: "GET",
      cache: "no-store",
    });
  },

  getChampionPatchNotes: async (
    championName: string
  ): Promise<RiotChampionPatchResponse> => {
    const encodedName = encodeURIComponent(championName);
    return apiJson<RiotChampionPatchResponse>(
      `/api/v1/patchnote/riot/champion/${encodedName}?fromYear=2026`,
      {
        method: "GET",
        cache: "no-store",
      }
    );
  },
};
