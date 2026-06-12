import Link from "next/link";

const NAV_ITEMS = [
  { id: 1, label: "챔피언 분석", href: "/champions", variant: "default" },
  { id: 2, label: "소환사 랭킹", href: "/ranking", variant: "default" },
  { id: 3, label: "공략", href: "/guides", variant: "default" },
  { id: 4, label: "패치노트", href: "/patch-notes", variant: "default" },
];

export const DesktopNavLinks = () => {
  return (
    <nav className="hidden items-center gap-8 lg:flex">
      {NAV_ITEMS.map((item) => (
        <Link
          key={item.id}
          href={item.href}
          className="rounded-full px-1 py-2 text-sm font-black text-[#69324b] transition-colors hover:text-[#e75491]"
        >
          {item.label}
        </Link>
      ))}
    </nav>
  );
};
