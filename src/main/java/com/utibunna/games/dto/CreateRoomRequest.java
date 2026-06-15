package com.utibunna.games.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Body of {@code POST /api/rooms}. */
public record CreateRoomRequest(
        @NotBlank String gameCode,
        @NotNull @PositiveOrZero Long bunnaTokens
) {
}
