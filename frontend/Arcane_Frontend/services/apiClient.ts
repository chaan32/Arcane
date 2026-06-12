import { API_URL, getApiUrl } from "@/constants/api";

const isAbsoluteUrl = (path: string) => /^https?:\/\//i.test(path);

export class ApiRequestError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
  }
}

const readApiErrorMessage = async (response: Response) => {
  try {
    const body = (await response.json()) as { message?: unknown };
    if (typeof body.message === "string" && body.message.trim().length > 0) {
      return body.message;
    }
  } catch {
    // Non-JSON error responses fall back to the HTTP status message.
  }

  return `HTTP error! status: ${response.status}`;
};

export const apiFetch = async (
  path: string,
  init?: RequestInit
): Promise<Response> => {
  const requestUrl = getApiUrl(path);

  try {
    return await fetch(requestUrl, init);
  } catch {
    if (isAbsoluteUrl(path)) {
      throw new Error(`요청 서버에 연결할 수 없습니다. URL(${requestUrl})을 확인해주세요.`);
    }

    throw new Error(
      `백엔드 서버에 연결할 수 없습니다. API 서버(${API_URL})가 실행 중인지 확인해주세요.`
    );
  }
};

export const apiJson = async <T>(
  path: string,
  init?: RequestInit
): Promise<T> => {
  const response = await apiFetch(path, init);

  if (!response.ok) {
    throw new ApiRequestError(
      response.status,
      await readApiErrorMessage(response)
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();

  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
};
