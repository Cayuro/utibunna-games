package com.utibunna.games.engine;

import com.utibunna.games.exception.InvalidMoveException;
import com.utibunna.games.exception.NotYourTurnException;
import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;
import com.utibunna.games.model.Move;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckersEngineTest {

    private final CheckersEngine engine = new CheckersEngine();
    private static final String HOST = "host";
    private static final String GUEST = "guest";

    private static Move mv(int fr, int fc, int tr, int tc) {
        return new Move(fr, fc, tr, tc, null, null, null);
    }

    private static GameState custom(int[][] board, String turn) {
        GameState s = new GameState();
        s.setGameCode("CHECKERS");
        s.setHostUserId(HOST);
        s.setGuestUserId(GUEST);
        s.setCurrentTurnUserId(turn);
        s.setStatus(GameStatus.IN_PROGRESS);
        s.setBoard(board);
        s.setMoveCount(0);
        return s;
    }

    @Test
    void initialPositionHasSevenMovesForHost() {
        GameState s = engine.createInitialState(HOST, GUEST);
        assertThat(engine.legalMoves(s)).hasSize(7);
    }

    @Test
    void simpleMoveFlipsTurn() {
        GameState s = engine.createInitialState(HOST, GUEST);
        engine.applyMove(s, HOST, mv(5, 0, 4, 1));
        assertThat(s.getBoard()[4][1]).isEqualTo(1);
        assertThat(s.getBoard()[5][0]).isZero();
        assertThat(s.getCurrentTurnUserId()).isEqualTo(GUEST);
        assertThat(s.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void captureIsMandatory() {
        int[][] b = new int[8][8];
        b[5][2] = 1; // host man
        b[4][3] = 2; // guest man, capturable to (3,4)
        b[0][1] = 2; // another guest man so the game keeps going after the capture
        GameState s = custom(b, HOST);

        // a non-capturing step is illegal while a capture exists
        assertThatThrownBy(() -> engine.applyMove(s, HOST, mv(5, 2, 4, 1)))
                .isInstanceOf(InvalidMoveException.class);

        // the capture itself is legal and removes the jumped piece
        engine.applyMove(s, HOST, mv(5, 2, 3, 4));
        assertThat(s.getBoard()[3][4]).isEqualTo(1);
        assertThat(s.getBoard()[4][3]).isZero();
        assertThat(s.getBoard()[5][2]).isZero();
        assertThat(s.getCurrentTurnUserId()).isEqualTo(GUEST);
    }

    @Test
    void multiJumpKeepsTurnThenEnds() {
        int[][] b = new int[8][8];
        b[6][3] = 1; // host man
        b[5][4] = 2; // first victim -> land (4,5)
        b[3][6] = 2; // second victim -> land (2,7)
        b[0][1] = 2; // keeps guest alive afterwards
        GameState s = custom(b, HOST);

        engine.applyMove(s, HOST, mv(6, 3, 4, 5)); // first jump
        assertThat(s.getBoard()[5][4]).isZero();
        assertThat(s.getBoard()[4][5]).isEqualTo(1);
        assertThat(s.getContinueRow()).isEqualTo(4);
        assertThat(s.getContinueCol()).isEqualTo(5);
        assertThat(s.getCurrentTurnUserId()).isEqualTo(HOST); // same player continues

        engine.applyMove(s, HOST, mv(4, 5, 2, 7)); // second jump
        assertThat(s.getBoard()[3][6]).isZero();
        assertThat(s.getBoard()[2][7]).isEqualTo(1);
        assertThat(s.getContinueRow()).isNull();
        assertThat(s.getCurrentTurnUserId()).isEqualTo(GUEST); // chain done, turn flips
    }

    @Test
    void manPromotesToKing() {
        int[][] b = new int[8][8];
        b[1][2] = 1; // host man one step from the top row
        b[5][0] = 2; // lone guest man elsewhere
        GameState s = custom(b, HOST);

        engine.applyMove(s, HOST, mv(1, 2, 0, 1));
        assertThat(s.getBoard()[0][1]).isEqualTo(3); // crowned
        assertThat(s.getCurrentTurnUserId()).isEqualTo(GUEST);
        assertThat(s.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void guestCannotMoveFirst() {
        GameState s = engine.createInitialState(HOST, GUEST);
        assertThatThrownBy(() -> engine.applyMove(s, GUEST, mv(2, 1, 3, 0)))
                .isInstanceOf(NotYourTurnException.class);
    }
}
