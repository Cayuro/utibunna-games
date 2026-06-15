package com.utibunna.games.engine;

import com.utibunna.games.exception.InvalidMoveException;
import com.utibunna.games.exception.NotYourTurnException;
import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;
import com.utibunna.games.model.Move;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicTacToeEngineTest {

    private final TicTacToeEngine engine = new TicTacToeEngine();
    private static final String HOST = "host";
    private static final String GUEST = "guest";

    private static Move cell(int r, int c) {
        return new Move(null, null, r, c, null, null, null);
    }

    @Test
    void hostWinsTopRow() {
        GameState s = engine.createInitialState(HOST, GUEST);
        engine.applyMove(s, HOST, cell(0, 0));
        engine.applyMove(s, GUEST, cell(1, 0));
        engine.applyMove(s, HOST, cell(0, 1));
        engine.applyMove(s, GUEST, cell(1, 1));
        engine.applyMove(s, HOST, cell(0, 2));

        assertThat(s.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(s.getWinnerUserId()).isEqualTo(HOST);
        assertThat(s.getCurrentTurnUserId()).isNull();
    }

    @Test
    void fullBoardIsDraw() {
        GameState s = engine.createInitialState(HOST, GUEST);
        engine.applyMove(s, HOST, cell(0, 0));
        engine.applyMove(s, GUEST, cell(0, 1));
        engine.applyMove(s, HOST, cell(0, 2));
        engine.applyMove(s, GUEST, cell(1, 1));
        engine.applyMove(s, HOST, cell(1, 0));
        engine.applyMove(s, GUEST, cell(1, 2));
        engine.applyMove(s, HOST, cell(2, 1));
        engine.applyMove(s, GUEST, cell(2, 0));
        engine.applyMove(s, HOST, cell(2, 2));

        assertThat(s.getStatus()).isEqualTo(GameStatus.DRAW);
        assertThat(s.getWinnerUserId()).isNull();
    }

    @Test
    void cannotPlayTakenCell() {
        GameState s = engine.createInitialState(HOST, GUEST);
        engine.applyMove(s, HOST, cell(0, 0));
        assertThatThrownBy(() -> engine.applyMove(s, GUEST, cell(0, 0)))
                .isInstanceOf(InvalidMoveException.class);
    }

    @Test
    void guestCannotMoveFirst() {
        GameState s = engine.createInitialState(HOST, GUEST);
        assertThatThrownBy(() -> engine.applyMove(s, GUEST, cell(0, 0)))
                .isInstanceOf(NotYourTurnException.class);
    }

    @Test
    void initialBoardHasNineLegalMoves() {
        GameState s = engine.createInitialState(HOST, GUEST);
        assertThat(engine.legalMoves(s)).hasSize(9);
    }
}
