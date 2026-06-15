package com.utibunna.games.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utibunna.games.model.GameState;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads/writes the live {@link GameState} as JSON in Redis at {@code game:{roomId}}.
 * Sole owner of the key convention and the TTL. No game rules here.
 */
@Service
@RequiredArgsConstructor
public class GameStateService {

    private static final String KEY_PREFIX = "game:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${games.redis.state-ttl-minutes:120}")
    private long ttlMinutes;

    private String key(UUID roomId) {
        return KEY_PREFIX + roomId;
    }

    /** Create/overwrite the active state, refreshing the TTL. Called on start and after every move. */
    public void save(UUID roomId, GameState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            redis.opsForValue().set(key(roomId), json, Duration.ofMinutes(ttlMinutes));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize game state for room " + roomId, e);
        }
    }

    /** Empty if there is no active game (never started or already cleaned up). */
    public Optional<GameState> get(UUID roomId) {
        String json = redis.opsForValue().get(key(roomId));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, GameState.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize game state for room " + roomId, e);
        }
    }

    public void delete(UUID roomId) {
        redis.delete(key(roomId));
    }

    public boolean exists(UUID roomId) {
        return Boolean.TRUE.equals(redis.hasKey(key(roomId)));
    }
}
