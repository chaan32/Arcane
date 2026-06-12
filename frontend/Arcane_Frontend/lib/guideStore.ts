"use client";

import type {
  AuthUser,
  ChatMessage,
  ChatRoom,
  GuideComment,
  GuidePost,
} from "@/types/community";
import { getDataDragonChampionIconUrl } from "@/services/dataDragonApi";

const GUIDE_STORAGE_KEY = "arcane.guides";
const GUIDE_COMMENT_STORAGE_KEY = "arcane.guide.comments";
const CHAT_ROOM_STORAGE_KEY = "arcane.chat.rooms";
export const CHAT_CHANGE_EVENT = "arcane-chat-change";

const isBrowser = () => typeof window !== "undefined";

const createId = (prefix: string) =>
  `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

const readJson = <T,>(key: string, fallback: T): T => {
  if (!isBrowser()) return fallback;

  const rawValue = window.localStorage.getItem(key);
  if (!rawValue) return fallback;

  try {
    return JSON.parse(rawValue) as T;
  } catch {
    window.localStorage.removeItem(key);
    return fallback;
  }
};

const writeJson = <T,>(key: string, value: T) => {
  if (!isBrowser()) return;
  window.localStorage.setItem(key, JSON.stringify(value));
};

const seedAuthor: AuthUser = {
  id: "arcane-editor",
  name: "Arcane 운영자",
  provider: "google",
  avatarUrl: "/sad_yumi.png",
};

const seedGuides: GuidePost[] = [
  {
    id: "guide-seed-aurora",
    title: "초반 라인전을 안정적으로 넘기는 오로라 운영",
    summary: "상대 정글 동선을 체크하면서 6레벨 전까지 손해를 줄이는 운영 메모.",
    champion: {
      id: 893,
      nameEn: "Aurora",
      nameKo: "오로라",
      imageFull: "Aurora.png",
    },
    markdown:
      "## 핵심 운영\n\n- 초반에는 라인을 과하게 밀지 않고 시야를 먼저 잡습니다.\n- 궁극기 이후 교전은 상대 이동기 소모를 보고 시작합니다.\n\n### 추천 상황\n\n상대가 진입형 조합일 때 받아치기 좋습니다.",
    coverImageUrl: getDataDragonChampionIconUrl("Aurora.png"),
    imageUrls: [],
    author: seedAuthor,
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 12).toISOString(),
    updatedAt: new Date(Date.now() - 1000 * 60 * 60 * 12).toISOString(),
    viewCount: 128,
    commentCount: 0,
  },
];

const ensureGuides = () => {
  const currentGuides = readJson<GuidePost[]>(GUIDE_STORAGE_KEY, []);

  if (currentGuides.length === 0) {
    writeJson(GUIDE_STORAGE_KEY, seedGuides);
    return seedGuides;
  }

  return currentGuides;
};

export const getGuides = (): GuidePost[] =>
  ensureGuides().sort(
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
  );

export const getGuideById = (guideId: string): GuidePost | null =>
  getGuides().find((guide) => guide.id === guideId) ?? null;

export const saveGuide = (
  input: Pick<
    GuidePost,
    "title" | "summary" | "champion" | "markdown" | "coverImageUrl" | "imageUrls"
  >,
  author: AuthUser
): GuidePost => {
  const now = new Date().toISOString();
  const guide: GuidePost = {
    id: createId("guide"),
    ...input,
    author,
    createdAt: now,
    updatedAt: now,
    viewCount: 0,
    commentCount: 0,
  };

  writeJson(GUIDE_STORAGE_KEY, [guide, ...getGuides()]);

  return guide;
};

export const increaseGuideView = (guideId: string) => {
  const guides = getGuides().map((guide) =>
    guide.id === guideId
      ? { ...guide, viewCount: guide.viewCount + 1 }
      : guide
  );

  writeJson(GUIDE_STORAGE_KEY, guides);
};

export const getGuideComments = (guideId: string): GuideComment[] =>
  readJson<GuideComment[]>(GUIDE_COMMENT_STORAGE_KEY, [])
    .filter((comment) => comment.guideId === guideId)
    .sort(
      (a, b) =>
        new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    );

export const addGuideComment = (
  guideId: string,
  content: string,
  author: AuthUser
): GuideComment => {
  const comment: GuideComment = {
    id: createId("comment"),
    guideId,
    content,
    author,
    createdAt: new Date().toISOString(),
    likes: 0,
    dislikes: 0,
  };
  const comments = readJson<GuideComment[]>(GUIDE_COMMENT_STORAGE_KEY, []);

  writeJson(GUIDE_COMMENT_STORAGE_KEY, [...comments, comment]);
  writeJson(
    GUIDE_STORAGE_KEY,
    getGuides().map((guide) =>
      guide.id === guideId
        ? { ...guide, commentCount: guide.commentCount + 1 }
        : guide
    )
  );

  return comment;
};

const emitChatChange = () => {
  if (isBrowser()) {
    window.dispatchEvent(new Event(CHAT_CHANGE_EVENT));
  }
};

export const getChatRooms = (userId?: string): ChatRoom[] => {
  const rooms = readJson<ChatRoom[]>(CHAT_ROOM_STORAGE_KEY, []);

  if (!userId) return [];

  return rooms
    .filter((room) =>
      room.participants.some((participant) => participant.id === userId)
    )
    .sort(
      (a, b) =>
        new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    );
};

export const getChatRoomById = (roomId: string): ChatRoom | null =>
  readJson<ChatRoom[]>(CHAT_ROOM_STORAGE_KEY, []).find(
    (room) => room.id === roomId
  ) ?? null;

export const getOrCreateGuideChatRoom = (
  guide: GuidePost,
  reader: AuthUser
): ChatRoom => {
  const rooms = readJson<ChatRoom[]>(CHAT_ROOM_STORAGE_KEY, []);
  const existingRoom = rooms.find(
    (room) =>
      room.guideId === guide.id &&
      room.participants.some((participant) => participant.id === reader.id) &&
      room.participants.some((participant) => participant.id === guide.author.id)
  );

  if (existingRoom) return existingRoom;

  const now = new Date().toISOString();
  const room: ChatRoom = {
    id: createId("room"),
    guideId: guide.id,
    guideTitle: guide.title,
    participants:
      reader.id === guide.author.id ? [guide.author] : [guide.author, reader],
    messages: [],
    blocked: false,
    updatedAt: now,
  };

  writeJson(CHAT_ROOM_STORAGE_KEY, [room, ...rooms]);
  emitChatChange();

  return room;
};

export const addChatMessage = (
  roomId: string,
  sender: AuthUser,
  content: string
): ChatRoom | null => {
  const rooms = readJson<ChatRoom[]>(CHAT_ROOM_STORAGE_KEY, []);
  const now = new Date().toISOString();
  let updatedRoom: ChatRoom | null = null;
  const nextRooms = rooms.map((room) => {
    if (room.id !== roomId) return room;

    const message: ChatMessage = {
      id: createId("message"),
      roomId,
      sender,
      content,
      createdAt: now,
    };

    updatedRoom = {
      ...room,
      messages: [...room.messages, message],
      updatedAt: now,
    };

    return updatedRoom;
  });

  writeJson(CHAT_ROOM_STORAGE_KEY, nextRooms);
  emitChatChange();

  return updatedRoom;
};
