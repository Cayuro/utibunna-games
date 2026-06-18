package com.utibunna.market.service;

import com.utibunna.market.dto.request.CreateProductRequest;
import com.utibunna.market.dto.request.UpdateStockRequest;
import com.utibunna.market.dto.response.ProductResponse;
import com.utibunna.market.entity.ProductEntity;
import com.utibunna.market.entity.SellerEntity;
import com.utibunna.market.exception.ApiException;
import com.utibunna.market.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final SellerService sellerService;

    /**
     * Lista todos los productos con status ACTIVE (catálogo público).
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> listActive() {
        return productRepository.findByStatus("ACTIVE")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Detalle de un producto por su ID.
     */
    @Transactional(readOnly = true)
    public ProductResponse getById(UUID productId) {
        ProductEntity product = findOrThrow(productId);
        return toResponse(product);
    }

    /**
     * Lista los productos de un seller específico (panel del vendedor).
     *
     * @param userId userId del seller autenticado
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(UUID userId) {
        SellerEntity seller = sellerService.getSellerEntityByUserId(userId);
        return productRepository.findBySeller_SellerId(seller.getSellerId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Crea un nuevo producto para el seller autenticado.
     *
     * @param userId  userId del seller autenticado (del header X-User-Id)
     * @param request datos del producto
     */
    @Transactional
    public ProductResponse create(UUID userId, CreateProductRequest request) {
        SellerEntity seller = sellerService.getSellerEntityByUserId(userId);

        if (!"ACTIVE".equals(seller.getStatus())) {
            throw ApiException.forbidden("Tu cuenta de vendedor está inactiva o suspendida");
        }

        ProductEntity product = ProductEntity.builder()
                .seller(seller)
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .imgUrl(request.getImgUrl())
                .stock(request.getStock())
                .status("ACTIVE")
                .build();

        return toResponse(productRepository.save(product));
    }

    /**
     * Actualiza el stock de un producto.
     * Solo el seller dueño del producto puede hacerlo.
     *
     * @param userId    userId del seller autenticado
     * @param productId ID del producto
     * @param request   nuevo stock
     */
    @Transactional
    public ProductResponse updateStock(UUID userId, UUID productId, UpdateStockRequest request) {
        SellerEntity seller = sellerService.getSellerEntityByUserId(userId);
        ProductEntity product = findOrThrow(productId);

        // Verifica que el producto pertenezca al seller autenticado
        if (!product.getSeller().getSellerId().equals(seller.getSellerId())) {
            throw ApiException.forbidden("No tienes permiso para modificar este producto");
        }

        product.setStock(request.getStock());

        // Si el stock llega a 0, marcar como SOLD_OUT automáticamente
        if (request.getStock() == 0) {
            product.setStatus("SOLD_OUT");
        } else if ("SOLD_OUT".equals(product.getStatus())) {
            product.setStatus("ACTIVE");
        }

        return toResponse(productRepository.save(product));
    }

    /**
     * Descuenta stock después de un pago confirmado.
     * Llamado internamente por SaleService.
     *
     * @param productId ID del producto
     * @param quantity  cantidad vendida
     */
    @Transactional
    public void decreaseStock(UUID productId, int quantity) {
        ProductEntity product = findOrThrow(productId);

        int newStock = product.getStock() - quantity;
        if (newStock < 0) {
            throw ApiException.badRequest("Stock insuficiente para el producto: " + product.getName());
        }

        product.setStock(newStock);
        if (newStock == 0) {
            product.setStatus("SOLD_OUT");
        }
        productRepository.save(product);
    }

    /**
     * Desactiva (oculta) un producto del catálogo.
     *
     * @param userId    userId del seller autenticado
     * @param productId ID del producto
     */
    @Transactional
    public ProductResponse deactivate(UUID userId, UUID productId) {
        SellerEntity seller = sellerService.getSellerEntityByUserId(userId);
        ProductEntity product = findOrThrow(productId);

        if (!product.getSeller().getSellerId().equals(seller.getSellerId())) {
            throw ApiException.forbidden("No tienes permiso para modificar este producto");
        }

        product.setStatus("INACTIVE");
        return toResponse(productRepository.save(product));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ProductEntity findOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + productId));
    }

    public ProductResponse toResponse(ProductEntity p) {
        return ProductResponse.builder()
                .id(p.getId())
                .sellerId(p.getSeller().getSellerId())
                .sellerUserId(p.getSeller().getUserId().toString())
                .name(p.getName())
                .price(p.getPrice())
                .imgUrl(p.getImgUrl())
                .description(p.getDescription())
                .stock(p.getStock())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
