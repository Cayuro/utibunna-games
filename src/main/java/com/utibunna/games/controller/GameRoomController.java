package com.utibunna.games.controller;

import com.utibunna.games.dto.CreateRoomRequest;
import com.utibunna.games.dto.GameRoomResponse;
import com.utibunna.games.entity.GameRoom;
import com.utibunna.games.service.GameRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST lobby / room lifecycle. The authenticated userId arrives in the gateway-forwarded
 * {@code X-User-Id} header (same trusted-gateway model as the WebSocket layer).
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class GameRoomController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final GameRoomService gameRoomService;

    @PostMapping
    public GameRoomResponse create(@RequestHeader(USER_ID_HEADER) String userId,
                                   @Valid @RequestBody CreateRoomRequest request) {
        GameRoom room = gameRoomService.createRoom(
                UUID.fromString(userId), request.gameCode(), request.bunnaTokens());
        return GameRoomResponse.from(room);
    }

    @GetMapping
    public List<GameRoomResponse> listWaiting() {
        return gameRoomService.listWaitingRooms().stream().map(GameRoomResponse::from).toList();
    }

    @GetMapping("/{roomId}")
    public GameRoomResponse get(@PathVariable String roomId) {
        return GameRoomResponse.from(gameRoomService.getRoom(UUID.fromString(roomId)));
    }

    @PostMapping("/{roomId}/join")
    public GameRoomResponse join(@RequestHeader(USER_ID_HEADER) String userId,
                                 @PathVariable String roomId) {
        GameRoom room = gameRoomService.joinRoom(UUID.fromString(roomId), UUID.fromString(userId));
        return GameRoomResponse.from(room);
    }
}
