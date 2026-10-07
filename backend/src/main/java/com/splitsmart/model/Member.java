package com.splitsmart.model;

import jakarta.persistence.*;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private ExpenseGroup group;

    protected Member() {}

    public Member(String name, ExpenseGroup group) {
        this.name = name;
        this.group = group;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ExpenseGroup getGroup() { return group; }
}
