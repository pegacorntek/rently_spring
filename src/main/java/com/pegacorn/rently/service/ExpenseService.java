package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.expense.*;
import com.pegacorn.rently.entity.Expense;
import com.pegacorn.rently.entity.ExpenseCategoryType;
import com.pegacorn.rently.entity.House;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.ExpenseRepository;
import com.pegacorn.rently.repository.HouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final HouseRepository houseRepository;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    public List<ExpenseDto> getAllByLandlord(String landlordId, String houseId, String categoryId, String status,
            String month, String year) {
        List<Expense> expenses;

        if (houseId != null && !houseId.isEmpty()) {
            // Validate house ownership
            House house = houseRepository.findById(houseId)
                    .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
            if (!house.getOwnerId().equals(landlordId)) {
                throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
            }
            expenses = expenseRepository.findByLandlordIdAndHouseId(landlordId, houseId);
        } else {
            expenses = expenseRepository.findByLandlordId(landlordId);
        }

        // Apply filters
        if (categoryId != null && !categoryId.isEmpty()) {
            expenses = expenses.stream()
                    .filter(e -> e.getCategoryId().equals(categoryId))
                    .toList();
        }

        if (status != null && !status.isEmpty()) {
            Expense.ExpenseStatus statusEnum = Expense.ExpenseStatus.valueOf(status);
            expenses = expenses.stream()
                    .filter(e -> e.getStatus() == statusEnum)
                    .toList();
        }

        if (month != null && year != null && !month.isEmpty() && !year.isEmpty()) {
            int m = Integer.parseInt(month);
            int y = Integer.parseInt(year);
            expenses = expenses.stream()
                    .filter(e -> e.getExpenseDate().getMonthValue() == m && e.getExpenseDate().getYear() == y)
                    .toList();
        }

        // Enrich with category and house names
        return enrichExpenses(expenses);
    }

    public ExpenseDto getById(String id, String landlordId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.EXPENSE_NOT_FOUND));

        House house = houseRepository.findById(expense.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        return enrichExpense(expense);
    }

    @Transactional
    public ExpenseDto create(CreateExpenseRequest request, String landlordId, MultipartFile receipt) {
        // Validate house ownership
        House house = houseRepository.findById(request.houseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
        if (!house.getOwnerId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // Validate category is a valid fixed category
        if (!ExpenseCategoryType.isValid(request.categoryId())) {
            throw ApiException.badRequest("Danh mục không hợp lệ");
        }

        // Handle receipt upload
        String receiptUrl = null;
        if (receipt != null && !receipt.isEmpty()) {
            receiptUrl = saveFile(receipt, "receipts");
        }

        LocalDate expenseDate = LocalDate.parse(request.expenseDate(), DateTimeFormatter.ISO_DATE);

        // Determine status - default to PENDING if not provided
        Expense.ExpenseStatus status = request.status() != null
                ? request.status()
                : Expense.ExpenseStatus.PENDING;

        Expense expense = Expense.builder()
                .id(UUID.randomUUID().toString())
                .houseId(request.houseId())
                .categoryId(request.categoryId())
                .title(request.title())
                .description(request.description())
                .amount(request.amount())
                .expenseDate(expenseDate)
                .receiptUrl(receiptUrl)
                .status(status)
                .paidAt(status == Expense.ExpenseStatus.PAID ? LocalDateTime.now() : null)
                .paymentMethod(status == Expense.ExpenseStatus.PAID ? request.paymentMethod() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        expenseRepository.save(expense);
        return enrichExpense(expense);
    }

    @Transactional
    public ExpenseDto update(String id, UpdateExpenseRequest request, String landlordId, MultipartFile receipt) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.EXPENSE_NOT_FOUND));

        House house = houseRepository.findById(expense.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (request.categoryId() != null) {
            if (!ExpenseCategoryType.isValid(request.categoryId())) {
                throw ApiException.badRequest("Danh mục không hợp lệ");
            }
            expense.setCategoryId(request.categoryId());
        }

        if (request.title() != null) {
            expense.setTitle(request.title());
        }
        if (request.description() != null) {
            expense.setDescription(request.description());
        }
        if (request.amount() != null) {
            expense.setAmount(request.amount());
        }
        if (request.expenseDate() != null) {
            expense.setExpenseDate(LocalDate.parse(request.expenseDate(), DateTimeFormatter.ISO_DATE));
        }

        if (receipt != null && !receipt.isEmpty()) {
            expense.setReceiptUrl(saveFile(receipt, "receipts"));
        }

        expense.setUpdatedAt(LocalDateTime.now());
        expenseRepository.save(expense);
        return enrichExpense(expense);
    }

    @Transactional
    public void delete(String id, String landlordId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.EXPENSE_NOT_FOUND));

        House house = houseRepository.findById(expense.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        expenseRepository.delete(expense);
    }

    @Transactional
    public ExpenseDto markAsPaid(String id, MarkExpensePaidRequest request, String landlordId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.EXPENSE_NOT_FOUND));

        House house = houseRepository.findById(expense.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (expense.getStatus() == Expense.ExpenseStatus.PAID) {
            throw ApiException.badRequest(MessageConstant.EXPENSE_ALREADY_PAID);
        }

        expense.setStatus(Expense.ExpenseStatus.PAID);
        expense.setPaidAt(LocalDateTime.now());
        expense.setPaymentMethod(request.paymentMethod());
        expense.setPaymentReference(request.paymentReference());
        expense.setUpdatedAt(LocalDateTime.now());

        expenseRepository.save(expense);
        return enrichExpense(expense);
    }

    public ExpenseSummaryDto getSummary(String landlordId, String houseId, Integer month, Integer year) {
        // Default to current month/year
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = year != null ? year : LocalDate.now().getYear();

        YearMonth ym = YearMonth.of(y, m);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        BigDecimal totalAmount;
        BigDecimal paidAmount;
        BigDecimal pendingAmount;

        if (houseId != null && !houseId.isEmpty()) {
            House house = houseRepository.findById(houseId)
                    .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
            if (!house.getOwnerId().equals(landlordId)) {
                throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
            }

            totalAmount = expenseRepository.sumAmountByHouseIdAndDateRange(houseId, startDate, endDate);
            // Get expenses for this house to calculate paid/pending
            List<Expense> expenses = expenseRepository.findByHouseId(houseId).stream()
                    .filter(e -> !e.getExpenseDate().isBefore(startDate) && !e.getExpenseDate().isAfter(endDate))
                    .toList();
            paidAmount = expenses.stream()
                    .filter(e -> e.getStatus() == Expense.ExpenseStatus.PAID)
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            pendingAmount = expenses.stream()
                    .filter(e -> e.getStatus() == Expense.ExpenseStatus.PENDING)
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            totalAmount = expenseRepository.sumAmountByLandlordIdAndDateRange(landlordId, startDate, endDate);
            paidAmount = expenseRepository.sumAmountByLandlordIdAndStatusAndDateRange(
                    landlordId, Expense.ExpenseStatus.PAID, startDate, endDate);
            pendingAmount = expenseRepository.sumAmountByLandlordIdAndStatusAndDateRange(
                    landlordId, Expense.ExpenseStatus.PENDING, startDate, endDate);
        }

        // Get counts
        List<Expense> allExpenses = houseId != null && !houseId.isEmpty()
                ? expenseRepository.findByHouseId(houseId)
                : expenseRepository.findByLandlordId(landlordId);

        List<Expense> monthExpenses = allExpenses.stream()
                .filter(e -> !e.getExpenseDate().isBefore(startDate) && !e.getExpenseDate().isAfter(endDate))
                .toList();

        int totalCount = monthExpenses.size();
        int paidCount = (int) monthExpenses.stream().filter(e -> e.getStatus() == Expense.ExpenseStatus.PAID).count();
        int pendingCount = (int) monthExpenses.stream().filter(e -> e.getStatus() == Expense.ExpenseStatus.PENDING)
                .count();

        return new ExpenseSummaryDto(
                totalAmount != null ? totalAmount : BigDecimal.ZERO,
                paidAmount != null ? paidAmount : BigDecimal.ZERO,
                pendingAmount != null ? pendingAmount : BigDecimal.ZERO,
                totalCount,
                paidCount,
                pendingCount);
    }

    // ============ HELPERS ============

    private List<ExpenseDto> enrichExpenses(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return List.of();
        }

        // Batch load houses
        List<String> houseIds = expenses.stream().map(Expense::getHouseId).distinct().toList();
        Map<String, House> houseMap = houseRepository.findAllById(houseIds)
                .stream()
                .collect(Collectors.toMap(House::getId, h -> h));

        return expenses.stream().map(expense -> {
            // Use fixed category enum
            ExpenseCategoryType category = ExpenseCategoryType.fromId(expense.getCategoryId());
            House house = houseMap.get(expense.getHouseId());

            expense.setCategoryName(category != null ? category.getNameVi() : null);
            expense.setCategoryIcon(category != null ? category.getIcon() : null);
            expense.setHouseName(house != null ? house.getName() : null);

            return ExpenseDto.from(expense);
        }).toList();
    }

    private ExpenseDto enrichExpense(Expense expense) {
        // Use fixed category enum
        ExpenseCategoryType category = ExpenseCategoryType.fromId(expense.getCategoryId());
        House house = houseRepository.findById(expense.getHouseId()).orElse(null);

        expense.setCategoryName(category != null ? category.getNameVi() : null);
        expense.setCategoryIcon(category != null ? category.getIcon() : null);
        expense.setHouseName(house != null ? house.getName() : null);

        return ExpenseDto.from(expense);
    }

    private String saveFile(MultipartFile file, String subFolder) {
        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path dir = Paths.get(uploadPath, subFolder);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(fileName);
            Files.write(filePath, file.getBytes());
            return "/files/" + subFolder + "/" + fileName;
        } catch (IOException e) {
            throw ApiException.badRequest(MessageConstant.FAILED_TO_SAVE_FILE + e.getMessage());
        }
    }
}
