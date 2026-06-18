package com.utibunna.market.service;

import com.utibunna.market.dto.request.CreateSaleRequest;
import com.utibunna.market.dto.request.UpdateSaleStatusRequest;
import com.utibunna.market.dto.response.SaleResponse;
import com.utibunna.market.entity.ProductEntity;
import com.utibunna.market.entity.SaleEntity;
import com.utibunna.market.entity.SellerEntity;
import com.utibunna.market.exception.ApiException;
import com.utibunna.market.repository.ProductRepository;
import com.utibunna.market.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final SellerService sellerService;
    private final ProductService productService;

    /**
     * Crea una orden de venta en estado PENDING.
     * Llamado por ms-payment cuando el usuario inicia el checkout.
     * El stock NO se descuenta aquí — se descuenta cuando MP confirma el pago.
     *
     * @param request datos de la orden (buyerId, productId, quantity, unitPrice)
     */
    @Transactional
    public SaleResponse createSale(CreateSaleRequest request) {
        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + request.getProductId()));

        if (!"ACTIVE".equals(product.getStatus())) {
            throw ApiException.badRequest("El producto no está disponible para la venta");
        }

        if (product.getStock() < request.getQuantity()) {
            throw ApiException.badRequest("Stock insuficiente. Disponible: " + product.getStock());
        }

        BigDecimal total = request.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        SaleEntity sale = SaleEntity.builder()
                .buyerId(request.getBuyerId())
                .product(product)
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalPrice(total)
                .status("PENDING")
                .build();

        return toResponse(saleRepository.save(sale));
    }

    /**
     * Actualiza el estado de la sale cuando MercadoPago notifica el resultado.
     * Si el pago fue aprobado (PAID), descuenta el stock del producto.
     *
     * @param saleId  ID de la sale
     * @param request nuevo status + paymentReference de MP
     */
    @Transactional
    public SaleResponse updateStatus(UUID saleId, UpdateSaleStatusRequest request) {
        SaleEntity sale = findOrThrow(saleId);

        // Evitar transiciones inválidas (ya pagada no puede volver a PENDING)
        if (!"PENDING".equals(sale.getStatus())) {
            throw ApiException.conflict(
                "La venta ya tiene un estado final: " + sale.getStatus()
            );
        }

        sale.setStatus(request.getStatus());
        sale.setPaymentReference(request.getPaymentReference());

        // Si el pago fue aprobado → descontar stock
        if ("PAID".equals(request.getStatus())) {
            productService.decreaseStock(sale.getProduct().getId(), sale.getQuantity());
        }

        return toResponse(saleRepository.save(sale));
    }

    /**
     * Historial de compras del usuario autenticado (como comprador).
     *
     * @param buyerId userId del comprador
     */
    @Transactional(readOnly = true)
    public List<SaleResponse> getMyPurchases(UUID buyerId) {
        return saleRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Ventas del seller autenticado (como vendedor).
     *
     * @param userId userId del seller autenticado
     */
    @Transactional(readOnly = true)
    public List<SaleResponse> getMySales(UUID userId) {
        SellerEntity seller = sellerService.getSellerEntityByUserId(userId);
        return saleRepository.findBySellerId(seller.getSellerId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private SaleEntity findOrThrow(UUID saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() -> ApiException.notFound("Sale no encontrada: " + saleId));
    }

    public SaleResponse toResponse(SaleEntity s) {
        return SaleResponse.builder()
                .id(s.getId())
                .buyerId(s.getBuyerId())
                .productId(s.getProduct().getId())
                .productName(s.getProduct().getName())
                .quantity(s.getQuantity())
                .unitPrice(s.getUnitPrice())
                .totalPrice(s.getTotalPrice())
                .status(s.getStatus())
                .paymentReference(s.getPaymentReference())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
