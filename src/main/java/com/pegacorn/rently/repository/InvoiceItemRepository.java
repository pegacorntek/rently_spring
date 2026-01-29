package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, String> {

    List<InvoiceItem> findByInvoiceId(String invoiceId);

    List<InvoiceItem> findByInvoiceIdIn(List<String> invoiceIds);

    @Modifying
    @Query("DELETE FROM InvoiceItem ii WHERE ii.invoiceId = :invoiceId")
    void deleteByInvoiceId(@Param("invoiceId") String invoiceId);
}
