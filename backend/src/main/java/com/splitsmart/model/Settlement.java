package com.splitsmart.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// A payment that actually happened ("Ravi paid Asha 400"). It reduces what is owed.
@Entity
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_member_id")
    private Member fromMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_member_id")
    private Member toMember;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected Settlement() {}

    public Settlement(ExpenseGroup group, Member fromMember, Member toMember, BigDecimal amount) {
        this.group = group;
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public ExpenseGroup getGroup() { return group; }
    public Member getFromMember() { return fromMember; }
    public Member getToMember() { return toMember; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
