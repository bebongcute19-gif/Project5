package com.omnichannel.order.service;

import com.omnichannel.order.dto.request.CreateOrderRequest;
import com.omnichannel.order.dto.request.UpdateOrderStatusRequest;
import com.omnichannel.order.dto.response.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    
    OrderDTO createOrder(UUID customerId, CreateOrderRequest request);
    
    OrderDTO getOrderById(UUID orderId);
    
    Page<OrderDTO> getUserOrders(UUID customerId, Pageable pageable);
    
    Page<OrderDTO> getAllOrders(Pageable pageable);
    
    void cancelOrder(UUID orderId);
    
    OrderDTO updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);
}
