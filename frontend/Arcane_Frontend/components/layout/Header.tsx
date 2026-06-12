"use client";

import Image from "next/image";
import Link from "next/link";
import { LogOut } from "lucide-react";
import { Hamburger } from "../icons/Hamburger";
import { useState } from "react";
import MobileMenu from "./MobileMenu";
import { DesktopNavLinks } from "./DesktopNavLinks";
import SearchBar from "@/components/search/SearchBar";
import LoginDialog from "@/components/auth/LoginDialog";
import ChatDock from "@/components/chat/ChatDock";
import { useMockAuth } from "@/hooks/useMockAuth";

export default function Header() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const { user, signOut } = useMockAuth();
  const accountHref = user?.role === "ADMIN" ? "/admin" : "/me";

  return (
    <header className="fixed top-0 z-50 w-full border-b border-[#ffe1ed] bg-white/90 text-[#69324b] shadow-[0_10px_34px_rgba(205,79,134,0.10)] backdrop-blur-xl">
      <div className="mx-auto my-2 flex h-16 max-w-[96rem] items-center justify-between gap-5 rounded-[1.6rem] border border-[#ffd1e3]/80 bg-white/88 px-4 shadow-[0_12px_34px_rgba(205,79,134,0.10)] lg:px-6">
        <div className="flex min-w-0 items-center gap-8">
          <Link href="/" className="shrink-0">
            <Image
              src="/logo_new.png"
              alt="Logo"
              width={100}
              height={31}
              className="h-auto w-[5.64519rem] lg:w-[6.25rem]"
              priority
            />
          </Link>
          <DesktopNavLinks />
        </div>

        <SearchBar variant="nav" />

        <div className="flex shrink-0 items-center gap-2">
          {user && <ChatDock user={user} />}

          {user ? (
            <>
              <Link
                href={accountHref}
                className="hidden h-11 max-w-[12rem] items-center gap-2 rounded-full border border-[#ffd1e3] bg-[#fff0f7] px-4 text-sm font-black text-[#69324b] transition-colors hover:bg-[#ffe0ee] lg:inline-flex"
              >
                <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-[#f45f9c]" />
                <span className="truncate">{user.name}</span>
                {user.role === "ADMIN" && (
                  <span className="rounded-full bg-[#f45f9c] px-2 py-0.5 text-[0.65rem] font-black text-white">
                    ADMIN
                  </span>
                )}
              </Link>
              <button
                type="button"
                onClick={signOut}
                className="hidden h-11 items-center gap-2 rounded-full border border-[#ffd1e3] bg-white px-4 text-sm font-black text-[#e75491] transition-colors hover:bg-[#fff0f7] lg:inline-flex"
              >
                <LogOut className="h-4 w-4" />
                로그아웃
              </button>
            </>
          ) : (
            <button
              type="button"
              onClick={() => setIsLoginOpen(true)}
              className="hidden h-11 items-center justify-center rounded-full bg-[#f45f9c] px-5 text-sm font-black text-white shadow-[0_14px_30px_rgba(231,84,145,0.22)] transition-all hover:-translate-y-0.5 hover:bg-[#e75491] lg:inline-flex"
            >
              로그인
            </button>
          )}

          <button
            type="button"
            onClick={() => setIsMenuOpen(true)}
            className="flex h-10 w-10 cursor-pointer items-center justify-center rounded-full bg-[#fff0f7] text-[#e75491] ring-1 ring-[#ffd1e3] lg:hidden"
          >
            <Hamburger className="h-6 w-6" />
          </button>
          <MobileMenu
            isOpen={isMenuOpen}
            onClose={() => setIsMenuOpen(false)}
          />
        </div>
      </div>
      <LoginDialog isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
    </header>
  );
}
