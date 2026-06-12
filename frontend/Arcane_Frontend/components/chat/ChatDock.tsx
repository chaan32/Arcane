"use client";

import { Ban, MessageCircle, Send, Trash2, X } from "lucide-react";
import type { Client, StompSubscription } from "@stomp/stompjs";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  blockChatRoom,
  COMMUNITY_CHAT_CHANGE_EVENT,
  deleteChatRoomFromList,
  fetchMyChatRooms,
} from "@/services/communityApi";
import {
  createChatStompClient,
  parseChatSocketPayload,
} from "@/services/chatStompClient";
import { getAuthToken } from "@/lib/mockAuth";
import type { AuthUser, ChatMessage, ChatRoom } from "@/types/community";

interface ChatDockProps {
  user: AuthUser;
}

type ChatReadState = Record<string, string>;

const CHAT_READ_STORAGE_PREFIX = "arcane.chat.read";

const formatChatTime = (value: string) =>
  new Intl.DateTimeFormat("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));

const getReadStorageKey = (userId: string) => `${CHAT_READ_STORAGE_PREFIX}.${userId}`;

const readChatReadState = (userId: string): ChatReadState => {
  if (typeof window === "undefined") return {};

  try {
    const savedValue = window.localStorage.getItem(getReadStorageKey(userId));
    if (!savedValue) return {};

    const parsedValue = JSON.parse(savedValue);
    return parsedValue && typeof parsedValue === "object" ? parsedValue : {};
  } catch {
    return {};
  }
};

const writeChatReadState = (userId: string, readState: ChatReadState) => {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(getReadStorageKey(userId), JSON.stringify(readState));
};

const hasMessages = (room: ChatRoom) => room.messages.length > 0;

const countUnreadReceivedMessages = (
  room: ChatRoom,
  userId: string,
  readState: ChatReadState
) => {
  const lastReadAt = readState[room.id] ? new Date(readState[room.id]).getTime() : 0;

  return room.messages.filter((chatMessage) => {
    if (chatMessage.sender.id === userId) return false;

    if (chatMessage.read === true) return false;

    const messageCreatedAt = new Date(chatMessage.createdAt).getTime();
    if (lastReadAt > 0 && messageCreatedAt <= lastReadAt) return false;

    return chatMessage.read === false || lastReadAt === 0;
  }).length;
};

const getLastReceivedMessage = (room: ChatRoom, userId: string) =>
  [...room.messages].reverse().find((chatMessage) => chatMessage.sender.id !== userId);

const isSameChatRoom = (currentRoom: ChatRoom, nextRoom: ChatRoom) =>
  currentRoom.id === nextRoom.id &&
  currentRoom.updatedAt === nextRoom.updatedAt &&
  currentRoom.blocked === nextRoom.blocked &&
  currentRoom.messages.length === nextRoom.messages.length &&
  currentRoom.messages.every((currentMessage, index) => {
    const nextMessage = nextRoom.messages[index];
    return (
      nextMessage &&
      currentMessage.id === nextMessage.id &&
      currentMessage.content === nextMessage.content &&
      currentMessage.createdAt === nextMessage.createdAt &&
      currentMessage.sender.id === nextMessage.sender.id &&
      currentMessage.read === nextMessage.read
    );
  });

export default function ChatDock({ user }: ChatDockProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [activeRoomId, setActiveRoomId] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [isMutatingRoom, setIsMutatingRoom] = useState(false);
  const [dockError, setDockError] = useState("");
  const [readState, setReadState] = useState<ChatReadState>({});
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const roomSubscriptionsRef = useRef<Record<string, StompSubscription>>({});
  const isComposingRef = useRef(false);
  const readPublishStateRef = useRef<Record<string, string>>({});
  const [isSocketConnected, setIsSocketConnected] = useState(false);

  const upsertRoom = useCallback((room: ChatRoom) => {
    setRooms((currentRooms) => [
      room,
      ...currentRooms.filter((currentRoom) => currentRoom.id !== room.id),
    ]);
  }, []);

  const markRoomAsReadUntil = useCallback(
    (roomId: string, readAt: string) => {
      setReadState((currentReadState) => {
        if (currentReadState[roomId] === readAt) {
          return currentReadState;
        }

        const nextReadState = {
          ...currentReadState,
          [roomId]: readAt,
        };
        writeChatReadState(user.id, nextReadState);
        return nextReadState;
      });
    },
    [user.id]
  );

  const publishRoomRead = useCallback((roomId: string, readTargetMessageId?: string) => {
    const client = stompClientRef.current;
    if (!client?.connected) return;
    if (readTargetMessageId && readPublishStateRef.current[roomId] === readTargetMessageId) {
      return;
    }

    client.publish({
      destination: `/pub/chat/rooms/${roomId}/read`,
      body: "{}",
    });

    if (readTargetMessageId) {
      readPublishStateRef.current[roomId] = readTargetMessageId;
    }
  }, []);

  const applySocketRoom = useCallback(
    (nextRoom: ChatRoom) => {
      setRooms((currentRooms) => {
        const currentRoom = currentRooms.find((room) => room.id === nextRoom.id);
        if (currentRoom && isSameChatRoom(currentRoom, nextRoom)) {
          return currentRooms;
        }

        return [
          nextRoom,
          ...currentRooms.filter((currentRoom) => currentRoom.id !== nextRoom.id),
        ];
      });

      const lastMessage = nextRoom.messages[nextRoom.messages.length - 1];
      if (isOpen && activeRoomId === nextRoom.id && lastMessage) {
        markRoomAsReadUntil(nextRoom.id, lastMessage.createdAt);
      }
    },
    [activeRoomId, isOpen, markRoomAsReadUntil]
  );

  const appendSocketMessage = useCallback(
    (nextMessage: ChatMessage) => {
      setRooms((currentRooms) => {
        const targetRoom = currentRooms.find((room) => room.id === nextMessage.roomId);
        if (!targetRoom) return currentRooms;

        const alreadyReceived = targetRoom.messages.some(
          (currentMessage) => currentMessage.id === nextMessage.id
        );
        if (alreadyReceived) return currentRooms;

        const messagesWithoutPending = targetRoom.messages.filter(
          (currentMessage) =>
            !(
              currentMessage.id.startsWith("pending-") &&
              currentMessage.sender.id === nextMessage.sender.id &&
              currentMessage.content === nextMessage.content
            )
        );
        const nextRoom: ChatRoom = {
          ...targetRoom,
          updatedAt: nextMessage.createdAt,
          messages: [...messagesWithoutPending, nextMessage],
        };

        return [
          nextRoom,
          ...currentRooms.filter((currentRoom) => currentRoom.id !== nextRoom.id),
        ];
      });

      if (isOpen && activeRoomId === nextMessage.roomId) {
        markRoomAsReadUntil(nextMessage.roomId, nextMessage.createdAt);
        if (nextMessage.sender.id !== user.id) {
          publishRoomRead(nextMessage.roomId, nextMessage.id);
        }
      }
    },
    [activeRoomId, isOpen, markRoomAsReadUntil, publishRoomRead, user.id]
  );

  const refreshRooms = useCallback(() => {
    const loadRooms = async () => {
      try {
        const nextRooms = await fetchMyChatRooms();
        const nextVisibleRooms = nextRooms.filter(hasMessages);
        setRooms(nextRooms);
        setActiveRoomId((currentRoomId) =>
          currentRoomId && nextVisibleRooms.some((room) => room.id === currentRoomId)
            ? currentRoomId
            : nextVisibleRooms[0]?.id ?? null
        );
      } catch {
        setRooms([]);
        setActiveRoomId(null);
      }
    };

    void loadRooms();
  }, []);

  useEffect(() => {
    refreshRooms();

    window.addEventListener(COMMUNITY_CHAT_CHANGE_EVENT, refreshRooms);
    window.addEventListener("storage", refreshRooms);

    return () => {
      window.removeEventListener(COMMUNITY_CHAT_CHANGE_EVENT, refreshRooms);
      window.removeEventListener("storage", refreshRooms);
    };
  }, [refreshRooms]);

  useEffect(() => {
    setReadState(readChatReadState(user.id));
  }, [user.id]);

  const visibleRooms = useMemo(() => rooms.filter(hasMessages), [rooms]);

  const activeRoom = useMemo(
    () => visibleRooms.find((room) => room.id === activeRoomId) ?? null,
    [activeRoomId, visibleRooms]
  );

  const visibleRoomIds = useMemo(
    () => visibleRooms.map((room) => room.id).sort().join(","),
    [visibleRooms]
  );

  useEffect(() => {
    const token = getAuthToken();
    if (!token) return;

    const client = createChatStompClient({
      token,
      onConnect: () => {
        setIsSocketConnected(true);
        setDockError("");
      },
      onDisconnect: () => {
        setIsSocketConnected(false);
      },
      onError: (message) => {
        setDockError(message);
      },
    });

    stompClientRef.current = client;
    client.activate();

    return () => {
      Object.values(roomSubscriptionsRef.current).forEach((subscription) => {
        subscription.unsubscribe();
      });
      roomSubscriptionsRef.current = {};
      stompClientRef.current = null;
      setIsSocketConnected(false);
      void client.deactivate();
    };
  }, [user.id]);

  useEffect(() => {
    const client = stompClientRef.current;
    if (!client || !isSocketConnected) return;

    const userRoomSubscription = client.subscribe("/user/sub/chat/rooms", (frame) => {
      const parsedPayload = parseChatSocketPayload(frame.body);
      if (parsedPayload?.kind === "room") {
        applySocketRoom(parsedPayload.room);
      }
    });

    const activeRoomIds = new Set(visibleRooms.map((room) => room.id));

    Object.entries(roomSubscriptionsRef.current).forEach(([roomId, subscription]) => {
      if (!activeRoomIds.has(roomId)) {
        subscription.unsubscribe();
        delete roomSubscriptionsRef.current[roomId];
      }
    });

    visibleRooms.forEach((room) => {
      if (roomSubscriptionsRef.current[room.id]) return;

      roomSubscriptionsRef.current[room.id] = client.subscribe(
        `/sub/chat/rooms/${room.id}`,
        (frame) => {
          const parsedPayload = parseChatSocketPayload(frame.body);
          if (!parsedPayload) return;

          if (parsedPayload.kind === "room") {
            applySocketRoom(parsedPayload.room);
            return;
          }

          appendSocketMessage(parsedPayload.message);
        }
      );
    });

    return () => {
      userRoomSubscription.unsubscribe();
    };
  }, [
    appendSocketMessage,
    applySocketRoom,
    isSocketConnected,
    visibleRoomIds,
    visibleRooms,
  ]);

  const unreadMessageCount = useMemo(
    () =>
      visibleRooms.reduce(
        (totalCount, room) => totalCount + countUnreadReceivedMessages(room, user.id, readState),
        0
      ),
    [readState, user.id, visibleRooms]
  );

  const markRoomAsRead = useCallback(
    (roomId: string) => {
      const room = rooms.find((currentRoom) => currentRoom.id === roomId);
      const lastMessage = room?.messages[room.messages.length - 1];
      if (!lastMessage) return;

      markRoomAsReadUntil(roomId, lastMessage.createdAt);
    },
    [markRoomAsReadUntil, rooms]
  );

  useEffect(() => {
    if (!isOpen || !activeRoomId || !activeRoom) return;

    const lastMessage = activeRoom.messages[activeRoom.messages.length - 1];
    if (lastMessage) {
      markRoomAsReadUntil(activeRoomId, lastMessage.createdAt);
    }

    const lastReceivedMessage = getLastReceivedMessage(activeRoom, user.id);
    if (lastReceivedMessage) {
      publishRoomRead(activeRoomId, lastReceivedMessage.id);
    }
  }, [
    activeRoom,
    activeRoomId,
    isOpen,
    isSocketConnected,
    markRoomAsReadUntil,
    publishRoomRead,
    user.id,
  ]);

  useEffect(() => {
    if (!isOpen || !activeRoom) return;

    window.requestAnimationFrame(() => {
      messagesEndRef.current?.scrollIntoView({ block: "end" });
    });
  }, [activeRoom, activeRoom?.messages.length, isOpen]);

  const sendMessage = () => {
    const trimmedMessage = message.trim();
    if (!activeRoom || !trimmedMessage || isSending) return;
    if (activeRoom.blocked) {
      setDockError("차단된 대화입니다. 더 이상 메시지를 보낼 수 없습니다.");
      return;
    }

    const client = stompClientRef.current;
    if (!client?.connected) {
      setDockError("채팅 서버와 연결 중입니다. 잠시 후 다시 보내주세요.");
      return;
    }

    setIsSending(true);
    setDockError("");

    const optimisticCreatedAt = new Date().toISOString();
    const optimisticMessageId = `pending-${optimisticCreatedAt}`;
    const optimisticRoom: ChatRoom = {
      ...activeRoom,
      updatedAt: optimisticCreatedAt,
      messages: [
        ...activeRoom.messages,
        {
          id: optimisticMessageId,
          roomId: activeRoom.id,
          sender: user,
          content: trimmedMessage,
          createdAt: optimisticCreatedAt,
          read: false,
        },
      ],
    };

    setMessage("");
    upsertRoom(optimisticRoom);
    setActiveRoomId(activeRoom.id);
    markRoomAsReadUntil(activeRoom.id, optimisticCreatedAt);

    try {
      client.publish({
        destination: `/pub/chat/rooms/${activeRoom.id}/messages`,
        body: JSON.stringify({ content: trimmedMessage }),
      });
    } catch (error) {
      setRooms((currentRooms) =>
        currentRooms.map((room) =>
          room.id === activeRoom.id
            ? {
                ...room,
                messages: room.messages.filter(
                  (chatMessage) => chatMessage.id !== optimisticMessageId
                ),
              }
            : room
        )
      );
      setMessage(trimmedMessage);
      setDockError(error instanceof Error ? error.message : "메시지를 보내지 못했습니다.");
    } finally {
      setIsSending(false);
    }
  };

  const blockActiveRoom = async () => {
    if (!activeRoom || isMutatingRoom) return;

    const shouldBlock = window.confirm(
      "차단하면 서로 더 이상 메시지를 보낼 수 없습니다. 차단하시겠습니까?"
    );
    if (!shouldBlock) return;

    setIsMutatingRoom(true);
    setDockError("");

    try {
      const blockedRoom = await blockChatRoom(activeRoom.id);
      setRooms((currentRooms) =>
        currentRooms.map((room) =>
          room.id === blockedRoom.id
            ? blockedRoom
            : room.participants.some((participant) => participant.id === blockedRoom.participants[0]?.id) &&
                room.participants.some((participant) => participant.id === blockedRoom.participants[1]?.id)
              ? { ...room, blocked: true, blockedBy: blockedRoom.blockedBy }
              : room
        )
      );
      setActiveRoomId(blockedRoom.id);
    } catch (error) {
      setDockError(error instanceof Error ? error.message : "채팅방을 차단하지 못했습니다.");
    } finally {
      setIsMutatingRoom(false);
    }
  };

  const deleteRoomFromList = async (roomId: string) => {
    if (isMutatingRoom) return;

    setIsMutatingRoom(true);
    setDockError("");

    try {
      await deleteChatRoomFromList(roomId);
      setRooms((currentRooms) => currentRooms.filter((room) => room.id !== roomId));
      setActiveRoomId((currentRoomId) => {
        if (currentRoomId !== roomId) return currentRoomId;
        return visibleRooms.find((room) => room.id !== roomId)?.id ?? null;
      });
    } catch (error) {
      setDockError(error instanceof Error ? error.message : "대화 목록을 삭제하지 못했습니다.");
    } finally {
      setIsMutatingRoom(false);
    }
  };

  return (
    <div className="relative hidden lg:block">
      <button
        type="button"
        onClick={() => setIsOpen((current) => !current)}
        className="relative flex h-11 items-center gap-2 rounded-full border border-[#ffd1e3] bg-[#fff0f7] px-4 text-sm font-black text-[#e75491] transition-colors hover:bg-[#ffe0ee]"
      >
        <MessageCircle className="h-4 w-4" />
        채팅
        {unreadMessageCount > 0 && (
          <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-[#f45f9c] px-1 text-[10px] text-white">
            {unreadMessageCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="fixed right-6 top-[4.75rem] z-50 grid h-[calc(100vh-6rem)] max-h-[34rem] min-h-0 w-[42rem] max-w-[calc(100vw-3rem)] grid-cols-[15rem_minmax(0,1fr)] overflow-hidden rounded-[1.75rem] border border-[#ffd1e3] bg-white shadow-[0_30px_72px_rgba(205,79,134,0.2)]">
          <aside className="flex min-h-0 flex-col border-r border-[#ffe1ed] bg-[#fff7fb]">
            <div className="flex shrink-0 items-center justify-between border-b border-[#ffe1ed] px-4 py-3">
              <div className="font-black text-[#69324b]">대화 목록</div>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="rounded-full p-1 text-[#a76886] hover:bg-[#fff0f7]"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="flex min-h-0 flex-1 flex-col overflow-y-auto p-2">
              {visibleRooms.length > 0 ? (
                visibleRooms.map((room) => {
                  const opponent = room.participants.find(
                    (participant) => participant.id !== user.id
                  );
                  const unreadCount = countUnreadReceivedMessages(room, user.id, readState);

                  return (
                    <div
                      key={room.id}
                      className={`rounded-2xl px-3 py-3 text-left transition-colors ${
                        activeRoomId === room.id
                          ? "bg-[#ffe0ee] text-[#69324b]"
                          : "text-[#a76886] hover:bg-[#fff0f7]"
                      }`}
                    >
                      <div className="flex items-start gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setActiveRoomId(room.id);
                            markRoomAsRead(room.id);
                          }}
                          className="min-w-0 flex-1 text-left"
                        >
                          <div className="flex items-center justify-between gap-2">
                            <div className="truncate text-sm font-black">
                              {opponent?.name ?? "내 메모"}
                            </div>
                            {unreadCount > 0 && (
                              <span className="flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full bg-[#f45f9c] px-1.5 text-[10px] font-black text-white">
                                {unreadCount}
                              </span>
                            )}
                          </div>
                          <div className="mt-1 line-clamp-2 text-xs font-bold">
                            {room.guideTitle}
                          </div>
                          {room.blocked && (
                            <div className="mt-1 text-[10px] font-black text-[#e75491]">
                              차단된 대화
                            </div>
                          )}
                        </button>
                        <button
                          type="button"
                          onClick={() => deleteRoomFromList(room.id)}
                          disabled={isMutatingRoom}
                          className="mt-0.5 rounded-full p-1.5 text-[#bd7b98] transition-colors hover:bg-white hover:text-[#e75491] disabled:opacity-50"
                          aria-label="대화 목록에서 삭제"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </div>
                  );
                })
              ) : (
                <div className="px-3 py-8 text-center text-sm font-bold text-[#a76886]">
                  아직 대화가 없습니다.
                </div>
              )}
            </div>
          </aside>

          <section className="flex min-h-0 min-w-0 flex-col">
            {activeRoom ? (
              <>
                <div className="flex shrink-0 items-start justify-between gap-3 border-b border-[#ffe1ed] px-5 py-3">
                  <div className="min-w-0">
                    <div className="truncate text-sm font-black text-[#69324b]">
                      {activeRoom.guideTitle}
                    </div>
                    <div className="mt-0.5 text-xs font-bold text-[#a76886]">
                      {activeRoom.participants.map((participant) => participant.name).join(" · ")}
                    </div>
                    {activeRoom.blocked && (
                      <div className="mt-1 text-xs font-black text-[#e75491]">
                        차단된 대화입니다.
                      </div>
                    )}
                  </div>
                  <div className="flex shrink-0 items-center gap-1">
                    <button
                      type="button"
                      onClick={blockActiveRoom}
                      disabled={activeRoom.blocked || isMutatingRoom}
                      className="rounded-full border border-[#ffd1e3] bg-[#fff0f7] p-2 text-[#e75491] transition-colors hover:bg-[#ffe0ee] disabled:cursor-not-allowed disabled:opacity-45"
                      aria-label="채팅방 차단"
                    >
                      <Ban className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => deleteRoomFromList(activeRoom.id)}
                      disabled={isMutatingRoom}
                      className="rounded-full border border-[#ffd1e3] bg-white p-2 text-[#a76886] transition-colors hover:bg-[#fff0f7] disabled:opacity-45"
                      aria-label="대화 목록에서 삭제"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
                {dockError && (
                  <div className="shrink-0 border-b border-[#ffe1ed] bg-[#fff0f7] px-5 py-2 text-xs font-black text-[#e75491]">
                    {dockError}
                  </div>
                )}
                <div className="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto bg-[#fffafd] px-5 py-4">
                  {activeRoom.messages.length > 0 ? (
                    activeRoom.messages.map((chatMessage) => {
                      const isMine = chatMessage.sender.id === user.id;

                      return (
                        <div
                          key={chatMessage.id}
                          className={`flex items-end gap-1.5 ${
                            isMine ? "justify-end" : "justify-start"
                          }`}
                        >
                          {isMine && chatMessage.read === false && (
                            <span className="mb-2 text-[10px] font-black leading-none text-[#e75491]">
                              1
                            </span>
                          )}
                          <div
                            className={`max-w-[75%] rounded-[1.25rem] px-4 py-2 text-sm font-bold ${
                              isMine
                                ? "bg-[#f45f9c] text-white"
                                : "bg-[#fff0f7] text-[#69324b]"
                            }`}
                          >
                            <div>{chatMessage.content}</div>
                            <div
                              className={`mt-1 text-[10px] ${
                                isMine ? "text-white/75" : "text-[#a76886]"
                              }`}
                            >
                              {formatChatTime(chatMessage.createdAt)}
                            </div>
                          </div>
                        </div>
                      );
                    })
                  ) : (
                    <div className="flex flex-1 items-center justify-center text-sm font-bold text-[#a76886]">
                      첫 메시지를 보내보세요.
                    </div>
                  )}
                  <div ref={messagesEndRef} />
                </div>
                <div className="flex shrink-0 gap-2 border-t border-[#ffe1ed] bg-white px-4 py-3">
                  <input
                    value={message}
                    onChange={(event) => setMessage(event.target.value)}
                    onCompositionStart={() => {
                      isComposingRef.current = true;
                    }}
                    onCompositionEnd={() => {
                      isComposingRef.current = false;
                    }}
                    onKeyDown={(event) => {
                      if (
                        event.key === "Enter" &&
                        !event.nativeEvent.isComposing &&
                        !isComposingRef.current
                      ) {
                        event.preventDefault();
                        sendMessage();
                      }
                    }}
                    disabled={activeRoom.blocked || !isSocketConnected}
                    placeholder={
                      activeRoom.blocked
                        ? "차단된 대화입니다"
                        : isSocketConnected
                          ? "메시지 입력"
                          : "채팅 서버 연결 중"
                    }
                    className="min-w-0 flex-1 rounded-full border border-[#ffd1e3] bg-[#fffafd] px-4 text-sm font-bold text-[#69324b] outline-none placeholder:text-[#bd7b98] disabled:bg-[#fff0f7] disabled:text-[#bd7b98]"
                  />
                  <button
                    type="button"
                    onClick={sendMessage}
                    disabled={isSending || activeRoom.blocked || !isSocketConnected}
                    className="flex h-10 w-10 items-center justify-center rounded-full bg-[#f45f9c] text-white shadow-[0_10px_20px_rgba(205,79,134,0.22)] disabled:bg-[#f4b6cf]"
                  >
                    <Send className="h-4 w-4" />
                  </button>
                </div>
              </>
            ) : (
              <div className="flex flex-1 items-center justify-center text-sm font-bold text-[#a76886]">
                대화를 선택해 주세요.
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
