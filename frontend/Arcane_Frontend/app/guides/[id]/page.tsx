"use client";

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import type { Client, StompSubscription } from "@stomp/stompjs";
import { Ban, Loader2, MessageCircle, Send, ThumbsDown, ThumbsUp } from "lucide-react";
import { ExternalImage } from "@/components/common/ExternalImage";
import MarkdownPreview from "@/components/guides/MarkdownPreview";
import { useMockAuth } from "@/hooks/useMockAuth";
import {
  blockChatRoom,
  COMMUNITY_CHAT_CHANGE_EVENT,
  createGuideComment,
  dislikeGuideComment,
  fetchGuideComments,
  fetchGuidePost,
  likeGuideComment,
  openGuideChatRoom,
} from "@/services/communityApi";
import {
  createChatStompClient,
  parseChatSocketPayload,
} from "@/services/chatStompClient";
import { getAuthToken } from "@/lib/mockAuth";
import type { ChatMessage, ChatRoom, GuideComment, GuidePost } from "@/types/community";
import LoginDialog from "@/components/auth/LoginDialog";
import { getDataDragonChampionIconUrl } from "@/services/dataDragonApi";

const formatDateTime = (value: string) =>
  new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));

const notifyChatRoomChanged = () => {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new Event(COMMUNITY_CHAT_CHANGE_EVENT));
  window.localStorage.setItem(`${COMMUNITY_CHAT_CHANGE_EVENT}.at`, String(Date.now()));
};

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

export default function GuideDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = React.use(params);
  const { user } = useMockAuth();
  const [guide, setGuide] = useState<GuidePost | null>(null);
  const [comments, setComments] = useState<GuideComment[]>([]);
  const [comment, setComment] = useState("");
  const [chatRoom, setChatRoom] = useState<ChatRoom | null>(null);
  const [chatMessage, setChatMessage] = useState("");
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isChatOpening, setIsChatOpening] = useState(false);
  const [isSendingChat, setIsSendingChat] = useState(false);
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);
  const [isBlockingChat, setIsBlockingChat] = useState(false);
  const [isSocketConnected, setIsSocketConnected] = useState(false);
  const [pendingCommentReactionId, setPendingCommentReactionId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState("");
  const chatMessagesEndRef = useRef<HTMLDivElement | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const roomSubscriptionRef = useRef<StompSubscription | null>(null);
  const isChatComposingRef = useRef(false);
  const isCommentComposingRef = useRef(false);
  const isSubmittingCommentRef = useRef(false);
  const readPublishStateRef = useRef<Record<string, string>>({});
  const chatRoomMessagesRef = useRef<ChatMessage[]>([]);

  useEffect(() => {
    let isMounted = true;

    const loadGuide = async () => {
      try {
        const [currentGuide, currentComments] = await Promise.all([
          fetchGuidePost(resolvedParams.id),
          fetchGuideComments(resolvedParams.id),
        ]);
        if (!isMounted) return;
        setGuide(currentGuide);
        setComments(currentComments);
        setErrorMessage("");
      } catch (error) {
        if (!isMounted) return;
        setErrorMessage(error instanceof Error ? error.message : "공략을 불러오지 못했습니다.");
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    void loadGuide();

    return () => {
      isMounted = false;
    };
  }, [resolvedParams.id]);

  const isAuthor = useMemo(
    () => Boolean(user && guide && user.id === guide.author.id),
    [guide, user]
  );

  useEffect(() => {
    if (!chatRoom) return;

    window.requestAnimationFrame(() => {
      chatMessagesEndRef.current?.scrollIntoView({ block: "end" });
    });
  }, [chatRoom, chatRoom?.messages.length]);

  useEffect(() => {
    chatRoomMessagesRef.current = chatRoom?.messages ?? [];
  }, [chatRoom?.messages]);

  const publishChatRead = useCallback((roomId: string, readTargetMessageId?: string) => {
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

  const applySocketRoom = useCallback((nextRoom: ChatRoom) => {
    setChatRoom((currentRoom) =>
      currentRoom && currentRoom.id === nextRoom.id && !isSameChatRoom(currentRoom, nextRoom)
        ? nextRoom
        : currentRoom
    );
  }, []);

  const appendSocketMessage = useCallback(
    (nextMessage: ChatMessage) => {
      setChatRoom((currentRoom) => {
        if (!currentRoom || currentRoom.id !== nextMessage.roomId) return currentRoom;

        const alreadyReceived = currentRoom.messages.some(
          (currentMessage) => currentMessage.id === nextMessage.id
        );
        if (alreadyReceived) return currentRoom;

        const messagesWithoutPending = currentRoom.messages.filter(
          (currentMessage) =>
            !(
              currentMessage.id.startsWith("pending-") &&
              currentMessage.sender.id === nextMessage.sender.id &&
              currentMessage.content === nextMessage.content
            )
        );

        return {
          ...currentRoom,
          updatedAt: nextMessage.createdAt,
          messages: [...messagesWithoutPending, nextMessage],
        };
      });

      if (nextMessage.sender.id !== user?.id) {
        publishChatRead(nextMessage.roomId, nextMessage.id);
      }
    },
    [publishChatRead, user?.id]
  );

  useEffect(() => {
    if (!user?.id || !chatRoom?.id) return;

    const token = getAuthToken();
    if (!token) return;

    const roomId = chatRoom.id;
    const client = createChatStompClient({
      token,
      onConnect: () => {
        setIsSocketConnected(true);
        setErrorMessage("");

        roomSubscriptionRef.current = client.subscribe(
          `/sub/chat/rooms/${roomId}`,
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
        const lastReceivedMessage = [...chatRoomMessagesRef.current]
          .reverse()
          .find((message) => message.sender.id !== user.id);
        publishChatRead(roomId, lastReceivedMessage?.id);
      },
      onDisconnect: () => {
        setIsSocketConnected(false);
      },
      onError: (message) => {
        setErrorMessage(message);
      },
    });

    stompClientRef.current = client;
    client.activate();

    return () => {
      roomSubscriptionRef.current?.unsubscribe();
      roomSubscriptionRef.current = null;
      stompClientRef.current = null;
      setIsSocketConnected(false);
      void client.deactivate();
    };
  }, [appendSocketMessage, applySocketRoom, chatRoom?.id, publishChatRead, user?.id]);

  const submitComment = async () => {
    const trimmedComment = comment.trim();
    if (!user || !guide || !trimmedComment || isSubmittingCommentRef.current) return;

    isSubmittingCommentRef.current = true;
    setIsSubmittingComment(true);
    try {
      const savedComment = await createGuideComment(guide.id, trimmedComment);
      setComment("");
      setComments((currentComments) =>
        currentComments.some((currentComment) => currentComment.id === savedComment.id)
          ? currentComments
          : [...currentComments, savedComment]
      );
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "댓글 작성에 실패했습니다.");
    } finally {
      isSubmittingCommentRef.current = false;
      setIsSubmittingComment(false);
    }
  };

  const reactToComment = async (
    commentId: string,
    reaction: "like" | "dislike"
  ) => {
    if (pendingCommentReactionId) return;

    const reactionField: "likes" | "dislikes" =
      reaction === "like" ? "likes" : "dislikes";

    setPendingCommentReactionId(commentId);
    setComments((currentComments) =>
      currentComments.map((currentComment) =>
        currentComment.id === commentId
          ? {
              ...currentComment,
              [reactionField]: currentComment[reactionField] + 1,
            }
          : currentComment
      )
    );

    try {
      if (reaction === "like") {
        await likeGuideComment(commentId);
      } else {
        await dislikeGuideComment(commentId);
      }

      setErrorMessage("");
    } catch (error) {
      setComments((currentComments) =>
        currentComments.map((currentComment) =>
          currentComment.id === commentId
            ? {
                ...currentComment,
                [reactionField]: Math.max(0, currentComment[reactionField] - 1),
              }
            : currentComment
        )
      );
      setErrorMessage(
        error instanceof Error ? error.message : "댓글 반응을 처리하지 못했습니다."
      );
    } finally {
      setPendingCommentReactionId(null);
    }
  };

  const openChat = async () => {
    if (!user || !guide) return;

    setIsChatOpening(true);
    try {
      const room = await openGuideChatRoom(guide.id);
      setChatRoom(room);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "채팅방을 열지 못했습니다.");
    } finally {
      setIsChatOpening(false);
    }
  };

  const sendChat = () => {
    const trimmedMessage = chatMessage.trim();
    if (!user || !chatRoom || !trimmedMessage || isSendingChat) return;
    if (chatRoom.blocked) {
      setErrorMessage("차단된 대화입니다. 더 이상 메시지를 보낼 수 없습니다.");
      return;
    }

    const client = stompClientRef.current;
    if (!client?.connected) {
      setErrorMessage("채팅 서버와 연결 중입니다. 잠시 후 다시 보내주세요.");
      return;
    }

    setIsSendingChat(true);
    const optimisticCreatedAt = new Date().toISOString();
    const optimisticMessageId = `pending-${optimisticCreatedAt}`;
    const optimisticRoom: ChatRoom = {
      ...chatRoom,
      updatedAt: optimisticCreatedAt,
      messages: [
        ...chatRoom.messages,
        {
          id: optimisticMessageId,
          roomId: chatRoom.id,
          sender: user,
          content: trimmedMessage,
          createdAt: optimisticCreatedAt,
          read: false,
        },
      ],
    };

    setChatMessage("");
    setChatRoom(optimisticRoom);

    try {
      client.publish({
        destination: `/pub/chat/rooms/${chatRoom.id}/messages`,
        body: JSON.stringify({ content: trimmedMessage }),
      });
      window.setTimeout(notifyChatRoomChanged, 300);
    } catch (error) {
      setChatRoom((currentRoom) =>
        currentRoom && currentRoom.id === chatRoom.id
          ? {
              ...currentRoom,
              messages: currentRoom.messages.filter(
                (message) => message.id !== optimisticMessageId
              ),
            }
          : currentRoom
      );
      setChatMessage(trimmedMessage);
      setErrorMessage(error instanceof Error ? error.message : "메시지를 보내지 못했습니다.");
    } finally {
      setIsSendingChat(false);
    }
  };

  const blockChat = async () => {
    if (!chatRoom || isBlockingChat) return;

    const shouldBlock = window.confirm(
      "차단하면 서로 더 이상 메시지를 보낼 수 없습니다. 차단하시겠습니까?"
    );
    if (!shouldBlock) return;

    setIsBlockingChat(true);
    try {
      const blockedRoom = await blockChatRoom(chatRoom.id);
      setChatRoom(blockedRoom);
      setErrorMessage("차단된 대화입니다. 더 이상 메시지를 보낼 수 없습니다.");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "채팅방을 차단하지 못했습니다.");
    } finally {
      setIsBlockingChat(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-12 text-[#69324b]">
        <div className="mx-auto max-w-[34rem] rounded-[2rem] border border-white/75 bg-white/94 p-8 text-center shadow-[0_26px_72px_rgba(205,79,134,0.16)]">
          <Loader2 className="mx-auto h-10 w-10 animate-spin text-[#e75491]" />
          <h1 className="mt-4 text-2xl font-black">공략을 불러오는 중입니다</h1>
        </div>
      </div>
    );
  }

  if (!guide) {
    return (
      <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-12 text-[#69324b]">
        <div className="mx-auto max-w-[34rem] rounded-[2rem] border border-white/75 bg-white/94 p-8 text-center shadow-[0_26px_72px_rgba(205,79,134,0.16)]">
          <Image
            src="/sad_mumu.png"
            alt="공략 없음"
            width={140}
            height={140}
            sizes="140px"
            className="mx-auto h-36 w-36 object-contain"
          />
          <h1 className="mt-4 text-2xl font-black">공략을 찾을 수 없습니다</h1>
          {errorMessage && (
            <p className="mt-2 text-sm font-bold text-[#a76886]">{errorMessage}</p>
          )}
          <Link
            href="/guides"
            className="mt-6 inline-flex rounded-full bg-[#f45f9c] px-5 py-3 text-sm font-black text-white"
          >
            공략 목록
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,_#ffe0ee_0%,_#fff6fb_46%,_#fffafd_100%)] px-4 py-8 text-[#69324b]">
      <div className="mx-auto grid w-full max-w-[90rem] gap-5 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="flex min-w-0 flex-col gap-4">
          <article className="overflow-hidden rounded-[2rem] border border-white/75 bg-white/94 shadow-[0_26px_72px_rgba(205,79,134,0.16)] ring-1 ring-[#f8dce8]/60">
            <div className="grid gap-5 p-5 lg:grid-cols-[9rem_minmax(0,1fr)]">
              <ExternalImage
                src={
                  guide.coverImageUrl ||
                  getDataDragonChampionIconUrl(guide.champion.imageFull)
                }
                alt={guide.champion.nameKo}
                width={144}
                height={144}
                sizes="144px"
                fallback={<div className="h-36 w-36 rounded-[1.75rem] bg-[#fff0f7]" />}
                className="h-36 w-36 rounded-[1.75rem] object-cover shadow-[0_18px_38px_rgba(205,79,134,0.16)]"
              />
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-[#fff0f7] px-3 py-1 text-xs font-black text-[#e75491] ring-1 ring-[#ffd1e3]">
                    {guide.champion.nameKo}
                  </span>
                  <span className="text-xs font-bold text-[#a76886]">
                    {formatDateTime(guide.updatedAt)}
                  </span>
                </div>
                <h1 className="mt-4 break-keep text-3xl font-black leading-tight tracking-normal lg:text-5xl">
                  {guide.title}
                </h1>
                <p className="mt-3 break-keep text-base font-bold leading-7 text-[#a76886]">
                  {guide.summary}
                </p>
                <div className="mt-5 flex flex-wrap items-center gap-2">
                  <span className="rounded-full bg-[#fff7fb] px-3 py-1.5 text-xs font-black text-[#a76886]">
                    작성자 {guide.author.name}
                  </span>
                  <span className="rounded-full bg-[#fff7fb] px-3 py-1.5 text-xs font-black text-[#a76886]">
                    조회 {guide.viewCount}
                  </span>
                  <span className="rounded-full bg-[#fff7fb] px-3 py-1.5 text-xs font-black text-[#a76886]">
                    댓글 {comments.length}
                  </span>
                  {user ? (
                    <button
                      type="button"
                      onClick={openChat}
                      className="inline-flex items-center gap-2 rounded-full bg-[#f45f9c] px-4 py-2 text-xs font-black text-white shadow-[0_12px_24px_rgba(205,79,134,0.22)]"
                    >
                      <MessageCircle className="h-4 w-4" />
                      {isChatOpening ? "채팅 여는 중" : isAuthor ? "내 공략 메모" : "작성자에게 채팅"}
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setIsLoginOpen(true)}
                      className="rounded-full border border-[#ffd1e3] bg-[#fff0f7] px-4 py-2 text-xs font-black text-[#e75491]"
                    >
                      로그인 후 채팅
                    </button>
                  )}
                </div>
              </div>
            </div>
            <div className="border-t border-[#ffe1ed] p-5">
              <MarkdownPreview markdown={guide.markdown} />
              {errorMessage && (
                <p className="mt-4 rounded-2xl bg-[#fff0f7] px-4 py-3 text-sm font-bold text-[#a76886]">
                  {errorMessage}
                </p>
              )}
            </div>
          </article>

          <section className="overflow-hidden rounded-[1.75rem] border border-white/75 bg-white/94 shadow-[0_18px_46px_rgba(205,79,134,0.12)] ring-1 ring-[#f8dce8]/55">
            <div className="border-b border-[#ffe1ed] px-5 py-4">
              <h2 className="text-lg font-black">댓글</h2>
            </div>
            <div className="flex flex-col gap-3 px-5 py-4">
              {comments.length > 0 ? (
                comments.map((item) => (
                  <div key={item.id} className="rounded-[1.25rem] bg-[#fff7fb] p-4">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-black">{item.author.name}</span>
                      <span className="text-[10px] font-bold text-[#a76886]">
                        {formatDateTime(item.createdAt)}
                      </span>
                    </div>
                    <p className="mt-2 whitespace-pre-wrap text-sm font-bold leading-6 text-[#a76886]">
                      {item.content}
                    </p>
                    <div className="mt-3 flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => reactToComment(item.id, "like")}
                        disabled={pendingCommentReactionId === item.id}
                        className="inline-flex items-center gap-1.5 rounded-full border border-[#ffd1e3] bg-white/80 px-3 py-1.5 text-xs font-black text-[#e75491] transition-colors hover:bg-[#fff0f7] disabled:cursor-not-allowed disabled:opacity-55"
                        aria-label="댓글 좋아요"
                      >
                        <ThumbsUp className="h-3.5 w-3.5" />
                        {item.likes}
                      </button>
                      <button
                        type="button"
                        onClick={() => reactToComment(item.id, "dislike")}
                        disabled={pendingCommentReactionId === item.id}
                        className="inline-flex items-center gap-1.5 rounded-full border border-[#f3d3de] bg-white/70 px-3 py-1.5 text-xs font-black text-[#a76886] transition-colors hover:bg-[#fff0f7] hover:text-[#e75491] disabled:cursor-not-allowed disabled:opacity-55"
                        aria-label="댓글 싫어요"
                      >
                        <ThumbsDown className="h-3.5 w-3.5" />
                        {item.dislikes}
                      </button>
                    </div>
                  </div>
                ))
              ) : (
                <div className="py-8 text-center text-sm font-bold text-[#a76886]">
                  아직 댓글이 없습니다.
                </div>
              )}
            </div>
            <div className="border-t border-[#ffe1ed] p-4">
              {user ? (
                <div className="flex gap-2">
                  <input
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                    onCompositionStart={() => {
                      isCommentComposingRef.current = true;
                    }}
                    onCompositionEnd={() => {
                      isCommentComposingRef.current = false;
                    }}
                    onKeyDown={(event) => {
                      if (
                        event.key === "Enter" &&
                        !event.nativeEvent.isComposing &&
                        !isCommentComposingRef.current
                      ) {
                        event.preventDefault();
                        submitComment();
                      }
                    }}
                    placeholder="댓글 입력"
                    disabled={isSubmittingComment}
                    className="min-w-0 flex-1 rounded-full border border-[#ffd1e3] bg-[#fffafd] px-4 text-sm font-bold text-[#69324b] outline-none placeholder:text-[#bd7b98]"
                  />
                  <button
                    type="button"
                    onClick={submitComment}
                    disabled={isSubmittingComment || !comment.trim()}
                    className="flex h-10 w-10 items-center justify-center rounded-full bg-[#f45f9c] text-white disabled:bg-[#f4b6cf]"
                  >
                    <Send className="h-4 w-4" />
                  </button>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => setIsLoginOpen(true)}
                  className="w-full rounded-full bg-[#f45f9c] px-5 py-3 text-sm font-black text-white"
                >
                  로그인 후 댓글 작성
                </button>
              )}
            </div>
          </section>
        </div>

        <aside className="flex flex-col gap-4">
          {chatRoom && user && (
            <section className="overflow-hidden rounded-[1.75rem] border border-white/75 bg-white/94 shadow-[0_18px_46px_rgba(205,79,134,0.12)] ring-1 ring-[#f8dce8]/55">
              <div className="flex items-center justify-between gap-3 border-b border-[#ffe1ed] px-5 py-4">
                <div>
                  <h2 className="text-lg font-black">채팅</h2>
                  {chatRoom.blocked && (
                    <p className="mt-1 text-xs font-black text-[#e75491]">
                      차단된 대화입니다.
                    </p>
                  )}
                </div>
                <button
                  type="button"
                  onClick={blockChat}
                  disabled={chatRoom.blocked || isBlockingChat}
                  className="rounded-full border border-[#ffd1e3] bg-[#fff0f7] p-2 text-[#e75491] transition-colors hover:bg-[#ffe0ee] disabled:cursor-not-allowed disabled:opacity-45"
                  aria-label="채팅방 차단"
                >
                  <Ban className="h-4 w-4" />
                </button>
              </div>
              <div className="flex max-h-80 flex-col gap-3 overflow-y-auto bg-[#fffafd] px-5 py-4">
                {chatRoom.messages.length > 0 ? (
                  chatRoom.messages.map((message) => {
                    const isMine = message.sender.id === user.id;

                    return (
                      <div
                        key={message.id}
                        className={`flex items-end gap-1.5 ${
                          isMine ? "justify-end" : "justify-start"
                        }`}
                      >
                        {isMine && message.read === false && (
                          <span className="mb-2 text-[10px] font-black leading-none text-[#e75491]">
                            1
                          </span>
                        )}
                        <div
                          className={`max-w-[85%] rounded-[1.25rem] px-4 py-2 text-sm font-bold ${
                            isMine
                              ? "bg-[#f45f9c] text-white"
                              : "bg-[#fff0f7] text-[#69324b]"
                          }`}
                        >
                          {message.content}
                        </div>
                      </div>
                    );
                  })
                ) : (
                  <div className="py-8 text-center text-sm font-bold text-[#a76886]">
                    아직 메시지가 없습니다.
                  </div>
                )}
                <div ref={chatMessagesEndRef} />
              </div>
              <div className="flex gap-2 border-t border-[#ffe1ed] p-4">
                <input
                  value={chatMessage}
                  onChange={(event) => setChatMessage(event.target.value)}
                  onCompositionStart={() => {
                    isChatComposingRef.current = true;
                  }}
                  onCompositionEnd={() => {
                    isChatComposingRef.current = false;
                  }}
                  onKeyDown={(event) => {
                    if (
                      event.key === "Enter" &&
                      !event.nativeEvent.isComposing &&
                      !isChatComposingRef.current
                    ) {
                      event.preventDefault();
                      sendChat();
                    }
                  }}
                  disabled={chatRoom.blocked || !isSocketConnected}
                  placeholder={
                    chatRoom.blocked
                      ? "차단된 대화입니다"
                      : isSocketConnected
                        ? "메시지 입력"
                        : "채팅 서버 연결 중"
                  }
                  className="min-w-0 flex-1 rounded-full border border-[#ffd1e3] bg-[#fffafd] px-4 text-sm font-bold outline-none disabled:bg-[#fff0f7] disabled:text-[#bd7b98]"
                />
                <button
                  type="button"
                  onClick={sendChat}
                  disabled={isSendingChat || chatRoom.blocked || !isSocketConnected}
                  className="flex h-10 w-10 items-center justify-center rounded-full bg-[#f45f9c] text-white disabled:bg-[#f4b6cf]"
                >
                  {isSendingChat ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                </button>
              </div>
            </section>
          )}
        </aside>
      </div>
      <LoginDialog isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
    </div>
  );
}
