package com.utibunna.games.engine;

import com.utibunna.games.exception.InvalidMoveException;
import com.utibunna.games.exception.NotYourTurnException;
import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;
import com.utibunna.games.model.Move;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChessEngineTest {

    private final ChessEngine engine = new ChessEngine();
    private static final String HOST = "host";  // WHITE
    private static final String GUEST = "guest"; // BLACK

    private static Move uci(String s) {
        return new Move(null, null, null, null, null, s, null);
    }

    @Test
    void startingPositionHasTwentyLegalMoves() {
        GameState s = engine.createInitialState(HOST, GUEST);
        assertThat(engine.legalMoves(s)).hasSize(20);
    }

    @Test
    void foolsMateBlackWins() {
        GameState s = engine.createInitialState(HOST, GUEST);
        engine.applyMove(s, HOST, uci("f2f3"));
        engine.applyMove(s, GUEST, uci("e7e5"));
        engine.applyMove(s, HOST, uci("g2g4"));
        engine.applyMove(s, GUEST, uci("d8h4")); // Qh4#

        assertThat(s.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(s.getWinnerUserId()).isEqualTo(GUEST);
        assertThat(s.getCurrentTurnUserId()).isNull();
    }

    @Test
    void scholarsMateWhiteWins() {
        GameState s = engine.createInitialState(HOST, GUEST);
        engine.applyMove(s, HOST, uci("e2e4"));
        engine.applyMove(s, GUEST, uci("e7e5"));
        engine.applyMove(s, HOST, uci("f1c4"));
        engine.applyMove(s, GUEST, uci("b8c6"));
        engine.applyMove(s, HOST, uci("d1h5"));
        engine.applyMove(s, GUEST, uci("g8f6"));
        engine.applyMove(s, HOST, uci("h5f7")); // Qxf7#

        assertThat(s.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(s.getWinnerUserId()).isEqualTo(HOST);
    }

    @Test
    void rejectsIllegalMove() {
        GameState s = engine.createInitialState(HOST, GUEST);
        assertThatThrownBy(() -> engine.applyMove(s, HOST, uci("e2e5"))) // pawn can't jump three
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    void guestCannotMoveFirst() {
        GameState s = engine.createInitialState(HOST, GUEST);
        assertThatThrownBy(() -> engine.applyMove(s, GUEST, uci("e7e5")))
                .isInstanceOf(NotYourTurnException.class);
    }
}
