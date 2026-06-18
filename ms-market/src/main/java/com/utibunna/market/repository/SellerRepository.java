package com.utibunna.market.repository;

import com.utibunna.market.entity.SellerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<SellerEntity, UUID> {

    Optional<SellerEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
