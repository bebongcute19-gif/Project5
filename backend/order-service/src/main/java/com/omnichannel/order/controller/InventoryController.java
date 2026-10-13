package com.omnichannel.order.controller;

import com.omnichannel.order.dto.response.InventoryDTO;
import com.omnichannel.order.repository.InventoryRepository;
import com.omnichannel.order.security.SecurityContextUtil;
import com.omnichannel.order.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final SecurityContextUtil securityContextUtil;
    
    @GetMapping("/{skuCode}")
    public ResponseEntity<InventoryDTO> getInventory(@PathVariable String skuCode) {
        InventoryDTO inventory = inventoryService.getInventory(skuCode);
        return ResponseEntity.ok(inventory);
    }
    
    @GetMapping("/admin/inventory")
    public ResponseEntity<Page<InventoryDTO>> getAllInventory(
            HttpServletRequest httpRequest,
            Pageable pageable) {
        securityContextUtil.validateAdminRole(httpRequest);
        
        Page<InventoryDTO> inventory = inventoryRepository.findAll(pageable)
                .map(inv -> InventoryDTO.builder()
                        .id(inv.getId())
                        .skuCode(inv.getSkuCode())
                        .totalQuantity(inv.getTotalQuantity())
                        .availableQuantity(inv.getAvailableQuantity())
                        .reservedQuantity(inv.getReservedQuantity())
                        .version(inv.getVersion())
                        .updatedAt(inv.getUpdatedAt())
                        .build());
        
        return ResponseEntity.ok(inventory);
    }
}
