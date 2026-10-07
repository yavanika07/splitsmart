package com.splitsmart.service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Core algorithm: turns net balances into a small list of payments.
 *
 * net balance = (total paid) - (total share owed)
 *   net > 0  -> creditor (should receive money)
 *   net < 0  -> debtor   (should pay money)
 *
 * Greedy min-cash-flow: repeatedly match the largest debtor with the
 * largest creditor and transfer min(|debt|, credit). Each step settles
 * at least one person fully, so there are at most (n - 1) payments.
 *
 * Plain Java (no Spring) so it can be unit tested in isolation.
 */
public final class SettlementCalculator {

    private SettlementCalculator() {}

    public record Payment(Long from, Long to, BigDecimal amount) {}

    private record Entry(Long memberId, BigDecimal amount) {}

    public static List<Payment> settle(Map<Long, BigDecimal> netBalances) {
        // Max-heaps by absolute amount
        PriorityQueue<Entry> creditors = new PriorityQueue<>(
                (a, b) -> b.amount().compareTo(a.amount()));
        PriorityQueue<Entry> debtors = new PriorityQueue<>(
                (a, b) -> b.amount().compareTo(a.amount()));

        for (Map.Entry<Long, BigDecimal> e : netBalances.entrySet()) {
            int sign = e.getValue().signum();
            if (sign > 0) creditors.add(new Entry(e.getKey(), e.getValue()));
            else if (sign < 0) debtors.add(new Entry(e.getKey(), e.getValue().negate()));
        }

        List<Payment> payments = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Entry credit = creditors.poll();
            Entry debt = debtors.poll();

            BigDecimal pay = credit.amount().min(debt.amount());
            payments.add(new Payment(debt.memberId(), credit.memberId(), pay));

            BigDecimal creditLeft = credit.amount().subtract(pay);
            BigDecimal debtLeft = debt.amount().subtract(pay);
            if (creditLeft.signum() > 0) creditors.add(new Entry(credit.memberId(), creditLeft));
            if (debtLeft.signum() > 0) debtors.add(new Entry(debt.memberId(), debtLeft));
        }
        return payments;
    }
}
