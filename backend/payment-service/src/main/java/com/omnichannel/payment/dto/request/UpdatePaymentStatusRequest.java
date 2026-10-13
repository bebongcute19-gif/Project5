package com.omnichannel.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePaymentStatusRequest {
    @NotBlank(message = "Status is required")
    private String status;

    private String failureReason;

    private String externalTransactionId;
}
