package com.omnichannel.order.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {
    
    private UUID id;
    private String skuCode;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
}
