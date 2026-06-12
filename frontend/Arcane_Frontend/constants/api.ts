const DEFAULT_API_URL = "http://localhost:8080";

const trimTrailingSlash = (value: string) => value.replace(/\/+$/, "");
const trimLeadingSlash = (value: string) => value.replace(/^\/+/, "");

export const API_URL = trimTrailingSlash(
  process.env.NEXT_PUBLIC_API_URL?.trim() || DEFAULT_API_URL
);

export const getApiUrl = (path: string) => {
  if (/^https?:\/\//i.test(path)) {
    return path;
  }

  return `${API_URL}/${trimLeadingSlash(path)}`;
};
