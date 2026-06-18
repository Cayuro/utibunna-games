package com.utibunna.market.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta pública de un seller.
 * No expone tokens de MercadoPago.
 */
@Data @Builder
public class SellerResponse {

    private UUID sellerId;
    private UUID userId;
    private String status;

    /** true si ya vinculó su cuenta de MercadoPago */
    private boolean mercadoPagoLinked;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
