"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { CheckCircle2, Link2, Loader2, Save, UserRound } from "lucide-react";
import { ExternalImage } from "@/components/common/ExternalImage";
import { apiFetch } from "@/services/apiClient";
import {
  getAuthToken,
  isAuthErrorResponse,
  signOutForAuthError,
  startOAuthLink,
  updateStoredUserNickName,
  updateStoredUserRole,
} from "@/lib/mockAuth";
import type { AuthProvider } from "@/types/community";

type MeProfile = {
  userId: number;
  loginId: string;
  nickName: string | null;
  email: string | null;
  profileImage: string | null;
  connectedProviders: AuthProvider[];
  onboardingCompleted: boolean;
  role: "USER" | "ADMIN";
};

const PROVIDERS: Array<{
  provider: AuthProvider;
  label: string;
  description: string;
  mark: string;
  className: string;
}> = [
  {
    provider: "google",
    label: "Google",
    description: "Google 계정을 Arcane 계정에 연결",
    mark: "G",
    className: "bg-white text-[#69324b] ring-[#ffd1e3]",
  },
  {
    provider: "naver",
    label: "Naver",
    description: "Naver 계정을 Arcane 계정에 연결",
    mark: "N",
    className: "bg-[#03c75a] text-white ring-[#03c75a]",
  },
];

export default function MyPage() {
  const router = useRouter();
  const [profile, setProfile] = useState<MeProfile | null>(null);
  const [nickName, setNickName] = useState("");
  const [message, setMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [linkingProvider, setLinkingProvider] = useState<AuthProvider | null>(null);

  const normalizedNickName = useMemo(() => nickName.trim(), [nickName]);
  const currentNickName = profile?.nickName ?? "";
  const canSave =
    normalizedNickName.length >= 2 &&
    normalizedNickName.length <= 20 &&
    normalizedNickName !== currentNickName &&
    !isSaving;

  useEffect(() => {
    const token = getAuthToken();
    if (!token) {
      setMessage("로그인이 필요합니다.");
      setIsLoading(false);
      return;
    }

    const fetchProfile = async () => {
      try {
        const response = await apiFetch("/api/v1/user/me", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        const data = (await response.json()) as MeProfile & { code?: string; message?: string };
        if (isAuthErrorResponse(response.status, data)) {
          signOutForAuthError();
          router.replace("/");
          throw new Error(data.message ?? "로그인이 만료되었습니다. 다시 로그인해주세요.");
        }
        if (!response.ok) {
          throw new Error(data.message ?? "내 정보를 불러오지 못했습니다.");
        }

        setProfile(data);
        setNickName(data.nickName ?? "");
        updateStoredUserRole(data.role);
        setMessage("");
        if (data.role === "ADMIN") {
          router.replace("/admin");
          return;
        }
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "내 정보를 불러오지 못했습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    void fetchProfile();
  }, [router]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!canSave) return;

    const token = getAuthToken();
    if (!token) {
      setMessage("로그인이 필요합니다.");
      return;
    }

    setIsSaving(true);
    setMessage("별명을 저장하는 중입니다.");

    try {
      const response = await apiFetch("/api/v1/user/me/nickname", {
        method: "PATCH",
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
        throw new Error(data.message ?? "별명 변경에 실패했습니다.");
      }

      updateStoredUserNickName(data.nickName);
      setProfile((current) =>
        current ? { ...current, nickName: data.nickName ?? current.nickName } : current
      );
      setNickName(data.nickName);
      setMessage("별명을 변경했습니다.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "별명 변경에 실패했습니다.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleLinkProvider = async (provider: AuthProvider) => {
    setLinkingProvider(provider);
    setMessage(`${provider === "google" ? "Google" : "Naver"} 연동을 시작합니다.`);

    try {
      await startOAuthLink(provider);
    } catch (error) {
      setLinkingProvider(null);
      setMessage(error instanceof Error ? error.message : "소셜 계정 연동을 시작하지 못했습니다.");
    }
  };

  if (isLoading) {
    return (
      <main className="min-h-[calc(100vh-5rem)] px-4 py-14 text-[#69324b]">
        <section className="mx-auto flex max-w-[34rem] flex-col items-center rounded-[2rem] bg-white/90 px-8 py-12 shadow-[0_28px_80px_rgba(205,79,134,0.16)] ring-1 ring-[#ffd1e3]/70">
          <Loader2 className="h-10 w-10 animate-spin text-[#e75491]" />
          <p className="mt-4 text-sm font-black text-[#a76886]">내 정보를 불러오는 중입니다.</p>
        </section>
      </main>
    );
  }

  if (!profile) {
    return (
      <main className="min-h-[calc(100vh-5rem)] px-4 py-14 text-[#69324b]">
        <section className="mx-auto max-w-[34rem] rounded-[2rem] bg-white/90 px-8 py-12 text-center shadow-[0_28px_80px_rgba(205,79,134,0.16)] ring-1 ring-[#ffd1e3]/70">
          <h1 className="text-2xl font-black">내 정보를 볼 수 없습니다</h1>
          <p className="mt-3 text-sm font-bold text-[#a76886]">{message}</p>
          <Link
            href="/"
            className="mt-6 inline-flex h-12 items-center justify-center rounded-full bg-[#f45f9c] px-6 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.24)]"
          >
            메인으로
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className="min-h-[calc(100vh-5rem)] px-4 py-10 text-[#69324b] lg:py-14">
      <section className="mx-auto grid max-w-[72rem] gap-5 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="rounded-[2rem] bg-white/92 p-6 shadow-[0_28px_80px_rgba(205,79,134,0.14)] ring-1 ring-[#ffd1e3]/75">
          <div className="inline-flex rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
            My Arcane
          </div>
          <div className="mt-7 flex items-center gap-5">
            <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-[1.75rem] bg-[#fff0f7] shadow-[0_16px_34px_rgba(205,79,134,0.16)] ring-1 ring-[#ffd1e3]">
              <ExternalImage
                src={profile.profileImage}
                alt="프로필 이미지"
                width={96}
                height={96}
                sizes="96px"
                className="h-full w-full object-cover"
                fallback={<UserRound className="h-10 w-10 text-[#e75491]" />}
              />
            </div>
            <div className="min-w-0">
              <h1 className="break-keep text-3xl font-black">{profile.nickName}</h1>
              <p className="mt-2 break-all text-sm font-bold text-[#a76886]">{profile.email ?? "이메일 정보 없음"}</p>
            </div>
          </div>

          <div className="mt-7 grid gap-3 rounded-[1.5rem] bg-[#fff7fb] p-4 ring-1 ring-[#ffd1e3]/70">
            <div>
              <p className="text-xs font-black text-[#a76886]">내부 로그인 ID</p>
              <p className="mt-1 break-all text-sm font-black">{profile.loginId}</p>
            </div>
            <div>
              <p className="text-xs font-black text-[#a76886]">연결된 소셜 로그인</p>
              <div className="mt-2 flex flex-wrap gap-2">
                {profile.connectedProviders.length > 0 ? (
                  profile.connectedProviders.map((provider) => (
                    <span
                      key={provider}
                      className="rounded-full bg-white px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]"
                    >
                      {provider === "google" ? "Google" : "Naver"}
                    </span>
                  ))
                ) : (
                  <span className="text-sm font-bold text-[#a76886]">아직 연결된 소셜 계정이 없습니다.</span>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="grid gap-5">
          <form
            onSubmit={handleSubmit}
            className="rounded-[2rem] bg-white/92 p-6 shadow-[0_24px_70px_rgba(205,79,134,0.12)] ring-1 ring-[#ffd1e3]/75"
          >
            <h2 className="text-xl font-black">별명 변경</h2>
            <p className="mt-2 text-sm font-bold text-[#a76886]">
              공략, 댓글, 채팅에서 표시되는 이름입니다.
            </p>
            <input
              value={nickName}
              onChange={(event) => setNickName(event.target.value)}
              maxLength={20}
              className="mt-5 h-14 w-full rounded-[1.25rem] border border-[#ffd1e3] bg-[#fffafd] px-5 text-lg font-black outline-none transition-colors placeholder:text-[#bd7b98] focus:border-[#f45f9c] focus:ring-4 focus:ring-[#ffe0ee]"
              placeholder="2자 이상 20자 이하"
            />
            <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
              <p className="text-sm font-bold text-[#a76886]">{message || "변경할 별명을 입력해주세요."}</p>
              <button
                type="submit"
                disabled={!canSave}
                className="inline-flex h-12 items-center gap-2 rounded-full bg-[#f45f9c] px-5 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.22)] transition-all enabled:hover:-translate-y-0.5 enabled:hover:bg-[#e75491] disabled:cursor-not-allowed disabled:bg-[#f6bed5] disabled:shadow-none"
              >
                {isSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                저장
              </button>
            </div>
          </form>

          <section className="rounded-[2rem] bg-white/92 p-6 shadow-[0_24px_70px_rgba(205,79,134,0.12)] ring-1 ring-[#ffd1e3]/75">
            <h2 className="text-xl font-black">소셜 로그인 연동</h2>
            <p className="mt-2 break-keep text-sm font-bold leading-6 text-[#a76886]">
              여기서 추가 로그인하면 새 계정을 만드는 게 아니라 현재 Arcane 계정에 소셜 계정을 연결합니다.
            </p>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              {PROVIDERS.map((item) => {
                const isConnected = profile.connectedProviders.includes(item.provider);
                const isLinking = linkingProvider === item.provider;

                return (
                  <button
                    key={item.provider}
                    type="button"
                    disabled={isConnected || linkingProvider !== null}
                    onClick={() => handleLinkProvider(item.provider)}
                    className={`flex min-h-24 items-center gap-4 rounded-[1.5rem] px-4 py-4 text-left shadow-[0_14px_30px_rgba(205,79,134,0.10)] ring-1 transition-all enabled:hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-70 ${item.className}`}
                  >
                    <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white/90 text-lg font-black text-[#e75491] shadow-inner">
                      {item.mark}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="flex items-center gap-2 text-base font-black">
                        {item.label}
                        {isConnected && <CheckCircle2 className="h-4 w-4" />}
                        {isLinking && <Loader2 className="h-4 w-4 animate-spin" />}
                      </span>
                      <span className="mt-1 block text-xs font-bold opacity-75">
                        {isConnected ? "이미 연동됨" : item.description}
                      </span>
                    </span>
                    {!isConnected && <Link2 className="h-5 w-5 shrink-0 opacity-70" />}
                  </button>
                );
              })}
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}
