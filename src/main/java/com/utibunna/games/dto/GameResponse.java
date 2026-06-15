package com.utibunna.games.dto;

import com.utibunna.games.entity.Game;

import java.util.UUID;

/** Catalog entry returned by {@code GET /api/games}. */
public record GameResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean active
) {
    public static GameResponse from(Game game) {
        return new GameResponse(game.getId(), game.getCode(), game.getName(), game.getDescription(), game.isActive());
    }
}
