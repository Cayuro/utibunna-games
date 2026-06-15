package com.utibunna.games.model;

/**
 * Engine-level state of a match in play (lives inside the Redis JSON).
 * Distinct from {@code RoomStatus}, which is the persistence lifecycle of a room row.
 */
public enum GameStatus {
    IN_PROGRESS,
    FINISHED,   // someone won -> winnerUserId is set
    DRAW        // ended with no winner -> winnerUserId is null
}
