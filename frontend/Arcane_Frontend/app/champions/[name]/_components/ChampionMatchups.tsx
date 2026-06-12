import Image from "next/image";
import type { ChampionDetailDto } from "@/types/championDetail";

interface ChampionMatchupsProps {
  data: ChampionDetailDto[];
  laneIndex: number;
  isLoading?: boolean;
}

const matchupCards = [
  {
    key: "hard",
    title: "상대하기 어려운 챔피언",
    message: "까다로운 상대 분석을 준비하고 있어요.",
  },
  {
    key: "easy",
    title: "상대하기 쉬운 챔피언",
    message: "편하게 상대할 수 있는 픽을 정리 중이에요.",
  },
  {
    key: "synergy",
    title: "함께하기 좋은 챔피언",
    message: "잘 맞는 조합 데이터를 모으고 있어요.",
  },
];

export function ChampionMatchups({
  data,
  laneIndex,
  isLoading,
}: ChampionMatchupsProps) {
  if (isLoading) {
    return (
      <section className="w-full lg:px-[10rem] lg:pt-[2.5rem] lg:pb-[5rem]">
        <div className="flex min-h-[16rem] items-center justify-center rounded-[1rem] border border-[#ffd1e3] bg-white/90 text-d-body3-r text-[#a76886] shadow-[0_18px_42px_rgba(244,114,182,0.14)]">
          매치업 데이터를 불러오는 중입니다.
        </div>
      </section>
    );
  }

  if (!data[laneIndex]) {
    return (
      <section className="w-full lg:px-[10rem] lg:pt-[2.5rem] lg:pb-[5rem]">
        <div className="flex min-h-[16rem] flex-col items-center justify-center gap-[0.5rem] rounded-[1rem] border border-[#ffd1e3] bg-white/90 text-center shadow-[0_18px_42px_rgba(244,114,182,0.14)]">
          <p className="text-d-body2 text-[#69324b]">매치업 분석 데이터가 없습니다.</p>
          <p className="text-d-body3-r text-[#a76886]">
            수집된 매치 분석이 완료되면 상대 챔피언별 승률이 표시됩니다.
          </p>
        </div>
      </section>
    );
  }

  return (
    <section className="w-full lg:px-[10rem] lg:pt-[2.5rem] lg:pb-[5rem]">
      <div className="grid grid-cols-3 gap-[1.25rem]">
        {matchupCards.map((card) => (
          <div
            key={card.key}
            className="flex min-h-[23rem] flex-col items-center justify-center rounded-[1.25rem] border border-[#ffd1e3] bg-white/90 px-[1.5rem] py-[2rem] text-center shadow-[0_18px_42px_rgba(244,114,182,0.14)]"
          >
            <Image
              src="/service-soon-matchup.svg"
              alt="서비스 예정"
              width={210}
              height={154}
              className="mb-[1.25rem] h-auto w-[13rem]"
            />
            <h3 className="text-d-body2 text-[#69324b]">{card.title}</h3>
            <p className="mt-[0.65rem] text-d-body3-r text-[#a76886]">
              {card.message}
            </p>
            <span className="mt-[1.25rem] rounded-full bg-[#fff0f7] px-[1rem] py-[0.4rem] text-[0.85rem] font-black text-[#e75491]">
              서비스 예정
            </span>
          </div>
        ))}
      </div>
    </section>
  );
}
