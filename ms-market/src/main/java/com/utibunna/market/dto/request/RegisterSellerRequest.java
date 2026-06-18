package com.utibunna.market.dto.request;

import lombok.Data;

/**
 * Body para registrar un seller.
 * El user_id viene del header X-User-Id inyectado por el Gateway,
 * no del body, así el cliente no puede suplantarlo.
 * Este DTO está disponible para datos opcionales de MercadoPago al vincular.
 */
@Data
public class RegisterSellerRequest {
    // Por ahora el registro es automático con solo el userId del header.
    // Cuando se integre OAuth de MercadoPago, se agregarán aquí los campos del token.
}
