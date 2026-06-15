package com.utibunna.games.exception;

public class InvalidMoveException extends GameDomainException {

    public InvalidMoveException(String message) {
        super("INVALID_MOVE", message);
    }
}
