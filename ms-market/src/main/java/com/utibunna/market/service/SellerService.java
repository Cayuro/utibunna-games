package com.utibunna.market.service;

import com.utibunna.market.dto.response.SellerResponse;
import com.utibunna.market.entity.SellerEntity;
import com.utibunna.market.exception.ApiException;
import com.utibunna.market.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;

    /**
     * Registra al usuario como seller.
     * Si ya tiene un perfil de seller, lanza CONFLICT (409).
     *
     * @param userId UUID del usuario autenticado (del header X-User-Id)
     */
    @Transactional
    public SellerResponse register(UUID userId) {
        if (sellerRepository.existsByUserId(userId)) {
            throw ApiException.conflict("Este usuario ya tiene un perfil de vendedor");
        }

        SellerEntity seller = SellerEntity.builder()
                .userId(userId)
                .status("ACTIVE")
                .build();

        SellerEntity saved = sellerRepository.save(seller);
        return toResponse(saved);
    }

    /**
     * Obtiene el perfil de seller del usuario autenticado.
     *
     * @param userId UUID del usuario autenticado
     */
    @Transactional(readOnly = true)
    public SellerResponse getMyProfile(UUID userId) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("No tienes un perfil de vendedor registrado"));
        return toResponse(seller);
    }

    /**
     * Obtiene el seller por su sellerId (usado internamente por ProductService).
     */
    @Transactional(readOnly = true)
    public SellerEntity getSellerEntityByUserId(UUID userId) {
        return sellerRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Seller no encontrado para userId: " + userId));
    }

    // ── Mapper ────────────────────────────────────────────────────────────

    public SellerResponse toResponse(SellerEntity seller) {
        return SellerResponse.builder()
                .sellerId(seller.getSellerId())
                .userId(seller.getUserId())
                .status(seller.getStatus())
                .mercadoPagoLinked(seller.getMercadoPagoAccessToken() != null)
                .createdAt(seller.getCreatedAt())
                .updatedAt(seller.getUpdatedAt())
                .build();
    }
}
