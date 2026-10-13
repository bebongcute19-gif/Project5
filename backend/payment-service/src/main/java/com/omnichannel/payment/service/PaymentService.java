package com.omnichannel.payment.service;

import com.omnichannel.payment.dto.request.CreatePaymentRequest;
import com.omnichannel.payment.dto.request.UpdatePaymentStatusRequest;
import com.omnichannel.payment.dto.response.PaymentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {
    PaymentDTO createPayment(UUID customerId, CreatePaymentRequest request);

    PaymentDTO getPaymentById(UUID paymentId);

    PaymentDTO getPaymentByOrderId(UUID orderId);

    Page<PaymentDTO> getPaymentsByCustomer(UUID customerId, Pageable pageable);

    Page<PaymentDTO> getPaymentsByStatus(String status, Pageable pageable);

    PaymentDTO updatePaymentStatus(UUID paymentId, UpdatePaymentStatusRequest request);

    void processPayment(UUID paymentId);

    void refundPayment(UUID paymentId);

    Page<PaymentDTO> getAllPayments(Pageable pageable);
}
