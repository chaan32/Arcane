// 위 아래 같이 있는 아이콘

interface IconProps {
  className?: string;
}

export const UpDownArrow = ({ className }: IconProps) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="24"
    height="24"
    viewBox="0 0 24 24"
    fill="none"
    className={className}
  >
    <path
      d="M11.3492 18.4422L7.05247 14.7593C6.34757 14.1551 6.77485 13 7.70326 13H16.2967C17.2251 13 17.6524 14.1551 16.9475 14.7593L12.6508 18.4422C12.2763 18.7632 11.7237 18.7632 11.3492 18.4422Z"
      fill="#CDF2FE"
    />
    <path
      d="M11.3492 5.55782L7.05247 9.24074C6.34757 9.84494 6.77485 11 7.70326 11H16.2967C17.2251 11 17.6524 9.84494 16.9475 9.24074L12.6508 5.55782C12.2763 5.23683 11.7237 5.23683 11.3492 5.55782Z"
      fill="#374449"
    />
  </svg>
);
