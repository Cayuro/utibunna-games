package com.utibunna.games.exception;

import lombok.Getter;

/**
 * Base for in-game (engine) errors. Carries a stable machine-readable {@code code} so the
 * frontend can switch on it without parsing the human-readable message.
 */
@Getter
public class GameDomainException extends RuntimeException {

    private final String code;

    public GameDomainException(String code, String message) {
        super(message);
        this.code = code;
    }
}
