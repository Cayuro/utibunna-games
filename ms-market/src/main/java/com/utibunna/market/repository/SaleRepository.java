package com.utibunna.market.repository;

import com.utibunna.market.entity.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<SaleEntity, UUID> {

    /** Compras del buyer (historial de un comprador) */
    List<SaleEntity> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    /**
     * Ventas de todos los productos de un seller.
     * Hacemos un JOIN hasta seller para evitar cargar la lista completa de productos.
     */
    @Query("""
        SELECT s FROM SaleEntity s
        WHERE s.product.seller.sellerId = :sellerId
        ORDER BY s.createdAt DESC
    """)
    List<SaleEntity> findBySellerId(@Param("sellerId") UUID sellerId);
}
