import Image from "next/image";

type MatchImageProps = {
  alt: string;
  className?: string;
  height: number;
  placeholderClassName?: string;
  sizes?: string;
  src?: string | null;
  width: number;
};

export function MatchImage({
  alt,
  className,
  height,
  placeholderClassName,
  sizes,
  src,
  width,
}: MatchImageProps) {
  if (!src) {
    return (
      <div
        aria-label={alt}
        className={placeholderClassName ?? className}
        role="img"
        style={{ width, height }}
      />
    );
  }

  return (
    <Image
      alt={alt}
      className={className}
      height={height}
      sizes={sizes ?? `${width}px`}
      src={src}
      width={width}
    />
  );
}
