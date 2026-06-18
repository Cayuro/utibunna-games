package com.utibunna.market.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Producto publicado en el marketplace.
 * Pertenece a un {@link SellerEntity} y puede tener múltiples {@link SaleEntity}.
 */
@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Vendedor propietario del producto */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private SellerEntity seller;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "img_url", columnDefinition = "TEXT")
    private String imgUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    /** ACTIVE | INACTIVE | SOLD_OUT */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    // ── Relaciones ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<SaleEntity> sales;

    // ── Auditoría ─────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
