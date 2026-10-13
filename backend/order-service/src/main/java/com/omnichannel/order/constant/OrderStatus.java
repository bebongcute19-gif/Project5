package com.omnichannel.order.constant;

import java.util.HashSet;
import java.util.Set;

public class OrderStatus {
    
    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String PROCESSING = "PROCESSING";
    public static final String SHIPPED = "SHIPPED";
    public static final String DELIVERED = "DELIVERED";
    public static final String CANCELLED = "CANCELLED";
    public static final String FAILED = "FAILED";
    
    public static final Set<String> ALL_STATUSES = Set.of(
        PENDING,
        CONFIRMED,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        FAILED
    );
    
    public static boolean isValidTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        if (!ALL_STATUSES.contains(newStatus)) {
            return false;
        }
        
        // Can transition to CANCELLED from any status
        if (CANCELLED.equals(newStatus)) {
            return !CANCELLED.equals(currentStatus) && !DELIVERED.equals(currentStatus);
        }
        
        // Can transition to FAILED from any status
        if (FAILED.equals(newStatus)) {
            return !FAILED.equals(currentStatus) && !DELIVERED.equals(currentStatus);
        }
        
        // Normal transitions
        return switch (currentStatus) {
            case PENDING -> CONFIRMED.equals(newStatus);
            case CONFIRMED -> PROCESSING.equals(newStatus);
            case PROCESSING -> SHIPPED.equals(newStatus);
            case SHIPPED -> DELIVERED.equals(newStatus);
            default -> false;
        };
    }
    
    private OrderStatus() {
    }
}
