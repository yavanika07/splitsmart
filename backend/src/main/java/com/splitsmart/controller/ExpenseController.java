package com.splitsmart.controller;

import com.splitsmart.dto.Dtos.*;
import com.splitsmart.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@PathVariable Long groupId, @Valid @RequestBody ExpenseRequest req) {
        return expenseService.create(groupId, req);
    }

    @GetMapping
    public List<ExpenseResponse> list(@PathVariable Long groupId) {
        return expenseService.findByGroup(groupId);
    }

    @PutMapping("/{expenseId}")
    public ExpenseResponse update(@PathVariable Long groupId, @PathVariable Long expenseId,
                                  @Valid @RequestBody ExpenseRequest req) {
        return expenseService.update(groupId, expenseId, req);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long groupId, @PathVariable Long expenseId) {
        expenseService.delete(groupId, expenseId);
    }
}
