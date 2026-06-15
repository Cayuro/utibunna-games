package com.utibunna.games.exception;

public class UnknownGameException extends GameDomainException {

    public UnknownGameException(String gameCode) {
        super("UNKNOWN_GAME", "No engine registered for game code: " + gameCode);
    }
}
