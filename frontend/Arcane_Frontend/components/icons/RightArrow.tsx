// 오른쪽 방향 아이콘

interface IconProps {
  className?: string;
}

export const RightArrow = ({ className }: IconProps) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    className={className}
  >
    <path
      d="M10 18L15.2929 12.7071C15.6834 12.3166 15.6834 11.6834 15.2929 11.2929L10 6"
      stroke="white"
      strokeLinecap="round"
    />
  </svg>
);
