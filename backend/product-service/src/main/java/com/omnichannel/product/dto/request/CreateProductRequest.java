package com.omnichannel.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    private String slug;

    private String description;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @NotBlank(message = "Brand ID is required")
    private String brandId;

    @Builder.Default
    private String status = "ACTIVE"; // DRAFT, ACTIVE, INACTIVE, ARCHIVED

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @NotEmpty(message = "At least one product variant is required")
    @Valid
    @Builder.Default
    private List<ProductVariantDTO> variants = new ArrayList<>();
}
