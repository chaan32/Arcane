import type { AuthProvider, AuthUser } from "@/types/community";
import { getApiUrl } from "@/constants/api";
import { apiFetch } from "@/services/apiClient";

export const AUTH_STORAGE_KEY = "arcane.auth.user";
export const AUTH_TOKEN_STORAGE_KEY = "arcane.auth.token";
export const AUTH_REDIRECT_STORAGE_KEY = "arcane.auth.redirectAfterLogin";
export const AUTH_CHANGE_EVENT = "arcane-auth-change";

interface OAuthLoginPayload {
  token: string;
  userId: string;
  loginId: string;
  nickName: string;
  role?: AuthUser["role"];
  provider?: AuthProvider;
}

const DEFAULT_PROVIDER_AVATAR: Record<AuthProvider, string> = {
  google: "/sad_mumu.png",
  naver: "/sad_lulu.png",
};

const isBrowser = () => typeof window !== "undefined";

const clearAuthStorage = () => {
  window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
  window.localStorage.removeItem(AUTH_REDIRECT_STORAGE_KEY);
};

export const isAuthErrorResponse = (status: number, data: unknown): boolean => {
  if (status !== 401 || typeof data !== "object" || data === null || !("code" in data)) {
    return false;
  }

  const code = String((data as { code?: unknown }).code);
  return code === "TOKEN_EXPIRED" || code === "INVALID_TOKEN";
};

export const signOutForAuthError = () => {
  if (!isBrowser()) return;
  clearAuthStorage();
  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
};

const guessProvider = (loginId: string): AuthProvider => {
  // 백엔드에서 OAuth 유저 loginId를 google_xxx / naver_xxx 형태로 만들고 있다.
  // provider 값을 따로 내려주지 않아도 프론트에서는 이 prefix로 어느 로그인인지 구분할 수 있다.
  if (loginId.startsWith("naver_")) return "naver";
  return "google";
};

export const getCurrentUser = (): AuthUser | null => {
  if (!isBrowser()) return null;

  const rawUser = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!rawUser) return null;

  try {
    return JSON.parse(rawUser) as AuthUser;
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
};

export const getAuthToken = (): string | null => {
  if (!isBrowser()) return null;
  return window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
};

export const startOAuthLogin = (provider: AuthProvider = "google") => {
  if (!isBrowser()) return;

  // 로그인 완료 후 사용자가 원래 보던 화면으로 돌아오게 하기 위해 현재 주소를 저장한다.
  // 예를 들어 /guides/new에서 로그인하면 OAuth 콜백 후 다시 /guides/new로 돌아간다.
  const currentPath = `${window.location.pathname}${window.location.search}`;
  window.localStorage.setItem(AUTH_REDIRECT_STORAGE_KEY, currentPath);

  // Spring Security OAuth2 기본 시작 URL이다.
  // 백엔드 SecurityConfig에서 /oauth2/**를 열어두고 google/naver registration을 연결해둔다.
  window.location.href = getApiUrl(`/oauth2/authorization/${provider}`);
};

export const startOAuthLink = async (provider: AuthProvider) => {
  if (!isBrowser()) return;

  const token = getAuthToken();
  if (!token) {
    throw new Error("로그인이 필요합니다.");
  }

  const response = await apiFetch(`/api/v1/user/oauth/link-intent/${provider}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
    },
    credentials: "include",
  });

  const data = (await response.json()) as {
    authorizationUrl?: string;
    code?: string;
    message?: string;
  };

  if (isAuthErrorResponse(response.status, data)) {
    signOutForAuthError();
    throw new Error(data.message ?? "로그인이 만료되었습니다. 다시 로그인해주세요.");
  }

  if (!response.ok || !data.authorizationUrl) {
    throw new Error(data.message ?? "소셜 계정 연동을 시작하지 못했습니다.");
  }

  window.localStorage.setItem(AUTH_REDIRECT_STORAGE_KEY, `/me?linked=${provider}`);
  window.location.href = getApiUrl(data.authorizationUrl);
};

export const saveOAuthLogin = ({ token, userId, loginId, nickName, role, provider: givenProvider }: OAuthLoginPayload): AuthUser => {
  const provider = givenProvider ?? guessProvider(loginId);
  const previousUser = getCurrentUser();
  const displayName =
    nickName ||
    (previousUser?.loginId === loginId ? previousUser.name : "") ||
    loginId;
  const user: AuthUser = {
    id: userId,
    loginId,
    name: displayName,
    provider,
    role: role ?? previousUser?.role ?? "USER",
    avatarUrl: DEFAULT_PROVIDER_AVATAR[provider],
  };

  // JWT는 API 요청 Authorization 헤더에 사용하고,
  // 유저 표기 정보는 Header/공략/채팅 UI에서 바로 읽을 수 있게 따로 저장한다.
  window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token);
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));

  return user;
};

export const updateStoredUserNickName = (nickName: string): AuthUser | null => {
  const currentUser = getCurrentUser();
  if (!currentUser) return null;

  const nextUser = {
    ...currentUser,
    name: nickName,
  };

  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextUser));
  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
  return nextUser;
};

export const updateStoredUserRole = (role: AuthUser["role"]): AuthUser | null => {
  const currentUser = getCurrentUser();
  if (!currentUser) return null;

  const nextUser = {
    ...currentUser,
    role,
  };

  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextUser));
  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
  return nextUser;
};

export const getRedirectAfterLogin = (): string => {
  if (!isBrowser()) return "/";

  const redirectPath = window.localStorage.getItem(AUTH_REDIRECT_STORAGE_KEY);
  window.localStorage.removeItem(AUTH_REDIRECT_STORAGE_KEY);

  // 콜백 페이지 자신으로 되돌아가는 상황은 막는다.
  if (!redirectPath || redirectPath.startsWith("/oauth/callback")) {
    return "/";
  }

  return redirectPath;
};

export const signOutMock = () => {
  clearAuthStorage();
  window.dispatchEvent(new Event(AUTH_CHANGE_EVENT));
};
