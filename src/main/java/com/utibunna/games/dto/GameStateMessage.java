package com.utibunna.games.dto;

import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;

import java.util.Map;
import java.util.UUID;

/**
 * Authoritative state broadcast to {@code /topic/rooms/{roomId}} after every change.
 * The frontend renders this verbatim. {@code board} is populated for grid games, {@code fen} for chess.
 */
public record GameStateMessage(
        String roomId,
        String gameCode,
        Object board,
        String fen,
        String currentTurnUserId,
        GameStatus status,
        String winnerUserId,
        Map<String, String> symbols,
        int moveCount
) {
    public static GameStateMessage from(UUID roomId, GameState s) {
        return new GameStateMessage(
                roomId.toString(), s.getGameCode(), s.getBoard(), s.getFen(),
                s.getCurrentTurnUserId(), s.getStatus(), s.getWinnerUserId(), s.getSymbols(), s.getMoveCount());
    }
}
