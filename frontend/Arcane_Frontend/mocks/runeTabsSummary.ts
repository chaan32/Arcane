export interface DummyRuneTabMetrics {
  id: number;
  playCount: string;
  pickRate: string;
}

// 룬 탭에 사용되는 임시 데이터
// 이미지는 해당 컴포넌트에서 룬 아이디로 가져옴
export const dummyRuneTabMetrics: DummyRuneTabMetrics[] = [
  { id: 0, playCount: "12,540", pickRate: "55.5%" },
  { id: 1, playCount: "8,210", pickRate: "22.3%" },
];
