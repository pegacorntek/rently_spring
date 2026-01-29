package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {

    List<Expense> findByHouseId(String houseId);

    List<Expense> findByCategoryId(String categoryId);

    @Query("SELECT e FROM Expense e JOIN House h ON h.id = e.houseId WHERE h.ownerId = :landlordId ORDER BY e.expenseDate DESC")
    List<Expense> findByLandlordId(@Param("landlordId") String landlordId);

    @Query("SELECT e FROM Expense e JOIN House h ON h.id = e.houseId WHERE h.ownerId = :landlordId AND MONTH(e.expenseDate) = :month AND YEAR(e.expenseDate) = :year ORDER BY e.expenseDate DESC")
    List<Expense> findByLandlordIdAndMonthYear(@Param("landlordId") String landlordId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT e FROM Expense e JOIN House h ON h.id = e.houseId WHERE h.ownerId = :landlordId AND e.houseId = :houseId ORDER BY e.expenseDate DESC")
    List<Expense> findByLandlordIdAndHouseId(@Param("landlordId") String landlordId, @Param("houseId") String houseId);

    @Query("SELECT e FROM Expense e JOIN House h ON h.id = e.houseId WHERE h.ownerId = :landlordId AND e.status = :status ORDER BY e.expenseDate DESC")
    List<Expense> findByLandlordIdAndStatus(@Param("landlordId") String landlordId, @Param("status") Expense.ExpenseStatus status);

    @Query("SELECT e FROM Expense e JOIN House h ON h.id = e.houseId WHERE h.ownerId = :landlordId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<Expense> findByLandlordIdAndDateRange(@Param("landlordId") String landlordId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e JOIN House h ON h.id = e.houseId WHERE h.ownerId = :landlordId AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByLandlordIdAndDateRange(@Param("landlordId") String landlordId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.houseId = :houseId AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByHouseIdAndDateRange(@Param("houseId") String houseId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e JOIN House h ON h.id = e.houseId WHERE h.ownerId = :landlordId AND e.status = :status AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByLandlordIdAndStatusAndDateRange(@Param("landlordId") String landlordId, @Param("status") Expense.ExpenseStatus status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    boolean existsByCategoryId(String categoryId);

    // Total expense amount by house (all time)
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.houseId = :houseId")
    BigDecimal sumAmountByHouseId(@Param("houseId") String houseId);
}
