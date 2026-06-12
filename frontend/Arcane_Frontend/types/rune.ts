// rune/{id} API 응답 타입
export interface Rune {
  id: number;
  name: string;
  key: string;
  icon: string;
  description: string;
}

export interface RuneTreeRune extends Rune {}

export interface RuneTreeSlot {
  id: number;
  runes: RuneTreeRune[];
}

export interface RuneTreePath {
  id: number;
  name: string;
  key: string;
  icon: string;
  slots: RuneTreeSlot[];
}
