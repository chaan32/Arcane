package com.arcane.Arcane.web.Chat.service;

import com.arcane.Arcane.common.Exception.Normal.CannotFoundGuide;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.web.Chat.domain.GuideChatMessage;
import com.arcane.Arcane.web.Chat.domain.GuideChatRoom;
import com.arcane.Arcane.web.Chat.dto.GuideChatDto;
import com.arcane.Arcane.web.Chat.repository.GuideChatMessageRepository;
import com.arcane.Arcane.web.Chat.repository.GuideChatRoomRepository;
import com.arcane.Arcane.web.Guide.domain.Guide;
import com.arcane.Arcane.web.Guide.repository.GuideRepository;
import com.arcane.Arcane.web.User.domain.User;
import com.arcane.Arcane.web.User.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GuideChatService {
    private final GuideChatRoomRepository roomRepository;
    private final GuideChatMessageRepository messageRepository;
    private final GuideRepository guideRepository;
    private final UserService userService;

    public record SocketMessageResult(
            GuideChatDto.MessageResponse message,
            GuideChatDto.RoomResponse room,
            List<String> participantLoginIds
    ) {
    }

    @Transactional(readOnly = true)
    public List<GuideChatDto.RoomResponse> findMyRooms(String loginId) {
        User me = userService.getCurrentUser(loginId);
        List<GuideChatDto.RoomResponse> rooms = roomRepository.findByAuthorOrReaderOrderByUpdatedAtDesc(me, me).stream()
                .filter(room -> room.isVisibleTo(me))
                .map(this::toRoomResponse)
                .filter(room -> !room.getMessages().isEmpty())
                .toList();
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "chat",
                "findRooms",
                "SUCCESS",
                "userId=" + me.getId() + ", roomCount=" + rooms.size()
        );
        return rooms;
    }

    public GuideChatDto.RoomResponse openGuideRoom(Long guideId, String loginId) throws CannotFoundGuide {
        User me = userService.getCurrentUser(loginId);
        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> new CannotFoundGuide("해당 공략 정보를 찾을 수 없습니다."));

        GuideChatRoom room = roomRepository.findByGuideAndReader(guide, me)
                .orElseGet(() -> {
                    if (roomRepository.existsBlockedRoomBetween(guide.getAuthor(), me)) {
                        log.warn(
                                ApiLogSupport.BUSINESS_FLOW,
                                "chat",
                                "openRoom",
                                "BLOCKED",
                                "guideId=" + guideId + ", readerId=" + me.getId() + ", authorId=" + guide.getAuthor().getId()
                        );
                        throw new IllegalStateException("차단된 사용자와는 새 채팅을 시작할 수 없습니다.");
                    }
                    return roomRepository.save(GuideChatRoom.of(guide, me));
                });

        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "chat",
                "openRoom",
                "SUCCESS",
                "roomId=" + room.getId() + ", guideId=" + guideId + ", readerId=" + me.getId()
        );
        return toRoomResponse(room);
    }

    // Stomp 메세지 전송용 메소드
    public SocketMessageResult sendSocketMessage(Long roomId, String loginId, String content){
        User me = userService.getCurrentUser(loginId);

        // 메세지를 보낼 채팅방 찾기
        GuideChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!room.hasParticipant((me))){ // 현재 유저가 이 채팅방의 author인지, reader인지 확인
            throw new IllegalStateException("이 채팅방에 메세지를 보낼 수 없습니다.");
        }

        if (room.isBlocked()) {
            throw new IllegalStateException("차단된 대화입니다. 더 이상 메세지를 보낼 수 없습니다.");
        }

        GuideChatMessage message = GuideChatMessage.of(room, me, content); // 채팅메세지를 만들어줌
        GuideChatMessage savedMsg = messageRepository.save(message); // 메세지를 저장한다

        room.restoreForAll(); // 누군가 대화 목록에서 지웠던 방이어도 새 메시지가 오면 양쪽 목록에 다시 보이게 한다
        room.touch(); // 채팅방 UpdateAt을 현재 시간으로 갱신해서 최신 대화방으로 올라오게 함 ]

        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "chat",
                "sendSocketMessage",
                "SUCCESS",
                "roomId=" + roomId + ", senderId=" + me.getId() + ", messageId=" + savedMsg.getId() + ", contentLength=" + safeContentLength(content)
        );
        return new SocketMessageResult(
                GuideChatDto.MessageResponse.of(savedMsg),
                toRoomResponse(room),
                List.of(room.getAuthor().getLoginId(), room.getReader().getLoginId())
        );
    }

    @Transactional
    public GuideChatDto.RoomResponse readRoom(Long roomId, String loginId) {
        User me = userService.getCurrentUser(loginId);
        GuideChatRoom room = roomRepository.findById(roomId).orElseThrow(
                ()-> new IllegalArgumentException("채팅방을 찾을 수 없습니다.")
        );

        if (room.hasParticipant(me) == false){
            throw new IllegalStateException("이 채팅방을 읽을 수 없습니다.");
        }

        // 리스트로 가져와서
        List<GuideChatMessage> unReadMessages = messageRepository.findByRoomAndSenderNotAndReadFalse(room, me);

        // 포문 돌려서 읽음 처리
        unReadMessages.forEach(GuideChatMessage::markRead);

        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "chat",
                "readRoom",
                "SUCCESS",
                "roomId=" + roomId + ", readerId=" + me.getId() + ", readCount=" + unReadMessages.size()
        );
        return toRoomResponse(room);
    }


    public GuideChatDto.RoomResponse sendMessage(Long roomId, String loginId, String content) {
        User me = userService.getCurrentUser(loginId);
        GuideChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!room.hasParticipant(me)) {
            throw new IllegalArgumentException("이 채팅방에 메시지를 보낼 수 없습니다.");
        }
        if (room.isBlocked()) {
            throw new IllegalStateException("차단된 대화입니다. 더 이상 메시지를 보낼 수 없습니다.");
        }

        GuideChatMessage message = GuideChatMessage.of(room, me, content);
        messageRepository.save(message);
        room.restoreForAll();
        room.touch();

        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "chat",
                "sendMessage",
                "SUCCESS",
                "roomId=" + roomId + ", senderId=" + me.getId() + ", contentLength=" + safeContentLength(content)
        );
        return toRoomResponse(room);
    }

    public GuideChatDto.RoomResponse blockRoom(Long roomId, String loginId) {
        User me = userService.getCurrentUser(loginId);
        GuideChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        if (!room.hasParticipant(me)) {
            throw new IllegalArgumentException("이 채팅방을 차단할 수 없습니다.");
        }

        roomRepository.findRoomsBetween(room.getAuthor(), room.getReader())
                .forEach(chatRoom -> chatRoom.block(me));

        log.warn(
                ApiLogSupport.BUSINESS_FLOW,
                "chat",
                "blockRoom",
                "BLOCKED",
                "roomId=" + roomId + ", blockedByUserId=" + me.getId()
        );
        return toRoomResponse(room);
    }

    public void hideRoom(Long roomId, String loginId) {
        User me = userService.getCurrentUser(loginId);
        GuideChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        room.hideFor(me);
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "chat",
                "hideRoom",
                "SUCCESS",
                "roomId=" + roomId + ", userId=" + me.getId()
        );
    }

    private GuideChatDto.RoomResponse toRoomResponse(GuideChatRoom room) {
        return GuideChatDto.RoomResponse.of(
                room,
                messageRepository.findByRoomOrderByCreatedAtAsc(room)
        );
    }

    private int safeContentLength(String content) {
        return content == null ? 0 : content.length();
    }
}
