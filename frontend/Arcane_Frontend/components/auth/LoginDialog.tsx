"use client";

import { X } from "lucide-react";
import { useEffect } from "react";
import { useMockAuth } from "@/hooks/useMockAuth";
import type { AuthProvider } from "@/types/community";

interface LoginDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

const LOGIN_OPTIONS: Array<{
  provider: AuthProvider;
  label: string;
  description: string;
  mark: string;
  className: string;
}> = [
  {
    provider: "google",
    label: "Google로 로그인",
    description: "Google 계정으로 빠르게 시작",
    mark: "G",
    className: "bg-white text-[#69324b] ring-[#ffd1e3] hover:bg-[#fff7fb]",
  },
  {
    provider: "naver",
    label: "Naver로 로그인",
    description: "Naver 계정으로 빠르게 시작",
    mark: "N",
    className: "bg-[#03c75a] text-white ring-[#03c75a] hover:bg-[#02b451]",
  },
];

export default function LoginDialog({ isOpen, onClose }: LoginDialogProps) {
  const { signIn } = useMockAuth();

  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = "";
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-[#3d1830]/35 px-4 backdrop-blur-sm">
      <button
        type="button"
        aria-label="로그인 창 닫기"
        className="absolute inset-0 cursor-default"
        onClick={onClose}
      />
      <section className="relative w-full max-w-[28rem] overflow-hidden rounded-[2rem] border border-white/80 bg-white/96 p-5 text-[#69324b] shadow-[0_30px_90px_rgba(205,79,134,0.28)] ring-1 ring-[#ffd1e3]/80">
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="inline-flex rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
              Arcane Login
            </div>
            <h2 className="mt-3 text-2xl font-black">로그인 방법 선택</h2>
            <p className="mt-2 break-keep text-sm font-bold leading-6 text-[#a76886]">
              사용할 계정을 선택하면 로그인 페이지로 이동합니다.
            </p>
          </div>
          <button
            type="button"
            aria-label="로그인 창 닫기"
            onClick={onClose}
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#fff0f7] text-[#a76886] ring-1 ring-[#ffd1e3] transition-colors hover:bg-[#ffe0ee] hover:text-[#e75491]"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-6 flex flex-col gap-3">
          {LOGIN_OPTIONS.map((option) => (
            <button
              key={option.provider}
              type="button"
              onClick={() => signIn(option.provider)}
              className={`flex w-full items-center gap-4 rounded-[1.35rem] px-4 py-4 text-left shadow-[0_14px_30px_rgba(205,79,134,0.12)] ring-1 transition-all hover:-translate-y-0.5 ${option.className}`}
            >
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white/85 text-lg font-black text-[#e75491] shadow-inner">
                {option.mark}
              </span>
              <span className="min-w-0 flex-1">
                <span className="block text-base font-black">
                  {option.label}
                </span>
                <span className="mt-1 block text-xs font-bold opacity-75">
                  {option.description}
                </span>
              </span>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}
