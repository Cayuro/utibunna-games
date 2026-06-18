package com.utibunna.market.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Body para crear una orden de venta.
 * Este endpoint es llamado por ms-payment, no directamente por el frontend.
 */
@Data
public class CreateSaleRequest {

    @NotNull(message = "buyerId es obligatorio")
    private UUID buyerId;

    @NotNull(message = "productId es obligatorio")
    private UUID productId;

    @NotNull(message = "quantity es obligatorio")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer quantity;

    /** Precio unitario al momento de la compra (snapshot del precio) */
    @NotNull(message = "unitPrice es obligatorio")
    private BigDecimal unitPrice;
}
