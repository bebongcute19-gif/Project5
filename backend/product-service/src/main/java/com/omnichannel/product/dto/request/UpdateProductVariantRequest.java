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
public class UpdateProductVariantRequest {

    @Size(max = 255, message = "Variant name must not exceed 255 characters")
    private String variantName;

    @DecimalMin(value = "0", message = "Regular price must be greater than or equal to 0")
    private BigDecimal regularPrice;

    @DecimalMin(value = "0", message = "Sale price must be greater than or equal to 0")
    private BigDecimal salePrice;

    @Size(max = 10, message = "Currency code must not exceed 10 characters")
    private String currency;

    private Map<String, String> attributes;

    private Boolean active;
}
