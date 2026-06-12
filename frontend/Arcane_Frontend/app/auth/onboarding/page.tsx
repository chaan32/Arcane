"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { RefreshCcw } from "lucide-react";
import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";
import { apiFetch } from "@/services/apiClient";
import {
  getAuthToken,
  getRedirectAfterLogin,
  isAuthErrorResponse,
  signOutForAuthError,
  updateStoredUserNickName,
} from "@/lib/mockAuth";
import { generateNicknameSuggestions } from "@/lib/nicknameSuggestions";

type NickNameState = "idle" | "checking" | "available" | "taken" | "invalid";

export default function OAuthOnboardingPage() {
  const router = useRouter();
  const [nickName, setNickName] = useState("");
  const [nickNameState, setNickNameState] = useState<NickNameState>("idle");
  const [message, setMessage] = useState("공략과 채팅에서 사용할 별명을 정해주세요.");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>(() =>
    generateNicknameSuggestions(3)
  );

  const normalizedNickName = useMemo(() => nickName.trim(), [nickName]);
  const canSubmit = nickNameState === "available" && !isSubmitting;

  useEffect(() => {
    const token = getAuthToken();
    if (!token) {
      router.replace("/");
    }
  }, [router]);

  useEffect(() => {
    if (!normalizedNickName) {
      setNickNameState("idle");
      setMessage("공략과 채팅에서 사용할 별명을 정해주세요.");
      return;
    }

    if (normalizedNickName.length < 2 || normalizedNickName.length > 20) {
      setNickNameState("invalid");
      setMessage("별명은 2자 이상 20자 이하로 입력해주세요.");
      return;
    }

    setNickNameState("checking");
    setMessage("별명 중복을 확인하는 중입니다.");

    const timer = window.setTimeout(async () => {
      try {
        const response = await apiFetch("/api/v1/user/check/nickName", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ nickName: normalizedNickName }),
        });

        if (!response.ok) {
          throw new Error("nickname check failed");
        }

        const data = (await response.json()) as { isPresentNickName?: boolean };
        const isTaken = Boolean(data.isPresentNickName);

        setNickNameState(isTaken ? "taken" : "available");
        setMessage(isTaken ? "이미 사용 중인 별명입니다." : "사용 가능한 별명입니다.");
      } catch {
        setNickNameState("invalid");
        setMessage("중복 확인에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    }, 350);

    return () => window.clearTimeout(timer);
  }, [normalizedNickName]);

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    setNickName(event.target.value);
  };

  const refreshSuggestions = () => {
    setSuggestions(generateNicknameSuggestions(3));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!canSubmit) return;

    const token = getAuthToken();
    if (!token) {
      router.replace("/");
      return;
    }

    setIsSubmitting(true);
    setMessage("별명을 저장하는 중입니다.");

    try {
      const response = await apiFetch("/api/v1/user/onboarding", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ nickName: normalizedNickName }),
      });

      const data = (await response.json()) as {
        nickName?: string;
        code?: string;
        message?: string;
      };

      if (isAuthErrorResponse(response.status, data)) {
        signOutForAuthError();
        router.replace("/");
        throw new Error(data.message ?? "로그인이 만료되었습니다. 다시 로그인해주세요.");
      }

      if (!response.ok || !data.nickName) {
        throw new Error(data.message ?? "별명 저장에 실패했습니다.");
      }

      updateStoredUserNickName(data.nickName);
      router.replace(getRedirectAfterLogin());
    } catch (error) {
      setIsSubmitting(false);
      setNickNameState("invalid");
      setMessage(error instanceof Error ? error.message : "별명 저장에 실패했습니다.");
    }
  };

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-16 text-[#69324b]">
      <section className="mx-auto grid w-full max-w-[58rem] overflow-hidden rounded-[2.25rem] border border-white/80 bg-white/95 shadow-[0_30px_90px_rgba(205,79,134,0.20)] ring-1 ring-[#ffd1e3]/70 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="flex flex-col items-center justify-center bg-[#fff0f7] p-8 text-center">
          <Image
            src="/sad_lulu.png"
            alt="별명 설정"
            width={180}
            height={180}
            className="h-44 w-44 object-contain"
            priority
          />
          <h1 className="mt-5 text-3xl font-black">처음 오셨군요</h1>
          <p className="mt-3 max-w-sm break-keep text-sm font-bold leading-6 text-[#a76886]">
            공략 작성자, 댓글 작성자, 채팅 상대를 구분할 수 있도록 Arcane에서 사용할 별명을 정합니다.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col justify-center p-8">
          <div className="inline-flex w-fit rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
            Profile Setup
          </div>
          <label className="mt-6 text-sm font-black text-[#a76886]" htmlFor="nickName">
            별명
          </label>
          <input
            id="nickName"
            value={nickName}
            onChange={handleChange}
            maxLength={20}
            autoFocus
            placeholder="2자 이상 20자 이하"
            className="mt-2 h-14 rounded-[1.25rem] border border-[#ffd1e3] bg-[#fffafd] px-5 text-lg font-black text-[#69324b] outline-none transition-colors placeholder:text-[#bd7b98] focus:border-[#f45f9c] focus:ring-4 focus:ring-[#ffe0ee]"
          />
          <p
            className={`mt-3 text-sm font-bold ${
              nickNameState === "available" ? "text-[#e75491]" : "text-[#a76886]"
            }`}
          >
            {message}
          </p>

          <div className="mt-6 rounded-[1.5rem] bg-[#fff7fb] p-4 ring-1 ring-[#ffd1e3]/70">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="text-sm font-black text-[#69324b]">추천 별명</h2>
                <p className="mt-1 text-xs font-bold text-[#a76886]">
                  마음에 드는 별명을 눌러도 되고 직접 입력해도 됩니다.
                </p>
              </div>
              <button
                type="button"
                onClick={refreshSuggestions}
                aria-label="추천 별명 새로고침"
                className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white text-[#e75491] ring-1 ring-[#ffd1e3] transition-colors hover:bg-[#ffe0ee]"
              >
                <RefreshCcw className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4 grid gap-2 sm:grid-cols-3">
              {suggestions.map((suggestion) => (
                <button
                  key={suggestion}
                  type="button"
                  onClick={() => setNickName(suggestion)}
                  className="min-h-11 rounded-full bg-white px-3 text-sm font-black text-[#e75491] shadow-[0_10px_24px_rgba(205,79,134,0.10)] ring-1 ring-[#ffd1e3] transition-all hover:-translate-y-0.5 hover:bg-[#fff0f7]"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>

          <button
            type="submit"
            disabled={!canSubmit}
            className="mt-8 h-14 rounded-full bg-[#f45f9c] text-base font-black text-white shadow-[0_16px_34px_rgba(231,84,145,0.25)] transition-all enabled:hover:-translate-y-0.5 enabled:hover:bg-[#e75491] disabled:cursor-not-allowed disabled:bg-[#f6bed5] disabled:shadow-none"
          >
            {isSubmitting ? "저장 중" : "시작하기"}
          </button>
        </form>
      </section>
    </main>
  );
}
