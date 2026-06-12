"use client";

import React, { useEffect, useState } from "react";
import { useChampionDetail } from "@/hooks/useChampionDetail";
import { LaneSelector } from "./_components/LaneSelector";
import { ChampionDetailHeader } from "./_components/ChampionDetailHeader";
import { ChampionBuildTab } from "./_components/ChampionBuildTab";
import { ChampionBaseStats } from "./_components/ChampionBaseStats";
import { ChampionPatchNotes } from "./_components/ChampionPatchNotes";
import { ChampionMatchups } from "./_components/ChampionMatchups";

export default function ChampionPage({
  params,
}: {
  params: Promise<{ name: string }>;
}) {
  const resolvedParams = React.use(params);
  const decodedName = decodeURIComponent(resolvedParams.name);
  const [laneIndex, setLaneIndex] = useState<number>(0);
  const [activeTab, setActiveTab] = useState("빌드");

  // 챔피언 데이터 가져오기
  const { data, skill, isLoading, error } = useChampionDetail(decodedName);
  const hasAnalysisData = data.length > 0;
  const championPatchName =
    skill?.nameKo || data[laneIndex]?.detailChampInfo.championName || decodedName;

  useEffect(() => {
    if (laneIndex >= data.length) {
      setLaneIndex(0);
    }
  }, [data.length, laneIndex]);

  // 하단 탭 관리
  const tabs = [
    { id: "빌드", label: "챔피언명 빌드" },
    { id: "정보", label: "기본 정보" },
    { id: "패치", label: "패치 정보" },
  ];

  // ========= 데탑 먼저 진행  =========
  return (
    <main className="flex flex-col text-[#69324b] lg:min-h-[calc(100vh-5rem)] lg:pt-[0.5rem]">
      {/* 상단 챔피언 요약 세션*/}
      <section className="relative w-full">
        {/* 배경 그라데이션 부분 */}
        <div
          className="absolute inset-0"
          style={{
            background:
              "linear-gradient(135deg, rgba(255, 194, 219, 0.7), rgba(255, 250, 253, 0.2))",
            filter: "blur(12px)",
          }}
        />
        {/* 콘텐츠 영역 */}
        {/* 배경 그라데이션 때문에 z-10을 줌 */}
        <div className="relative flex flex-col z-10 lg:px-[10rem] lg:py-[2.5rem]">
          {/* 라인 선택 탭 */}
          {hasAnalysisData && (
            <LaneSelector
              data={data}
              laneIndex={laneIndex}
              onLaneChange={setLaneIndex}
            />
          )}

          {/* 챔피언 상세 정보*/}
          <ChampionDetailHeader
            data={data}
            laneIndex={laneIndex}
            skill={skill}
            championDisplayName={decodedName}
          />
        </div>
      </section>

      {/* 2. 중간 콘텐츠 영역 */}
      <div className="w-full lg:px-[10rem] lg:pt-[3rem]">
        <nav className="flex gap-[0.25rem]">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`lg:px-[1rem] lg:py-[0.5rem] transition-all rounded-t-[0.25rem] cursor-pointer ${
                activeTab === tab.id
                  ? "bg-[#ff7aae] text-white text-d-body2-r shadow-[0_8px_18px_rgba(244,114,182,0.22)]"
                  : "bg-transparent text-[#a76886] text-d-body2 hover:text-[#d94683]"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>

        {/* 탭 내용 영역  */}
        <div className="lg:px-[1.25rem] lg:mb-[2.5rem] border border-[#ffd1e3] bg-white/90 rounded-b-[0.75rem] shadow-[0_18px_42px_rgba(244,114,182,0.14)]">
          {/* 챔피언 빌드 영역 */}
          {activeTab === "빌드" && (
            <ChampionBuildTab
              data={data}
              laneIndex={laneIndex}
              isLoading={isLoading}
              errorMessage={error?.message}
            />
          )}

          {/* 기본 정보 영역 */}
          {activeTab === "정보" && <ChampionBaseStats stats={skill?.stats} />}

          {/* 패치 정보 영역 */}
          {activeTab === "패치" && (
            <ChampionPatchNotes championName={championPatchName} />
          )}
        </div>
      </div>

      {/* 3. 하단 영역 */}
      {/* 챔피언 매치업 정보 */}
      <ChampionMatchups
        data={data}
        laneIndex={laneIndex}
        isLoading={isLoading}
      />
    </main>
  );
}
