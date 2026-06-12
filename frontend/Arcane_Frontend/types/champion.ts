// 챔피언 기본 정보 및 스킬 관련 타입

export interface ChampionInfo {
  id: number;
  nameEn: string;
  nameKo: string;
  title: string;
  blurb: string;
  imageFull: string;
  version?: string | null;
  tags: string[];
  info: ChampionInfoStats;
  stats: ChampionStats;
  passive: ChampionPassive;
  spells: ChampionSpell[];
  skins: ChampionSkin[];
}

export interface ChampionInfoStats {
  attack: number;
  defense: number;
  magic: number;
  difficulty: number;
}

export interface ChampionStats {
  hp: number;
  mp: number;
  armor: number;
  crit: number;
  hpperlevel: number;
  mpperlevel: number;
  movespeed: number;
  armorperlevel: number;
  spellblock: number;
  spellblockperlevel: number;
  attackrange: number;
  hpregen: number;
  hpregenperlevel: number;
  mpregen: number;
  mpregenperlevel: number;
  critperlevel: number;
  attackdamage: number;
  attackdamageperlevel: number;
  attackspeedperlevel: number;
  attackspeed: number;
}

export interface ChampionPassive {
  name: string;
  description: string;
  imageFull: string;
}

export interface ChampionSpell {
  id: string;
  name: string;
  description: string;
  spellKey: string;
  imageFull: string;
  version?: string | null;
  cooldown: string;
  cost: string;
}

export interface ChampionSkin {
  id: string;
  num: number;
  name: string;
  chromas: boolean;
}
