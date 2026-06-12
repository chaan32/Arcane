export type AuthProvider = "google" | "naver";

export interface AuthUser {
  id: string;
  name: string;
  provider: AuthProvider;
  role?: "USER" | "ADMIN";
  loginId?: string;
  avatarUrl?: string;
}

export interface GuideChampion {
  id: number;
  nameEn: string;
  nameKo: string;
  imageFull: string;
}

export interface GuidePost {
  id: string;
  title: string;
  summary: string;
  champion: GuideChampion;
  markdown: string;
  coverImageUrl?: string;
  imageUrls: string[];
  author: AuthUser;
  createdAt: string;
  updatedAt: string;
  viewCount: number;
  commentCount: number;
}

export interface GuideComment {
  id: string;
  guideId: string;
  author: AuthUser;
  content: string;
  createdAt: string;
  likes: number;
  dislikes: number;
}

export interface ChatMessage {
  id: string;
  roomId: string;
  sender: AuthUser;
  content: string;
  createdAt: string;
  read?: boolean;
}

export interface ChatRoom {
  id: string;
  guideId: string;
  guideTitle: string;
  participants: AuthUser[];
  messages: ChatMessage[];
  blocked: boolean;
  blockedBy?: AuthUser;
  updatedAt: string;
}
