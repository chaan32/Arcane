import { API_URL } from "@/constants/api";
import { apiFetch } from "@/services/apiClient";
import { getDataDragonChampionIconUrl } from "@/services/dataDragonApi";
import { getAuthToken, isAuthErrorResponse, signOutForAuthError } from "@/lib/mockAuth";
import type { AuthUser, ChatRoom, GuideComment, GuidePost } from "@/types/community";

export const COMMUNITY_CHAT_CHANGE_EVENT = "arcane-community-chat-change";

type BackendGuide = {
  id: number;
  title: string;
  content?: string;
  summary?: string;
  authorName: string;
  authorId: number;
  championId: number;
  championNameKo: string;
  championNameEn: string;
  championImageFull: string;
  views?: number;
  createdAt: string;
  updatedAt?: string;
};

type BackendGuideUpload = {
  id: number;
  title: string;
  authorName: string;
  authorId: number;
  championId: number;
};

type BackendGuideSearchResponse = {
  engine: "database" | "elasticsearch";
  keyword: string;
  limit: number;
  elapsedMs: number;
  totalHits: number;
  results: BackendGuide[];
};

type BackendComment = {
  id: number;
  authorName: string;
  authorId?: number;
  content: string;
  createdAt: string;
  likes?: number;
  dislikes?: number;
};

type BackendUserBrief = {
  id: number;
  name: string;
  profileImage?: string | null;
};

type BackendChatMessage = {
  id: number;
  roomId: number;
  sender: BackendUserBrief;
  content: string;
  createdAt: string;
  read?: boolean;
};

type BackendChatRoom = {
  id: number;
  guideId: number;
  guideTitle: string;
  participants: BackendUserBrief[];
  messages: BackendChatMessage[];
  blocked?: boolean;
  blockedBy?: BackendUserBrief | null;
  updatedAt: string;
};

const emitChatChange = () => {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(COMMUNITY_CHAT_CHANGE_EVENT));
    window.localStorage.setItem(`${COMMUNITY_CHAT_CHANGE_EVENT}.at`, String(Date.now()));
  }
};

const authHeaders = () => {
  const token = getAuthToken();
  if (!token) {
    throw new Error("로그인이 필요합니다.");
  }

  return {
    Authorization: `Bearer ${token}`,
  };
};

const requestJson = async <T>(path: string, init?: RequestInit): Promise<T> => {
  let response: Response;

  try {
    response = await apiFetch(path, init);
  } catch {
    throw new Error(`백엔드 서버에 연결할 수 없습니다. API 서버(${API_URL})가 실행 중인지 확인해주세요.`);
  }

  const contentType = response.headers.get("content-type") ?? "";
  const data = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    if (isAuthErrorResponse(response.status, data)) {
      signOutForAuthError();
    }

    const message =
      typeof data === "object" && data !== null && "message" in data
        ? String((data as { message?: unknown }).message)
        : "요청을 처리하지 못했습니다.";
    throw new Error(message);
  }

  return data as T;
};

const toAuthUser = (user: BackendUserBrief): AuthUser => ({
  id: String(user.id),
  name: user.name,
  provider: "google",
  avatarUrl: user.profileImage ?? undefined,
});

const toGuidePost = (guide: BackendGuide): GuidePost => {
  const championImageFull =
    guide.championImageFull || `${guide.championNameEn}.png`;

  return {
    id: String(guide.id),
    title: guide.title,
    summary: guide.summary || guide.content || "",
    champion: {
      id: guide.championId,
      nameKo: guide.championNameKo,
      nameEn: guide.championNameEn,
      imageFull: championImageFull,
    },
    markdown: guide.content ?? "",
    coverImageUrl: getDataDragonChampionIconUrl(championImageFull),
    imageUrls: [],
    author: {
      id: String(guide.authorId),
      name: guide.authorName,
      provider: "google",
    },
    createdAt: guide.createdAt,
    updatedAt: guide.updatedAt ?? guide.createdAt,
    viewCount: guide.views ?? 0,
    commentCount: 0,
  };
};

const toGuideComment = (guideId: string, comment: BackendComment): GuideComment => ({
  id: String(comment.id),
  guideId,
  content: comment.content,
  createdAt: comment.createdAt,
  likes: comment.likes ?? 0,
  dislikes: comment.dislikes ?? 0,
  author: {
    id: String(comment.authorId ?? comment.authorName),
    name: comment.authorName,
    provider: "google",
  },
});

const toChatRoom = (room: BackendChatRoom): ChatRoom => ({
  id: String(room.id),
  guideId: String(room.guideId),
  guideTitle: room.guideTitle,
  participants: room.participants.map(toAuthUser),
  blocked: Boolean(room.blocked),
  blockedBy: room.blockedBy ? toAuthUser(room.blockedBy) : undefined,
  updatedAt: room.updatedAt,
  messages: room.messages.map((message) => ({
    id: String(message.id),
    roomId: String(message.roomId),
    sender: toAuthUser(message.sender),
    content: message.content,
    createdAt: message.createdAt,
    read: message.read,
  })),
});

export const fetchGuidePosts = async (): Promise<GuidePost[]> => {
  const guides = await requestJson<BackendGuide[]>("/api/v1/strategy/find/all", {
    cache: "no-store",
  });
  return guides.map(toGuidePost);
};

export const searchGuidePosts = async (
  keyword: string,
  engine: "database" | "elasticsearch" = "elasticsearch",
  limit = 50
): Promise<GuidePost[]> => {
  const params = new URLSearchParams({
    keyword,
    limit: String(limit),
  });
  const path =
    engine === "database"
      ? `/api/v1/strategy/search/db?${params.toString()}`
      : `/api/v1/strategy/search/elasticsearch?${params.toString()}`;
  const response = await requestJson<BackendGuideSearchResponse>(path, {
    cache: "no-store",
  });
  return response.results.map(toGuidePost);
};

export const fetchGuidePost = async (guideId: string): Promise<GuidePost> => {
  const guide = await requestJson<BackendGuide>(`/api/v1/strategy/find/${guideId}`, {
    cache: "no-store",
  });
  return toGuidePost(guide);
};

export const createGuidePost = async (input: {
  title: string;
  championId: number;
  markdown: string;
}): Promise<BackendGuideUpload> =>
  requestJson<BackendGuideUpload>("/api/v1/strategy/upload", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
    },
    body: JSON.stringify({
      title: input.title,
      championId: input.championId,
      content: input.markdown,
    }),
  });

export const fetchGuideComments = async (guideId: string): Promise<GuideComment[]> => {
  const comments = await requestJson<BackendComment[]>(`/api/v1/comment/${guideId}/guide`, {
    cache: "no-store",
  });
  return comments.map((comment) => toGuideComment(guideId, comment));
};

export const createGuideComment = async (
  guideId: string,
  content: string
): Promise<GuideComment> => {
  const comment = await requestJson<BackendComment>(`/api/v1/comment/write/guide/${guideId}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
    },
    body: JSON.stringify({ content }),
  });
  return toGuideComment(guideId, comment);
};

export const likeGuideComment = async (commentId: string): Promise<void> => {
  await requestJson<{ message: string }>(`/api/v1/comment/show/${commentId}/likes`, {
    method: "POST",
  });
};

export const dislikeGuideComment = async (commentId: string): Promise<void> => {
  await requestJson<{ message: string }>(`/api/v1/comment/show/${commentId}/dislikes`, {
    method: "POST",
  });
};

export const fetchMyChatRooms = async (): Promise<ChatRoom[]> => {
  const rooms = await requestJson<BackendChatRoom[]>("/api/v1/chat/rooms", {
    headers: authHeaders(),
    cache: "no-store",
  });
  return rooms.map(toChatRoom);
};

export const openGuideChatRoom = async (guideId: string): Promise<ChatRoom> => {
  const room = await requestJson<BackendChatRoom>(`/api/v1/chat/guides/${guideId}/room`, {
    method: "POST",
    headers: authHeaders(),
  });
  return toChatRoom(room);
};

export const sendChatMessage = async (
  roomId: string,
  content: string
): Promise<ChatRoom> => {
  const room = await requestJson<BackendChatRoom>(`/api/v1/chat/rooms/${roomId}/messages`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
    },
    body: JSON.stringify({ content }),
  });
  emitChatChange();
  return toChatRoom(room);
};

export const blockChatRoom = async (roomId: string): Promise<ChatRoom> => {
  const room = await requestJson<BackendChatRoom>(`/api/v1/chat/rooms/${roomId}/block`, {
    method: "POST",
    headers: authHeaders(),
  });
  emitChatChange();
  return toChatRoom(room);
};

export const deleteChatRoomFromList = async (roomId: string): Promise<void> => {
  await requestJson<void>(`/api/v1/chat/rooms/${roomId}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  emitChatChange();
};
