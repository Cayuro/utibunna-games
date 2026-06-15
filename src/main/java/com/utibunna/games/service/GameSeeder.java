package com.utibunna.games.service;

import com.utibunna.games.entity.Game;
import com.utibunna.games.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the four game catalog rows on startup. Idempotent (safe to run on every boot).
 * The {@code code} values MUST match each engine's {@code gameCode()}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameSeeder implements CommandLineRunner {

    private final GameRepository gameRepository;

    @Override
    public void run(String... args) {
        seed("TIC_TAC_TOE", "Tic Tac Toe", "Classic 3x3 game");
        seed("CONNECT_FOUR", "Connect Four", "Drop discs, connect four in a row");
        seed("CHECKERS", "Checkers", "Draughts on an 8x8 board");
        seed("CHESS", "Chess", "Full chess rules powered by chesslib");
    }

    private void seed(String code, String name, String description) {
        if (gameRepository.existsByCode(code)) {
            return;
        }
        gameRepository.save(Game.builder()
                .code(code)
                .name(name)
                .description(description)
                .active(true)
                .build());
        log.info("Seeded game: {}", code);
    }
}
