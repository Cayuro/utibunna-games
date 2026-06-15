package com.utibunna.games.engine;

import com.utibunna.games.exception.InvalidMoveException;
import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;
import com.utibunna.games.model.Move;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 3x3 Tic Tac Toe. Board encoding: 0 empty, 1 = HOST (X), 2 = GUEST (O). HOST moves first. */
@Component
public class TicTacToeEngine implements GameEngine {

    private static final int SIZE = 3;

    @Override
    public String gameCode() {
        return "TIC_TAC_TOE";
    }

    @Override
    public GameState createInitialState(String hostUserId, String guestUserId) {
        return GameState.builder()
                .gameCode(gameCode())
                .hostUserId(hostUserId)
                .guestUserId(guestUserId)
                .currentTurnUserId(hostUserId)
                .status(GameStatus.IN_PROGRESS)
                .board(new int[SIZE][SIZE])
                .symbols(Map.of(hostUserId, "X", guestUserId, "O"))
                .moveCount(0)
                .build();
    }

    @Override
    public GameState applyMove(GameState state, String userId, Move move) {
        assertPlayable(state, userId);

        Integer r = move.toRow();
        Integer c = move.toCol();
        if (r == null || c == null || r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
            throw new InvalidMoveException("Cell out of bounds");
        }
        int[][] board = state.getBoard();
        if (board[r][c] != 0) {
            throw new InvalidMoveException("Cell already taken");
        }

        int mark = userId.equals(state.getHostUserId()) ? 1 : 2;
        board[r][c] = mark;
        state.setMoveCount(state.getMoveCount() + 1);

        if (isWin(board, mark)) {
            state.setStatus(GameStatus.FINISHED);
            state.setWinnerUserId(userId);
            state.setCurrentTurnUserId(null);
        } else if (state.getMoveCount() == SIZE * SIZE) {
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
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 0) {
                    moves.add(new Move(null, null, r, c, null, null, null));
                }
            }
        }
        return moves;
    }

    private boolean isWin(int[][] b, int mark) {
        for (int i = 0; i < SIZE; i++) {
            if (b[i][0] == mark && b[i][1] == mark && b[i][2] == mark) return true; // row
            if (b[0][i] == mark && b[1][i] == mark && b[2][i] == mark) return true; // col
        }
        if (b[0][0] == mark && b[1][1] == mark && b[2][2] == mark) return true;     // diagonal
        return b[0][2] == mark && b[1][1] == mark && b[2][0] == mark;               // anti-diagonal
    }
}
