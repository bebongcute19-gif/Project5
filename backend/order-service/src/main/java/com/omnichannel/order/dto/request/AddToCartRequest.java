package com.omnichannel.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {
    
    @NotBlank(message = "SKU code is required")
    private String skuCode;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
