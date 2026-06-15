package com.utibunna.games.repository;

import com.utibunna.games.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    Optional<Game> findByCode(String code);

    List<Game> findByActiveTrue();

    boolean existsByCode(String code);
}
