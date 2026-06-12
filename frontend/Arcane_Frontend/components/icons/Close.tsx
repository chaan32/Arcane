// 닫기 아이콘
// 여러 곳에서 사용됨으로 색상도 받기
interface IconProps {
  className?: string;
}

export const Close = ({ className }: IconProps) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 20 20"
    fill="none"
    className={className}
  >
    <path
      d="M3.33325 3.33333L16.6666 16.6667"
      stroke="currentColor"
      strokeWidth="0.833333"
      strokeLinecap="round"
    />
    <path
      d="M16.6667 3.33333L3.33342 16.6667"
      stroke="currentColor"
      strokeWidth="0.833333"
      strokeLinecap="round"
    />
  </svg>
);
