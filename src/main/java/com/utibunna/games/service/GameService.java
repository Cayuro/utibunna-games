package com.utibunna.games.service;

import com.utibunna.games.dto.GameResponse;
import com.utibunna.games.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read-only catalog queries. */
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    public List<GameResponse> listActiveGames() {
        return gameRepository.findByActiveTrue().stream()
                .map(GameResponse::from)
                .toList();
    }
}
