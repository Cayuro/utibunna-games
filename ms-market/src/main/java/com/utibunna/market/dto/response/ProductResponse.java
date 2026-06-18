package com.utibunna.market.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class ProductResponse {

    private UUID id;
    private UUID sellerId;
    private String sellerUserId;    // userId del vendedor (útil para el frontend)
    private String name;
    private BigDecimal price;
    private String imgUrl;
    private String description;
    private Integer stock;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
