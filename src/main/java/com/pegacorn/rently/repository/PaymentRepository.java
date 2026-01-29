package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByInvoiceId(String invoiceId);

    List<Payment> findByInvoiceIdAndStatus(String invoiceId, Payment.PaymentStatus status);

    boolean existsByInvoiceIdAndStatus(String invoiceId, Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p JOIN Invoice i ON i.id = p.invoiceId WHERE i.tenantId = :tenantId")
    List<Payment> findByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT p FROM Payment p JOIN Invoice i ON i.id = p.invoiceId JOIN Contract c ON c.id = i.contractId WHERE c.landlordId = :landlordId")
    List<Payment> findByLandlordId(@Param("landlordId") String landlordId);

    Optional<Payment> findBySepayTransactionId(String sepayTransactionId);
}
