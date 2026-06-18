package com.utibunna.market.repository;

import com.utibunna.market.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    /** Todos los productos activos (para el catálogo público) */
    List<ProductEntity> findByStatus(String status);

    /** Productos de un seller específico */
    List<ProductEntity> findBySeller_SellerIdAndStatus(UUID sellerId, String status);

    /** Todos los productos de un seller (activos e inactivos — para el panel del vendedor) */
    List<ProductEntity> findBySeller_SellerId(UUID sellerId);
}
