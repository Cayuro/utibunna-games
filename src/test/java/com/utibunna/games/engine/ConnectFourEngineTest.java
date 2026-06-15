package com.utibunna.games.engine;

import com.utibunna.games.exception.InvalidMoveException;
import com.utibunna.games.exception.NotYourTurnException;
import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;
import com.utibunna.games.model.Move;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectFourEngineTest {

    private final ConnectFourEngine engine = new ConnectFourEngine();
    private static final String HOST = "host";
    private static final String GUEST = "guest";

    private static Move col(int c) {
        return new Move(null, null, null, null, c, null, null);
    }

    @Test
    void hostWinsVertically() {
        GameState s = engine.createInitialState(HOST, GUEST);
        engine.applyMove(s, HOST, col(0));
        engine.applyMove(s, GUEST, col(1));
        engine.applyMove(s, HOST, col(0));
        engine.applyMove(s, GUEST, col(1));
        engine.applyMove(s, HOST, col(0));
        engine.applyMove(s, GUEST, col(1));
        engine.applyMove(s, HOST, col(0)); // four in column 0

        assertThat(s.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(s.getWinnerUserId()).isEqualTo(HOST);
    }

    @Test
    void rejectsColumnOutOfBounds() {
        GameState s = engine.createInitialState(HOST, GUEST);
        assertThatThrownBy(() -> engine.applyMove(s, HOST, col(9)))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    void rejectsFullColumn() {
        GameState s = engine.createInitialState(HOST, GUEST);
        // alternate in column 0 to fill it without anyone making four in a row
        for (int i = 0; i < 3; i++) {
            engine.applyMove(s, HOST, col(0));
            engine.applyMove(s, GUEST, col(0));
        }
        assertThat(s.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThatThrownBy(() -> engine.applyMove(s, HOST, col(0)))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    void guestCannotMoveFirst() {
        GameState s = engine.createInitialState(HOST, GUEST);
        assertThatThrownBy(() -> engine.applyMove(s, GUEST, col(0)))
                .isInstanceOf(NotYourTurnException.class);
    }

    @Test
    void initialBoardHasSevenLegalColumns() {
        GameState s = engine.createInitialState(HOST, GUEST);
        assertThat(engine.legalMoves(s)).hasSize(7);
    }
}
