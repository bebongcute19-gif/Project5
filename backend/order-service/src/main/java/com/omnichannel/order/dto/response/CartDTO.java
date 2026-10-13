package com.omnichannel.order.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDTO {
    
    @Builder.Default
    private List<CartItemDTO> items = new ArrayList<>();
    
    private BigDecimal subtotal;
    private BigDecimal total;
    private Integer itemCount;
}
