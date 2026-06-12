// 패치노트 아이콘

interface IconProps {
  className?: string;
}

export const PatchNote = ({ className }: IconProps) => (
  <svg
    className={className}
    xmlns="http://www.w3.org/2000/svg"
    width="16"
    height="16"
    viewBox="0 0 16 16"
    fill="none"
  >
    <path
      d="M8.66667 2H2.33333C2.14924 2 2 2.14924 2 2.33333V13.6667C2 13.8508 2.14924 14 2.33333 14H13.6667C13.8508 14 14 13.8508 14 13.6667V7.33333"
      stroke="currentColor"
      strokeWidth="0.666667"
      strokeLinecap="round"
    />
    <path
      d="M13.0001 3L8.66675 7.33333"
      stroke="currentColor"
      strokeWidth="0.666667"
      strokeLinecap="round"
    />
    <path
      d="M10.6667 2.66666H13.0001C13.1842 2.66666 13.3334 2.81589 13.3334 2.99999V5.33332"
      stroke="currentColor"
      strokeWidth="0.666667"
      strokeLinecap="round"
    />
  </svg>
);
