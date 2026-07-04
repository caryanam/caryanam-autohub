package com.autohub.repository;

import com.autohub.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage>
    findByRoomIdOrderBySentAtAsc(
            String roomId
    );

    List<ChatMessage>
    findByRoomIdOrderBySentAtDesc(
            String roomId
    );

    Optional<ChatMessage>
    findTopByRoomIdOrderBySentAtDesc(
            String roomId
    );

    Long countByReceiverIdAndReceiverRoleAndIsReadFalse(
            Long receiverId,
            String receiverRole
    );

    @Modifying
    @Query("""
            UPDATE ChatMessage c
            SET c.isRead = true,
                c.readAt = CURRENT_TIMESTAMP
            WHERE c.roomId = :roomId
            AND c.receiverId = :receiverId
            AND c.receiverRole = :receiverRole
            AND c.isRead = false
            """)
    int markAsRead(
            @Param("roomId") String roomId,
            @Param("receiverId") Long receiverId,
            @Param("receiverRole") String receiverRole
    );

    @Query("""
            SELECT COUNT(c)
            FROM ChatMessage c
            WHERE c.roomId = :roomId
            AND c.receiverId = :receiverId
            AND c.receiverRole = :receiverRole
            AND c.isRead = false
            """)
    Long getUnreadCountForRoom(
            @Param("roomId") String roomId,
            @Param("receiverId") Long receiverId,
            @Param("receiverRole") String receiverRole
    );



    // ================= GROUP =================

    List<ChatMessage>
    findByGroupIdOrderBySentAtAsc(
            String groupId
    );

    Optional<ChatMessage>
    findTopByGroupIdOrderBySentAtDesc(
            String groupId
    );
}