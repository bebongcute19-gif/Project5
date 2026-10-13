package com.omnichannel.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichannel.order.dto.response.CartDTO;
import com.omnichannel.order.dto.response.CartItemDTO;
import com.omnichannel.order.exception.BadRequestException;
import com.omnichannel.order.exception.ResourceNotFoundException;
import com.omnichannel.order.service.ProductGrpcClient;
import com.omnichannel.product.grpc.VariantInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ProductGrpcClient productGrpcClient;
    
    private ObjectMapper objectMapper;
    private CartServiceImpl cartService;
    private UUID testUserId;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cartService = new CartServiceImpl(redisTemplate, productGrpcClient, objectMapper);
        testUserId = UUID.randomUUID();
    }
    
    @Test
    void testAddItemToCart_Success() {
        // Given
        String skuCode = "SKU123";
        Integer quantity = 2;
        VariantInfo variantInfo = createMockVariantInfo(skuCode, "Test Product", 19.99, true);
        
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        when(productGrpcClient.getVariantInfo(skuCode)).thenReturn(variantInfo);
        
        // When
        cartService.addItemToCart(testUserId, skuCode, quantity);
        
        // Then
        verify(redisTemplate, atLeastOnce()).opsForValue();
        verify(productGrpcClient).getVariantInfo(skuCode);
    }
    
    @Test
    void testAddItemToCart_InvalidQuantity() {
        // Given
        String skuCode = "SKU123";
        
        // When & Then
        assertThrows(BadRequestException.class, () -> 
            cartService.addItemToCart(testUserId, skuCode, 0));
    }
    
    @Test
    void testAddItemToCart_InactiveProduct() {
        // Given
        String skuCode = "SKU123";
        Integer quantity = 2;
        VariantInfo variantInfo = createMockVariantInfo(skuCode, "Inactive Product", 19.99, false);
        
        when(productGrpcClient.getVariantInfo(skuCode)).thenReturn(variantInfo);
        
        // When & Then
        assertThrows(BadRequestException.class, () -> 
            cartService.addItemToCart(testUserId, skuCode, quantity));
    }
    
    @Test
    void testGetCart_Empty() {
        // Given
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        
        // When
        CartDTO cart = cartService.getCart(testUserId);
        
        // Then
        assertNotNull(cart);
        assertTrue(cart.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, cart.getSubtotal());
        assertEquals(0, cart.getItemCount());
    }
    
    @Test
    void testRemoveCartItem_Success() {
        // Given
        String skuCode = "SKU123";
        UUID itemId = UUID.randomUUID();
        
        CartItemDTO item = CartItemDTO.builder()
                .itemId(itemId)
                .skuCode(skuCode)
                .productName("Test Product")
                .price(BigDecimal.valueOf(19.99))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(39.98))
                .build();
        
        Map<String, CartItemDTO> cart = new HashMap<>();
        cart.put(skuCode, item);
        
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(cart);
        
        // When
        cartService.removeCartItem(testUserId, itemId);
        
        // Then
        verify(redisTemplate).delete(anyString());
    }
    
    @Test
    void testClearCart_Success() {
        // Given
        // When
        cartService.clearCart(testUserId);
        
        // Then
        verify(redisTemplate).delete(anyString());
    }
    
    private VariantInfo createMockVariantInfo(String skuCode, String productName, 
                                               double price, boolean isActive) {
        return VariantInfo.newBuilder()
                .setSkuCode(skuCode)
                .setProductName(productName)
                .setPrice(price)
                .setIsActive(isActive)
                .build();
    }
}
