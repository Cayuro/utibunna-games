package com.utibunna.games.exception;

public class NotYourTurnException extends GameDomainException {

    public NotYourTurnException(String message) {
        super("NOT_YOUR_TURN", message);
    }
}
