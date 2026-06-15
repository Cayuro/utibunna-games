package com.utibunna.games.dto;

/** Private error sent only to the offending user at {@code /user/queue/errors}. */
public record GameErrorMessage(
        String code,
        String message
) {
}
