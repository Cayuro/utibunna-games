package com.utibunna.games.dto;

import com.utibunna.games.entity.GameRoom;
import com.utibunna.games.entity.enums.RoomStatus;

import java.time.Instant;
import java.util.UUID;

/** Room view returned by the REST lobby endpoints. */
public record GameRoomResponse(
        UUID id,
        UUID gameId,
        UUID hostUserId,
        UUID guestUserId,
        Long bunnaTokens,
        RoomStatus status,
        UUID winnerUserId,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {
    public static GameRoomResponse from(GameRoom r) {
        return new GameRoomResponse(
                r.getId(), r.getGameId(), r.getHostUserId(), r.getGuestUserId(), r.getBunnaTokens(),
                r.getStatus(), r.getWinnerUserId(), r.getCreatedAt(), r.getStartedAt(), r.getFinishedAt());
    }
}
