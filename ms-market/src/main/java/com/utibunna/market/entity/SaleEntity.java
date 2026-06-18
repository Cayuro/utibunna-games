package com.utibunna.market.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orden de venta generada cuando un comprador inicia el proceso de pago.
 *
 * Ciclo de vida del status:
 *   PENDING  →  PAID      (MercadoPago confirmó el pago)
 *   PENDING  →  REJECTED  (MercadoPago rechazó el pago)
 *   PENDING  →  CANCELLED (el comprador canceló antes de pagar)
 *
 * buyer_id viene del JWT; no hay FK cross-service a la tabla users de ms-auth.
 */
@Entity
@Table(name = "sales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID del comprador extraído del JWT en el Gateway */
    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    /** Producto comprado */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    /** PENDING | PAID | REJECTED | CANCELLED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /**
     * ID del pago en MercadoPago.
     * Lo rellena ms-payment cuando recibe el webhook de confirmación.
     */
    @Column(name = "payment_reference", length = 150)
    private String paymentReference;

    // ── Auditoría ─────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
