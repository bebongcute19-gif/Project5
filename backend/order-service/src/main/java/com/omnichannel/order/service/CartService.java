package com.omnichannel.order.service;

import com.omnichannel.order.dto.response.CartDTO;
import com.omnichannel.order.dto.response.CartItemDTO;

import java.util.UUID;

public interface CartService {
    
    void addItemToCart(UUID userId, String skuCode, Integer quantity);
    
    void updateCartItemQuantity(UUID userId, UUID itemId, Integer quantity);
    
    void removeCartItem(UUID userId, UUID itemId);
    
    CartDTO getCart(UUID userId);
    
    void clearCart(UUID userId);
}
