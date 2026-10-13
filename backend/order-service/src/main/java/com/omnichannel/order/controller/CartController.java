package com.omnichannel.order.controller;

import com.omnichannel.order.dto.request.AddToCartRequest;
import com.omnichannel.order.dto.request.UpdateCartItemRequest;
import com.omnichannel.order.dto.response.CartDTO;
import com.omnichannel.order.security.SecurityContextUtil;
import com.omnichannel.order.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    private final SecurityContextUtil securityContextUtil;
    
    @PostMapping("/items")
    public ResponseEntity<Void> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = securityContextUtil.getCurrentUserId(httpRequest);
        cartService.addItemToCart(userId, request.getSkuCode(), request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @GetMapping
    public ResponseEntity<CartDTO> getCart(HttpServletRequest httpRequest) {
        UUID userId = securityContextUtil.getCurrentUserId(httpRequest);
        CartDTO cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }
    
    @PutMapping("/items/{itemId}")
    public ResponseEntity<Void> updateCartItemQuantity(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = securityContextUtil.getCurrentUserId(httpRequest);
        cartService.updateCartItemQuantity(userId, itemId, request.getQuantity());
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeCartItem(
            @PathVariable UUID itemId,
            HttpServletRequest httpRequest) {
        UUID userId = securityContextUtil.getCurrentUserId(httpRequest);
        cartService.removeCartItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping
    public ResponseEntity<Void> clearCart(HttpServletRequest httpRequest) {
        UUID userId = securityContextUtil.getCurrentUserId(httpRequest);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
