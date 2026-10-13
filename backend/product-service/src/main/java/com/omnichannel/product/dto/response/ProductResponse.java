package com.omnichannel.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;
    private String name;
    private String slug;
    private String description;
    private String categoryId;
    private String brandId;
    private String status;
    private List<String> images;
    private List<ProductVariantResponse> variants;
    private Instant createdAt;
    private Instant updatedAt;
}

