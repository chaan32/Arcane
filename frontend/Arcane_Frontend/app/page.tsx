"use client";

import Image from "next/image";
import SearchBar from "@/components/search/SearchBar";

export default function Home() {
  return (
    <main className="relative flex min-h-[calc(100vh-5rem)] flex-col items-center overflow-hidden bg-[radial-gradient(circle_at_top_right,_#ffe0ee_0%,_#fff6fb_42%,_#fffafd_100%)] px-4 py-8 text-[#69324b] lg:justify-start lg:py-10">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-40 bg-[linear-gradient(180deg,_rgba(244,95,156,0.15),_rgba(255,246,251,0))]" />
      <section className="relative mx-auto flex w-full max-w-[76rem] flex-col items-center px-5 py-6 lg:py-8">
        <div className="relative -mb-24 lg:-mb-36">
          <div className="hidden lg:block">
            <Image
              src="/logo_new.png"
              alt="ARCANE Title"
              width={564}
              height={385}
              priority
            />
          </div>

          <div className="block lg:hidden">
            <Image
              src="/logo_new.png"
              alt="ARCANE Title"
              width={351}
              height={240}
              priority
            />
          </div>
        </div>

        <div className="w-full">
          <SearchBar />
        </div>

      </section>
    </main>
  );
}
