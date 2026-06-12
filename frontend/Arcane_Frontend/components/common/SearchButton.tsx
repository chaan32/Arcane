"use client";

import { Search } from "lucide-react";
import { cn } from "../../lib/utils";

interface SearchButtonProps {
  className?: string;
  onClick?: () => void;
  type?: "button" | "submit";
}

export default function SearchButton({
  className,
  onClick,
  type = "button",
}: SearchButtonProps) {
  return (
    <button
      type={type}
      onClick={onClick}
      className={cn(
        "flex items-center justify-center bg-blue-300 rounded-full cursor-pointer text-white shadow-[0_12px_24px_rgba(231,84,145,0.22)] transition-all hover:-translate-y-0.5 hover:bg-[#e75491]",
        className
      )}
    >
      <Search className="w-[1rem] h-[1rem] lg:w-[1.75rem] lg:h-[1.75rem]" />
    </button>
  );
}
