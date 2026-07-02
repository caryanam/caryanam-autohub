package com.autohub.repository;
import com.autohub.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository
        extends JpaRepository<ChatRoom,Long> {

    Optional<ChatRoom> findByRoomId(
            String roomId
    );
}

