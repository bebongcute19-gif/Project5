package com.omnichannel.order.service.impl;

import com.omnichannel.order.dto.response.CartDTO;
import com.omnichannel.order.dto.response.CartItemDTO;
import com.omnichannel.order.exception.BadRequestException;
import com.omnichannel.order.exception.ResourceNotFoundException;
import com.omnichannel.order.service.CartService;
import com.omnichannel.order.service.ProductGrpcClient;
import com.omnichannel.product.grpc.VariantInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductGrpcClient productGrpcClient;
    private final ObjectMapper objectMapper;

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_EXPIRY_SECONDS = 24 * 60 * 60; // 24 hours

    @Override
    public void addItemToCart(UUID userId, String skuCode, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        // Validate product exists via gRPC
        VariantInfo variantInfo = productGrpcClient.getVariantInfo(skuCode);
        
        if (!variantInfo.getIsActive()) {
            throw new BadRequestException("Product is not available");
        }

        Map<String, CartItemDTO> cart = getCartMap(userId);

        if (cart.containsKey(skuCode)) {
            CartItemDTO item = cart.get(skuCode);
            item.setQuantity(item.getQuantity() + quantity);
            item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        } else {
            CartItemDTO newItem = CartItemDTO.builder()
                    .itemId(UUID.randomUUID())
                    .skuCode(skuCode)
                    .productName(variantInfo.getProductName())
                    .price(BigDecimal.valueOf(variantInfo.getPrice()))
                    .quantity(quantity)
                    .subtotal(BigDecimal.valueOf(variantInfo.getPrice()).multiply(BigDecimal.valueOf(quantity)))
                    .build();
            cart.put(skuCode, newItem);
        }

        saveCart(userId, cart);
    }

    @Override
    public void updateCartItemQuantity(UUID userId, UUID itemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        Map<String, CartItemDTO> cart = getCartMap(userId);
        
        CartItemDTO item = cart.values().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        item.setQuantity(quantity);
        item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(quantity)));

        saveCart(userId, cart);
    }

    @Override
    public void removeCartItem(UUID userId, UUID itemId) {
        Map<String, CartItemDTO> cart = getCartMap(userId);
        
        CartItemDTO removedItem = cart.values().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        String skuToRemove = removedItem.getSkuCode();
        cart.remove(skuToRemove);

        if (cart.isEmpty()) {
            clearCart(userId);
        } else {
            saveCart(userId, cart);
        }
    }

    @Override
    public CartDTO getCart(UUID userId) {
        Map<String, CartItemDTO> cart = getCartMap(userId);

        BigDecimal subtotal = cart.values().stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer itemCount = cart.values().stream()
                .mapToInt(CartItemDTO::getQuantity)
                .sum();

        return CartDTO.builder()
                .items(new ArrayList<>(cart.values()))
                .subtotal(subtotal)
                .total(subtotal)
                .itemCount(itemCount)
                .build();
    }

    @Override
    public void clearCart(UUID userId) {
        String cartKey = CART_KEY_PREFIX + userId;
        redisTemplate.delete(cartKey);
    }

    @SuppressWarnings("unchecked")
    private Map<String, CartItemDTO> getCartMap(UUID userId) {
        String cartKey = CART_KEY_PREFIX + userId;
        Object cartObj = redisTemplate.opsForValue().get(cartKey);

        if (cartObj == null) {
            return new HashMap<>();
        }

        try {
            return objectMapper.convertValue(cartObj, new TypeReference<Map<String, CartItemDTO>>() {});
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    private void saveCart(UUID userId, Map<String, CartItemDTO> cart) {
        String cartKey = CART_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(cartKey, cart, java.time.Duration.ofSeconds(CART_EXPIRY_SECONDS));
    }
}
