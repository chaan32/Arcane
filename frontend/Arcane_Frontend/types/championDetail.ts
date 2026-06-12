// statistics/championDetail/championName API 응답 타입 정의
export interface ChampionDetailDto {
  detailChampInfo: ChampionDetailChampInfo;
  detailChampBuild: ChampionDetailBuild;
  relativeWinRate: ChampionDetailList;
  synergyChampion: ChampionDetailList;
}

// 챔피언 기본 정보
export interface ChampionDetailChampInfo {
  championId?: number;
  championName: string;
  championNameEn: string;
  championImageFull?: string;
  version?: string | null;
  tier: number;
  winRate: number;
  pickRate: number;
  banRate: number;
  gameCount: number;
  percent: number;
}

export interface ChampionDetailBuild {
  // 라인
  lane: string;
  // 아이템
  item01: number;
  item02: number;
  item03: number;
  item04: number;
  item05: number;
  item06: number;
  // 주문
  summoner1Id: number;
  summoner2Id: number;
  summoner3Id: number;
  summoner4Id: number;
  perks: PerksDto;
  topItems?: ChampionOptionStatDto[];
  topSummonerSpells?: ChampionOptionStatDto[];
}

export interface ChampionOptionStatDto {
  itemId?: number | null;
  spell1Id?: number | null;
  spell2Id?: number | null;
  games?: number;
  winRate?: number;
  pickRate?: number;
}

export interface PerksDto {
  // 공격/방어/유틸 수치
  statPerks: StatPerksDto;
  // 룬
  styles: StyleDto[];
}

// 공격/방어/유틸 수치
export interface StatPerksDto {
  defense: number;
  flex: number;
  offense: number;
}

// 룬 스타일 + 선택한 룬
// description : 핵심, 서브
export interface StyleDto {
  description: string;
  style: number;
  selections: SelectionDto[];
}

// 선택한 룬 정보
export interface SelectionDto {
  perk: number;
  var1: number;
  var2: number;
  var3: number;
}

export interface ChampionDetailList {
  champions: ChampionDetailWith[];
}

//  관련 챔피언 정보
export interface ChampionDetailWith {
  championId?: number;
  championName: string;
  championNameEn: string;
  championImageFull?: string;
  gameCount: number;
  winRate: number;
}
