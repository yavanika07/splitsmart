package com.splitsmart.dto;

import com.splitsmart.model.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// All request/response shapes in one file using Java records.
public final class Dtos {
    private Dtos() {}

    // ---------- Auth ----------
    public record RegisterRequest(
            @NotBlank @Size(max = 60) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record UserResponse(Long id, String name, String email) {}

    public record AuthResponse(String token, UserResponse user) {}

    // ---------- Groups & members ----------
    public record CreateGroupRequest(
            @NotBlank @Size(max = 80) String name,
            @NotEmpty List<@NotBlank String> memberNames) {}

    public record RenameGroupRequest(@NotBlank @Size(max = 80) String name) {}

    public record AddMemberRequest(@NotBlank @Size(max = 60) String name) {}

    public record MemberResponse(Long id, String name) {}

    public record GroupResponse(Long id, String name, List<MemberResponse> members,
                                BigDecimal totalSpent, LocalDateTime createdAt) {}

    // ---------- Expenses ----------
    /**
     * EQUAL      -> only memberId is needed
     * EXACT      -> amount is that member's share
     * PERCENTAGE -> percent is that member's percentage (all must total 100)
     */
    public record SplitShare(@NotNull Long memberId, BigDecimal amount, BigDecimal percent) {}

    public record ExpenseRequest(
            @NotBlank @Size(max = 120) String description,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull Long paidById,
            @NotNull SplitType splitType,
            LocalDate expenseDate,
            @NotEmpty List<@Valid SplitShare> participants) {}

    public record SplitResponse(Long memberId, String memberName, BigDecimal share, BigDecimal percent) {}

    public record ExpenseResponse(Long id, String description, BigDecimal amount,
                                  MemberResponse paidBy, SplitType splitType,
                                  LocalDate expenseDate, LocalDateTime createdAt,
                                  List<SplitResponse> splits) {}

    // ---------- Settlement ----------
    public record RecordPaymentRequest(
            @NotNull Long fromMemberId,
            @NotNull Long toMemberId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount) {}

    public record PaymentResponse(Long id, MemberResponse from, MemberResponse to,
                                  BigDecimal amount, LocalDateTime createdAt) {}

    /** paid = what they paid for expenses, owed = their share of expenses, net = what they should get back. */
    public record BalanceResponse(Long memberId, String memberName,
                                  BigDecimal paid, BigDecimal owed, BigDecimal net) {}

    public record Transaction(Long fromId, String fromName,
                              Long toId, String toName, BigDecimal amount) {}

    public record SettlementResponse(BigDecimal totalSpent,
                                     List<BalanceResponse> balances,
                                     List<Transaction> suggestedPayments) {}
}
