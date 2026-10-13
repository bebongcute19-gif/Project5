package com.omnichannel.payment.repository;

import com.omnichannel.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByPaymentId(UUID paymentId);
    List<PaymentTransaction> findByPaymentIdAndType(UUID paymentId, String type);
}
