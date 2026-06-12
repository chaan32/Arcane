// 소환사 정보 공통 단위
export interface SummonerIdentifier {
  id: number;
  puuid: string;
  gameName: string;
  tagLine: string;
}

// 드롭다운 내용 타입
export interface SummonerDropdownType extends SummonerIdentifier {
  profileUrl: string;
  level: number;
}

// 추후 정리
//
// 키워드 검색 결과
export interface SummonerBase extends SummonerIdentifier {
  icon: number;
  level: number;
  soloRank: string;
  soloRankLp: number;
}

// 랭크 정보 상세
interface RankDetail {
  rankTier: string;
  rankLP: number;
  rankWin: number;
  rankDefeat: number;
}

// 티어 정보 상세
export interface SummonerTier extends SummonerIdentifier {
  soloRank: RankDetail;
  flexRank: RankDetail;
}

// 프로필 이미지와 레벨
export interface SummonerProfile {
  profileIconId: number;
  summonerLevel: number;
  profileUrl: string;
}
