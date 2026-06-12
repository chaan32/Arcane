package com.arcane.Arcane.web.Chat.repository;

import com.arcane.Arcane.web.Chat.domain.GuideChatRoom;
import com.arcane.Arcane.web.Guide.domain.Guide;
import com.arcane.Arcane.web.User.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuideChatRoomRepository extends JpaRepository<GuideChatRoom, Long> {
    Optional<GuideChatRoom> findByGuideAndReader(Guide guide, User reader);
    List<GuideChatRoom> findByAuthorOrReaderOrderByUpdatedAtDesc(User author, User reader);

    @Query("""
            select room
            from GuideChatRoom room
            where (room.author = :firstUser and room.reader = :secondUser)
               or (room.author = :secondUser and room.reader = :firstUser)
            """)
    List<GuideChatRoom> findRoomsBetween(
            @Param("firstUser") User firstUser,
            @Param("secondUser") User secondUser
    );

    @Query("""
            select count(room) > 0
            from GuideChatRoom room
            where room.blocked = true
              and (
                    (room.author = :firstUser and room.reader = :secondUser)
                 or (room.author = :secondUser and room.reader = :firstUser)
              )
            """)
    boolean existsBlockedRoomBetween(
            @Param("firstUser") User firstUser,
            @Param("secondUser") User secondUser
    );
}
