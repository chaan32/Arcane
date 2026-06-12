"use client";

import Image from "next/image";

type StatusViewProps = {
  message: string;
  onRetry: () => void;
};

type LoadingViewProps = {
  frameSrc: string;
};

export function SummonerRateLimitView({ message, onRetry }: StatusViewProps) {
  return (
    <div className="flex min-h-screen items-center justify-center overflow-hidden bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_42%,_#fffafd_100%)] px-5 py-24 text-[#69324b]">
      <div className="relative w-full max-w-[34rem] overflow-hidden rounded-[2.5rem] border border-white/75 bg-white/92 px-8 py-10 text-center shadow-[0_28px_78px_rgba(244,114,182,0.2)] ring-1 ring-[#f8dce8]/70 backdrop-blur">
        <div className="pointer-events-none absolute -left-16 -top-16 h-44 w-44 rounded-full bg-[#ffd9e5]/75 blur-2xl" />
        <div className="pointer-events-none absolute -bottom-20 -right-12 h-52 w-52 rounded-full bg-[#c7f4e4]/70 blur-2xl" />
        <div className="relative z-10 mx-auto flex h-44 w-44 items-center justify-center rounded-[2rem] bg-[#fff0f7] shadow-[inset_0_1px_0_rgba(255,255,255,0.92),0_18px_42px_rgba(205,79,134,0.15)]">
          <Image
            src="/sad_yumi.png"
            alt="요청 제한 안내 이미지"
            width={176}
            height={176}
            className="h-40 w-40 object-contain"
          />
        </div>
        <div className="relative z-10 mx-auto mt-6 w-fit rounded-full bg-[#fff0f7] px-4 py-1.5 text-sm font-black text-[#f45f9c] ring-1 ring-[#ffd1e3]">
          서버 내부 에러
        </div>
        <h1 className="relative z-10 mt-4 text-2xl font-black tracking-normal text-[#69324b] lg:text-3xl">
          잠시 후 다시 시도해 주세요
        </h1>
        <p className="relative z-10 mx-auto mt-3 max-w-[28rem] break-keep text-base font-bold leading-7 text-[#a76886]">
          {message}
        </p>
        <button
          type="button"
          onClick={onRetry}
          className="relative z-10 mt-8 rounded-full bg-[#ff7aae] px-8 py-3.5 text-sm font-black text-white shadow-[0_16px_32px_rgba(244,114,182,0.28)] transition-colors hover:bg-[#f85f9d]"
        >
          다시 검색하기
        </button>
      </div>
    </div>
  );
}

export function SummonerNotFoundView({ message, onRetry }: StatusViewProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_42%,_#fffafd_100%)] px-5 py-24 text-[#69324b]">
      <div className="relative w-full max-w-[54rem] overflow-hidden rounded-[2.75rem] border border-[#ffd1e3] bg-white/88 p-8 text-center shadow-[0_26px_70px_rgba(244,114,182,0.18)] backdrop-blur lg:p-12">
        <div className="pointer-events-none absolute -left-16 -top-16 h-44 w-44 rounded-full bg-[#ffd9e5]/70 blur-2xl" />
        <div className="pointer-events-none absolute -bottom-20 -right-12 h-56 w-56 rounded-full bg-[#c7f4e4]/70 blur-2xl" />
        <div className="relative z-10 mx-auto mb-6 flex h-[13rem] w-full max-w-[22rem] items-center justify-center rounded-[2.25rem] bg-[#fff0f7] shadow-inner shadow-[#ffd1e3]/45">
          <Image
            src="/angry_lulu.png"
            alt="소환사를 찾지 못한 안내 이미지"
            width={352}
            height={208}
            className="h-full w-full object-contain px-5 py-4"
          />
        </div>
        <h1 className="relative z-10 text-3xl font-black tracking-normal text-[#69324b] lg:text-4xl">
          소환사를 찾을 수 없습니다
        </h1>
        <p className="relative z-10 mx-auto mt-4 max-w-[35rem] break-keep text-lg font-bold leading-8 text-[#5f7291]">
          {message}
        </p>
        <p className="relative z-10 mx-auto mt-3 max-w-[30rem] break-keep text-sm font-semibold leading-6 text-[#8aa1c0]">
          이름이나 태그가 바뀌었을 수 있어요. 입력한 소환사명과 태그를 다시 확인해 주세요.
        </p>
        <button
          type="button"
          onClick={onRetry}
          className="relative z-10 mt-8 rounded-full bg-[#ff7aae] px-8 py-3.5 text-sm font-black text-white shadow-[0_16px_32px_rgba(244,114,182,0.28)] transition-colors hover:bg-[#f85f9d]"
        >
          다시 검색하기
        </button>
      </div>
    </div>
  );
}

export function SummonerLoadingView({ frameSrc }: LoadingViewProps) {
  return (
    <div className="flex min-h-screen items-start justify-center overflow-hidden bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_42%,_#fffafd_100%)] px-5 pb-24 pt-[13vh] text-[#69324b]">
      <div className="relative w-full max-w-[36rem] overflow-hidden rounded-[2.75rem] border border-white/75 bg-white/90 px-9 py-12 text-center shadow-[0_30px_86px_rgba(244,114,182,0.2)] ring-1 ring-[#f8dce8]/70 backdrop-blur">
        <div className="pointer-events-none absolute -left-20 -top-20 h-56 w-56 rounded-full bg-[#ffd9e5]/75 blur-2xl" />
        <div className="pointer-events-none absolute -bottom-24 -right-16 h-64 w-64 rounded-full bg-[#c7f4e4]/70 blur-2xl" />
        <div className="relative z-10 mx-auto flex h-60 w-60 items-center justify-center rounded-[2.5rem] bg-[#fff0f7] shadow-[inset_0_1px_0_rgba(255,255,255,0.92),0_20px_46px_rgba(205,79,134,0.16)]">
          <Image
            src={frameSrc}
            alt="전적 검색 로딩 이미지"
            width={240}
            height={240}
            className="h-56 w-56 object-contain"
          />
        </div>
        <h1 className="relative z-10 mt-7 text-3xl font-black tracking-normal text-[#69324b]">
          전적을 불러오는 중
        </h1>
        <p className="relative z-10 mt-3 break-keep text-base font-bold leading-7 text-[#a76886]">
          Riot 데이터를 확인하고 있어요
        </p>
        <div className="relative z-10 mx-auto mt-7 flex w-44 items-center justify-center gap-2.5">
          {[0, 1, 2].map((dot) => (
            <span
              key={dot}
              className="h-3 w-3 animate-pulse rounded-full bg-[#ff7aae] opacity-50"
              style={{ animationDelay: `${dot * 160}ms` }}
            />
          ))}
        </div>
      </div>
    </div>
  );
}
