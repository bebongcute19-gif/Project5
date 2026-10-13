package com.omnichannel.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCartItemRequest {
    
    @NotNull(message = "Item ID is required")
    private UUID itemId;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
