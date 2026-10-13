package com.omnichannel.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDTO {

    private String id;

    @NotBlank(message = "SKU Code is required")
    private String skuCode;

    private String variantName;

    @NotNull(message = "Regular price is required")
    @Min(value = 0, message = "Regular price must be greater than or equal to 0")
    private Double regularPrice;

    @Min(value = 0, message = "Sale price must be greater than or equal to 0")
    private Double salePrice;

    @Builder.Default
    private String currency = "VND";

    private Map<String, String> attributes;

    @Builder.Default
    private Boolean active = true;
}
