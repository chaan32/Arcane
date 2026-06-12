import { Suspense } from "react";
import OAuthCallbackClient from "./OAuthCallbackClient";

export default function OAuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <main className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-16 text-[#69324b]">
          <section className="mx-auto max-w-[32rem] rounded-[2rem] border border-white/75 bg-white/94 px-8 py-10 text-center shadow-[0_26px_72px_rgba(205,79,134,0.16)] ring-1 ring-[#f8dce8]/60">
            <h1 className="text-2xl font-black">로그인 처리 중입니다</h1>
          </section>
        </main>
      }
    >
      <OAuthCallbackClient />
    </Suspense>
  );
}
