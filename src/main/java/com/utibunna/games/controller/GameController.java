package com.utibunna.games.controller;

import com.utibunna.games.dto.GameResponse;
import com.utibunna.games.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** REST catalog endpoint for the lobby. */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping
    public List<GameResponse> listGames() {
        return gameService.listActiveGames();
    }
}
