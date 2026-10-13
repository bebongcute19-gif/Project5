package com.omnichannel.order.service.impl;

import com.omnichannel.order.dto.response.InventoryDTO;
import com.omnichannel.order.entity.Inventory;
import com.omnichannel.order.entity.StockReservation;
import com.omnichannel.order.exception.BadRequestException;
import com.omnichannel.order.exception.ConflictException;
import com.omnichannel.order.exception.ResourceNotFoundException;
import com.omnichannel.order.repository.InventoryRepository;
import com.omnichannel.order.repository.StockReservationRepository;
import com.omnichannel.order.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository stockReservationRepository;
    
    private static final int MAX_RETRIES = 3;
    private static final long RESERVATION_EXPIRY_MINUTES = 15;
    
    @Override
    @Transactional(readOnly = true)
    public InventoryDTO getInventory(String skuCode) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new BadRequestException("SKU code cannot be empty");
        }
        
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + skuCode));
        
        return mapToDTO(inventory);
    }
    
    @Override
    @Transactional
    public boolean reserveInventory(UUID orderId, String skuCode, Integer quantity) {
        if (orderId == null) {
            throw new BadRequestException("Order ID cannot be null");
        }
        if (skuCode == null || skuCode.isBlank()) {
            throw new BadRequestException("SKU code cannot be empty");
        }
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }
        
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                return performReservation(orderId, skuCode, quantity);
            } catch (ObjectOptimisticLockingFailureException ex) {
                retries++;
                if (retries >= MAX_RETRIES) {
                    throw new ConflictException("Failed to reserve inventory after " + MAX_RETRIES + " attempts");
                }
                log.warn("Optimistic lock conflict, retrying... attempt {}/{}", retries, MAX_RETRIES);
            }
        }
        
        return false;
    }
    
    private boolean performReservation(UUID orderId, String skuCode, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuCodeWithOptimisticLock(skuCode)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + skuCode));
        
        if (inventory.getAvailableQuantity() < quantity) {
            throw new ConflictException("Insufficient inventory for SKU: " + skuCode + 
                    ". Available: " + inventory.getAvailableQuantity() + ", Requested: " + quantity);
        }
        
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);
        
        StockReservation reservation = StockReservation.builder()
                .orderId(orderId)
                .skuCode(skuCode)
                .quantity(quantity)
                .status("RESERVED")
                .expiresAt(Instant.now().plusSeconds(RESERVATION_EXPIRY_MINUTES * 60))
                .build();
        
        stockReservationRepository.save(reservation);
        
        return true;
    }
    
    @Override
    @Transactional
    public void releaseReservation(UUID orderId, String skuCode) {
        if (orderId == null) {
            throw new BadRequestException("Order ID cannot be null");
        }
        if (skuCode == null || skuCode.isBlank()) {
            throw new BadRequestException("SKU code cannot be empty");
        }
        
        StockReservation reservation = stockReservationRepository.findByOrderId(orderId).stream()
                .filter(r -> r.getSkuCode().equals(skuCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock reservation not found for order: " + orderId + ", SKU: " + skuCode));
        
        if ("RELEASED".equals(reservation.getStatus()) || "CONFIRMED".equals(reservation.getStatus())) {
            throw new ConflictException("Reservation has already been processed");
        }
        
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + skuCode));
        
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - reservation.getQuantity()));
        inventoryRepository.save(inventory);
        
        reservation.setStatus("RELEASED");
        stockReservationRepository.save(reservation);
    }
    
    @Override
    @Transactional
    public void confirmInventoryDeduction(UUID orderId, String skuCode) {
        if (orderId == null) {
            throw new BadRequestException("Order ID cannot be null");
        }
        if (skuCode == null || skuCode.isBlank()) {
            throw new BadRequestException("SKU code cannot be empty");
        }
        
        StockReservation reservation = stockReservationRepository.findByOrderId(orderId).stream()
                .filter(r -> r.getSkuCode().equals(skuCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock reservation not found for order: " + orderId + ", SKU: " + skuCode));
        
        if ("CONFIRMED".equals(reservation.getStatus())) {
            return;
        }
        
        if ("RELEASED".equals(reservation.getStatus())) {
            throw new ConflictException("Cannot confirm a released reservation");
        }
        
        reservation.setStatus("CONFIRMED");
        stockReservationRepository.save(reservation);
    }
    
    private InventoryDTO mapToDTO(Inventory inventory) {
        return InventoryDTO.builder()
                .id(inventory.getId())
                .skuCode(inventory.getSkuCode())
                .totalQuantity(inventory.getTotalQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .version(inventory.getVersion())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
