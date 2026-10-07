package com.splitsmart.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    // BigDecimal, never double, for money (avoids 0.1 + 0.2 != 0.3 errors)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_id")
    private Member paidBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitType splitType;

    private LocalDate expenseDate;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseSplit> splits = new ArrayList<>();

    protected Expense() {}

    public Expense(ExpenseGroup group) {
        this.group = group;
    }

    /** Used for both create and edit: replaces all details and splits. */
    public void update(String description, BigDecimal amount, Member paidBy,
                       SplitType splitType, LocalDate expenseDate) {
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.expenseDate = expenseDate;
        this.splits.clear();
    }

    public void addSplit(Member member, BigDecimal share, BigDecimal percent) {
        splits.add(new ExpenseSplit(this, member, share, percent));
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public Member getPaidBy() { return paidBy; }
    public ExpenseGroup getGroup() { return group; }
    public SplitType getSplitType() { return splitType; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ExpenseSplit> getSplits() { return splits; }
}
