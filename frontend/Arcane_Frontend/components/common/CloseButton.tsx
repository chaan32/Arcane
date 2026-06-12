"use client";

import { cn } from "../../lib/utils";
import { Close } from "../icons/Close";

interface CloseButtonProps {
  className?: string;
  onClick?: () => void;
}

// 닫기 버튼은 여러곳에서 사용되어서 공용으로 만들고 크기를 받도록.
export default function CloseButton({ className, onClick }: CloseButtonProps) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "flex items-center justify-center bg-gray-200 rounded-[0.75rem] p-[0.38rem] text-text-default cursor-pointer",
        className
      )}
    >
      <Close className="w-[8.25rem] h-[8.25rem]" />
    </button>
  );
}
