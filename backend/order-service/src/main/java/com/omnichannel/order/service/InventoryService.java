package com.omnichannel.order.service;

import com.omnichannel.order.dto.response.InventoryDTO;

import java.util.UUID;

public interface InventoryService {
    
    InventoryDTO getInventory(String skuCode);
    
    boolean reserveInventory(UUID orderId, String skuCode, Integer quantity);
    
    void releaseReservation(UUID orderId, String skuCode);
    
    void confirmInventoryDeduction(UUID orderId, String skuCode);
}
