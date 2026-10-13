package com.omnichannel.order.controller;

import com.omnichannel.order.dto.request.CreateOrderRequest;
import com.omnichannel.order.dto.request.UpdateOrderStatusRequest;
import com.omnichannel.order.dto.response.OrderDTO;
import com.omnichannel.order.security.SecurityContextUtil;
import com.omnichannel.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    private final SecurityContextUtil securityContextUtil;
    
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        UUID customerId = securityContextUtil.getCurrentUserId(httpRequest);
        OrderDTO order = orderService.createOrder(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID customerId = securityContextUtil.getCurrentUserId(httpRequest);
        boolean isAdmin = securityContextUtil.hasRole(httpRequest, "ADMIN");
        
        OrderDTO order = orderService.getOrderById(id);
        
        // Validate ownership for non-admin users
        if (!isAdmin) {
            securityContextUtil.validateOwnership(httpRequest, order.getCustomerId());
        }
        
        return ResponseEntity.ok(order);
    }
    
    @GetMapping
    public ResponseEntity<Page<OrderDTO>> getOrders(
            HttpServletRequest httpRequest,
            Pageable pageable) {
        UUID customerId = securityContextUtil.getCurrentUserId(httpRequest);
        boolean isAdmin = securityContextUtil.hasRole(httpRequest, "ADMIN");
        
        Page<OrderDTO> orders;
        if (isAdmin) {
            orders = orderService.getAllOrders(pageable);
        } else {
            orders = orderService.getUserOrders(customerId, pageable);
        }
        
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID customerId = securityContextUtil.getCurrentUserId(httpRequest);
        
        OrderDTO order = orderService.getOrderById(id);
        securityContextUtil.validateOwnership(httpRequest, order.getCustomerId());
        
        orderService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            HttpServletRequest httpRequest) {
        securityContextUtil.validateAdminRole(httpRequest);
        
        OrderDTO order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(order);
    }
}
