"use client";

import Image, {
  type ImageLoaderProps,
  type ImageProps,
} from "next/image";
import { type ReactNode, useEffect, useState } from "react";

type ExternalImageProps = Omit<ImageProps, "loader" | "src"> & {
  fallback?: ReactNode;
  src?: string | null;
};

const passthroughLoader = ({ src }: ImageLoaderProps) => src;

const isRenderableImageSrc = (src: string) =>
  src.startsWith("/") ||
  src.startsWith("http://") ||
  src.startsWith("https://");

const isRemoteImageSrc = (src: string) =>
  src.startsWith("http://") || src.startsWith("https://");

const isDefaultOptimizedRemoteSrc = (src: string) => {
  try {
    const { hostname } = new URL(src);

    return (
      hostname === "ddragon.leagueoflegends.com" ||
      hostname === "raw.communitydragon.org"
    );
  } catch {
    return false;
  }
};

export function ExternalImage({
  alt,
  fallback = null,
  onError,
  src,
  unoptimized,
  ...props
}: ExternalImageProps) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null);

  useEffect(() => {
    setFailedSrc(null);
  }, [src]);

  if (!src || !isRenderableImageSrc(src) || failedSrc === src) {
    return <>{fallback}</>;
  }

  const usePassthroughLoader =
    isRemoteImageSrc(src) && !isDefaultOptimizedRemoteSrc(src);

  const imageProps = usePassthroughLoader
    ? { loader: passthroughLoader, unoptimized: true }
    : { unoptimized };

  return (
    <Image
      {...props}
      {...imageProps}
      alt={alt}
      src={src}
      onError={(event) => {
        setFailedSrc(src);
        onError?.(event);
      }}
    />
  );
}
