export interface PatchNote {
  id: number;
  version: string;
  items: string[];
}

export const dummyPatchNotes: PatchNote[] = [
  {
    id: 1,
    version: "패치버전",
    items: [
      "패치 내용 1",
      "패치 내용 2(텍필 950 / 길이 넘어가면 다음 줄로 넘어주세요)",
    ],
  },
  {
    id: 2,
    version: "패치버전",
    items: [
      "패치 내용 1",
      "패치 내용 2(텍필 950 / 길이 넘어가면 다음 줄로 넘어주세요)",
    ],
  },
  {
    id: 3,
    version: "패치버전",
    items: [
      "패치 내용 1",
      "패치 내용 2(텍필 950 / 길이 넘어가면 다음 줄로 넘어주세요)",
    ],
  },
  {
    id: 4,
    version: "패치버전",
    items: [
      "패치 내용 1",
      "패치 내용 2(텍필 950 / 길이 넘어가면 다음 줄로 넘어주세요)",
    ],
  },
  {
    id: 5,
    version: "패치버전",
    items: [
      "패치 내용 1",
      "패치 내용 2(텍필 950 / 길이 넘어가면 다음 줄로 넘어주세요)",
    ],
  },
];
