package com.utibunna.market.controller;

import com.utibunna.market.dto.request.CreateSaleRequest;
import com.utibunna.market.dto.request.UpdateSaleStatusRequest;
import com.utibunna.market.dto.response.SaleResponse;
import com.utibunna.market.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/market/sales")
@RequiredArgsConstructor
@Tag(name = "Sales", description = "Órdenes de venta del marketplace")
public class SaleController {

    private final SaleService saleService;

    /**
     * POST /api/market/sales
     * Crea una orden PENDING.
     * Llamado por ms-payment al iniciar el checkout, NO por el frontend directamente.
     */
    @Operation(summary = "[Interno] Crear una orden de venta (llamado por ms-payment)")
    @PostMapping
    public ResponseEntity<SaleResponse> createSale(
            @Valid @RequestBody CreateSaleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleService.createSale(request));
    }

    /**
     * PATCH /api/market/sales/{id}/status
     * Actualiza el estado de la sale.
     * Llamado por ms-payment cuando MercadoPago notifica el resultado del pago.
     */
    @Operation(summary = "[Interno] Actualizar estado de una venta (llamado por ms-payment)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<SaleResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSaleStatusRequest request
    ) {
        return ResponseEntity.ok(saleService.updateStatus(id, request));
    }

    /**
     * GET /api/market/sales/my-purchases
     * Historial de compras del usuario autenticado.
     * Requiere header: X-User-Id
     */
    @Operation(summary = "Mis compras (historial de comprador)")
    @GetMapping("/my-purchases")
    public ResponseEntity<List<SaleResponse>> getMyPurchases(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(saleService.getMyPurchases(userId));
    }

    /**
     * GET /api/market/sales/my-sales
     * Ventas realizadas del seller autenticado.
     * Requiere header: X-User-Id
     */
    @Operation(summary = "Mis ventas (panel del vendedor)")
    @GetMapping("/my-sales")
    public ResponseEntity<List<SaleResponse>> getMySales(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(saleService.getMySales(userId));
    }
}
