package com.splitsmart.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SplitCalculatorTest {

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    private static BigDecimal sum(List<BigDecimal> list) {
        return list.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void equalSplitGivesLeftoverPaisaToFirstPeople() {
        List<BigDecimal> shares = SplitCalculator.equal(bd("100"), 3);
        assertEquals(List.of(bd("33.34"), bd("33.33"), bd("33.33")), shares);
        assertEquals(0, sum(shares).compareTo(bd("100")));
    }

    @Test
    void exactSplitMustMatchTotal() {
        assertEquals(List.of(bd("60.00"), bd("40.00")),
                SplitCalculator.exact(bd("100"), List.of(bd("60"), bd("40"))));

        assertThrows(IllegalArgumentException.class,
                () -> SplitCalculator.exact(bd("100"), List.of(bd("60"), bd("30"))));
    }

    @Test
    void percentageSplitAlwaysSumsToTotal() {
        List<BigDecimal> shares = SplitCalculator.percentage(bd("999.99"),
                List.of(bd("33.33"), bd("33.33"), bd("33.34")));
        assertEquals(0, sum(shares).compareTo(bd("999.99")));
    }

    @Test
    void percentagesMustTotal100() {
        assertThrows(IllegalArgumentException.class,
                () -> SplitCalculator.percentage(bd("100"), List.of(bd("50"), bd("40"))));
    }

    @Test
    void zeroParticipantsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SplitCalculator.equal(bd("10"), 0));
    }
}
