package com.omnichannel.order.service.impl;

import com.omnichannel.order.constant.OrderStatus;
import com.omnichannel.order.dto.request.CreateOrderRequest;
import com.omnichannel.order.dto.request.UpdateOrderStatusRequest;
import com.omnichannel.order.dto.response.CartDTO;
import com.omnichannel.order.dto.response.CartItemDTO;
import com.omnichannel.order.dto.response.OrderDTO;
import com.omnichannel.order.entity.Order;
import com.omnichannel.order.entity.OrderItem;
import com.omnichannel.order.exception.BadRequestException;
import com.omnichannel.order.exception.ConflictException;
import com.omnichannel.order.exception.ResourceNotFoundException;
import com.omnichannel.order.repository.OrderItemRepository;
import com.omnichannel.order.repository.OrderRepository;
import com.omnichannel.order.service.CartService;
import com.omnichannel.order.service.InventoryService;
import com.omnichannel.order.service.ProductGrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderItemRepository orderItemRepository;
    
    @Mock
    private InventoryService inventoryService;
    
    @Mock
    private CartService cartService;
    
    @Mock
    private ProductGrpcClient productGrpcClient;
    
    private OrderServiceImpl orderService;
    private UUID testCustomerId;
    private UUID testOrderId;
    
    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, orderItemRepository, 
                inventoryService, cartService, productGrpcClient);
        testCustomerId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
    }
    
    @Test
    void testCreateOrder_Success() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddressId(UUID.randomUUID())
                .build();
        
        CartItemDTO cartItem = CartItemDTO.builder()
                .itemId(UUID.randomUUID())
                .skuCode("SKU-001")
                .productName("Test Product")
                .price(BigDecimal.valueOf(19.99))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(39.98))
                .build();
        
        CartDTO cartDTO = CartDTO.builder()
                .items(List.of(cartItem))
                .subtotal(BigDecimal.valueOf(39.98))
                .total(BigDecimal.valueOf(39.98))
                .itemCount(2)
                .build();
        
        Order savedOrder = createTestOrder(testOrderId, testCustomerId);
        
        when(cartService.getCart(testCustomerId)).thenReturn(cartDTO);
        when(inventoryService.reserveInventory(any(UUID.class), anyString(), anyInt())).thenReturn(true);
        when(orderRepository.saveAndFlush(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(cartService).clearCart(testCustomerId);
        
        // When
        OrderDTO result = orderService.createOrder(testCustomerId, request);
        
        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(testCustomerId, result.getCustomerId());
        assertTrue(result.getOrderNumber().startsWith("ORD-"));
        verify(cartService).getCart(testCustomerId);
        verify(inventoryService).reserveInventory(any(UUID.class), anyString(), anyInt());
        verify(orderRepository).saveAndFlush(any(Order.class));
        verify(cartService).clearCart(testCustomerId);
    }
    
    @Test
    void testCreateOrder_EmptyCart() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddressId(UUID.randomUUID())
                .build();
        
        CartDTO cartDTO = CartDTO.builder()
                .items(new ArrayList<>())
                .build();
        
        when(cartService.getCart(testCustomerId)).thenReturn(cartDTO);
        
        // When & Then
        assertThrows(BadRequestException.class, () -> 
            orderService.createOrder(testCustomerId, request));
    }
    
    @Test
    void testGetOrderById_Success() {
        // Given
        Order order = createTestOrder(testOrderId, testCustomerId);
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(order));
        
        // When
        OrderDTO result = orderService.getOrderById(testOrderId);
        
        // Then
        assertNotNull(result);
        assertEquals(testOrderId, result.getId());
        assertEquals(testCustomerId, result.getCustomerId());
        verify(orderRepository).findById(testOrderId);
    }
    
    @Test
    void testGetOrderById_NotFound() {
        // Given
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            orderService.getOrderById(testOrderId));
    }
    
    @Test
    void testCancelOrder_Success() {
        // Given
        Order order = createTestOrder(testOrderId, testCustomerId);
        order.setStatus(OrderStatus.PENDING);
        
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(order));
        doNothing().when(inventoryService).releaseReservation(any(UUID.class), anyString());
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        
        // When
        orderService.cancelOrder(testOrderId);
        
        // Then
        verify(orderRepository).findById(testOrderId);
        verify(inventoryService).releaseReservation(any(UUID.class), anyString());
        verify(orderRepository).save(any(Order.class));
    }
    
    @Test
    void testCancelOrder_InvalidStatus() {
        // Given
        Order order = createTestOrder(testOrderId, testCustomerId);
        order.setStatus(OrderStatus.SHIPPED);
        
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(order));
        
        // When & Then
        assertThrows(ConflictException.class, () -> 
            orderService.cancelOrder(testOrderId));
    }
    
    @Test
    void testUpdateOrderStatus_Success() {
        // Given
        Order order = createTestOrder(testOrderId, testCustomerId);
        order.setStatus(OrderStatus.PENDING);
        
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();
        
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        
        // When
        OrderDTO result = orderService.updateOrderStatus(testOrderId, request);
        
        // Then
        assertNotNull(result);
        verify(orderRepository).findById(testOrderId);
        verify(orderRepository).save(any(Order.class));
    }
    
    @Test
    void testUpdateOrderStatus_InvalidTransition() {
        // Given
        Order order = createTestOrder(testOrderId, testCustomerId);
        order.setStatus(OrderStatus.PENDING);
        
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.SHIPPED)
                .build();
        
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(order));
        
        // When & Then
        assertThrows(ConflictException.class, () -> 
            orderService.updateOrderStatus(testOrderId, request));
    }
    
    @Test
    void testUpdateOrderStatus_ToDelivered() {
        // Given
        Order order = createTestOrder(testOrderId, testCustomerId);
        order.setStatus(OrderStatus.SHIPPED);
        
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.DELIVERED)
                .build();
        
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(order));
        doNothing().when(inventoryService).confirmInventoryDeduction(any(UUID.class), anyString());
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        
        // When
        OrderDTO result = orderService.updateOrderStatus(testOrderId, request);
        
        // Then
        assertNotNull(result);
        verify(inventoryService).confirmInventoryDeduction(any(UUID.class), anyString());
        verify(orderRepository).save(any(Order.class));
    }
    
    private Order createTestOrder(UUID orderId, UUID customerId) {
        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .skuCode("SKU-001")
                .productName("Test Product")
                .price(BigDecimal.valueOf(19.99))
                .quantity(2)
                .build();
        
        Order order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-" + System.currentTimeMillis() + "-TEST")
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(39.98))
                .shippingAddressId(UUID.randomUUID())
                .build();
        
        order.addItem(item);
        return order;
    }
}
