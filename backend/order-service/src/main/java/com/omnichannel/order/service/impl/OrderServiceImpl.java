
package com.omnichannel.order.service.impl;

import com.omnichannel.order.constant.OrderStatus;
import com.omnichannel.order.dto.request.CreateOrderRequest;
import com.omnichannel.order.dto.request.UpdateOrderStatusRequest;
import com.omnichannel.order.dto.response.CartDTO;
import com.omnichannel.order.dto.response.CartItemDTO;
import com.omnichannel.order.dto.response.OrderDTO;
import com.omnichannel.order.dto.response.OrderItemDTO;
import com.omnichannel.order.entity.Order;
import com.omnichannel.order.entity.OrderItem;
import com.omnichannel.order.exception.BadRequestException;
import com.omnichannel.order.exception.ConflictException;
import com.omnichannel.order.exception.ResourceNotFoundException;
import com.omnichannel.order.repository.OrderItemRepository;
import com.omnichannel.order.repository.OrderRepository;
import com.omnichannel.order.service.CartService;
import com.omnichannel.order.service.InventoryService;
import com.omnichannel.order.service.OrderService;
import com.omnichannel.order.service.ProductGrpcClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;
    private final CartService cartService;
    private final ProductGrpcClient productGrpcClient;

    @Override
    @Transactional
    public OrderDTO createOrder(UUID customerId, CreateOrderRequest request) {

        // 1. Validate request
        if (customerId == null) {
            throw new BadRequestException("Customer ID cannot be null");
        }

        if (request == null || request.getShippingAddressId() == null) {
            throw new BadRequestException("Shipping address ID is required");
        }

        // 2. Get cart
        CartDTO cart = cartService.getCart(customerId);

        if (cart == null || cart.getItems() == null
                || cart.getItems().isEmpty()) {
            throw new BadRequestException(
                    "Cart is empty. Cannot create order from an empty cart"
            );
        }

        // 3. Prepare order items and calculate total
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItemDTO cartItem : cart.getItems()) {

            if (cartItem.getSkuCode() == null
                    || cartItem.getSkuCode().isBlank()) {
                throw new BadRequestException("SKU code is required");
            }

            if (cartItem.getQuantity() == null
                    || cartItem.getQuantity() < 1) {
                throw new BadRequestException(
                        "Invalid quantity for SKU: " + cartItem.getSkuCode()
                );
            }

            if (cartItem.getPrice() == null) {
                throw new BadRequestException(
                        "Price is missing for SKU: " + cartItem.getSkuCode()
                );
            }

            OrderItem orderItem = OrderItem.builder()
                    .skuCode(cartItem.getSkuCode())
                    .productName(cartItem.getProductName())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();

            orderItems.add(orderItem);

            BigDecimal itemTotal = cartItem.getPrice().multiply(
                    BigDecimal.valueOf(cartItem.getQuantity())
            );

            totalAmount = totalAmount.add(itemTotal);
        }

        // 4. Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingAddressId(request.getShippingAddressId())
                .build();

        // 5. Attach order items
        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        // 6. Save order FIRST to generate a valid order ID
        Order savedOrder = orderRepository.saveAndFlush(order);

        UUID orderId = savedOrder.getId();

        log.info("Order created with ID: {}", orderId);

        // 7. Reserve inventory using the REAL order ID
        for (CartItemDTO cartItem : cart.getItems()) {

            try {
                boolean reserved = inventoryService.reserveInventory(
                        orderId,
                        cartItem.getSkuCode(),
                        cartItem.getQuantity()
                );

                if (!reserved) {
                    throw new ConflictException(
                            "Failed to reserve inventory for SKU: "
                                    + cartItem.getSkuCode()
                    );
                }

                log.info(
                        "Inventory reserved. Order: {}, SKU: {}, Quantity: {}",
                        orderId,
                        cartItem.getSkuCode(),
                        cartItem.getQuantity()
                );

            } catch (Exception ex) {

                log.error(
                        "Error reserving inventory. Order: {}, SKU: {}",
                        orderId,
                        cartItem.getSkuCode(),
                        ex
                );

                throw new ConflictException(
                        "Failed to reserve inventory for SKU: "
                                + cartItem.getSkuCode()
                );
            }
        }

        // 8. Clear cart only after successful reservations
        cartService.clearCart(customerId);

        log.info(
                "Order successfully created. ID: {}, Total: {}",
                orderId,
                totalAmount
        );

        // 9. Return order
        return mapToDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(UUID orderId) {

        if (orderId == null) {
            throw new BadRequestException("Order ID cannot be null");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        return mapToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getUserOrders(UUID customerId, Pageable pageable) {

        if (customerId == null) {
            throw new BadRequestException("Customer ID cannot be null");
        }

        return orderRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(Pageable pageable) {

        return orderRepository.findAllOrders(pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public void cancelOrder(UUID orderId) {

        if (orderId == null) {
            throw new BadRequestException("Order ID cannot be null");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        if (!OrderStatus.PENDING.equals(order.getStatus())
                && !OrderStatus.CONFIRMED.equals(order.getStatus())) {

            throw new ConflictException(
                    "Cannot cancel order with status: " + order.getStatus()
            );
        }

        // Release inventory reservations
        for (OrderItem item : order.getItems()) {

            try {
                inventoryService.releaseReservation(
                        order.getId(),
                        item.getSkuCode()
                );

            } catch (Exception ex) {

                log.error(
                        "Error releasing inventory. Order: {}, SKU: {}",
                        orderId,
                        item.getSkuCode(),
                        ex
                );

                throw new ConflictException(
                        "Failed to release inventory reservation for SKU: "
                                + item.getSkuCode()
                );
            }
        }

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);

        log.info("Order cancelled: {}", orderId);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(
            UUID orderId,
            UpdateOrderStatusRequest request
    ) {

        if (orderId == null) {
            throw new BadRequestException("Order ID cannot be null");
        }

        if (request == null
                || request.getStatus() == null
                || request.getStatus().isBlank()) {

            throw new BadRequestException("Status is required");
        }

        String newStatus = request.getStatus().toUpperCase();

        if (!OrderStatus.ALL_STATUSES.contains(newStatus)) {
            throw new BadRequestException(
                    "Invalid order status: " + newStatus
            );
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        if (!OrderStatus.isValidTransition(
                order.getStatus(),
                newStatus
        )) {

            throw new ConflictException(
                    "Cannot transition from "
                            + order.getStatus()
                            + " to "
                            + newStatus
            );
        }

        order.setStatus(newStatus);

        // Confirm inventory deduction when delivered
        if (OrderStatus.DELIVERED.equals(newStatus)) {

            for (OrderItem item : order.getItems()) {

                try {
                    inventoryService.confirmInventoryDeduction(
                            order.getId(),
                            item.getSkuCode()
                    );

                } catch (Exception ex) {

                    log.error(
                            "Error confirming inventory deduction. Order: {}, SKU: {}",
                            orderId,
                            item.getSkuCode(),
                            ex
                    );

                    throw new ConflictException(
                            "Failed to confirm inventory deduction for SKU: "
                                    + item.getSkuCode()
                    );
                }
            }
        }

        Order updatedOrder = orderRepository.save(order);

        log.info(
                "Order status updated. ID: {}, Status: {}",
                orderId,
                newStatus
        );

        return mapToDTO(updatedOrder);
    }

    private String generateOrderNumber() {

        long timestamp = System.currentTimeMillis();

        String uuidPrefix = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return "ORD-" + timestamp + "-" + uuidPrefix;
    }

    private OrderDTO mapToDTO(Order order) {

        List<OrderItemDTO> itemDTOs =
                order.getItems() != null
                        ? order.getItems()
                        .stream()
                        .map(this::mapItemToDTO)
                        .collect(Collectors.toList())
                        : new ArrayList<>();

        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .taxAmount(order.getTaxAmount())
                .shippingFee(order.getShippingFee())
                .shippingAddressId(order.getShippingAddressId())
                .items(itemDTOs)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemDTO mapItemToDTO(OrderItem item) {

        return OrderItemDTO.builder()
                .id(item.getId())
                .skuCode(item.getSkuCode())
                .productName(item.getProductName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .build();
    }
}
