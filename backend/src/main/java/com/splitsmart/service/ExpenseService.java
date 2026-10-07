package com.splitsmart.service;

import com.splitsmart.dto.Dtos.*;
import com.splitsmart.exception.NotFoundException;
import com.splitsmart.model.*;
import com.splitsmart.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepo;
    private final GroupService groupService;

    public ExpenseService(ExpenseRepository expenseRepo, GroupService groupService) {
        this.expenseRepo = expenseRepo;
        this.groupService = groupService;
    }

    @Transactional
    public ExpenseResponse create(Long groupId, ExpenseRequest req) {
        ExpenseGroup group = groupService.getGroup(groupId);
        Expense expense = new Expense(group);
        apply(expense, group, req);
        return toResponse(expenseRepo.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long groupId, Long expenseId, ExpenseRequest req) {
        ExpenseGroup group = groupService.getGroup(groupId);
        Expense expense = findInGroup(groupId, expenseId);
        apply(expense, group, req);
        return toResponse(expenseRepo.saveAndFlush(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findByGroup(Long groupId) {
        groupService.getGroup(groupId);
        return expenseRepo.findByGroupIdOrderByExpenseDateDescCreatedAtDesc(groupId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long groupId, Long expenseId) {
        groupService.getGroup(groupId);
        expenseRepo.delete(findInGroup(groupId, expenseId));
    }

    /** Validates the request and fills the expense (shared by create and edit). */
    private void apply(Expense expense, ExpenseGroup group, ExpenseRequest req) {
        Map<Long, Member> members = new HashMap<>();
        group.getMembers().forEach(m -> members.put(m.getId(), m));

        Member payer = member(members, req.paidById());
        BigDecimal total = req.amount().setScale(2, RoundingMode.HALF_UP);

        List<Member> people = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (SplitShare s : req.participants()) {
            if (!seen.add(s.memberId())) {
                throw new IllegalArgumentException("A member appears twice in the split");
            }
            people.add(member(members, s.memberId()));
        }

        List<BigDecimal> shares = switch (req.splitType()) {
            case EQUAL -> SplitCalculator.equal(total, people.size());
            case EXACT -> SplitCalculator.exact(total,
                    req.participants().stream().map(SplitShare::amount).toList());
            case PERCENTAGE -> SplitCalculator.percentage(total,
                    req.participants().stream().map(SplitShare::percent).toList());
        };

        LocalDate date = req.expenseDate() != null ? req.expenseDate() : LocalDate.now();
        expense.update(req.description().trim(), total, payer, req.splitType(), date);
        for (int i = 0; i < people.size(); i++) {
            BigDecimal pct = req.splitType() == SplitType.PERCENTAGE ? req.participants().get(i).percent() : null;
            expense.addSplit(people.get(i), shares.get(i), pct);
        }
    }

    private Member member(Map<Long, Member> members, Long id) {
        Member m = members.get(id);
        if (m == null) throw new IllegalArgumentException("Member " + id + " is not in this group");
        return m;
    }

    private Expense findInGroup(Long groupId, Long expenseId) {
        return expenseRepo.findById(expenseId)
                .filter(e -> e.getGroup().getId().equals(groupId))
                .orElseThrow(() -> new NotFoundException("Expense " + expenseId + " not found in this group"));
    }

    private ExpenseResponse toResponse(Expense e) {
        List<SplitResponse> splits = e.getSplits().stream()
                .map(s -> new SplitResponse(s.getMember().getId(), s.getMember().getName(),
                        s.getShareAmount(), s.getPercent()))
                .toList();
        Member p = e.getPaidBy();
        return new ExpenseResponse(e.getId(), e.getDescription(), e.getAmount(),
                new MemberResponse(p.getId(), p.getName()), e.getSplitType(),
                e.getExpenseDate(), e.getCreatedAt(), splits);
    }
}
