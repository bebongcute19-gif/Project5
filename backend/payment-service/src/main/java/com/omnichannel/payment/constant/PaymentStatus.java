package com.omnichannel.payment.constant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PaymentStatus {
    public static final String PENDING = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";
    public static final String CANCELLED = "CANCELLED";
    public static final String REFUNDED = "REFUNDED";

    public static final Set<String> ALL_STATUSES = new HashSet<>(Arrays.asList(
            PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELLED, REFUNDED
    ));

    public static boolean isValidStatus(String status) {
        return status != null && ALL_STATUSES.contains(status.toUpperCase());
    }

    public static boolean isValidTransition(String currentStatus, String newStatus) {
        if (!isValidStatus(currentStatus) || !isValidStatus(newStatus)) {
            return false;
        }

        return switch (currentStatus.toUpperCase()) {
            case PENDING -> newStatus.equalsIgnoreCase(PROCESSING) || newStatus.equalsIgnoreCase(CANCELLED);
            case PROCESSING -> newStatus.equalsIgnoreCase(SUCCEEDED) || newStatus.equalsIgnoreCase(FAILED);
            case SUCCEEDED -> newStatus.equalsIgnoreCase(REFUNDED);
            case FAILED -> newStatus.equalsIgnoreCase(CANCELLED);
            case CANCELLED -> false;
            case REFUNDED -> false;
            default -> false;
        };
    }

    private PaymentStatus() {}
}
