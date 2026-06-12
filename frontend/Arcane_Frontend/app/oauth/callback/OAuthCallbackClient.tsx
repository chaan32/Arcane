"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { getRedirectAfterLogin, saveOAuthLogin } from "@/lib/mockAuth";

type CallbackState = "loading" | "success" | "error";

export default function OAuthCallbackClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [state, setState] = useState<CallbackState>("loading");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    const error = searchParams.get("error");
    const message = searchParams.get("message");
    const token = searchParams.get("token");
    const userId = searchParams.get("userId");
    const loginId = searchParams.get("loginId");
    const nickName = searchParams.get("nickName") ?? "";
    const role = searchParams.get("role");
    const provider = searchParams.get("provider");
    const needsOnboarding = searchParams.get("needsOnboarding") === "true";

    // 백엔드 OAuth2FailureHandler가 error 파라미터를 붙여서 보내면 실패로 처리한다.
    if (error) {
      setErrorMessage(message ?? "OAuth 인증 정보를 확인하지 못했습니다.");
      setState("error");
      return;
    }

    // 성공 콜백에는 JWT와 최소 사용자 정보가 모두 있어야 한다.
    // 하나라도 빠지면 로그인 상태를 만들 수 없으므로 실패 화면을 보여준다.
    if (!token || !userId || !loginId) {
      setState("error");
      return;
    }

    saveOAuthLogin({
      token,
      userId,
      loginId,
      nickName,
      role: role === "ADMIN" ? "ADMIN" : "USER",
      provider: provider === "naver" ? "naver" : "google",
    });

    setState("success");

    // 로그인 버튼을 눌렀던 원래 화면으로 이동한다.
    // 너무 즉시 이동하면 성공 화면이 깜빡이기만 해서 짧게 상태를 보여준 뒤 이동한다.
    const redirectPath = needsOnboarding ? "/auth/onboarding" : getRedirectAfterLogin();
    const timer = window.setTimeout(() => {
      router.replace(redirectPath);
    }, 650);

    return () => window.clearTimeout(timer);
  }, [router, searchParams]);

  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-16 text-[#69324b]">
      <section className="mx-auto flex max-w-[32rem] flex-col items-center rounded-[2rem] border border-white/75 bg-white/94 px-8 py-10 text-center shadow-[0_26px_72px_rgba(205,79,134,0.16)] ring-1 ring-[#f8dce8]/60">
        <Image
          src={state === "error" ? "/sad_mumu.png" : "/sad_lulu.png"}
          alt="OAuth 로그인 상태"
          width={132}
          height={132}
          className="h-32 w-32 object-contain"
          priority
        />
        <h1 className="mt-5 text-2xl font-black">
          {state === "error" ? "로그인에 실패했습니다" : "로그인 처리 중입니다"}
        </h1>
        <p className="mt-3 break-keep text-sm font-bold leading-6 text-[#a76886]">
          {state === "error"
            ? errorMessage || "OAuth 인증 정보를 확인하지 못했습니다. 잠시 후 다시 시도해주세요."
            : "계정 정보를 저장하고 원래 화면으로 돌아가는 중입니다."}
        </p>
        {state === "error" && (
          <div className="mt-6 flex flex-wrap justify-center gap-2">
            <Link
              href="/me"
              className="inline-flex h-11 items-center justify-center rounded-full bg-[#f45f9c] px-5 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.22)]"
            >
              내 정보로 돌아가기
            </Link>
            <Link
              href="/"
              className="inline-flex h-11 items-center justify-center rounded-full border border-[#ffd1e3] bg-[#fff0f7] px-5 text-sm font-black text-[#e75491]"
            >
              메인으로
            </Link>
          </div>
        )}
        {state !== "error" && (
          <div className="mt-6 h-2 w-full overflow-hidden rounded-full bg-[#fff0f7]">
            <div className="h-full w-1/2 animate-[oauthLoading_1s_ease-in-out_infinite] rounded-full bg-[#f45f9c]" />
          </div>
        )}
      </section>
      <style jsx global>{`
        @keyframes oauthLoading {
          0% {
            transform: translateX(-100%);
          }
          50% {
            transform: translateX(60%);
          }
          100% {
            transform: translateX(220%);
          }
        }
      `}</style>
    </main>
  );
}
