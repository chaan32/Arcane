// 돋보기 아이콘

interface IconProps {
  className?: string;
}

export const Search = ({ className }: IconProps) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 16 16"
    fill="none"
    className={className}
  >
    <circle
      cx="6.66675"
      cy="6.66666"
      r="4"
      stroke="white"
      strokeWidth="0.666667"
    />
    <path
      d="M9.66675 9.66666L13.3334 13.3333"
      stroke="white"
      strokeWidth="0.666667"
      strokeLinecap="round"
    />
  </svg>
);
