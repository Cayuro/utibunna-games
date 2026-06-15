package com.utibunna.games.service;

import com.utibunna.games.dto.GameStateMessage;
import com.utibunna.games.engine.GameEngine;
import com.utibunna.games.engine.GameEngineRegistry;
import com.utibunna.games.entity.Game;
import com.utibunna.games.entity.GameRoom;
import com.utibunna.games.entity.enums.RoomStatus;
import com.utibunna.games.exception.GameDomainException;
import com.utibunna.games.exception.InvalidGameActionException;
import com.utibunna.games.exception.ResourceNotFoundException;
import com.utibunna.games.model.GameState;
import com.utibunna.games.model.GameStatus;
import com.utibunna.games.model.Move;
import com.utibunna.games.repository.GameRepository;
import com.utibunna.games.repository.GameRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates a match across PostgreSQL (durable room rows), Redis (live state),
 * the game engines, and STOMP broadcasts. The single entry point for the WS/REST layers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRepository gameRepository;
    private final GameStateService gameStateService;
    private final GameEngineRegistry engineRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    // ---------------------------------------------------------------- lobby / lifecycle (REST)

    @Transactional
    public GameRoom createRoom(UUID hostUserId, String gameCode, Long bunnaTokens) {
        Game game = gameRepository.findByCode(gameCode)
                .filter(Game::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("No active game with code: " + gameCode));

        GameRoom room = GameRoom.builder()
                .gameId(game.getId())
                .hostUserId(hostUserId)
                .bunnaTokens(bunnaTokens)
                .status(RoomStatus.WAITING)
                .build();
        return gameRoomRepository.save(room);
    }

    @Transactional
    public GameRoom joinRoom(UUID roomId, UUID guestUserId) {
        GameRoom room = getRoomOrThrow(roomId);
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new InvalidGameActionException("Room is not open for joining");
        }
        if (room.getHostUserId().equals(guestUserId)) {
            throw new InvalidGameActionException("You cannot join your own room");
        }
        if (room.getGuestUserId() != null) {
            throw new InvalidGameActionException("Room is already full");
        }
        room.setGuestUserId(guestUserId);
        gameRoomRepository.save(room);
        startGame(room); // a room is exactly two players, so joining = ready
        return room;
    }

    /** Initializes the live state, flips the row to IN_PROGRESS, and broadcasts the opening board. */
    private void startGame(GameRoom room) {
        Game game = gameRepository.findById(room.getGameId())
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + room.getGameId()));
        GameEngine engine = engineRegistry.get(game.getCode());

        GameState initial = engine.createInitialState(
                room.getHostUserId().toString(), room.getGuestUserId().toString());
        gameStateService.save(room.getId(), initial); // first Redis write

        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setStartedAt(Instant.now());
        gameRoomRepository.save(room);

        broadcast(room.getId(), initial);
        log.debug("Game started for room {} ({})", room.getId(), game.getCode());
    }

    @Transactional(readOnly = true)
    public GameRoom getRoom(UUID roomId) {
        return getRoomOrThrow(roomId);
    }

    @Transactional(readOnly = true)
    public List<GameRoom> listWaitingRooms() {
        return gameRoomRepository.findByStatus(RoomStatus.WAITING);
    }

    // ---------------------------------------------------------------- in-game (WebSocket)

    /**
     * Applies a move: load state from Redis, run the engine (validates turn + legality), persist,
     * finish if terminal, and broadcast the new authoritative state to the room.
     * {@code @Transactional} so the finish flow's DB writes commit atomically.
     */
    @Transactional
    public GameState handleMove(UUID roomId, UUID userId, Move move) {
        GameState state = gameStateService.get(roomId)
                .orElseThrow(() -> new GameDomainException("NO_ACTIVE_GAME", "No active game for room " + roomId));

        GameEngine engine = engineRegistry.get(state.getGameCode());
        GameState next = engine.applyMove(state, userId.toString(), move);

        if (next.getStatus() == GameStatus.IN_PROGRESS) {
            gameStateService.save(roomId, next);
        } else {
            UUID winner = next.getWinnerUserId() == null ? null : UUID.fromString(next.getWinnerUserId());
            finishRoom(roomId, winner);
        }
        broadcast(roomId, next);
        return next;
    }

    /** Either player concedes; the opponent wins. */
    @Transactional
    public GameState resign(UUID roomId, UUID userId) {
        GameState state = gameStateService.get(roomId)
                .orElseThrow(() -> new GameDomainException("NO_ACTIVE_GAME", "No active game for room " + roomId));

        String uid = userId.toString();
        if (!uid.equals(state.getHostUserId()) && !uid.equals(state.getGuestUserId())) {
            throw new GameDomainException("NOT_A_PLAYER", "You are not a player in this room");
        }
        String opponent = uid.equals(state.getHostUserId()) ? state.getGuestUserId() : state.getHostUserId();

        state.setStatus(GameStatus.FINISHED);
        state.setWinnerUserId(opponent);
        state.setCurrentTurnUserId(null);

        finishRoom(roomId, UUID.fromString(opponent));
        broadcast(roomId, state);
        return state;
    }

    /** Re-broadcasts the current state (used when a client (re)subscribes via the WS join handler). */
    public void resendState(UUID roomId) {
        gameStateService.get(roomId).ifPresent(state -> broadcast(roomId, state));
    }

    // ---------------------------------------------------------------- finish flow

    /**
     * Records the outcome durably, THEN clears Redis. Persisting to PostgreSQL first means a crash
     * between the two steps leaves the durable result intact (the Redis key just TTLs out).
     * Idempotent: a second terminal trigger is a no-op.
     *
     * @param winnerUserId the winner, or {@code null} for a draw
     */
    @Transactional
    public void finishRoom(UUID roomId, UUID winnerUserId) {
        GameRoom room = getRoomOrThrow(roomId);
        if (room.getStatus() == RoomStatus.FINISHED) {
            return; // already finished — avoid double-write
        }
        room.setWinnerUserId(winnerUserId);
        room.setStatus(RoomStatus.FINISHED);
        room.setFinishedAt(Instant.now());
        gameRoomRepository.save(room);   // PostgreSQL = source of truth for the result

        gameStateService.delete(roomId); // free the live state from Redis
        log.debug("Room {} finished. winner={}", roomId, winnerUserId);
    }

    // ---------------------------------------------------------------- helpers

    private GameRoom getRoomOrThrow(UUID roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
    }

    private void broadcast(UUID roomId, GameState state) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, GameStateMessage.from(roomId, state));
    }
}
