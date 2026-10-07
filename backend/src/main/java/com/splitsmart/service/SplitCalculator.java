package com.splitsmart.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Works out each participant's share of an expense, to the paisa.
 * Rule for every split type: the shares ALWAYS add up exactly to the total.
 *
 * Plain Java (no Spring) so it can be unit tested in isolation.
 */
public final class SplitCalculator {

    private static final BigDecimal PAISA = new BigDecimal("0.01");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private SplitCalculator() {}

    /** 100.00 among 3 -> [33.34, 33.33, 33.33]: leftover paisa go to the first participants. */
    public static List<BigDecimal> equal(BigDecimal total, int n) {
        if (n <= 0) throw new IllegalArgumentException("Pick at least one participant");
        total = money(total);
        BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        int extraPaisa = total.subtract(base.multiply(BigDecimal.valueOf(n)))
                .movePointRight(2).intValueExact();

        List<BigDecimal> shares = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            shares.add(i < extraPaisa ? base.add(PAISA) : base);
        }
        return shares;
    }

    /** Caller gives exact amounts; they must add up to the total. */
    public static List<BigDecimal> exact(BigDecimal total, List<BigDecimal> amounts) {
        total = money(total);
        List<BigDecimal> shares = new ArrayList<>(amounts.size());
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal a : amounts) {
            if (a == null || a.signum() < 0) {
                throw new IllegalArgumentException("Exact split needs an amount (0 or more) for every participant");
            }
            BigDecimal share = money(a);
            shares.add(share);
            sum = sum.add(share);
        }
        if (sum.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                    "Shares add up to " + sum.toPlainString() + " but the expense is " + total.toPlainString());
        }
        return shares;
    }

    /**
     * Caller gives percentages that must total 100.
     * Uses the "largest remainder" method so rounding is fair:
     * 100.00 at 33.33% / 33.33% / 33.34% -> 33.33 / 33.33 / 33.34.
     */
    public static List<BigDecimal> percentage(BigDecimal total, List<BigDecimal> percents) {
        total = money(total);
        BigDecimal pctSum = BigDecimal.ZERO;
        for (BigDecimal p : percents) {
            if (p == null || p.signum() < 0 || p.compareTo(HUNDRED) > 0) {
                throw new IllegalArgumentException("Each percentage must be between 0 and 100");
            }
            pctSum = pctSum.add(p);
        }
        if (pctSum.compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("Percentages add up to " + pctSum.stripTrailingZeros().toPlainString() + "%, not 100%");
        }

        int n = percents.size();
        List<BigDecimal> shares = new ArrayList<>(n);
        List<BigDecimal> remainders = new ArrayList<>(n);
        BigDecimal floorSum = BigDecimal.ZERO;
        for (BigDecimal p : percents) {
            BigDecimal raw = total.multiply(p).divide(HUNDRED);      // exact (dividing by 100)
            BigDecimal floor = raw.setScale(2, RoundingMode.DOWN);
            shares.add(floor);
            remainders.add(raw.subtract(floor));
            floorSum = floorSum.add(floor);
        }

        int extraPaisa = total.subtract(floorSum).movePointRight(2).intValueExact();
        List<Integer> order = IntStream.range(0, n).boxed()
                .sorted(Comparator.comparing((Integer i) -> remainders.get(i)).reversed()
                        .thenComparing(i -> i))
                .toList();
        for (int k = 0; k < extraPaisa; k++) {
            int i = order.get(k);
            shares.set(i, shares.get(i).add(PAISA));
        }
        return shares;
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
