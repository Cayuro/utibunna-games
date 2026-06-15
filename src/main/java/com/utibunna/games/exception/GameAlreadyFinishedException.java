package com.utibunna.games.exception;

public class GameAlreadyFinishedException extends GameDomainException {

    public GameAlreadyFinishedException(String message) {
        super("GAME_FINISHED", message);
    }
}
