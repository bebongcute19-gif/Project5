package com.omnichannel.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDTO {
    
    private UUID id;
    private String orderNumber;
    private UUID customerId;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingFee;
    private UUID shippingAddressId;
    
    @Builder.Default
    private List<OrderItemDTO> items = new ArrayList<>();
    
    private Instant createdAt;
    private Instant updatedAt;
}
