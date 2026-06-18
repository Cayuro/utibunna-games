package com.utibunna.market.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Representa a un vendedor registrado en el marketplace.
 *
 * user_id viene del JWT que el Gateway valida — no hay FK a la tabla users
 * porque esa tabla vive en ms-auth (buena práctica en microservicios: cada
 * servicio es dueño de sus propios datos).
 */
@Entity
@Table(name = "sellers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SellerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "seller_id")
    private UUID sellerId;

    /** UUID del usuario en ms-auth (extraído del JWT por el Gateway) */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    // ── Datos de MercadoPago (se rellenan al vincular la cuenta) ──────────

    @Column(name = "mercado_pago_user_id", length = 120, unique = true)
    private String mercadoPagoUserId;

    @Column(name = "mercado_pago_account_id", length = 120, unique = true)
    private String mercadoPagoAccountId;

    @Column(name = "mercado_pago_access_token", columnDefinition = "TEXT")
    private String mercadoPagoAccessToken;

    @Column(name = "mercado_pago_refresh_token", columnDefinition = "TEXT")
    private String mercadoPagoRefreshToken;

    @Column(name = "mercado_pago_public_key", columnDefinition = "TEXT")
    private String mercadoPagoPublicKey;

    @Column(name = "mercado_pago_expires_at")
    private LocalDateTime mercadoPagoExpiresAt;

    // ── Estado ────────────────────────────────────────────────────────────

    /** ACTIVE | INACTIVE | SUSPENDED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    // ── Relaciones ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductEntity> products;

    // ── Auditoría ─────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
