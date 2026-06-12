"use client";

import React, { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { spellApi } from "@/services/spellApi";
import type { Spell } from "@/types/spell";
import { runeApi } from "@/services/runeApi";
import type { Rune } from "@/types/rune";

// 시간 차이를 계산하는 함수
const getTimeAgo = (timestamp: number): string => {
  const now = Date.now();
  const diff = now - timestamp;

  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  const months = Math.floor(days / 30);
  const years = Math.floor(days / 365);

  if (years > 0) {
    return `${years}년 전`;
  } else if (months > 0) {
    return `${months}개월 전`;
  } else if (days > 0) {
    return `${days}일 전`;
  } else if (hours > 0) {
    return `${hours}시간 전`;
  } else if (minutes > 0) {
    return `${minutes}분 전`;
  } else {
    return `${seconds}초 전`;
  }
};

// 게임 시간을 분:초 형식으로 변환하는 함수
const formatGameDuration = (durationInSeconds: number): string => {
  const minutes = Math.floor(durationInSeconds / 60);
  const seconds = durationInSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
};

// 티어명에서 로마숫자를 제거하는 함수
const extractTierName = (fullTier: string): string => {
  if (!fullTier) return "";
  // 로마숫자 패턴 제거 (I, II, III, IV, V)
  return fullTier.replace(/\s+[IVX]+$/, "");
};

interface Summoner {
  id: number;
  gameName: string;
  tagLine: string;
  puuid: string;
  soloRank: {
    rankTier: string;
    rankLP: number;
    rankWin: number;
    rankDefeat: number;
    rankType: string;
  };
  flexRank: {
    rankTier: string;
    rankLP: number;
    rankWin: number;
    rankDefeat: number;
    rankType: string;
  };
}

interface Match {
  metaData: {
    matchId: string;
    gameCreation: number;
    gameEndTimestamp: number;
    gameDuration: number;
    gameMode: string;
    queueId: number;
  };
  myData: {
    puuid: string;
    win: boolean;
    championId: number;
    championNameEn: string;
    championNameKo: string;
    champLevel: number;
    teamPosition: string;
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
    rune: {
      mainRune: {
        typeDesc: string;
        typeId: number;
        mainRune: {
          id: number;
          desc: string;
        };
        rune1: {
          id: number;
          desc: string;
        };
        rune2: {
          id: number;
          desc: string;
        };
        rune3: {
          id: number;
          desc: string;
        };
      };
      subRune: {
        styleId: number;
        mainRune: {
          id: number;
          desc: string;
        };
        rune1: {
          id: number;
          desc: string;
        };
        rune2: {
          id: number;
          desc: string;
        };
      };
      statRune: {
        defense: number;
        flex: number;
        offense: number;
      };
    };
  };
  participants: {
    player0: {
      puuid: string;
      gameName: string;
      tagLine: string;
      championId: number;
      championNameEn: string;
      championNameKo: string;
      champLevel: number;
      teamPostition: string;
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
      wardKilled: number;
      wardPlaced: number;
      visionWardsBoughtInGame: number;
      visionScore: number;
      rune: {
        mainRune: {
          typeDesc: string;
          typeId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
          rune3: {
            id: number;
            desc: string;
          };
        };
        subRune: {
          styleId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
        };
        statRune: {
          defense: number;
          flex: number;
          offense: number;
        };
      };
    };
    player1: {
      puuid: string;
      gameName: string;
      tagLine: string;
      championId: number;
      championNameEn: string;
      championNameKo: string;
      champLevel: number;
      teamPostition: string;
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
      wardKilled: number;
      wardPlaced: number;
      visionWardsBoughtInGame: number;
      visionScore: number;
      rune: {
        mainRune: {
          typeDesc: string;
          typeId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
          rune3: {
            id: number;
            desc: string;
          };
        };
        subRune: {
          styleId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
        };
        statRune: {
          defense: number;
          flex: number;
          offense: number;
        };
      };
    };
    player2: {
      puuid: string;
      gameName: string;
      tagLine: string;
      championId: number;
      championNameEn: string;
      championNameKo: string;
      champLevel: number;
      teamPostition: string;
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
      wardKilled: number;
      wardPlaced: number;
      visionWardsBoughtInGame: number;
      visionScore: number;
      rune: {
        mainRune: {
          typeDesc: string;
          typeId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
          rune3: {
            id: number;
            desc: string;
          };
        };
        subRune: {
          styleId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
        };
        statRune: {
          defense: number;
          flex: number;
          offense: number;
        };
      };
    };
    player3: {
      puuid: string;
      gameName: string;
      tagLine: string;
      championId: number;
      championNameEn: string;
      championNameKo: string;
      champLevel: number;
      teamPostition: string;
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
      wardKilled: number;
      wardPlaced: number;
      visionWardsBoughtInGame: number;
      visionScore: number;
      rune: {
        mainRune: {
          typeDesc: string;
          typeId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
          rune3: {
            id: number;
            desc: string;
          };
        };
        subRune: {
          styleId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
        };
        statRune: {
          defense: number;
          flex: number;
          offense: number;
        };
      };
    };
    player4: {
      puuid: string;
      gameName: string;
      tagLine: string;
      championId: number;
      championNameEn: string;
      championNameKo: string;
      champLevel: number;
      teamPostition: string;
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
      wardKilled: number;
      wardPlaced: number;
      visionWardsBoughtInGame: number;
      visionScore: number;
      rune: {
        mainRune: {
          typeDesc: string;
          typeId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
          rune3: {
            id: number;
            desc: string;
          };
        };
        subRune: {
          styleId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
        };
        statRune: {
          defense: number;
          flex: number;
          offense: number;
        };
      };
    };
    player5: {
      puuid: string;
      gameName: string;
      tagLine: string;
      championId: number;
      championNameEn: string;
      championNameKo: string;
      champLevel: number;
      teamPostition: string;
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
      wardKilled: number;
      wardPlaced: number;
      visionWardsBoughtInGame: number;
      visionScore: number;
      rune: {
        mainRune: {
          typeDesc: string;
          typeId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
          rune3: {
            id: number;
            desc: string;
          };
        };
        subRune: {
          styleId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
        };
        statRune: {
          defense: number;
          flex: number;
          offense: number;
        };
      };
    };
    player6: {
      puuid: string;
      gameName: string;
      tagLine: string;
      championId: number;
      championNameEn: string;
      championNameKo: string;
      champLevel: number;
      teamPostition: string;
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
      wardKilled: number;
      wardPlaced: number;
      visionWardsBoughtInGame: number;
      visionScore: number;
      rune: {
        mainRune: {
          typeDesc: string;
          typeId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
          rune3: {
            id: number;
            desc: string;
          };
        };
        subRune: {
          styleId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
        };
        statRune: {
          defense: number;
          flex: number;
          offense: number;
        };
      };
    };
    player7: {
      puuid: string;
      gameName: string;
      tagLine: string;
      championId: number;
      championNameEn: string;
      championNameKo: string;
      champLevel: number;
      teamPostition: string;
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
      wardKilled: number;
      wardPlaced: number;
      visionWardsBoughtInGame: number;
      visionScore: number;
      rune: {
        mainRune: {
          typeDesc: string;
          typeId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
          rune3: {
            id: number;
            desc: string;
          };
        };
        subRune: {
          styleId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
        };
        statRune: {
          defense: number;
          flex: number;
          offense: number;
        };
      };
    };
    player8: {
      puuid: string;
      gameName: string;
      tagLine: string;
      championId: number;
      championNameEn: string;
      championNameKo: string;
      champLevel: number;
      teamPostition: string;
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
      wardKilled: number;
      wardPlaced: number;
      visionWardsBoughtInGame: number;
      visionScore: number;
      rune: {
        mainRune: {
          typeDesc: string;
          typeId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
          rune3: {
            id: number;
            desc: string;
          };
        };
        subRune: {
          styleId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
        };
        statRune: {
          defense: number;
          flex: number;
          offense: number;
        };
      };
    };
    player9: {
      puuid: string;
      gameName: string;
      tagLine: string;
      championId: number;
      championNameEn: string;
      championNameKo: string;
      champLevel: number;
      teamPostition: string;
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
      wardKilled: number;
      wardPlaced: number;
      visionWardsBoughtInGame: number;
      visionScore: number;
      rune: {
        mainRune: {
          typeDesc: string;
          typeId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
          rune3: {
            id: number;
            desc: string;
          };
        };
        subRune: {
          styleId: number;
          mainRune: {
            id: number;
            desc: string;
          };
          rune1: {
            id: number;
            desc: string;
          };
          rune2: {
            id: number;
            desc: string;
          };
        };
        statRune: {
          defense: number;
          flex: number;
          offense: number;
        };
      };
    };
  };
}

interface Profile {
  profileIconId: number;
  summonerLevel: number;
  profileUrl: string;
}

const queue = [
  { id: 420, name: "솔로랭크" },
  { id: 440, name: "자유랭크" },
  { id: 400, name: "일반" },
  { id: 450, name: "칼바람 나락" },
  { id: 700, name: "격전" },
  { id: 900, name: "URF" },
  { id: 1020, name: "단일 챔피언" },
  { id: 1400, name: "궁극기 주문서" },
  { id: 2000, name: "튜토리얼" },
  { id: 1700, name: "아레나" },
];

export default function SummonerPage({
  params,
}: {
  params: Promise<{ name: string }>;
}) {
  const resolvedParams = React.use(params);
  const decodedName = decodeURIComponent(resolvedParams.name);
  const router = useRouter();
  const [gameName, tagLine] = decodedName.includes("-")
    ? decodedName.split("-")
    : [decodedName, "KR1"]; // 기본 tagLine 설정

  const [data, setData] = useState<Summoner | null>(null);
  const [matchData, setMatchData] = useState<Match[]>([]);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [spell, setSpell] = useState<Record<number, Spell> | null>(null);
  const [rune, setRune] = useState<Record<number, Rune> | null>(null);
  const [expandedMatches, setExpandedMatches] = useState<
    Record<string, boolean>
  >({});

  const toggleMatchExpand = (matchId: string) => {
    setExpandedMatches((prev) => ({
      ...prev,
      [matchId]: !prev[matchId],
    }));
  };

  const kill = (match: Match) => {
    if (match.myData.pentaKills > 0) {
      return "펜타 킬";
    } else if (match.myData.quadraKills > 0) {
      return "쿼드라 킬";
    } else if (match.myData.tripleKills > 0) {
      return "트리플 킬";
    } else if (match.myData.doubleKills > 0) {
      return "더블 킬";
    } else {
      return "";
    }
  };
  useEffect(() => {
    const fetchSummonerData = async () => {
      try {
        const res = await fetch(
          `http://localhost:8080/api/v1/summoner/tier?gameName=${gameName}&tagLine=${tagLine}`,
          {
            method: "GET",
          }
        );

        if (!res.ok) {
          throw new Error(`HTTP error! status: ${res.status}`);
        }

        const result = await res.json();
        setData(result);
      } catch (error) {
        console.error("❌ API 요청 실패:", error);
        setData(null);
      }
    };

    fetchSummonerData();
  }, [gameName, tagLine]);

  console.log(data);

  // useEffect(() => {
  //   const mostChampionData = async () => {
  //     try {
  //       const res = await fetch(
  //         `http://localhost:8080/api/v1/summoner/most?gameName=${gameName}&tagLine=${tagLine}`,
  //         {
  //           method: "GET",
  //         }
  //       );

  //       const result = await res.json();
  //       console.log(result);
  //     } catch (error) {
  //       console.error("❌ API 요청 실패:", error);
  //     }
  //   };

  //   mostChampionData();
  // }, [gameName, tagLine]);

  useEffect(() => {
    const fetchMatchData = async () => {
      const res = await fetch(
        `http://localhost:8080/api/v1/summoner/matches?gameName=${gameName}&tagLine=${tagLine}`,
        {
          method: "GET",
        }
      );

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const result = await res.json();
      console.log(result);
      setMatchData(result);
    };

    fetchMatchData();
  }, [gameName, tagLine]);

  useEffect(() => {
    const fetchProfileData = async () => {
      const res = await fetch(
        `http://localhost:8080/api/v1/summoner/profile?gameName=${gameName}&tagLine=${tagLine}`
      );

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const result = await res.json();
      setProfile(result);
    };

    fetchProfileData();
  }, [gameName, tagLine]);

  // 소환사 주문 데이터 가져오기 (모든 매치의 모든 플레이어에서 스펠 ID 수집)
  const fetchSpellData = useCallback(async () => {
    if (matchData.length === 0) return;

    // 모든 매치의 모든 플레이어에서 스펠 ID 수집
    const allSpellIds: number[] = [];
    matchData.forEach((match) => {
      // myData에서 스펠 ID 수집
      if (match.myData.summoner1Id) allSpellIds.push(match.myData.summoner1Id);
      if (match.myData.summoner2Id) allSpellIds.push(match.myData.summoner2Id);

      // 모든 플레이어(player0~player9)에서 스펠 ID 수집
      for (let i = 0; i < 10; i++) {
        const player =
          match.participants[`player${i}` as keyof typeof match.participants];
        if (player?.summoner1Id) allSpellIds.push(player.summoner1Id);
        if (player?.summoner2Id) allSpellIds.push(player.summoner2Id);
      }
    });

    // 중복 제거 후 fetch
    const uniqueIds = [...new Set(allSpellIds)];
    const spellData = await spellApi.fetchSpellsByIds(uniqueIds);
    setSpell(spellData);
  }, [matchData]);

  // 룬 데이터 가져오기 (모든 매치의 모든 플레이어에서 룬 스타일 ID 수집)
  const fetchRuneData = useCallback(async () => {
    if (matchData.length === 0) return;

    // 모든 매치의 모든 플레이어에서 룬 스타일 ID 수집
    const allRuneIds: number[] = [];
    matchData.forEach((match) => {
      // myData에서 룬 ID 수집
      if (match.myData.primaryStyle) allRuneIds.push(match.myData.primaryStyle);
      if (match.myData.subStyle) allRuneIds.push(match.myData.subStyle);

      // 모든 플레이어(player0~player9)에서 룬 ID 수집
      for (let i = 0; i < 10; i++) {
        const player =
          match.participants[`player${i}` as keyof typeof match.participants];
        // 스타일 ID (카테고리)
        if (player?.rune?.mainRune?.typeId)
          allRuneIds.push(player.rune.mainRune.typeId);
        if (player?.rune?.subRune?.styleId)
          allRuneIds.push(player.rune.subRune.styleId);
        // 개별 룬 ID
        if (player?.rune?.mainRune?.mainRune?.id)
          allRuneIds.push(player.rune.mainRune.mainRune.id);
      }
    });

    // 중복 제거 후 fetch
    const uniqueIds = [...new Set(allRuneIds)];
    const runeData = await runeApi.getRunesByIds(uniqueIds);
    setRune(runeData);
  }, [matchData]);

  // data가 로드된 후 스펠/룬 데이터 가져오기
  useEffect(() => {
    fetchSpellData();
    fetchRuneData();
  }, [matchData]);

  return (
    <div className="flex justify-center bg-[#161619] text-black">
      <div className="flex flex-col max-w-6xl py-8">
        <div className="w-full">
          <div className="flex flex-col p-5 bg-white rounded">
            <div className="flex gap-0.5 mb-3.5">
              <div className="bg-yellow-500 rounded">gold</div>
              <div className="bg-yellow-500 rounded">gold</div>
            </div>
            <div className="flex">
              <div className="relative">
                <img
                  src={profile?.profileUrl}
                  alt="profile"
                  width={110}
                  height={110}
                  className="rounded"
                />
                <div className="absolute top-9/10 left-1/3 text-white rounded bg-red-500 px-1">
                  {profile?.summonerLevel}
                </div>
              </div>
              <div className="flex flex-col ml-5">
                <div className="flex">
                  <div className="text-2xl font-bold">{data?.gameName}</div>
                  <div className="text-gray-500 text-2xl ml-1">
                    #{data?.tagLine}
                  </div>
                </div>
                <button className="bg-blue-500 text-white rounded p-1 cursor-pointer">
                  전적 갱신
                </button>
              </div>
            </div>
          </div>
        </div>
        <div className="flex flex-col md:flex-row mt-2.5">
          <div className="flex flex-col gap-2.5">
            {data?.soloRank.rankTier ? (
              <div className="flex flex-col bg-white rounded">
                <div className="px-2.5 border-b">솔로랭크</div>
                <div className="px-3.5">
                  <div className="flex">
                    <img
                      src={`https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-shared-components/global/default/${extractTierName(
                        data?.soloRank.rankTier || ""
                      ).toLowerCase()}.png`}
                      alt="profile"
                      width={62}
                      height={62}
                    />
                    <div className="flex flex-col">
                      <div className="flex justify-between">
                        <div>{data?.soloRank.rankTier}</div>
                        <div>
                          {data?.soloRank.rankWin}승 {data?.soloRank.rankDefeat}
                          패
                        </div>
                      </div>
                      <div className="flex mt-3 justify-between">
                        <div>{data?.soloRank.rankLP} LP</div>
                        <div>
                          승률{" "}
                          {data?.soloRank.rankWin && data?.soloRank.rankDefeat
                            ? (
                                (data?.soloRank.rankWin /
                                  (data?.soloRank.rankWin +
                                    data?.soloRank.rankDefeat)) *
                                100
                              ).toFixed(2)
                            : "0.00"}
                          %
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex bg-white rounded justify-between px-2.5">
                <div>솔로 랭크</div>
                <div className="text-gray-500">Unranked</div>
              </div>
            )}
            {data?.flexRank.rankTier ? (
              <div className="flex flex-col bg-white rounded">
                <div className="px-2.5 border-b">자유 랭크</div>
                <div className="px-3.5">
                  <div className="flex">
                    <img
                      src={`https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-shared-components/global/default/${extractTierName(
                        data?.flexRank.rankTier || ""
                      ).toLowerCase()}.png`}
                      alt="profile"
                      width={62}
                      height={62}
                    />
                    <div className="flex flex-col">
                      <div className="flex justify-between">
                        <div>{data?.flexRank.rankTier}</div>
                        <div>
                          {data?.flexRank.rankWin}승 {data?.flexRank.rankDefeat}
                          패
                        </div>
                      </div>
                      <div className="flex mt-3 justify-between">
                        <div>{data?.flexRank.rankLP} LP</div>
                        <div>
                          승률{" "}
                          {data?.flexRank.rankWin && data?.flexRank.rankDefeat
                            ? (
                                (data?.flexRank.rankWin /
                                  (data?.flexRank.rankWin +
                                    data?.flexRank.rankDefeat)) *
                                100
                              ).toFixed(2)
                            : "0.00"}
                          %
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex bg-white rounded justify-between px-2.5">
                <div>자유 랭크</div>
                <div className="text-gray-500">Unranked</div>
              </div>
            )}
            <div className="flex flex-col bg-white rounded">
              <ul className="flex gap-1.5 p-2.5 border-b">
                <li className="cursor-pointer flex items-center justify-center rounded bg-gray-300">
                  전체
                </li>
                <li className="cursor-pointer flex items-center justify-center rounded bg-gray-300">
                  솔로랭크
                </li>
                <li className="cursor-pointer flex items-center justify-center rounded bg-gray-300">
                  자유랭크
                </li>
              </ul>
              <ul className="flex flex-col">
                <li className="flex px-2.5 py-1">
                  <img src="https://placehold.co/36x36" alt="profile" />
                  <div className="flex flex-col ml-2.5 py-0.5">
                    <div>판테온</div>
                    <div>2.75 KDA</div>
                  </div>
                  <div className="flex flex-col ml-auto">
                    <div>50%</div>
                    <div>8게임</div>
                  </div>
                </li>
              </ul>
            </div>
          </div>
          <div className="flex flex-col ml-2.5 gap-2.5">
            <div className="flex gap-1.5">
              <div className="rounded bg-white">전체</div>
              <div className="rounded bg-white">칼바람 나락</div>
              <div className="rounded bg-white">자유랭크</div>
            </div>
            <div className="flex gap-2.5 justify-between md:flex-row flex-col">
              <div className="flex flex-col gap-1">
                <div className="flex rounded bg-white">
                  <div>
                    <img src="https://placehold.co/60x60" alt="profile" />
                  </div>
                  <div className="flex flex-col">
                    <div>20게임</div>
                    <div>10승 10패</div>
                  </div>
                  <div className="flex flex-col">
                    <div>1.00 KDA</div>
                    <div>10/10/10</div>
                  </div>
                  <div className="flex flex-col">
                    <div>50</div>
                    <div>AI Score</div>
                  </div>
                </div>
                <div className="flex gap-2.5 justify-between">
                  <div className="flex flex-col bg-white rounded">
                    <div>AI 티어 예측</div>
                    <div>Gold 2</div>
                  </div>
                  <div className="flex flex-col bg-white rounded">
                    <div>팀운</div>
                    <div>보통</div>
                  </div>
                </div>
              </div>
              <div className="flex flex-col bg-white rounded">
                <div>최근 많이 플레이한 챔피언</div>
                <div className="flex flex-col">
                  <div className="flex">
                    <img
                      src="https://placehold.co/28x28"
                      width={28}
                      height={28}
                      alt="profile"
                    />
                    <div className="flex flex-col">
                      <div>50%</div>
                      <div>3.00 KDA</div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="md:flex hidden flex-col bg-white rounded">
                <div>포지션 분포</div>
              </div>
            </div>
            <div className="flex flex-col gap-2.5">
              {matchData?.map((match) => {
                const isExpanded = expandedMatches[match.metaData.matchId];
                const team1Players = [0, 1, 2, 3, 4];
                const team2Players = [5, 6, 7, 8, 9];

                // 팀 1이 진 팀인지 확인 (myData가 team1에 있고 졌거나, team2에 있고 이겼으면 team1이 패배)
                const myPlayerIndex = team1Players.find(
                  (i) =>
                    match.participants[
                      `player${i}` as keyof typeof match.participants
                    ].puuid === match.myData.puuid
                );
                const isMyTeamOne = myPlayerIndex !== undefined;
                const team1Lost = isMyTeamOne
                  ? !match.myData.win
                  : match.myData.win;

                return (
                  <div key={match.metaData.matchId} className="flex flex-col">
                    {/* 요약 카드 */}
                    <div
                      className={`flex ${
                        match.myData.win ? "bg-blue-300" : "bg-red-300"
                      } rounded-t cursor-pointer items-center justify-between p-1 ${
                        !isExpanded ? "rounded-b" : ""
                      }`}
                    >
                      <div className="flex flex-col justify-center items-start gap-1">
                        <div
                          className={`${
                            match.myData.win ? "text-blue-500" : "text-red-500"
                          }`}
                        >
                          {queue.map((q) =>
                            q.id === match.metaData.queueId ? q.name : ""
                          )}
                        </div>
                        <div className="text-sm">
                          {getTimeAgo(match.metaData.gameEndTimestamp)}
                        </div>
                        <div className="flex text-sm gap-1">
                          <div
                            className={`${
                              match.myData.win
                                ? "text-blue-500"
                                : "text-red-500"
                            }`}
                          >
                            {match.myData.win ? "승리" : "패배"}
                          </div>
                          <div>
                            {formatGameDuration(match.metaData.gameDuration)}
                          </div>
                        </div>
                      </div>
                      <div className="flex">
                        <div className="flex flex-col">
                          <div className="flex gap-1">
                            <div className="flex items-center gap-0.5">
                              <div>
                                <img
                                  src={`https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${match.myData.championNameEn}.png`}
                                  alt="profile"
                                  width={52}
                                  height={52}
                                  className="rounded"
                                />
                              </div>
                              <div className="flex flex-col gap-0.5">
                                <img
                                  src={
                                    spell?.[match.myData.summoner1Id]?.imageFull
                                  }
                                  alt="summoner spell 1"
                                  width={24}
                                  height={24}
                                  className="rounded"
                                />
                                <img
                                  src={
                                    spell?.[match.myData.summoner2Id]?.imageFull
                                  }
                                  alt="summoner spell 2"
                                  width={24}
                                  height={24}
                                  className="rounded"
                                />
                              </div>
                              <div className="flex flex-col gap-0.5">
                                <img
                                  src={rune?.[match.myData.primaryStyle]?.icon}
                                  alt="primary rune"
                                  width={24}
                                  height={24}
                                  className="rounded"
                                />
                                <img
                                  src={rune?.[match.myData.subStyle]?.icon}
                                  alt="sub rune"
                                  width={24}
                                  height={24}
                                  className="rounded"
                                />
                              </div>
                            </div>

                            <div className="flex flex-col">
                              <div className="flex gap-0.5">
                                <span>{match.myData.kills}</span>
                                <span>/</span>
                                <span className="text-red-500">
                                  {match.myData.deaths}
                                </span>
                                <span>/</span>
                                <span>{match.myData.assists}</span>
                              </div>
                              <p className="text-xs">
                                {match.myData.kda
                                  ? match.myData.kda.toFixed(2)
                                  : ""}
                                KDA
                              </p>
                              <div>{kill(match)}</div>
                            </div>
                          </div>
                        </div>
                        <div className="flex flex-col justify-center items-center">
                          <h5>AI-Score</h5>
                          <div>{match.myData.ourScore}</div>
                          <div>몇 등</div>
                        </div>
                      </div>
                      {/* 팀 1 (0-4번 플레이어) */}
                      <div className="text-xs flex flex-col gap-0.5 w-32">
                        {team1Players.map((playerIndex) => {
                          const player =
                            match.participants[
                              `player${playerIndex}` as keyof typeof match.participants
                            ];
                          return (
                            <div
                              key={playerIndex}
                              className="flex items-center gap-0.5"
                            >
                              <div className="shrink-0">
                                <img
                                  src={`https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${player.championNameEn}.png`}
                                  alt="profile"
                                  width={22}
                                  height={22}
                                  className="rounded"
                                />
                              </div>
                              <div
                                className="cursor-pointer hover:text-blue-500 overflow-hidden text-ellipsis whitespace-nowrap"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  router.push(
                                    `/summoner/${player.gameName}-${player.tagLine}`
                                  );
                                }}
                              >
                                {player.gameName}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                      {/* 팀 2 (5-9번 플레이어) */}
                      <div className="text-xs flex flex-col gap-0.5 w-32">
                        {team2Players.map((playerIndex) => {
                          const player =
                            match.participants[
                              `player${playerIndex}` as keyof typeof match.participants
                            ];
                          return (
                            <div
                              key={playerIndex}
                              className="flex items-center gap-0.5"
                            >
                              <div className="shrink-0">
                                <img
                                  src={`https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${player.championNameEn}.png`}
                                  alt="profile"
                                  width={22}
                                  height={22}
                                  className="rounded"
                                />
                              </div>
                              <div
                                className="cursor-pointer hover:text-blue-500 overflow-hidden text-ellipsis whitespace-nowrap"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  router.push(
                                    `/summoner/${player.gameName}-${player.tagLine}`
                                  );
                                }}
                              >
                                {player.gameName}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                      {/* 확장 버튼 */}
                      <button
                        onClick={() =>
                          toggleMatchExpand(match.metaData.matchId)
                        }
                        className={`flex items-center justify-center w-8 h-full ${
                          match.myData.win
                            ? "bg-blue-400 hover:bg-blue-500"
                            : "bg-red-400 hover:bg-red-500"
                        } rounded-r transition-colors`}
                      >
                        <svg
                          className={`w-4 h-4 text-white transition-transform ${
                            isExpanded ? "rotate-180" : ""
                          }`}
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M19 9l-7 7-7-7"
                          />
                        </svg>
                      </button>
                    </div>

                    {/* 확장된 상세 정보 */}
                    {isExpanded && (
                      <div className="bg-[#1a1a2e] text-white rounded-b overflow-hidden">
                        {/* 탭 네비게이션 */}
                        <div className="flex border-b border-gray-700">
                          <button className="px-4 py-2 bg-red-600 text-white text-sm font-medium">
                            AI 기본 분석
                          </button>
                          <button className="px-4 py-2 text-gray-400 text-sm hover:text-white">
                            시간대별 분석
                          </button>
                          <button className="px-4 py-2 text-gray-400 text-sm hover:text-white">
                            빌드
                          </button>
                        </div>

                        {/* 팀 1 테이블 (패배팀) */}
                        <div
                          className={`${
                            team1Lost ? "bg-red-900/20" : "bg-blue-900/20"
                          }`}
                        >
                          <div className="flex items-center gap-2 px-4 py-2 border-b border-gray-700">
                            <span
                              className={`font-medium ${
                                team1Lost ? "text-red-400" : "text-blue-400"
                              }`}
                            >
                              {team1Lost ? "패배" : "승리"}
                            </span>
                            <span className="text-gray-400 text-sm">
                              (블루팀)
                            </span>
                          </div>
                          <table className="w-full text-sm">
                            <thead>
                              <tr className="text-gray-400 text-xs border-b border-gray-700">
                                <th className="text-left py-2 px-4 w-48"></th>
                                <th className="py-2 px-2">AI-Score</th>
                                <th className="py-2 px-2">KDA</th>
                                <th className="py-2 px-2">피해량</th>
                                <th className="py-2 px-2">CS</th>
                                <th className="py-2 px-2">와드</th>
                                <th className="py-2 px-2">아이템</th>
                              </tr>
                            </thead>
                            <tbody>
                              {team1Players.map((i) => {
                                const p =
                                  match.participants[
                                    `player${i}` as keyof typeof match.participants
                                  ];
                                const gameDurationMinutes =
                                  match.metaData.gameDuration / 60;
                                const csPerMin = (
                                  p.totalMinionKills / gameDurationMinutes
                                ).toFixed(1);
                                return (
                                  <tr
                                    key={i}
                                    className="border-b border-gray-700/50 hover:bg-gray-800/50"
                                  >
                                    <td className="py-2 px-4">
                                      <div className="flex items-center gap-2">
                                        <div className="relative">
                                          <img
                                            src={`https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${p.championNameEn}.png`}
                                            alt={p.championNameEn}
                                            width={32}
                                            height={32}
                                            className="rounded"
                                          />
                                          <span className="absolute -bottom-1 -left-1 bg-gray-800 text-[10px] px-1 rounded">
                                            {p.champLevel}
                                          </span>
                                        </div>
                                        <div className="flex flex-col gap-0.5">
                                          <img
                                            src={
                                              spell?.[p.summoner1Id]?.imageFull
                                            }
                                            alt="spell"
                                            width={16}
                                            height={16}
                                            className="rounded"
                                          />
                                          <img
                                            src={
                                              spell?.[p.summoner2Id]?.imageFull
                                            }
                                            alt="spell"
                                            width={16}
                                            height={16}
                                            className="rounded"
                                          />
                                        </div>
                                        <div className="flex flex-col gap-0.5">
                                          <img
                                            src={
                                              rune?.[
                                                p.rune.mainRune.mainRune.id
                                              ]?.icon
                                            }
                                            alt="rune"
                                            width={16}
                                            height={16}
                                            className="rounded"
                                          />
                                          <img
                                            src={
                                              rune?.[p.rune.subRune.styleId]
                                                ?.icon
                                            }
                                            alt="rune"
                                            width={16}
                                            height={16}
                                            className="rounded"
                                          />
                                        </div>
                                        <div className="flex flex-col">
                                          <span
                                            className="hover:text-blue-400 cursor-pointer text-sm"
                                            onClick={() =>
                                              router.push(
                                                `/summoner/${p.gameName}-${p.tagLine}`
                                              )
                                            }
                                          >
                                            {p.gameName}
                                          </span>
                                        </div>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span className="text-lg font-bold text-yellow-400">
                                          {p.ourScore || p.teamLuckScore}
                                        </span>
                                        <span className="text-[10px] text-gray-400">
                                          {i + 1}등
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span>
                                          {p.kills} /{" "}
                                          <span className="text-red-400">
                                            {p.deaths}
                                          </span>{" "}
                                          / {p.assists}
                                        </span>
                                        <span className="text-xs text-gray-400">
                                          {p.kda?.toFixed(2) || "Perfect"}
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span>
                                          {p.totalDamageDealtToChampions?.toLocaleString()}
                                        </span>
                                        <div className="w-16 h-1 bg-gray-700 rounded overflow-hidden">
                                          <div
                                            className="h-full bg-red-500"
                                            style={{
                                              width: `${Math.min(
                                                100,
                                                (p.totalDamageDealtToChampions /
                                                  50000) *
                                                  100
                                              )}%`,
                                            }}
                                          ></div>
                                        </div>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span>{p.totalMinionKills}</span>
                                        <span className="text-[10px] text-gray-400">
                                          ({csPerMin}/분)
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span className="text-gray-400">
                                          {p.visionWardsBoughtInGame}
                                        </span>
                                        <span className="text-gray-400">
                                          {p.wardPlaced} / {p.wardKilled}
                                        </span>
                                      </div>
                                    </td>
                                    <td className="py-2 px-2">
                                      <div className="flex gap-0.5 justify-center">
                                        {[
                                          p.item0,
                                          p.item1,
                                          p.item2,
                                          p.item3,
                                          p.item4,
                                          p.item5,
                                          p.item6,
                                        ].map((itemId, idx) =>
                                          itemId > 0 ? (
                                            <img
                                              key={idx}
                                              src={`https://ddragon.leagueoflegends.com/cdn/16.11.1/img/item/${itemId}.png`}
                                              alt="item"
                                              width={24}
                                              height={24}
                                              className="rounded"
                                            />
                                          ) : (
                                            <div
                                              key={idx}
                                              className="w-6 h-6 bg-gray-700 rounded"
                                            ></div>
                                          )
                                        )}
                                      </div>
                                    </td>
                                  </tr>
                                );
                              })}
                            </tbody>
                          </table>
                        </div>

                        {/* 팀 2 테이블 (승리팀) */}
                        <div
                          className={`${
                            !team1Lost ? "bg-red-900/20" : "bg-blue-900/20"
                          }`}
                        >
                          <div className="flex items-center gap-2 px-4 py-2 border-b border-gray-700">
                            <span
                              className={`font-medium ${
                                !team1Lost ? "text-red-400" : "text-blue-400"
                              }`}
                            >
                              {!team1Lost ? "패배" : "승리"}
                            </span>
                            <span className="text-gray-400 text-sm">
                              (레드팀)
                            </span>
                          </div>
                          <table className="w-full text-sm">
                            <thead>
                              <tr className="text-gray-400 text-xs border-b border-gray-700">
                                <th className="text-left py-2 px-4 w-48"></th>
                                <th className="py-2 px-2">AI-Score</th>
                                <th className="py-2 px-2">KDA</th>
                                <th className="py-2 px-2">피해량</th>
                                <th className="py-2 px-2">CS</th>
                                <th className="py-2 px-2">와드</th>
                                <th className="py-2 px-2">아이템</th>
                              </tr>
                            </thead>
                            <tbody>
                              {team2Players.map((i) => {
                                const p =
                                  match.participants[
                                    `player${i}` as keyof typeof match.participants
                                  ];
                                const gameDurationMinutes =
                                  match.metaData.gameDuration / 60;
                                const csPerMin = (
                                  p.totalMinionKills / gameDurationMinutes
                                ).toFixed(1);
                                return (
                                  <tr
                                    key={i}
                                    className="border-b border-gray-700/50 hover:bg-gray-800/50"
                                  >
                                    <td className="py-2 px-4">
                                      <div className="flex items-center gap-2">
                                        <div className="relative">
                                          <img
                                            src={`https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${p.championNameEn}.png`}
                                            alt={p.championNameEn}
                                            width={32}
                                            height={32}
                                            className="rounded"
                                          />
                                          <span className="absolute -bottom-1 -left-1 bg-gray-800 text-[10px] px-1 rounded">
                                            {p.champLevel}
                                          </span>
                                        </div>
                                        <div className="flex flex-col gap-0.5">
                                          <img
                                            src={
                                              spell?.[p.summoner1Id]?.imageFull
                                            }
                                            alt="spell"
                                            width={16}
                                            height={16}
                                            className="rounded"
                                          />
                                          <img
                                            src={
                                              spell?.[p.summoner2Id]?.imageFull
                                            }
                                            alt="spell"
                                            width={16}
                                            height={16}
                                            className="rounded"
                                          />
                                        </div>
                                        <div className="flex flex-col gap-0.5">
                                          <img
                                            src={
                                              rune?.[
                                                p.rune.mainRune.mainRune.id
                                              ]?.icon
                                            }
                                            alt="rune"
                                            width={16}
                                            height={16}
                                            className="rounded"
                                          />
                                          <img
                                            src={
                                              rune?.[p.rune.subRune.styleId]
                                                ?.icon
                                            }
                                            alt="rune"
                                            width={16}
                                            height={16}
                                            className="rounded"
                                          />
                                        </div>
                                        <div className="flex flex-col">
                                          <span
                                            className="hover:text-blue-400 cursor-pointer text-sm"
                                            onClick={() =>
                                              router.push(
                                                `/summoner/${p.gameName}-${p.tagLine}`
                                              )
                                            }
                                          >
                                            {p.gameName}
                                          </span>
                                        </div>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span className="text-lg font-bold text-yellow-400">
                                          {p.ourScore || p.teamLuckScore}
                                        </span>
                                        <span className="text-[10px] text-gray-400">
                                          {i - 4}등
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span>
                                          {p.kills} /{" "}
                                          <span className="text-red-400">
                                            {p.deaths}
                                          </span>{" "}
                                          / {p.assists}
                                        </span>
                                        <span className="text-xs text-gray-400">
                                          {p.kda?.toFixed(2) || "Perfect"}
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span>
                                          {p.totalDamageDealtToChampions?.toLocaleString()}
                                        </span>
                                        <div className="w-16 h-1 bg-gray-700 rounded overflow-hidden">
                                          <div
                                            className="h-full bg-red-500"
                                            style={{
                                              width: `${Math.min(
                                                100,
                                                (p.totalDamageDealtToChampions /
                                                  50000) *
                                                  100
                                              )}%`,
                                            }}
                                          ></div>
                                        </div>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span>{p.totalMinionKills}</span>
                                        <span className="text-[10px] text-gray-400">
                                          ({csPerMin}/분)
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span className="text-gray-400">
                                          {p.visionWardsBoughtInGame}
                                        </span>
                                        <span className="text-gray-400">
                                          {p.wardPlaced} / {p.wardKilled}
                                        </span>
                                      </div>
                                    </td>
                                    <td className="py-2 px-2">
                                      <div className="flex gap-0.5 justify-center">
                                        {[
                                          p.item0,
                                          p.item1,
                                          p.item2,
                                          p.item3,
                                          p.item4,
                                          p.item5,
                                          p.item6,
                                        ].map((itemId, idx) =>
                                          itemId > 0 ? (
                                            <img
                                              key={idx}
                                              src={`https://ddragon.leagueoflegends.com/cdn/16.11.1/img/item/${itemId}.png`}
                                              alt="item"
                                              width={24}
                                              height={24}
                                              className="rounded"
                                            />
                                          ) : (
                                            <div
                                              key={idx}
                                              className="w-6 h-6 bg-gray-700 rounded"
                                            ></div>
                                          )
                                        )}
                                      </div>
                                    </td>
                                  </tr>
                                );
                              })}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
