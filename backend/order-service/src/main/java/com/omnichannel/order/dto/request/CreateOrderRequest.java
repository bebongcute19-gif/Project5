package com.omnichannel.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    
    @NotNull(message = "Shipping address ID is required")
    private UUID shippingAddressId;
}
