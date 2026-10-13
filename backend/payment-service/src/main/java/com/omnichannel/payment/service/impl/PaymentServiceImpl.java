package com.omnichannel.payment.service.impl;

import com.omnichannel.payment.constant.PaymentStatus;
import com.omnichannel.payment.dto.request.CreatePaymentRequest;
import com.omnichannel.payment.dto.request.UpdatePaymentStatusRequest;
import com.omnichannel.payment.dto.response.PaymentDTO;
import com.omnichannel.payment.dto.response.PaymentTransactionDTO;
import com.omnichannel.payment.entity.Payment;
import com.omnichannel.payment.entity.PaymentTransaction;
import com.omnichannel.payment.exception.BadRequestException;
import com.omnichannel.payment.exception.ConflictException;
import com.omnichannel.payment.exception.ResourceNotFoundException;
import com.omnichannel.payment.repository.PaymentRepository;
import com.omnichannel.payment.repository.PaymentTransactionRepository;
import com.omnichannel.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    @Transactional
    public PaymentDTO createPayment(UUID customerId, CreatePaymentRequest request) {
        if (customerId == null) {
            throw new BadRequestException("Customer ID cannot be null");
        }
        if (request == null || request.getOrderId() == null) {
            throw new BadRequestException("Order ID is required");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }

        // Check for idempotency
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                return mapToDTO(existing.get());
            }
        }

        // Generate transaction ID
        String transactionId = generateTransactionId();

        // Create Payment entity
        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .orderId(request.getOrderId())
                .customerId(customerId)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created: {} for Order: {}", transactionId, request.getOrderId());

        return mapToDTO(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(UUID paymentId) {
        if (paymentId == null) {
            throw new BadRequestException("Payment ID cannot be null");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        return mapToDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentByOrderId(UUID orderId) {
        if (orderId == null) {
            throw new BadRequestException("Order ID cannot be null");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for Order: " + orderId));

        return mapToDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> getPaymentsByCustomer(UUID customerId, Pageable pageable) {
        if (customerId == null) {
            throw new BadRequestException("Customer ID cannot be null");
        }

        return paymentRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> getPaymentsByStatus(String status, Pageable pageable) {
        if (!PaymentStatus.isValidStatus(status)) {
            throw new BadRequestException("Invalid payment status: " + status);
        }

        return paymentRepository.findByStatus(status.toUpperCase(), pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public PaymentDTO updatePaymentStatus(UUID paymentId, UpdatePaymentStatusRequest request) {
        if (paymentId == null) {
            throw new BadRequestException("Payment ID cannot be null");
        }
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new BadRequestException("Status is required");
        }

        String newStatus = request.getStatus().toUpperCase();

        if (!PaymentStatus.isValidStatus(newStatus)) {
            throw new BadRequestException("Invalid payment status: " + newStatus);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        // Validate status transition
        if (!PaymentStatus.isValidTransition(payment.getStatus(), newStatus)) {
            throw new ConflictException("Cannot transition from " + payment.getStatus() + " to " + newStatus);
        }

        payment.setStatus(newStatus);
        if (request.getFailureReason() != null) {
            payment.setFailureReason(request.getFailureReason());
        }
        if (request.getExternalTransactionId() != null) {
            payment.setExternalTransactionId(request.getExternalTransactionId());
        }

        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Payment {} status updated to {}", paymentId, newStatus);

        return mapToDTO(updatedPayment);
    }

    @Override
    @Transactional
    public void processPayment(UUID paymentId) {
        if (paymentId == null) {
            throw new BadRequestException("Payment ID cannot be null");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            throw new ConflictException("Can only process payments in PENDING status. Current status: " + payment.getStatus());
        }

        // Update status to PROCESSING
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Add transaction record
        PaymentTransaction transaction = PaymentTransaction.builder()
                .payment(payment)
                .type("AUTHORIZATION")
                .status("PENDING")
                .build();
        paymentTransactionRepository.save(transaction);

        // TODO: Call payment gateway/provider here
        // For now, simulate success
        simulatePaymentProcessing(payment, transaction);

        log.info("Payment {} processing initiated", paymentId);
    }

    @Override
    @Transactional
    public void refundPayment(UUID paymentId) {
        if (paymentId == null) {
            throw new BadRequestException("Payment ID cannot be null");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (!PaymentStatus.SUCCEEDED.equals(payment.getStatus())) {
            throw new ConflictException("Can only refund succeeded payments. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        // Add refund transaction record
        PaymentTransaction refundTx = PaymentTransaction.builder()
                .payment(payment)
                .type("REFUND")
                .status("SUCCEEDED")
                .build();
        paymentTransactionRepository.save(refundTx);

        log.info("Payment {} refunded", paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    private void simulatePaymentProcessing(Payment payment, PaymentTransaction transaction) {
        // Simulate successful payment
        transaction.setStatus("SUCCEEDED");
        transaction.setReference("SIM-" + UUID.randomUUID().toString());
        paymentTransactionRepository.save(transaction);

        // Update payment status to succeeded
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setExternalTransactionId(transaction.getReference());
        paymentRepository.save(payment);
    }

    private String generateTransactionId() {
        long timestamp = System.currentTimeMillis();
        String uuidPrefix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PAY-" + timestamp + "-" + uuidPrefix;
    }

    private PaymentDTO mapToDTO(Payment payment) {
        List<PaymentTransactionDTO> transactionDTOs = payment.getTransactions() != null ?
                payment.getTransactions().stream()
                        .map(this::mapTransactionToDTO)
                        .collect(Collectors.toList()) :
                List.of();

        return PaymentDTO.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .failureReason(payment.getFailureReason())
                .externalTransactionId(payment.getExternalTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .transactions(transactionDTOs)
                .build();
    }

    private PaymentTransactionDTO mapTransactionToDTO(PaymentTransaction transaction) {
        return PaymentTransactionDTO.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .reference(transaction.getReference())
                .errorMessage(transaction.getErrorMessage())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
