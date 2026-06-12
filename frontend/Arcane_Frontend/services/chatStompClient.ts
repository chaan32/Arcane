import { Client } from "@stomp/stompjs";
import { API_URL } from "@/constants/api";
import { signOutForAuthError } from "@/lib/mockAuth";
import type { AuthUser, ChatMessage, ChatRoom } from "@/types/community";

type BackendUserBrief = {
  id: number | string;
  name: string;
  profileImage?: string | null;
};

type BackendChatMessage = {
  id: number | string;
  roomId: number | string;
  sender: BackendUserBrief;
  content: string;
  createdAt: string;
  read?: boolean;
};

type BackendChatRoom = {
  id: number | string;
  guideId: number | string;
  guideTitle: string;
  participants: BackendUserBrief[];
  messages: BackendChatMessage[];
  blocked?: boolean;
  blockedBy?: BackendUserBrief | null;
  updatedAt: string;
};

export type ChatSocketPayload =
  | { kind: "message"; message: ChatMessage }
  | { kind: "room"; room: ChatRoom };

type CreateChatStompClientOptions = {
  token: string;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (message: string) => void;
};

const CHAT_SOCKET_PATH = "/ws/chat";

const toSocketUrl = () => {
  const baseUrl = API_URL.replace(/\/$/, "");
  const socketBaseUrl = baseUrl.replace(/^https?:/, (protocol) =>
    protocol === "https:" ? "wss:" : "ws:"
  );

  return `${socketBaseUrl}${CHAT_SOCKET_PATH}`;
};

const toAuthUser = (user: BackendUserBrief): AuthUser => ({
  id: String(user.id),
  name: user.name,
  provider: "google",
  avatarUrl: user.profileImage ?? undefined,
});

const toChatMessage = (message: BackendChatMessage): ChatMessage => ({
  id: String(message.id),
  roomId: String(message.roomId),
  sender: toAuthUser(message.sender),
  content: message.content,
  createdAt: message.createdAt,
  read: message.read,
});

const toChatRoom = (room: BackendChatRoom): ChatRoom => ({
  id: String(room.id),
  guideId: String(room.guideId),
  guideTitle: room.guideTitle,
  participants: room.participants.map(toAuthUser),
  messages: room.messages.map(toChatMessage),
  blocked: Boolean(room.blocked),
  blockedBy: room.blockedBy ? toAuthUser(room.blockedBy) : undefined,
  updatedAt: room.updatedAt,
});

const isObject = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null;

const isBackendMessage = (value: unknown): value is BackendChatMessage =>
  isObject(value) &&
  "id" in value &&
  "roomId" in value &&
  "sender" in value &&
  "content" in value &&
  "createdAt" in value;

const isBackendRoom = (value: unknown): value is BackendChatRoom =>
  isObject(value) &&
  "id" in value &&
  "guideId" in value &&
  "guideTitle" in value &&
  Array.isArray(value.participants) &&
  Array.isArray(value.messages);

const isAuthSocketError = (message: string) => {
  const lowerMessage = message.toLowerCase();
  return (
    lowerMessage.includes("token") ||
    lowerMessage.includes("jwt") ||
    message.includes("인증") ||
    message.includes("토큰")
  );
};

export const parseChatSocketPayload = (body: string): ChatSocketPayload | null => {
  try {
    const parsedBody: unknown = JSON.parse(body);

    if (isBackendRoom(parsedBody)) {
      return { kind: "room", room: toChatRoom(parsedBody) };
    }

    if (isBackendMessage(parsedBody)) {
      return { kind: "message", message: toChatMessage(parsedBody) };
    }

    return null;
  } catch {
    return null;
  }
};

export const createChatStompClient = ({
  token,
  onConnect,
  onDisconnect,
  onError,
}: CreateChatStompClientOptions) =>
  new Client({
    brokerURL: toSocketUrl(),
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => undefined,
    onConnect,
    onDisconnect,
    onWebSocketClose: onDisconnect,
    onWebSocketError: () => {
      onError?.("채팅 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    },
    onStompError: (frame) => {
      const message =
        frame.headers.message || frame.body || "채팅 서버에서 메시지를 처리하지 못했습니다.";

      if (isAuthSocketError(message)) {
        signOutForAuthError();
      }

      onError?.(message);
    },
  });
