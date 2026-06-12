package com.arcane.Arcane.web.Chat.controller;

import com.arcane.Arcane.common.Exception.Normal.CannotFoundGuide;
import com.arcane.Arcane.web.Chat.dto.GuideChatDto;
import com.arcane.Arcane.web.Chat.service.GuideChatService;
import com.arcane.Arcane.web.Chat.service.GuideChatService.SocketMessageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class GuideChatController {
    private final GuideChatService guideChatService;
    private final SimpMessagingTemplate messagingTemplate;
    @GetMapping("/rooms")
    public ResponseEntity<List<GuideChatDto.RoomResponse>> getMyRooms(
            @AuthenticationPrincipal String loginId
    ) {
        return ResponseEntity.ok(guideChatService.findMyRooms(loginId));
    }

    @PostMapping("/guides/{guideId}/room")
    public ResponseEntity<GuideChatDto.RoomResponse> openGuideRoom(
            @PathVariable Long guideId,
            @AuthenticationPrincipal String loginId
    ) throws CannotFoundGuide {
        return ResponseEntity.ok(guideChatService.openGuideRoom(guideId, loginId));
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<GuideChatDto.RoomResponse> sendMessage(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String loginId,
            @RequestBody GuideChatDto.SendMessageRequest request
    ) {
        return ResponseEntity.ok(guideChatService.sendMessage(roomId, loginId, request.getContent()));
    }

    @PostMapping("/rooms/{roomId}/block")
    public ResponseEntity<GuideChatDto.RoomResponse> blockRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String loginId
    ) {
        return ResponseEntity.ok(guideChatService.blockRoom(roomId, loginId));
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> hideRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String loginId
    ) {
        guideChatService.hideRoom(roomId, loginId);
        return ResponseEntity.noContent().build();
    }

    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable Long roomId, @Payload GuideChatDto.SendMessageRequest request, Principal principal) {
        SocketMessageResult result =
                guideChatService.sendSocketMessage(roomId, principal.getName(), request.getContent());

        messagingTemplate.convertAndSend("/sub/chat/rooms/" + roomId, result.message());
        result.participantLoginIds().forEach(loginId ->
                messagingTemplate.convertAndSendToUser(loginId, "/sub/chat/rooms", result.room())
        );
    }

    @MessageMapping("/chat/rooms/{roomId}/read")
    public void readRoom(@DestinationVariable Long roomId, Principal principal) {
        GuideChatDto.RoomResponse room =
                guideChatService.readRoom(roomId, principal.getName());

        messagingTemplate.convertAndSend("/sub/chat/rooms/" + roomId, room);
    }
}
