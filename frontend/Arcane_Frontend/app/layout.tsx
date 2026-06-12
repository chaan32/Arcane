import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";
import Header from "../components/layout/Header";
import { Footer } from "../components/layout/Footer";
import ReactQueryProvider from "@/providers/ReactQueryProvider";

export const metadata: Metadata = {
  title: "ARCANE | 아케인",
  description: "아케인 | 리그오브레전드 전적 검색",
  icons: {
    icon: "/favicon2.png",
  },
};

// 폰트 로컬로 처리
const pretendard = localFont({
  src: "./fonts/PretendardVariable.woff2",
  display: "swap",
  weight: "45 920",
  variable: "--font-pretendard",
});

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${pretendard.variable}`}>
      <head>
        <link rel="dns-prefetch" href="https://ddragon.leagueoflegends.com" />
        <link rel="dns-prefetch" href="https://raw.communitydragon.org" />
        <link rel="preconnect" href="https://ddragon.leagueoflegends.com" />
        <link rel="preconnect" href="https://raw.communitydragon.org" />
      </head>
      <body>
        <ReactQueryProvider>
          <Header />
          {/* 일단은 데스크탑만 고려 */}
          <main className="lg:pt-[5rem]">{children}</main>
          <Footer />
        </ReactQueryProvider>
      </body>
    </html>
  );
}
