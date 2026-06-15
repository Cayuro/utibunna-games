package com.utibunna.games.engine;

import com.utibunna.games.exception.GameAlreadyFinishedException;
import com.utibunna.games.exception.NotYourTurnException;
import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;
import com.utibunna.games.model.Move;

import java.util.List;

/**
 * Common contract every game implements. Engines are stateless Spring {@code @Component}s
 * (pure functions {@code (state, userId, move) -> state}), so they are thread-safe by construction.
 */
public interface GameEngine {

    /** Stable code, identical to the {@code games.code} column and the registry key. */
    String gameCode();

    /** Build the starting state. By convention HOST moves first (and HOST = WHITE in chess). */
    GameState createInitialState(String hostUserId, String guestUserId);

    /**
     * Validate it is {@code userId}'s turn AND the move is legal, apply it, update
     * {@code currentTurnUserId}, and set {@code status}/{@code winnerUserId} when the game ends.
     *
     * @throws com.utibunna.games.exception.NotYourTurnException if it is not the caller's turn
     * @throws com.utibunna.games.exception.InvalidMoveException if the move is illegal
     * @throws com.utibunna.games.exception.GameAlreadyFinishedException if the game already ended
     */
    GameState applyMove(GameState state, String userId, Move move);

    /** Legal moves from the current state, for the frontend to render valid options. */
    List<Move> legalMoves(GameState state);

    // ---- shared helpers ----

    /** Reject moves on a finished game or out of turn. Call first in every {@code applyMove}. */
    default void assertPlayable(GameState state, String userId) {
        if (state.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameAlreadyFinishedException("Game already finished");
        }
        if (!userId.equals(state.getCurrentTurnUserId())) {
            throw new NotYourTurnException("It is not your turn");
        }
    }

    /** The other player's id. */
    default String opponentOf(GameState state, String userId) {
        return userId.equals(state.getHostUserId()) ? state.getGuestUserId() : state.getHostUserId();
    }
}
