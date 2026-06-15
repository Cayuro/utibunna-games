package com.utibunna.games.engine;

import com.github.bhlangonijr.chesslib.Board;
import com.utibunna.games.exception.InvalidMoveException;
import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;
import com.utibunna.games.model.Move;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Chess via the chesslib library — no chess rules implemented by hand. The board is stored as a FEN
 * string in {@link GameState#getFen()}; chesslib validates moves, check, mate, stalemate, draws,
 * castling, promotion and en passant. HOST = WHITE and moves first.
 *
 * <p>Clients should send moves as UCI (e.g. {@code "e2e4"}, {@code "e7e8q"}). From/to coordinates are
 * also accepted as a fallback (row 0 = rank 8, col 0 = file a).</p>
 */
@Component
public class ChessEngine implements GameEngine {

    @Override
    public String gameCode() {
        return "CHESS";
    }

    @Override
    public GameState createInitialState(String hostUserId, String guestUserId) {
        String startFen = new Board().getFen(); // standard starting position
        return GameState.builder()
                .gameCode(gameCode())
                .hostUserId(hostUserId)
                .guestUserId(guestUserId)
                .currentTurnUserId(hostUserId)
                .status(GameStatus.IN_PROGRESS)
                .fen(startFen)
                .symbols(Map.of(hostUserId, "WHITE", guestUserId, "BLACK"))
                .moveCount(0)
                .build();
    }

    @Override
    public GameState applyMove(GameState state, String userId, Move move) {
        assertPlayable(state, userId);

        Board board = new Board();
        board.loadFromFen(state.getFen());

        String uci = toUci(move);
        com.github.bhlangonijr.chesslib.move.Move chessMove;
        try {
            chessMove = new com.github.bhlangonijr.chesslib.move.Move(uci, board.getSideToMove());
        } catch (RuntimeException e) {
            throw new InvalidMoveException("Malformed chess move: " + uci);
        }
        if (!board.legalMoves().contains(chessMove)) {
            throw new InvalidMoveException("Illegal chess move: " + uci);
        }
        board.doMove(chessMove);

        state.setFen(board.getFen());
        state.setMoveCount(state.getMoveCount() + 1);

        if (board.isMated()) {
            state.setStatus(GameStatus.FINISHED);
            state.setWinnerUserId(userId); // the side that just moved delivered checkmate
            state.setCurrentTurnUserId(null);
        } else if (board.isStaleMate() || board.isDraw()) {
            state.setStatus(GameStatus.DRAW);
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
        Board board = new Board();
        board.loadFromFen(state.getFen());
        return board.legalMoves().stream()
                .map(m -> new Move(null, null, null, null, null, m.toString(), null))
                .toList();
    }

    /** Build a UCI string: prefer the explicit {@code uci}, else from/to coordinates (+ promotion). */
    private String toUci(Move move) {
        String uci;
        if (move.uci() != null && !move.uci().isBlank()) {
            uci = move.uci().trim().toLowerCase();
            if (uci.length() == 4 && hasPromotion(move)) {
                uci = uci + move.promotion().trim().toLowerCase();
            }
        } else if (move.fromRow() != null && move.fromCol() != null
                && move.toRow() != null && move.toCol() != null) {
            uci = square(move.fromRow(), move.fromCol()) + square(move.toRow(), move.toCol());
            if (hasPromotion(move)) {
                uci = uci + move.promotion().trim().toLowerCase();
            }
        } else {
            throw new InvalidMoveException("Chess move needs 'uci' or from/to coordinates");
        }
        if (uci.length() < 4 || uci.length() > 5) {
            throw new InvalidMoveException("Malformed chess move: " + uci);
        }
        return uci;
    }

    private boolean hasPromotion(Move move) {
        return move.promotion() != null && !move.promotion().isBlank();
    }

    /** row 0 = rank 8 (top), col 0 = file a. */
    private String square(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) {
            throw new InvalidMoveException("Square out of bounds");
        }
        char file = (char) ('a' + col);
        int rank = 8 - row;
        return "" + file + rank;
    }
}
