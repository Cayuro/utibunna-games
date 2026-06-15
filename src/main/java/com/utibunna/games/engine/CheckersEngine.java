package com.utibunna.games.engine;

import com.utibunna.games.exception.InvalidMoveException;
import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;
import com.utibunna.games.model.Move;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * American checkers: mandatory capture, multi-jump chains, king promotion (which ends the turn).
 * Win = the opponent has no legal move. No automatic draws (out of MVP scope).
 * Board encoding and move math live in {@link CheckersRules}.
 */
@Component
public class CheckersEngine implements GameEngine {

    private static final int SIZE = CheckersRules.SIZE;

    @Override
    public String gameCode() {
        return "CHECKERS";
    }

    @Override
    public GameState createInitialState(String hostUserId, String guestUserId) {
        int[][] board = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if ((r + c) % 2 == 1) {              // dark squares only
                    if (r <= 2) {
                        board[r][c] = 2;             // GUEST at the top
                    } else if (r >= 5) {
                        board[r][c] = 1;             // HOST at the bottom
                    }
                }
            }
        }
        return GameState.builder()
                .gameCode(gameCode())
                .hostUserId(hostUserId)
                .guestUserId(guestUserId)
                .currentTurnUserId(hostUserId)
                .status(GameStatus.IN_PROGRESS)
                .board(board)
                .symbols(Map.of(hostUserId, "RED", guestUserId, "BLACK"))
                .moveCount(0)
                .build();
    }

    @Override
    public GameState applyMove(GameState state, String userId, Move move) {
        assertPlayable(state, userId);
        boolean host = userId.equals(state.getHostUserId());
        int[][] board = state.getBoard();

        Integer fr = move.fromRow(), fc = move.fromCol(), tr = move.toRow(), tc = move.toCol();
        if (fr == null || fc == null || tr == null || tc == null) {
            throw new InvalidMoveException("Checkers move needs fromRow/fromCol/toRow/toCol");
        }

        List<Move> legal = legalMovesForTurn(state, host);
        boolean ok = legal.stream().anyMatch(m ->
                Objects.equals(m.fromRow(), fr) && Objects.equals(m.fromCol(), fc)
                        && Objects.equals(m.toRow(), tr) && Objects.equals(m.toCol(), tc));
        if (!ok) {
            throw new InvalidMoveException("Illegal checkers move");
        }

        int piece = board[fr][fc];
        boolean isJump = Math.abs(tr - fr) == 2;
        board[tr][tc] = piece;
        board[fr][fc] = 0;
        if (isJump) {
            board[(fr + tr) / 2][(fc + tc) / 2] = 0; // remove the captured piece
        }
        state.setMoveCount(state.getMoveCount() + 1);

        boolean promoted = false;
        if (piece == 1 && tr == 0) {
            board[tr][tc] = 3;                       // HOST man -> king
            promoted = true;
        } else if (piece == 2 && tr == SIZE - 1) {
            board[tr][tc] = 4;                       // GUEST man -> king
            promoted = true;
        }

        // Multi-jump: same piece keeps capturing (unless it was just crowned).
        if (isJump && !promoted && !CheckersRules.jumpsFrom(board, tr, tc).isEmpty()) {
            state.setContinueRow(tr);
            state.setContinueCol(tc);
            return state; // turn continues for the same player
        }
        state.setContinueRow(null);
        state.setContinueCol(null);

        if (CheckersRules.legalMoves(board, !host).isEmpty()) {
            state.setStatus(GameStatus.FINISHED);
            state.setWinnerUserId(userId);
            state.setCurrentTurnUserId(null);
        } else {
            state.setCurrentTurnUserId(opponentOf(state, userId));
        }
        return state;
    }

    @Override
    public List<Move> legalMoves(GameState state) {
        if (state.getStatus() != GameStatus.IN_PROGRESS) {
            return List.of();
        }
        boolean host = Objects.equals(state.getCurrentTurnUserId(), state.getHostUserId());
        return legalMovesForTurn(state, host);
    }

    /** Respects an in-progress multi-jump: only the continuing piece's further captures are legal. */
    private List<Move> legalMovesForTurn(GameState state, boolean host) {
        if (state.getContinueRow() != null && state.getContinueCol() != null) {
            return CheckersRules.jumpsFrom(state.getBoard(), state.getContinueRow(), state.getContinueCol());
        }
        return CheckersRules.legalMoves(state.getBoard(), host);
    }
}
