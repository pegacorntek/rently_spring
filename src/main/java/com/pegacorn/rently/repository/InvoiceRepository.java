package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findByContractId(String contractId);

    List<Invoice> findByContractIdAndPeriodMonth(String contractId, String periodMonth);

    List<Invoice> findByTenantId(String tenantId);

    List<Invoice> findByTenantIdAndStatus(String tenantId, Invoice.InvoiceStatus status);

    @Query("SELECT i FROM Invoice i JOIN Contract c ON c.id = i.contractId WHERE c.landlordId = :landlordId")
    List<Invoice> findByLandlordId(@Param("landlordId") String landlordId);

    @Query("SELECT i FROM Invoice i JOIN Contract c ON c.id = i.contractId WHERE c.landlordId = :landlordId AND i.status = :status")
    List<Invoice> findByLandlordIdAndStatus(@Param("landlordId") String landlordId, @Param("status") Invoice.InvoiceStatus status);

    @Query("SELECT i FROM Invoice i JOIN Contract c ON c.id = i.contractId JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId")
    List<Invoice> findByHouseId(@Param("houseId") String houseId);

    @Query("SELECT i FROM Invoice i JOIN Contract c ON c.id = i.contractId WHERE c.landlordId = :landlordId AND i.periodMonth = :periodMonth")
    List<Invoice> findByLandlordIdAndPeriodMonth(@Param("landlordId") String landlordId, @Param("periodMonth") String periodMonth);

    boolean existsByContractIdAndPeriodMonthAndStatusNot(String contractId, String periodMonth, Invoice.InvoiceStatus status);

    boolean existsByContractIdAndPeriodMonthAndInvoiceTypeAndStatusNot(
            String contractId, String periodMonth,
            Invoice.InvoiceType invoiceType, Invoice.InvoiceStatus status);

    @Query("SELECT i.contractId FROM Invoice i WHERE i.periodMonth = :periodMonth AND i.status <> 'CANCELLED'")
    List<String> findContractIdsWithInvoiceForPeriod(@Param("periodMonth") String periodMonth);
}
