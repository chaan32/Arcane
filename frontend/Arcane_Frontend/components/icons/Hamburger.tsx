// 햄버거 아이콘

interface IconProps {
  className?: string;
}

export const Hamburger = ({ className }: IconProps) => (
  <svg
    className={className}
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
  >
    <path d="M4 7H20" stroke="currentColor" strokeLinecap="round" />
    <path d="M4 12H20" stroke="currentColor" strokeLinecap="round" />
    <path d="M4 17H20" stroke="currentColor" strokeLinecap="round" />
  </svg>
);
