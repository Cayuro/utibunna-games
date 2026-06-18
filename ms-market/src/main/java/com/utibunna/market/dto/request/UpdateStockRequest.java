package com.utibunna.market.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body para actualizar el stock de un producto.
 * Usado por el seller o por ms-payment cuando se confirma un pago.
 */
@Data
public class UpdateStockRequest {

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;
}
