"use client";

import { cn } from "@/lib/utils";
import Link from "next/link";
import CloseButton from "@/components/common/CloseButton";
import LoginDialog from "@/components/auth/LoginDialog";
import { useMockAuth } from "@/hooks/useMockAuth";
import { useState } from "react";

interface MobileMenuProps {
  isOpen: boolean;
  onClose: () => void;
}

const MENU_ITEMS = [
  { id: 1, title: "챔피언 분석", href: "/champions", variant: "default" },
  { id: 2, title: "소환사 랭킹", href: "/ranking", variant: "default" },
  { id: 3, title: "공략", href: "/guides", variant: "default" },
  { id: 4, title: "패치노트", href: "/patch-notes", variant: "default" },
];

export default function MobileMenu({ isOpen, onClose }: MobileMenuProps) {
  const { user, signOut } = useMockAuth();
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const accountHref = user?.role === "ADMIN" ? "/admin" : "/me";

  return (
    <>
      <aside
        className={cn(
          "fixed right-0 top-0 z-[90] flex h-full w-full flex-col bg-[#fff7fb] text-[#69324b] transition-transform duration-300 ease-in-out",
          isOpen ? "translate-x-0" : "translate-x-full"
        )}
      >
        <header className="border-b border-[#ffe1ed]">
          <div className="flex justify-end px-5 py-3">
            <CloseButton onClick={onClose} className="h-8 w-8" />
          </div>
        </header>

        <div className="mt-6 mb-10 flex flex-1 flex-col px-5">
          {user ? (
            <div className="rounded-[1.5rem] bg-white p-4 shadow-[0_16px_34px_rgba(205,79,134,0.12)] ring-1 ring-[#ffd1e3]">
              <p className="text-xs font-black text-[#a76886]">로그인됨</p>
              <Link
                href={accountHref}
                onClick={onClose}
                className="mt-1 block text-lg font-black text-[#69324b]"
              >
                {user.name}
                {user.role === "ADMIN" && (
                  <span className="ml-2 rounded-full bg-[#f45f9c] px-2 py-0.5 text-xs font-black text-white">
                    ADMIN
                  </span>
                )}
              </Link>
              <button
                type="button"
                onClick={() => {
                  signOut();
                  onClose();
                }}
                className="mt-3 rounded-full bg-[#fff0f7] px-4 py-2 text-sm font-black text-[#e75491]"
              >
                로그아웃
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setIsLoginOpen(true)}
              className="w-full cursor-pointer rounded-full bg-blue-300 py-3 text-m-btn text-white shadow-[0_14px_30px_rgba(231,84,145,0.24)]"
            >
              로그인
            </button>
          )}

          <nav className="mt-6">
            <ul className="flex flex-col gap-3">
              {MENU_ITEMS.map((item) => (
                <li key={item.id} className="border-b border-[#ffe1ed]">
                  <Link
                    href={item.href}
                    className="flex w-full items-center gap-2 px-1 pb-3 pt-1 text-m-menu1 font-black text-[#69324b] transition-colors hover:text-[#e75491]"
                    onClick={onClose}
                  >
                    <span>{item.title}</span>
                  </Link>
                </li>
              ))}
            </ul>
          </nav>

          <div className="flex-1" />

          <footer className="flex items-center gap-1 text-m-body3 text-[#bd7b98]">
            <span>이용약관</span>
            <span>|</span>
            <span>개인정보처리방침</span>
          </footer>
        </div>
      </aside>
      <LoginDialog isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
    </>
  );
}
