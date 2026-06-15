package com.utibunna.games.exception;

/** An illegal lifecycle action (e.g. joining a full room, joining your own room). Mapped to HTTP 400. */
public class InvalidGameActionException extends RuntimeException {

    public InvalidGameActionException(String message) {
        super(message);
    }
}
