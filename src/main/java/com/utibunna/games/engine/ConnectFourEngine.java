package com.utibunna.games.engine;

import com.utibunna.games.exception.InvalidMoveException;
import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;
import com.utibunna.games.model.Move;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Connect Four on a 6-row x 7-column board. Encoding: 0 empty, 1 = HOST (RED), 2 = GUEST (YELLOW).
 * Discs fall to the lowest empty row of the chosen column. HOST moves first.
 */
@Component
public class ConnectFourEngine implements GameEngine {

    private static final int ROWS = 6;
    private static final int COLS = 7;
    private static final int CONNECT = 4;

    @Override
    public String gameCode() {
        return "CONNECT_FOUR";
    }

    @Override
    public GameState createInitialState(String hostUserId, String guestUserId) {
        return GameState.builder()
                .gameCode(gameCode())
                .hostUserId(hostUserId)
                .guestUserId(guestUserId)
                .currentTurnUserId(hostUserId)
                .status(GameStatus.IN_PROGRESS)
                .board(new int[ROWS][COLS])
                .symbols(Map.of(hostUserId, "RED", guestUserId, "YELLOW"))
                .moveCount(0)
                .build();
    }

    @Override
    public GameState applyMove(GameState state, String userId, Move move) {
        assertPlayable(state, userId);

        Integer col = move.column();
        if (col == null || col < 0 || col >= COLS) {
            throw new InvalidMoveException("Column out of bounds");
        }
        int[][] board = state.getBoard();
        int row = dropRow(board, col);
        if (row < 0) {
            throw new InvalidMoveException("Column is full");
        }

        int mark = userId.equals(state.getHostUserId()) ? 1 : 2;
        board[row][col] = mark;
        state.setMoveCount(state.getMoveCount() + 1);

        if (isWin(board, row, col, mark)) {
            state.setStatus(GameStatus.FINISHED);
            state.setWinnerUserId(userId);
            state.setCurrentTurnUserId(null);
        } else if (state.getMoveCount() == ROWS * COLS) {
            state.setStatus(GameStatus.DRAW);
            state.setCurrentTurnUserId(null);
        } else {
            state.setCurrentTurnUserId(opponentOf(state, userId));
        }
        return state;
    }

    @Override
    public List<Move> legalMoves(GameState state) {
        List<Move> moves = new ArrayList<>();
        if (state.getStatus() != GameStatus.IN_PROGRESS) {
            return moves;
        }
        int[][] board = state.getBoard();
        for (int c = 0; c < COLS; c++) {
            if (board[0][c] == 0) { // top cell free => column playable
                moves.add(new Move(null, null, null, null, c, null, null));
            }
        }
        return moves;
    }

    /** Lowest empty row in a column, or -1 if full. */
    private int dropRow(int[][] board, int col) {
        for (int r = ROWS - 1; r >= 0; r--) {
            if (board[r][col] == 0) {
                return r;
            }
        }
        return -1;
    }

    private boolean isWin(int[][] board, int row, int col, int mark) {
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] d : directions) {
            int count = 1
                    + countDir(board, row, col, d[0], d[1], mark)
                    + countDir(board, row, col, -d[0], -d[1], mark);
            if (count >= CONNECT) {
                return true;
            }
        }
        return false;
    }

    private int countDir(int[][] board, int row, int col, int dr, int dc, int mark) {
        int count = 0;
        int r = row + dr;
        int c = col + dc;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == mark) {
            count++;
            r += dr;
            c += dc;
        }
        return count;
    }
}
