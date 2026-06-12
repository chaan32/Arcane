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
    <div
      data-summoner-page
      className="flex min-h-screen justify-center bg-[#f5f7fb] text-[#191f28]"
    >
      <div className="flex w-full max-w-6xl flex-col px-4 py-8 lg:py-10">
        <div className="w-full">
          <div className="toss-profile-card flex flex-col rounded-[1.75rem] border border-[#e5e8eb] bg-white p-6 shadow-[0_12px_32px_rgba(25,31,40,0.07)]">
            <div className="mb-3.5 flex gap-2">
              <div className="rounded-full bg-[#fff6e5] px-3 py-1 text-xs font-bold text-[#f59f00]">
                gold
              </div>
              <div className="rounded-full bg-[#fff6e5] px-3 py-1 text-xs font-bold text-[#f59f00]">
                gold
              </div>
            </div>
            <div className="flex items-center">
              <div className="relative">
                <img
                  src={profile?.profileUrl}
                  alt="profile"
                  width={110}
                  height={110}
                  className="rounded-[1.5rem] border border-[#e5e8eb] shadow-[0_8px_20px_rgba(25,31,40,0.08)]"
                />
                <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 rounded-full bg-[#191f28] px-2 py-0.5 text-xs font-semibold text-white">
                  {profile?.summonerLevel}
                </div>
              </div>
              <div className="ml-6 flex flex-col">
                <div className="flex items-end">
                  <div className="text-3xl font-bold tracking-normal">
                    {data?.gameName}
                  </div>
                  <div className="ml-1 text-2xl text-[#8b95a1]">
                    #{data?.tagLine}
                  </div>
                </div>
                <button className="mt-4 w-fit cursor-pointer rounded-full bg-[#3182f6] px-5 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#1b64da]">
                  전적 갱신
                </button>
              </div>
            </div>
          </div>
        </div>
        <div className="mt-5 flex flex-col gap-4 md:flex-row">
          <div className="toss-sidebar flex shrink-0 flex-col gap-3 md:w-[18rem]">
            {data?.soloRank.rankTier ? (
              <div className="flex flex-col rounded-[1.5rem] border border-[#e5e8eb] bg-white shadow-[0_8px_24px_rgba(25,31,40,0.06)]">
                <div className="border-b border-[#e5e8eb] px-5 py-3 text-sm font-semibold text-[#4e5968]">
                  솔로랭크
                </div>
                <div className="px-5 py-4">
                  <div className="flex items-center gap-3">
                    <img
                      src={`https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-shared-components/global/default/${extractTierName(
                        data?.soloRank.rankTier || ""
                      ).toLowerCase()}.png`}
                      alt="profile"
                      width={62}
                      height={62}
                    />
                    <div className="flex min-w-0 flex-1 flex-col">
                      <div className="flex justify-between gap-2 text-sm">
                        <div className="font-bold">{data?.soloRank.rankTier}</div>
                        <div className="text-[#8b95a1]">
                          {data?.soloRank.rankWin}승 {data?.soloRank.rankDefeat}
                          패
                        </div>
                      </div>
                      <div className="mt-2 flex justify-between text-sm">
                        <div className="font-semibold text-[#3182f6]">
                          {data?.soloRank.rankLP} LP
                        </div>
                        <div className="text-[#6b7684]">
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
              <div className="flex justify-between rounded-[1.5rem] border border-[#e5e8eb] bg-white px-5 py-4 shadow-[0_8px_24px_rgba(25,31,40,0.06)]">
                <div className="font-semibold">솔로 랭크</div>
                <div className="text-[#8b95a1]">Unranked</div>
              </div>
            )}
            {data?.flexRank.rankTier ? (
              <div className="flex flex-col rounded-[1.5rem] border border-[#e5e8eb] bg-white shadow-[0_8px_24px_rgba(25,31,40,0.06)]">
                <div className="border-b border-[#e5e8eb] px-5 py-3 text-sm font-semibold text-[#4e5968]">
                  자유 랭크
                </div>
                <div className="px-5 py-4">
                  <div className="flex items-center gap-3">
                    <img
                      src={`https://raw.communitydragon.org/latest/plugins/rcp-fe-lol-shared-components/global/default/${extractTierName(
                        data?.flexRank.rankTier || ""
                      ).toLowerCase()}.png`}
                      alt="profile"
                      width={62}
                      height={62}
                    />
                    <div className="flex min-w-0 flex-1 flex-col">
                      <div className="flex justify-between gap-2 text-sm">
                        <div className="font-bold">{data?.flexRank.rankTier}</div>
                        <div className="text-[#8b95a1]">
                          {data?.flexRank.rankWin}승 {data?.flexRank.rankDefeat}
                          패
                        </div>
                      </div>
                      <div className="mt-2 flex justify-between text-sm">
                        <div className="font-semibold text-[#3182f6]">
                          {data?.flexRank.rankLP} LP
                        </div>
                        <div className="text-[#6b7684]">
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
              <div className="flex justify-between rounded-[1.5rem] border border-[#e5e8eb] bg-white px-5 py-4 shadow-[0_8px_24px_rgba(25,31,40,0.06)]">
                <div className="font-semibold">자유 랭크</div>
                <div className="text-[#8b95a1]">Unranked</div>
              </div>
            )}
            <div className="flex flex-col rounded-[1.5rem] border border-[#e5e8eb] bg-white shadow-[0_8px_24px_rgba(25,31,40,0.06)]">
              <ul className="flex gap-1.5 border-b border-[#e5e8eb] p-3">
                <li className="flex cursor-pointer items-center justify-center rounded-full bg-[#3182f6] px-3 py-1.5 text-sm font-semibold text-white">
                  전체
                </li>
                <li className="flex cursor-pointer items-center justify-center rounded-full bg-[#f2f4f6] px-3 py-1.5 text-sm font-semibold text-[#4e5968]">
                  솔로랭크
                </li>
                <li className="flex cursor-pointer items-center justify-center rounded-full bg-[#f2f4f6] px-3 py-1.5 text-sm font-semibold text-[#4e5968]">
                  자유랭크
                </li>
              </ul>
              <ul className="flex flex-col">
                <li className="flex px-5 py-3">
                  <img
                    src="https://placehold.co/36x36"
                    alt="profile"
                    className="rounded-xl"
                  />
                  <div className="flex flex-col ml-2.5 py-0.5">
                    <div className="font-semibold">판테온</div>
                    <div className="text-sm text-[#8b95a1]">2.75 KDA</div>
                  </div>
                  <div className="flex flex-col ml-auto text-right text-sm">
                    <div>50%</div>
                    <div className="text-[#8b95a1]">8게임</div>
                  </div>
                </li>
              </ul>
            </div>
          </div>
          <div className="flex min-w-0 flex-1 flex-col gap-4 md:ml-0">
            <div className="flex gap-1.5">
              <div className="rounded-full bg-[#191f28] px-4 py-2 text-sm font-semibold text-white shadow-[0_6px_16px_rgba(25,31,40,0.14)]">
                전체
              </div>
              <div className="rounded-full border border-[#e5e8eb] bg-white px-4 py-2 text-sm font-semibold text-[#4e5968]">
                칼바람 나락
              </div>
              <div className="rounded-full border border-[#e5e8eb] bg-white px-4 py-2 text-sm font-semibold text-[#4e5968]">
                자유랭크
              </div>
            </div>
            <div className="flex flex-col justify-between gap-2.5 md:flex-row">
              <div className="flex flex-col gap-1">
                <div className="flex items-center gap-4 rounded-[1.5rem] border border-[#e5e8eb] bg-white p-4 shadow-[0_8px_24px_rgba(25,31,40,0.06)]">
                  <div>
                    <img
                      src="https://placehold.co/60x60"
                      alt="profile"
                      className="rounded-2xl"
                    />
                  </div>
                  <div className="flex flex-col">
                    <div className="font-bold">20게임</div>
                    <div className="text-sm text-[#8b95a1]">10승 10패</div>
                  </div>
                  <div className="flex flex-col">
                    <div className="font-bold">1.00 KDA</div>
                    <div className="text-sm text-[#8b95a1]">10/10/10</div>
                  </div>
                  <div className="flex flex-col">
                    <div className="font-bold text-[#3182f6]">50</div>
                    <div className="text-sm text-[#8b95a1]">AI Score</div>
                  </div>
                </div>
                <div className="flex justify-between gap-2.5">
                  <div className="flex flex-1 flex-col rounded-[1.5rem] border border-[#e5e8eb] bg-white p-4 shadow-[0_8px_24px_rgba(25,31,40,0.06)]">
                    <div className="text-sm text-[#8b95a1]">AI 티어 예측</div>
                    <div className="mt-1 font-bold">Gold 2</div>
                  </div>
                  <div className="flex flex-1 flex-col rounded-[1.5rem] border border-[#e5e8eb] bg-white p-4 shadow-[0_8px_24px_rgba(25,31,40,0.06)]">
                    <div className="text-sm text-[#8b95a1]">팀운</div>
                    <div className="mt-1 font-bold">보통</div>
                  </div>
                </div>
              </div>
              <div className="flex flex-col rounded-[1.5rem] border border-[#e5e8eb] bg-white p-4 shadow-[0_8px_24px_rgba(25,31,40,0.06)]">
                <div className="font-semibold">최근 많이 플레이한 챔피언</div>
                <div className="flex flex-col">
                  <div className="mt-3 flex items-center gap-3">
                    <img
                      src="https://placehold.co/28x28"
                      width={28}
                      height={28}
                      alt="profile"
                      className="rounded-lg"
                    />
                    <div className="flex flex-col">
                      <div className="font-semibold">50%</div>
                      <div className="text-sm text-[#8b95a1]">3.00 KDA</div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="hidden flex-col rounded-[1.5rem] border border-[#e5e8eb] bg-white p-4 shadow-[0_8px_24px_rgba(25,31,40,0.06)] md:flex">
                <div className="font-semibold">포지션 분포</div>
              </div>
            </div>
            <div className="flex flex-col gap-3">
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
                  <div
                    key={match.metaData.matchId}
                    className="flex flex-col overflow-hidden rounded-[1.5rem] border border-[#e5e8eb] bg-white shadow-[0_8px_24px_rgba(25,31,40,0.06)]"
                  >
                    {/* 요약 카드 */}
                    <div
                      className={`flex cursor-pointer items-center justify-between gap-4 border-l-4 bg-white p-4 transition-colors hover:bg-[#f8fafc] ${
                        match.myData.win
                          ? "border-l-[#3182f6]"
                          : "border-l-[#f04452]"
                      }`}
                    >
                      <div className="flex w-24 shrink-0 flex-col items-start justify-center gap-1">
                        <div
                          className={`text-sm font-bold ${
                            match.myData.win
                              ? "text-[#3182f6]"
                              : "text-[#f04452]"
                          }`}
                        >
                          {queue.map((q) =>
                            q.id === match.metaData.queueId ? q.name : ""
                          )}
                        </div>
                        <div className="text-xs font-medium text-[#8b95a1]">
                          {getTimeAgo(match.metaData.gameEndTimestamp)}
                        </div>
                        <div className="flex gap-1 text-xs">
                          <div
                            className={`font-semibold ${
                              match.myData.win
                                ? "text-[#3182f6]"
                                : "text-[#f04452]"
                            }`}
                          >
                            {match.myData.win ? "승리" : "패배"}
                          </div>
                          <div className="text-[#6b7684]">
                            {formatGameDuration(match.metaData.gameDuration)}
                          </div>
                        </div>
                      </div>
                      <div className="flex min-w-0 flex-1 items-center justify-between gap-5">
                        <div className="flex flex-col">
                          <div className="flex gap-3">
                            <div className="flex items-center gap-1">
                              <div>
                                <img
                                  src={`https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${match.myData.championNameEn}.png`}
                                  alt="profile"
                                  width={52}
                                  height={52}
                                  className="rounded-2xl"
                                />
                              </div>
                              <div className="flex flex-col gap-1">
                                <img
                                  src={
                                    spell?.[match.myData.summoner1Id]?.imageFull
                                  }
                                  alt="summoner spell 1"
                                  width={24}
                                  height={24}
                                  className="rounded-lg"
                                />
                                <img
                                  src={
                                    spell?.[match.myData.summoner2Id]?.imageFull
                                  }
                                  alt="summoner spell 2"
                                  width={24}
                                  height={24}
                                  className="rounded-lg"
                                />
                              </div>
                              <div className="flex flex-col gap-1">
                                <img
                                  src={rune?.[match.myData.primaryStyle]?.icon}
                                  alt="primary rune"
                                  width={24}
                                  height={24}
                                  className="rounded-lg"
                                />
                                <img
                                  src={rune?.[match.myData.subStyle]?.icon}
                                  alt="sub rune"
                                  width={24}
                                  height={24}
                                  className="rounded-lg"
                                />
                              </div>
                            </div>

                            <div className="flex flex-col justify-center">
                              <div className="flex gap-1 text-lg font-bold">
                                <span>{match.myData.kills}</span>
                                <span>/</span>
                                <span className="text-[#f04452]">
                                  {match.myData.deaths}
                                </span>
                                <span>/</span>
                                <span>{match.myData.assists}</span>
                              </div>
                              <p className="text-xs font-semibold text-[#6b7684]">
                                {match.myData.kda
                                  ? match.myData.kda.toFixed(2)
                                  : ""}
                                KDA
                              </p>
                              <div className="mt-1 text-xs text-[#8b95a1]">
                                {kill(match)}
                              </div>
                            </div>
                          </div>
                        </div>
                        <div className="flex min-w-[5rem] flex-col items-center justify-center rounded-2xl bg-[#f2f4f6] px-4 py-3">
                          <h5 className="text-xs font-semibold text-[#8b95a1]">
                            AI-Score
                          </h5>
                          <div className="text-2xl font-bold text-[#3182f6]">
                            {match.myData.ourScore}
                          </div>
                          <div className="text-xs text-[#8b95a1]">몇 등</div>
                        </div>
                      </div>
                      {/* 팀 1 (0-4번 플레이어) */}
                      <div className="flex w-32 flex-col gap-1 text-xs text-[#4e5968]">
                        {team1Players.map((playerIndex) => {
                          const player =
                            match.participants[
                              `player${playerIndex}` as keyof typeof match.participants
                            ];
                          return (
                            <div
                              key={playerIndex}
                              className="flex items-center gap-1"
                            >
                              <div className="shrink-0">
                                <img
                                  src={`https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${player.championNameEn}.png`}
                                  alt="profile"
                                  width={22}
                                  height={22}
                                  className="rounded-lg"
                                />
                              </div>
                              <div
                                className="cursor-pointer overflow-hidden text-ellipsis whitespace-nowrap hover:text-[#3182f6]"
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
                      <div className="flex w-32 flex-col gap-1 text-xs text-[#4e5968]">
                        {team2Players.map((playerIndex) => {
                          const player =
                            match.participants[
                              `player${playerIndex}` as keyof typeof match.participants
                            ];
                          return (
                            <div
                              key={playerIndex}
                              className="flex items-center gap-1"
                            >
                              <div className="shrink-0">
                                <img
                                  src={`https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${player.championNameEn}.png`}
                                  alt="profile"
                                  width={22}
                                  height={22}
                                  className="rounded-lg"
                                />
                              </div>
                              <div
                                className="cursor-pointer overflow-hidden text-ellipsis whitespace-nowrap hover:text-[#3182f6]"
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
                        className={`flex h-12 w-12 items-center justify-center rounded-2xl transition-colors ${
                          match.myData.win
                            ? "bg-[#e8f3ff] text-[#3182f6] hover:bg-[#dbeeff]"
                            : "bg-[#fff0f1] text-[#f04452] hover:bg-[#ffe3e5]"
                        }`}
                      >
                        <svg
                          className={`h-4 w-4 transition-transform ${
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
                      <div className="overflow-hidden border-t border-[#e5e8eb] bg-white text-[#191f28]">
                        {/* 탭 네비게이션 */}
                        <div className="flex gap-2 border-b border-[#e5e8eb] bg-[#f8fafc] px-4 py-3">
                          <button className="rounded-full bg-[#3182f6] px-4 py-2 text-sm font-semibold text-white">
                            AI 기본 분석
                          </button>
                          <button className="rounded-full px-4 py-2 text-sm font-semibold text-[#6b7684] hover:bg-white hover:text-[#191f28]">
                            시간대별 분석
                          </button>
                          <button className="rounded-full px-4 py-2 text-sm font-semibold text-[#6b7684] hover:bg-white hover:text-[#191f28]">
                            빌드
                          </button>
                        </div>

                        {/* 팀 1 테이블 (패배팀) */}
                        <div
                          className={`${
                            team1Lost ? "bg-[#fff5f5]" : "bg-[#f3f8ff]"
                          }`}
                        >
                          <div className="flex items-center gap-2 border-b border-[#e5e8eb] px-4 py-3">
                            <span
                              className={`font-medium ${
                                team1Lost ? "text-[#f04452]" : "text-[#3182f6]"
                              }`}
                            >
                              {team1Lost ? "패배" : "승리"}
                            </span>
                            <span className="text-sm text-[#8b95a1]">
                              (블루팀)
                            </span>
                          </div>
                          <table className="w-full text-sm text-[#191f28]">
                            <thead>
                              <tr className="border-b border-[#e5e8eb] text-xs text-[#8b95a1]">
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
                                    className="border-b border-[#e5e8eb] hover:bg-white/80"
                                  >
                                    <td className="py-2 px-4">
                                      <div className="flex items-center gap-2">
                                        <div className="relative">
                                          <img
                                            src={`https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${p.championNameEn}.png`}
                                            alt={p.championNameEn}
                                            width={32}
                                            height={32}
                                            className="rounded-xl"
                                          />
                                          <span className="absolute -bottom-1 -left-1 rounded bg-[#191f28] px-1 text-[10px] text-white">
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
                                            className="rounded-md"
                                          />
                                          <img
                                            src={
                                              spell?.[p.summoner2Id]?.imageFull
                                            }
                                            alt="spell"
                                            width={16}
                                            height={16}
                                            className="rounded-md"
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
                                            className="rounded-md"
                                          />
                                          <img
                                            src={
                                              rune?.[p.rune.subRune.styleId]
                                                ?.icon
                                            }
                                            alt="rune"
                                            width={16}
                                            height={16}
                                            className="rounded-md"
                                          />
                                        </div>
                                        <div className="flex flex-col">
                                          <span
                                            className="cursor-pointer text-sm font-semibold hover:text-[#3182f6]"
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
                                        <span className="text-lg font-bold text-[#3182f6]">
                                          {p.ourScore || p.teamLuckScore}
                                        </span>
                                        <span className="text-[10px] text-[#8b95a1]">
                                          {i + 1}등
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span>
                                          {p.kills} /{" "}
                                          <span className="text-[#f04452]">
                                            {p.deaths}
                                          </span>{" "}
                                          / {p.assists}
                                        </span>
                                        <span className="text-xs text-[#8b95a1]">
                                          {p.kda?.toFixed(2) || "Perfect"}
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span>
                                          {p.totalDamageDealtToChampions?.toLocaleString()}
                                        </span>
                                        <div className="h-1 w-16 overflow-hidden rounded bg-[#e5e8eb]">
                                          <div
                                            className="h-full bg-[#f04452]"
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
                                        <span className="text-[10px] text-[#8b95a1]">
                                          ({csPerMin}/분)
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span className="text-[#6b7684]">
                                          {p.visionWardsBoughtInGame}
                                        </span>
                                        <span className="text-[#8b95a1]">
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
                                              className="rounded-lg"
                                            />
                                          ) : (
                                            <div
                                              key={idx}
                                              className="h-6 w-6 rounded-lg bg-[#e5e8eb]"
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
                            !team1Lost ? "bg-[#fff5f5]" : "bg-[#f3f8ff]"
                          }`}
                        >
                          <div className="flex items-center gap-2 border-b border-[#e5e8eb] px-4 py-3">
                            <span
                              className={`font-medium ${
                                !team1Lost
                                  ? "text-[#f04452]"
                                  : "text-[#3182f6]"
                              }`}
                            >
                              {!team1Lost ? "패배" : "승리"}
                            </span>
                            <span className="text-sm text-[#8b95a1]">
                              (레드팀)
                            </span>
                          </div>
                          <table className="w-full text-sm text-[#191f28]">
                            <thead>
                              <tr className="border-b border-[#e5e8eb] text-xs text-[#8b95a1]">
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
                                    className="border-b border-[#e5e8eb] hover:bg-white/80"
                                  >
                                    <td className="py-2 px-4">
                                      <div className="flex items-center gap-2">
                                        <div className="relative">
                                          <img
                                            src={`https://ddragon.leagueoflegends.com/cdn/15.24.1/img/champion/${p.championNameEn}.png`}
                                            alt={p.championNameEn}
                                            width={32}
                                            height={32}
                                            className="rounded-xl"
                                          />
                                          <span className="absolute -bottom-1 -left-1 rounded bg-[#191f28] px-1 text-[10px] text-white">
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
                                            className="rounded-md"
                                          />
                                          <img
                                            src={
                                              spell?.[p.summoner2Id]?.imageFull
                                            }
                                            alt="spell"
                                            width={16}
                                            height={16}
                                            className="rounded-md"
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
                                            className="rounded-md"
                                          />
                                          <img
                                            src={
                                              rune?.[p.rune.subRune.styleId]
                                                ?.icon
                                            }
                                            alt="rune"
                                            width={16}
                                            height={16}
                                            className="rounded-md"
                                          />
                                        </div>
                                        <div className="flex flex-col">
                                          <span
                                            className="cursor-pointer text-sm font-semibold hover:text-[#3182f6]"
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
                                        <span className="text-lg font-bold text-[#3182f6]">
                                          {p.ourScore || p.teamLuckScore}
                                        </span>
                                        <span className="text-[10px] text-[#8b95a1]">
                                          {i - 4}등
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span>
                                          {p.kills} /{" "}
                                          <span className="text-[#f04452]">
                                            {p.deaths}
                                          </span>{" "}
                                          / {p.assists}
                                        </span>
                                        <span className="text-xs text-[#8b95a1]">
                                          {p.kda?.toFixed(2) || "Perfect"}
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span>
                                          {p.totalDamageDealtToChampions?.toLocaleString()}
                                        </span>
                                        <div className="h-1 w-16 overflow-hidden rounded bg-[#e5e8eb]">
                                          <div
                                            className="h-full bg-[#f04452]"
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
                                        <span className="text-[10px] text-[#8b95a1]">
                                          ({csPerMin}/분)
                                        </span>
                                      </div>
                                    </td>
                                    <td className="text-center py-2 px-2">
                                      <div className="flex flex-col items-center">
                                        <span className="text-[#6b7684]">
                                          {p.visionWardsBoughtInGame}
                                        </span>
                                        <span className="text-[#8b95a1]">
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
                                              className="rounded-lg"
                                            />
                                          ) : (
                                            <div
                                              key={idx}
                                              className="h-6 w-6 rounded-lg bg-[#e5e8eb]"
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
