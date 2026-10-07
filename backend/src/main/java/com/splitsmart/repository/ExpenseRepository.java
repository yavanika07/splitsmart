package com.splitsmart.repository;

import com.splitsmart.model.Expense;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Load payer + splits in one query instead of one query per expense (avoids the N+1 problem)
    @EntityGraph(attributePaths = {"paidBy", "splits", "splits.member"})
    List<Expense> findByGroupIdOrderByExpenseDateDescCreatedAtDesc(Long groupId);

    /** Returns null when the group has no expenses (handled in GroupService). */
    @Query("select sum(e.amount) from Expense e where e.group.id = :groupId")
    BigDecimal totalForGroup(@Param("groupId") Long groupId);

    boolean existsByPaidById(Long memberId);

    @Query("select count(s) > 0 from ExpenseSplit s where s.member.id = :memberId")
    boolean memberHasSplits(@Param("memberId") Long memberId);
}
