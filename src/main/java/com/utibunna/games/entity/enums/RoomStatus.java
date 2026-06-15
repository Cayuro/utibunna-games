package com.utibunna.games.entity.enums;

/**
 * Persistence lifecycle of a room row (stored as VARCHAR via @Enumerated(STRING)).
 * Distinct from the engine-level {@code GameStatus}.
 */
public enum RoomStatus {
    WAITING,      // created by host, waiting for a guest
    IN_PROGRESS,  // both players in, match running (live state in Redis)
    FINISHED,     // match ended (result persisted)
    CANCELLED     // abandoned before finishing
}
