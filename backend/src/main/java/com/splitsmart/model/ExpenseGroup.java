package com.splitsmart.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Named ExpenseGroup because "group" is a reserved word in SQL.
@Entity
@Table(name = "expense_group")
public class ExpenseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Member> members = new ArrayList<>();

    protected ExpenseGroup() {}

    public ExpenseGroup(String name, AppUser owner) {
        this.name = name;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AppUser getOwner() { return owner; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Member> getMembers() { return members; }
}
