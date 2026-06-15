package com.utibunna.games.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * The full live state of a match. One concrete class for all four games (no polymorphism):
 * the {@code gameCode} tells callers which engine interprets {@code board}/{@code fen}.
 * Serialized to JSON and stored in Redis at {@code game:{roomId}} by GameStateService,
 * and projected to the frontend as {@code GameStateMessage}.
 *
 * <p>Must stay a plain Jackson-friendly POJO (no-arg ctor + getters/setters) so it round-trips through Redis.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameState {

    private String gameCode;          // TIC_TAC_TOE | CONNECT_FOUR | CHECKERS | CHESS
    private String hostUserId;
    private String guestUserId;

    private String currentTurnUserId; // whose turn it is (null once finished)
    private GameStatus status;
    private String winnerUserId;      // null while IN_PROGRESS or on DRAW

    private int[][] board;            // grid games (TTT/Connect Four/Checkers); null for chess
    private String fen;               // chess only (chesslib FEN); null otherwise

    private Map<String, String> symbols; // {hostUserId:"X", guestUserId:"O"} / {hostUserId:"WHITE", ...}
    private int moveCount;

    // Checkers multi-jump continuation: when set, only this piece may move and only by capturing.
    private Integer continueRow;
    private Integer continueCol;
}
