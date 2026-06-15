package com.utibunna.games.model;

/**
 * A single generic move payload that covers all four games. Each engine reads only the fields it needs:
 * <ul>
 *   <li>Tic Tac Toe: {@code toRow}, {@code toCol}</li>
 *   <li>Connect Four: {@code column}</li>
 *   <li>Checkers: {@code fromRow}, {@code fromCol}, {@code toRow}, {@code toCol}</li>
 *   <li>Chess: {@code uci} (preferred, e.g. "e2e4" / "e7e8q") or {@code fromRow/fromCol/toRow/toCol} (+ {@code promotion})</li>
 * </ul>
 * Sent directly as the STOMP {@code @Payload}; deserialized by Jackson (records supported).
 */
public record Move(
        Integer fromRow,
        Integer fromCol,
        Integer toRow,
        Integer toCol,
        Integer column,
        String uci,
        String promotion
) {
}
