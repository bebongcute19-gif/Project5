package com.omnichannel.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {
    private UUID id;
    private String transactionId;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private String failureReason;
    private String externalTransactionId;

    private Instant createdAt;

    private Instant updatedAt;

    private List<PaymentTransactionDTO> transactions;
}
