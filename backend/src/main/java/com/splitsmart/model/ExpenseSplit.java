package com.splitsmart.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

// One row per (expense, member): how much that member owes for that expense.
@Entity
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shareAmount;

    // Only filled for PERCENTAGE splits, so the edit form can show the original %.
    @Column(precision = 5, scale = 2)
    private BigDecimal percent;

    protected ExpenseSplit() {}

    public ExpenseSplit(Expense expense, Member member, BigDecimal shareAmount, BigDecimal percent) {
        this.expense = expense;
        this.member = member;
        this.shareAmount = shareAmount;
        this.percent = percent;
    }

    public Long getId() { return id; }
    public Expense getExpense() { return expense; }
    public Member getMember() { return member; }
    public BigDecimal getShareAmount() { return shareAmount; }
    public BigDecimal getPercent() { return percent; }
}
