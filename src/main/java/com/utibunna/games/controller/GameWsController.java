package com.utibunna.games.controller;

import com.utibunna.games.dto.GameErrorMessage;
import com.utibunna.games.exception.GameDomainException;
import com.utibunna.games.model.Move;
import com.utibunna.games.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * In-game STOMP endpoints. All game logic runs in the service/engine; this controller is thin.
 * The new authoritative state is broadcast by {@link GameRoomService} to {@code /topic/rooms/{roomId}}.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWsController {

    private final GameRoomService gameRoomService;

    /** Client -> /app/rooms/{roomId}/move with a Move payload. */
    @MessageMapping("/rooms/{roomId}/move")
    public void move(@DestinationVariable String roomId, @Payload Move move, Principal principal) {
        gameRoomService.handleMove(UUID.fromString(roomId), UUID.fromString(principal.getName()), move);
    }

    /** Client -> /app/rooms/{roomId}/join (empty body); re-pushes current state to subscribers. */
    @MessageMapping("/rooms/{roomId}/join")
    public void join(@DestinationVariable String roomId, Principal principal) {
        gameRoomService.resendState(UUID.fromString(roomId));
    }

    /** Client -> /app/rooms/{roomId}/resign; the opponent wins. */
    @MessageMapping("/rooms/{roomId}/resign")
    public void resign(@DestinationVariable String roomId, Principal principal) {
        gameRoomService.resign(UUID.fromString(roomId), UUID.fromString(principal.getName()));
    }

    /** Domain errors (illegal/out-of-turn move, etc.) go only to the offending user. */
    @MessageExceptionHandler(GameDomainException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public GameErrorMessage onDomainError(GameDomainException ex) {
        return new GameErrorMessage(ex.getCode(), ex.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public GameErrorMessage onUnexpected(Exception ex) {
        log.warn("Unexpected WS error", ex);
        return new GameErrorMessage("INTERNAL", "Unexpected error");
    }
}
