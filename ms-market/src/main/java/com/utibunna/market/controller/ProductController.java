package com.utibunna.market.controller;

import com.utibunna.market.dto.request.CreateProductRequest;
import com.utibunna.market.dto.request.UpdateStockRequest;
import com.utibunna.market.dto.response.ProductResponse;
import com.utibunna.market.service.ProductService;
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
@RequestMapping("/api/market/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Catálogo de productos del marketplace")
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/market/products
     * Catálogo público — todos los productos ACTIVE.
     * No requiere autenticación (el Gateway lo puede dejar pasar sin JwtAuthFilter si así se configura).
     */
    @Operation(summary = "Listar productos disponibles (catálogo público)")
    @GetMapping
    public ResponseEntity<List<ProductResponse>> listActive() {
        return ResponseEntity.ok(productService.listActive());
    }

    /**
     * GET /api/market/products/{id}
     * Detalle de un producto.
     */
    @Operation(summary = "Ver detalle de un producto")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /**
     * GET /api/market/products/my
     * Mis productos como vendedor.
     * Requiere header: X-User-Id
     */
    @Operation(summary = "Listar mis productos (como vendedor)")
    @GetMapping("/my")
    public ResponseEntity<List<ProductResponse>> getMyProducts(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return ResponseEntity.ok(productService.getMyProducts(userId));
    }

    /**
     * POST /api/market/products
     * Crear un nuevo producto.
     * Requiere header: X-User-Id
     */
    @Operation(summary = "Publicar un producto")
    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(userId, request));
    }

    /**
     * PATCH /api/market/products/{id}/stock
     * Actualizar el stock de un producto.
     * Solo el seller dueño puede hacerlo.
     */
    @Operation(summary = "Actualizar stock de un producto")
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStockRequest request
    ) {
        return ResponseEntity.ok(productService.updateStock(userId, id, request));
    }

    /**
     * PATCH /api/market/products/{id}/deactivate
     * Ocultar un producto del catálogo sin eliminarlo.
     */
    @Operation(summary = "Desactivar un producto")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProductResponse> deactivate(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(productService.deactivate(userId, id));
    }
}
