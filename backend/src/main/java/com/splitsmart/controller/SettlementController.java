package com.splitsmart.controller;

import com.splitsmart.dto.Dtos.*;
import com.splitsmart.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /** Balances + the minimum set of payments that would settle everyone. */
    @GetMapping("/settlement")
    public SettlementResponse summary(@PathVariable Long groupId) {
        return settlementService.summary(groupId);
    }

    /** Record that a payment actually happened. */
    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse record(@PathVariable Long groupId, @Valid @RequestBody RecordPaymentRequest req) {
        return settlementService.recordPayment(groupId, req);
    }

    @GetMapping("/payments")
    public List<PaymentResponse> payments(@PathVariable Long groupId) {
        return settlementService.listPayments(groupId);
    }

    @DeleteMapping("/payments/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable Long groupId, @PathVariable Long paymentId) {
        settlementService.deletePayment(groupId, paymentId);
    }
}
