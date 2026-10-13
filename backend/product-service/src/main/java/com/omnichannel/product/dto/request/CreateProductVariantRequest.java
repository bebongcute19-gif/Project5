package com.omnichannel.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductVariantRequest {

    @NotBlank(message = "SKU code is required")
    @Size(max = 100, message = "SKU code must not exceed 100 characters")
    private String skuCode;

    @Size(max = 255, message = "Variant name must not exceed 255 characters")
    private String variantName;

    @NotNull(message = "Regular price is required")
    @DecimalMin(value = "0", message = "Regular price must be greater than or equal to 0")
    private BigDecimal regularPrice;

    @DecimalMin(value = "0", message = "Sale price must be greater than or equal to 0")
    private BigDecimal salePrice;

    @Size(max = 10, message = "Currency code must not exceed 10 characters")
    private String currency;

    private Map<String, String> attributes;

    @Builder.Default
    private Boolean active = true;
}
