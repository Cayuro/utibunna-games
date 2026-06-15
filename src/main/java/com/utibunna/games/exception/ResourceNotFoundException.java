package com.utibunna.games.exception;

/** A requested entity (game, room, ...) does not exist. Mapped to HTTP 404 on the REST side. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
