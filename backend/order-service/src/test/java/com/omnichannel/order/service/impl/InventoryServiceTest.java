package com.omnichannel.order.service.impl;

import com.omnichannel.order.dto.response.InventoryDTO;
import com.omnichannel.order.entity.Inventory;
import com.omnichannel.order.entity.StockReservation;
import com.omnichannel.order.exception.BadRequestException;
import com.omnichannel.order.exception.ConflictException;
import com.omnichannel.order.exception.ResourceNotFoundException;
import com.omnichannel.order.repository.InventoryRepository;
import com.omnichannel.order.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private StockReservationRepository stockReservationRepository;
    
    private InventoryServiceImpl inventoryService;
    private String testSkuCode;
    private UUID testOrderId;
    
    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(inventoryRepository, stockReservationRepository);
        testSkuCode = "TEST-SKU-001";
        testOrderId = UUID.randomUUID();
    }
    
    @Test
    void testGetInventory_Success() {
        // Given
        Inventory inventory = createTestInventory(testSkuCode);
        when(inventoryRepository.findBySkuCode(testSkuCode)).thenReturn(Optional.of(inventory));
        
        // When
        InventoryDTO result = inventoryService.getInventory(testSkuCode);
        
        // Then
        assertNotNull(result);
        assertEquals(testSkuCode, result.getSkuCode());
        assertEquals(100, result.getTotalQuantity());
        assertEquals(50, result.getAvailableQuantity());
        verify(inventoryRepository).findBySkuCode(testSkuCode);
    }
    
    @Test
    void testGetInventory_NotFound() {
        // Given
        when(inventoryRepository.findBySkuCode(testSkuCode)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            inventoryService.getInventory(testSkuCode));
    }
    
    @Test
    void testGetInventory_EmptySkuCode() {
        // When & Then
        assertThrows(BadRequestException.class, () -> 
            inventoryService.getInventory(""));
    }
    
    @Test
    void testReserveInventory_Success() {
        // Given
        int quantity = 10;
        Inventory inventory = createTestInventory(testSkuCode);
        when(inventoryRepository.findBySkuCodeWithOptimisticLock(testSkuCode))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(stockReservationRepository.save(any(StockReservation.class)))
                .thenReturn(null);
        
        // When
        boolean result = inventoryService.reserveInventory(testOrderId, testSkuCode, quantity);
        
        // Then
        assertTrue(result);
        verify(inventoryRepository).findBySkuCodeWithOptimisticLock(testSkuCode);
        verify(inventoryRepository).save(any(Inventory.class));
        verify(stockReservationRepository).save(any(StockReservation.class));
    }
    
    @Test
    void testReserveInventory_InsufficientInventory() {
        // Given
        int quantity = 100; // More than available
        Inventory inventory = createTestInventory(testSkuCode);
        when(inventoryRepository.findBySkuCodeWithOptimisticLock(testSkuCode))
                .thenReturn(Optional.of(inventory));
        
        // When & Then
        assertThrows(ConflictException.class, () -> 
            inventoryService.reserveInventory(testOrderId, testSkuCode, quantity));
    }
    
    @Test
    void testReserveInventory_InvalidQuantity() {
        // When & Then
        assertThrows(BadRequestException.class, () -> 
            inventoryService.reserveInventory(testOrderId, testSkuCode, 0));
    }
    
    @Test
    void testReleaseReservation_Success() {
        // Given
        StockReservation reservation = StockReservation.builder()
                .id(UUID.randomUUID())
                .orderId(testOrderId)
                .skuCode(testSkuCode)
                .quantity(10)
                .status("RESERVED")
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        
        Inventory inventory = createTestInventory(testSkuCode);
        
        when(stockReservationRepository.findByOrderId(testOrderId))
                .thenReturn(Collections.singletonList(reservation));
        when(inventoryRepository.findBySkuCode(testSkuCode))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(stockReservationRepository.save(any(StockReservation.class))).thenReturn(reservation);
        
        // When
        inventoryService.releaseReservation(testOrderId, testSkuCode);
        
        // Then
        verify(stockReservationRepository).findByOrderId(testOrderId);
        verify(inventoryRepository).save(any(Inventory.class));
        verify(stockReservationRepository).save(any(StockReservation.class));
    }
    
    @Test
    void testReleaseReservation_NotFound() {
        // Given
        when(stockReservationRepository.findByOrderId(testOrderId))
                .thenReturn(Collections.emptyList());
        
        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            inventoryService.releaseReservation(testOrderId, testSkuCode));
    }
    
    @Test
    void testConfirmInventoryDeduction_Success() {
        // Given
        StockReservation reservation = StockReservation.builder()
                .id(UUID.randomUUID())
                .orderId(testOrderId)
                .skuCode(testSkuCode)
                .quantity(10)
                .status("RESERVED")
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        
        when(stockReservationRepository.findByOrderId(testOrderId))
                .thenReturn(Collections.singletonList(reservation));
        when(stockReservationRepository.save(any(StockReservation.class))).thenReturn(reservation);
        
        // When
        inventoryService.confirmInventoryDeduction(testOrderId, testSkuCode);
        
        // Then
        verify(stockReservationRepository).save(any(StockReservation.class));
    }
    
    private Inventory createTestInventory(String skuCode) {
        return Inventory.builder()
                .id(UUID.randomUUID())
                .skuCode(skuCode)
                .totalQuantity(100)
                .availableQuantity(50)
                .reservedQuantity(50)
                .version(1L)
                .build();
    }
}
