package com.omnichannel.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private String id;
    private String productId;
    private String skuCode;
    private String variantName;
    private BigDecimal regularPrice;
    private BigDecimal salePrice;
    private String currency;
    private Map<String, String> attributes;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
