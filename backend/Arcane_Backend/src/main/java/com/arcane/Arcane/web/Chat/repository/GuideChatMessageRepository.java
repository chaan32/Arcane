package com.arcane.Arcane.web.Chat.repository;

import com.arcane.Arcane.web.Chat.domain.GuideChatMessage;
import com.arcane.Arcane.web.Chat.domain.GuideChatRoom;
import com.arcane.Arcane.web.User.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideChatMessageRepository extends JpaRepository<GuideChatMessage, Long> {
    List<GuideChatMessage> findByRoomOrderByCreatedAtAsc(GuideChatRoom room);

    List<GuideChatMessage> findByRoomAndSenderNotAndReadFalse(
            GuideChatRoom room,
            User sender
    );
}
