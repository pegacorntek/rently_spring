package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.SepayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SepayTransactionRepository extends JpaRepository<SepayTransaction, String> {

    boolean existsBySepayTransactionId(String sepayTransactionId);

    Optional<SepayTransaction> findBySepayTransactionId(String sepayTransactionId);

    List<SepayTransaction> findByInvoiceId(String invoiceId);

    List<SepayTransaction> findByStatus(SepayTransaction.TransactionStatus status);
}
