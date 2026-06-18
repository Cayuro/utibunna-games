package com.utibunna.market.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Body para actualizar el estado de una sale.
 * ms-payment llama a este endpoint cuando MercadoPago notifica el resultado del pago.
 */
@Data
public class UpdateSaleStatusRequest {

    /** PAID | REJECTED | CANCELLED */
    @NotBlank(message = "El status es obligatorio")
    private String status;

    /** ID del pago en MercadoPago (para trazabilidad) */
    private String paymentReference;
}
