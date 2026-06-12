import { ReactNode } from "react";

interface SkillTooltipProps {
  children: ReactNode;
  title: string;
  description: string;
  additionalInfo?: string;
}

// 임시로 만든 스킬 툴팁 (추후 디자인 확정되면 수정 필요)
export function SkillTooltip({
  children,
  title,
  description,
  additionalInfo,
}: SkillTooltipProps) {
  return (
    <div className="relative group">
      {children}
      {/* 툴팁 */}
      <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-3 w-[18rem] p-4 bg-gray-900 border border-blue-300 rounded-[0.25rem] opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-50 pointer-events-none">
        <div className="text-d-body3 text-blue-400 mb-2">{title}</div>
        <div
          className="text-d-body4-r text-white mb-2"
          dangerouslySetInnerHTML={{ __html: description }}
        />
        {additionalInfo && (
          <div className="text-d-body4-r text-gray-500">{additionalInfo}</div>
        )}
        {/* 화살표 */}
        <div className="absolute top-full left-1/2 -translate-x-1/2 w-0 h-0 border-l-[6px] border-l-transparent border-r-[6px] border-r-transparent border-t-[6px] border-t-blue-300" />
      </div>
    </div>
  );
}
