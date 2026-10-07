package com.splitsmart.service;

import com.splitsmart.dto.Dtos.*;
import com.splitsmart.exception.NotFoundException;
import com.splitsmart.model.*;
import com.splitsmart.repository.ExpenseRepository;
import com.splitsmart.repository.SettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class SettlementService {

    private final ExpenseRepository expenseRepo;
    private final SettlementRepository settlementRepo;
    private final GroupService groupService;

    public SettlementService(ExpenseRepository expenseRepo, SettlementRepository settlementRepo,
                             GroupService groupService) {
        this.expenseRepo = expenseRepo;
        this.settlementRepo = settlementRepo;
        this.groupService = groupService;
    }

    /**
     * net = paid - owed + (payments made) - (payments received)
     * net > 0 -> should get money back, net < 0 -> still owes.
     */
    @Transactional(readOnly = true)
    public SettlementResponse summary(Long groupId) {
        ExpenseGroup group = groupService.getGroup(groupId);

        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, BigDecimal> paid = new HashMap<>();
        Map<Long, BigDecimal> owed = new HashMap<>();
        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        for (Member m : group.getMembers()) {
            names.put(m.getId(), m.getName());
            paid.put(m.getId(), BigDecimal.ZERO);
            owed.put(m.getId(), BigDecimal.ZERO);
            net.put(m.getId(), BigDecimal.ZERO);
        }

        BigDecimal totalSpent = BigDecimal.ZERO;
        for (Expense e : expenseRepo.findByGroupIdOrderByExpenseDateDescCreatedAtDesc(groupId)) {
            totalSpent = totalSpent.add(e.getAmount());
            paid.merge(e.getPaidBy().getId(), e.getAmount(), BigDecimal::add);
            net.merge(e.getPaidBy().getId(), e.getAmount(), BigDecimal::add);
            for (ExpenseSplit s : e.getSplits()) {
                owed.merge(s.getMember().getId(), s.getShareAmount(), BigDecimal::add);
                net.merge(s.getMember().getId(), s.getShareAmount().negate(), BigDecimal::add);
            }
        }

        for (Settlement p : settlementRepo.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            net.merge(p.getFromMember().getId(), p.getAmount(), BigDecimal::add);
            net.merge(p.getToMember().getId(), p.getAmount().negate(), BigDecimal::add);
        }

        List<BalanceResponse> balances = names.keySet().stream()
                .map(id -> new BalanceResponse(id, names.get(id),
                        money(paid.get(id)), money(owed.get(id)), money(net.get(id))))
                .toList();

        List<Transaction> suggestions = SettlementCalculator.settle(net).stream()
                .map(p -> new Transaction(p.from(), names.get(p.from()),
                                          p.to(), names.get(p.to()), money(p.amount())))
                .toList();

        return new SettlementResponse(money(totalSpent), balances, suggestions);
    }

    @Transactional
    public PaymentResponse recordPayment(Long groupId, RecordPaymentRequest req) {
        ExpenseGroup group = groupService.getGroup(groupId);
        if (req.fromMemberId().equals(req.toMemberId())) {
            throw new IllegalArgumentException("Payer and receiver must be different people");
        }
        Member from = member(group, req.fromMemberId());
        Member to = member(group, req.toMemberId());
        Settlement saved = settlementRepo.save(new Settlement(group, from, to, money(req.amount())));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(Long groupId) {
        groupService.getGroup(groupId);
        return settlementRepo.findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deletePayment(Long groupId, Long paymentId) {
        groupService.getGroup(groupId);
        Settlement s = settlementRepo.findById(paymentId)
                .filter(x -> x.getGroup().getId().equals(groupId))
                .orElseThrow(() -> new NotFoundException("Payment " + paymentId + " not found in this group"));
        settlementRepo.delete(s);
    }

    private Member member(ExpenseGroup group, Long id) {
        return group.getMembers().stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Member " + id + " is not in this group"));
    }

    private PaymentResponse toResponse(Settlement s) {
        return new PaymentResponse(s.getId(),
                new MemberResponse(s.getFromMember().getId(), s.getFromMember().getName()),
                new MemberResponse(s.getToMember().getId(), s.getToMember().getName()),
                s.getAmount(), s.getCreatedAt());
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
