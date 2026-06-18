package com.utibunna.market.controller;

import com.utibunna.market.dto.response.SellerResponse;
import com.utibunna.market.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints de sellers.
 *
 * El Gateway inyecta el UUID del usuario autenticado en el header X-User-Id.
 * Los controllers lo leen desde ahí — NUNCA del body — para evitar suplantación.
 */
@RestController
@RequestMapping("/api/market/sellers")
@RequiredArgsConstructor
@Tag(name = "Sellers", description = "Gestión de vendedores")
public class SellerController {

    private final SellerService sellerService;

    /**
     * POST /api/market/sellers
     * Registra al usuario autenticado como vendedor.
     * Requiere header: X-User-Id: <uuid>
     */
    @Operation(summary = "Registrarme como vendedor")
    @PostMapping
    public ResponseEntity<SellerResponse> register(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerService.register(userId));
    }

    /**
     * GET /api/market/sellers/me
     * Obtiene el perfil de seller del usuario autenticado.
     */
    @Operation(summary = "Ver mi perfil de vendedor")
    @GetMapping("/me")
    public ResponseEntity<SellerResponse> getMyProfile(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(sellerService.getMyProfile(userId));
    }
}
