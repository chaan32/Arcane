export type SummonerRank = {
  rankTier: string;
  rankLP: number;
  rankWin: number;
  rankDefeat: number;
  rankType: string;
};

export type Summoner = {
  id: number;
  gameName: string;
  tagLine: string;
  puuid: string;
  updateAt?: string | null;
  soloRank: SummonerRank;
  flexRank: SummonerRank;
};

export type Profile = {
  profileIconId: number;
  summonerLevel: number;
  profileUrl: string;
};

export type Mastery = {
  championName: string;
  championId: number;
  championPoints: number;
  championLevel: number;
};

export type MatchPlayerIndex = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9;

export type MatchParticipantKey = `player${MatchPlayerIndex}`;

type RuneDetail = {
  id: number;
  desc: string;
};

export type MatchRune = {
  mainRune: {
    typeDesc: string;
    typeId: number;
    mainRune: RuneDetail;
    rune1: RuneDetail;
    rune2: RuneDetail;
    rune3: RuneDetail;
  };
  subRune: {
    styleId: number;
    mainRune: RuneDetail;
    rune1: RuneDetail;
    rune2: RuneDetail;
  };
  statRune: {
    defense: number;
    flex: number;
    offense: number;
  };
};

type MatchPlayerBase = {
  puuid: string;
  championId: number;
  championNameEn: string;
  championNameKo: string;
  champLevel: number;
  item0: number;
  item1: number;
  item2: number;
  item3: number;
  item4: number;
  item5: number;
  item6: number;
  kda: number;
  kills: number;
  deaths: number;
  assists: number;
  totalDamageDealtToChampions: number;
  totalDamageTaken: number;
  totalMinionKills: number;
  doubleKills: number;
  tripleKills: number;
  quadraKills: number;
  pentaKills: number;
  teamLuckScore: number;
  ourScore: number;
  primaryStyle: number;
  subStyle: number;
  spell1Casts: number;
  spell2Casts: number;
  spell3Casts: number;
  spell4Casts: number;
  summoner1Id: number;
  summoner1Casts: number;
  summoner2Id: number;
  summoner2Casts: number;
  rune: MatchRune;
};

export type MatchParticipant = MatchPlayerBase & {
  gameName: string;
  tagLine: string;
  teamPostition?: string;
  teamPosition?: string;
  wardKilled: number;
  wardPlaced: number;
  visionWardsBoughtInGame: number;
  visionScore: number;
  soloRankTier?: string | null;
  soloRankLP?: number | null;
};

export type MatchMyData = MatchPlayerBase & {
  win: boolean;
  teamPosition: string;
};

export type Match = {
  metaData: {
    matchId: string;
    gameCreation: number;
    gameEndTimestamp: number;
    gameDuration: number;
    gameMode: string;
    queueId: number;
    gameVersion?: string | null;
  };
  myData: MatchMyData;
  participants: Record<MatchParticipantKey, MatchParticipant>;
};
