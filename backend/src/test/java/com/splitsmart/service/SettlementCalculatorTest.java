package com.splitsmart.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SettlementCalculatorTest {

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    @Test
    void threeFriendsTripSettlesInTwoPayments() {
        // A paid 900 for hotel, B paid 300 for food; split equally among A, B, C (400 each)
        // net: A = +500, B = -100, C = -400
        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        net.put(1L, bd("500.00"));
        net.put(2L, bd("-100.00"));
        net.put(3L, bd("-400.00"));

        List<SettlementCalculator.Payment> payments = SettlementCalculator.settle(net);

        assertEquals(2, payments.size());
        assertEquals(new SettlementCalculator.Payment(3L, 1L, bd("400.00")), payments.get(0));
        assertEquals(new SettlementCalculator.Payment(2L, 1L, bd("100.00")), payments.get(1));
    }

    @Test
    void alreadySettledGroupNeedsNoPayments() {
        Map<Long, BigDecimal> net = Map.of(1L, bd("0.00"), 2L, bd("0.00"));
        assertTrue(SettlementCalculator.settle(net).isEmpty());
    }

    @Test
    void neverMoreThanNMinusOnePayments() {
        Map<Long, BigDecimal> net = new HashMap<>();
        net.put(1L, bd("70.00"));
        net.put(2L, bd("30.00"));
        net.put(3L, bd("-25.00"));
        net.put(4L, bd("-25.00"));
        net.put(5L, bd("-50.00"));

        List<SettlementCalculator.Payment> payments = SettlementCalculator.settle(net);
        assertTrue(payments.size() <= net.size() - 1);

        // Applying every payment must bring everyone back to zero
        Map<Long, BigDecimal> check = new HashMap<>(net);
        for (var p : payments) {
            check.merge(p.from(), p.amount(), BigDecimal::add);
            check.merge(p.to(), p.amount().negate(), BigDecimal::add);
        }
        check.values().forEach(v -> assertEquals(0, v.signum()));
    }
}
