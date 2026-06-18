package com.utibunna.market.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class SaleResponse {

    private UUID id;
    private UUID buyerId;
    private UUID productId;
    private String productName;     // nombre del producto al momento de la venta
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    /** PENDING | PAID | REJECTED | CANCELLED */
    private String status;

    /** ID del pago en MercadoPago — null hasta que MP confirme */
    private String paymentReference;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
