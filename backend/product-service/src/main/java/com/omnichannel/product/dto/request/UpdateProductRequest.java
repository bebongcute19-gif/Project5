package com.omnichannel.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    private String slug;

    private String description;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @NotBlank(message = "Brand ID is required")
    private String brandId;

    private String status;

    private List<String> images;

    @Valid
    private List<ProductVariantDTO> variants;
}
