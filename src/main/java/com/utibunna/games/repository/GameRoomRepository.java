package com.utibunna.games.repository;

import com.utibunna.games.entity.GameRoom;
import com.utibunna.games.entity.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameRoomRepository extends JpaRepository<GameRoom, UUID> {

    List<GameRoom> findByStatus(RoomStatus status);

    List<GameRoom> findByStatusAndGameId(RoomStatus status, UUID gameId);

    List<GameRoom> findByHostUserIdOrGuestUserId(UUID hostUserId, UUID guestUserId);
}
