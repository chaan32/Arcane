export interface RiotPatchNoteListResponse {
  patches: RiotPatchNoteSummary[];
}

export interface RiotPatchNoteSummary {
  patchVersion: string;
  title: string;
  url: string;
  publishedAt: string;
}

export interface RiotChampionPatchResponse {
  championName: string;
  patches: RiotChampionPatchNote[];
}

export interface RiotChampionPatchNote {
  patchVersion: string;
  title: string;
  url: string;
  publishedAt: string;
  changes: RiotChampionPatchChange[];
}

export interface RiotChampionPatchChange {
  sectionTitle: string;
  items: string[];
}
