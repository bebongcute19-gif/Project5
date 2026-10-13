package com.omnichannel.payment.controller;

import com.omnichannel.payment.dto.request.CreatePaymentRequest;
import com.omnichannel.payment.dto.request.UpdatePaymentStatusRequest;
import com.omnichannel.payment.dto.response.PaymentDTO;
import com.omnichannel.payment.security.SecurityContextUtil;
import com.omnichannel.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final SecurityContextUtil securityContextUtil;

    @PostMapping
    public ResponseEntity<PaymentDTO> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {
        UUID customerId = securityContextUtil.getCurrentUserId(httpRequest);
        PaymentDTO payment = paymentService.createPayment(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getPaymentById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID customerId = securityContextUtil.getCurrentUserId(httpRequest);
        boolean isAdmin = securityContextUtil.hasRole(httpRequest, "ADMIN");

        PaymentDTO payment = paymentService.getPaymentById(id);

        // Validate ownership for non-admin users
        if (!isAdmin) {
            securityContextUtil.validateOwnership(httpRequest, payment.getCustomerId());
        }

        return ResponseEntity.ok(payment);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDTO> getPaymentByOrderId(
            @PathVariable UUID orderId,
            HttpServletRequest httpRequest) {
        UUID customerId = securityContextUtil.getCurrentUserId(httpRequest);
        boolean isAdmin = securityContextUtil.hasRole(httpRequest, "ADMIN");

        PaymentDTO payment = paymentService.getPaymentByOrderId(orderId);

        // Validate ownership for non-admin users
        if (!isAdmin) {
            securityContextUtil.validateOwnership(httpRequest, payment.getCustomerId());
        }

        return ResponseEntity.ok(payment);
    }

    @GetMapping
    public ResponseEntity<Page<PaymentDTO>> getPayments(
            HttpServletRequest httpRequest,
            Pageable pageable) {
        UUID customerId = securityContextUtil.getCurrentUserId(httpRequest);
        boolean isAdmin = securityContextUtil.hasRole(httpRequest, "ADMIN");

        Page<PaymentDTO> payments;
        if (isAdmin) {
            payments = paymentService.getAllPayments(pageable);
        } else {
            payments = paymentService.getPaymentsByCustomer(customerId, pageable);
        }

        return ResponseEntity.ok(payments);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<PaymentDTO> updatePaymentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentStatusRequest request,
            HttpServletRequest httpRequest) {
        securityContextUtil.validateAdminRole(httpRequest);

        PaymentDTO payment = paymentService.updatePaymentStatus(id, request);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<Void> processPayment(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        securityContextUtil.validateAdminRole(httpRequest);

        paymentService.processPayment(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Void> refundPayment(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        securityContextUtil.validateAdminRole(httpRequest);

        paymentService.refundPayment(id);
        return ResponseEntity.ok().build();
    }
}
