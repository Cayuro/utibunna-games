package com.utibunna.games.engine;

import com.utibunna.games.exception.UnknownGameException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves a {@link GameEngine} by game code. Spring injects every engine bean, so adding a
 * new game is one new {@code @Component} — no edits here.
 */
@Component
public class GameEngineRegistry {

    private final Map<String, GameEngine> byCode;

    public GameEngineRegistry(List<GameEngine> engines) {
        this.byCode = engines.stream()
                .collect(Collectors.toUnmodifiableMap(GameEngine::gameCode, Function.identity()));
    }

    public GameEngine get(String gameCode) {
        GameEngine engine = byCode.get(gameCode);
        if (engine == null) {
            throw new UnknownGameException(gameCode);
        }
        return engine;
    }
}
