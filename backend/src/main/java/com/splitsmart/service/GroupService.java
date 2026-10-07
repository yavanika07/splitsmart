package com.splitsmart.service;

import com.splitsmart.dto.Dtos.*;
import com.splitsmart.exception.NotFoundException;
import com.splitsmart.model.AppUser;
import com.splitsmart.model.ExpenseGroup;
import com.splitsmart.model.Member;
import com.splitsmart.repository.ExpenseGroupRepository;
import com.splitsmart.repository.ExpenseRepository;
import com.splitsmart.repository.MemberRepository;
import com.splitsmart.repository.SettlementRepository;
import com.splitsmart.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupService {

    private final ExpenseGroupRepository groupRepo;
    private final MemberRepository memberRepo;
    private final ExpenseRepository expenseRepo;
    private final SettlementRepository settlementRepo;
    private final AuthService authService;
    private final CurrentUser currentUser;

    public GroupService(ExpenseGroupRepository groupRepo, MemberRepository memberRepo,
                        ExpenseRepository expenseRepo, SettlementRepository settlementRepo,
                        AuthService authService, CurrentUser currentUser) {
        this.groupRepo = groupRepo;
        this.memberRepo = memberRepo;
        this.expenseRepo = expenseRepo;
        this.settlementRepo = settlementRepo;
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @Transactional
    public GroupResponse create(CreateGroupRequest req) {
        AppUser owner = authService.currentUserEntity();
        ExpenseGroup group = new ExpenseGroup(req.name().trim(), owner);

        Set<String> seen = new HashSet<>();
        for (String raw : req.memberNames()) {
            String name = raw.trim();
            if (!seen.add(name.toLowerCase())) {
                throw new IllegalArgumentException("Member name '" + name + "' is repeated");
            }
            group.getMembers().add(new Member(name, group));
        }
        return toResponse(groupRepo.save(group));
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> findAllForCurrentUser() {
        return groupRepo.findByOwnerIdOrderByCreatedAtDesc(currentUser.id())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse findById(Long id) {
        return toResponse(getGroup(id));
    }

    @Transactional
    public GroupResponse rename(Long id, RenameGroupRequest req) {
        ExpenseGroup group = getGroup(id);
        group.setName(req.name().trim());
        return toResponse(group);
    }

    @Transactional
    public void delete(Long id) {
        ExpenseGroup group = getGroup(id);
        // Delete children first (payments, then expenses), then the group + members.
        settlementRepo.deleteAll(settlementRepo.findByGroupIdOrderByCreatedAtDesc(id));
        expenseRepo.deleteAll(expenseRepo.findByGroupIdOrderByExpenseDateDescCreatedAtDesc(id));
        settlementRepo.flush();
        expenseRepo.flush();
        groupRepo.delete(group);
    }

    @Transactional
    public MemberResponse addMember(Long groupId, AddMemberRequest req) {
        ExpenseGroup group = getGroup(groupId);
        String name = req.name().trim();
        boolean duplicate = group.getMembers().stream().anyMatch(m -> m.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw new IllegalArgumentException("'" + name + "' is already in this group");
        }
        Member saved = memberRepo.save(new Member(name, group));
        return new MemberResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public void removeMember(Long groupId, Long memberId) {
        ExpenseGroup group = getGroup(groupId);
        Member member = group.getMembers().stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Member " + memberId + " not found in this group"));

        boolean used = expenseRepo.existsByPaidById(memberId)
                || expenseRepo.memberHasSplits(memberId)
                || settlementRepo.existsByFromMemberIdOrToMemberId(memberId, memberId);
        if (used) {
            throw new IllegalArgumentException(
                    member.getName() + " has expenses or payments in this group, so they can't be removed");
        }
        group.getMembers().remove(member);   // orphanRemoval deletes the row
    }

    /** Loads a group only if it belongs to the logged-in user (otherwise behaves as "not found"). */
    ExpenseGroup getGroup(Long id) {
        return groupRepo.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Group " + id + " not found"));
    }

    private GroupResponse toResponse(ExpenseGroup g) {
        List<MemberResponse> members = g.getMembers().stream()
                .map(m -> new MemberResponse(m.getId(), m.getName()))
                .toList();
        BigDecimal total = expenseRepo.totalForGroup(g.getId());
        if (total == null) total = BigDecimal.ZERO;
        return new GroupResponse(g.getId(), g.getName(), members,
                total.setScale(2, RoundingMode.HALF_UP), g.getCreatedAt());
    }
}
