package com.splitsmart.controller;

import com.splitsmart.dto.Dtos.*;
import com.splitsmart.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@Valid @RequestBody CreateGroupRequest req) {
        return groupService.create(req);
    }

    @GetMapping
    public List<GroupResponse> myGroups() {
        return groupService.findAllForCurrentUser();
    }

    @GetMapping("/{id}")
    public GroupResponse one(@PathVariable Long id) {
        return groupService.findById(id);
    }

    @PatchMapping("/{id}")
    public GroupResponse rename(@PathVariable Long id, @Valid @RequestBody RenameGroupRequest req) {
        return groupService.rename(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        groupService.delete(id);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest req) {
        return groupService.addMember(id, req);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        groupService.removeMember(id, memberId);
    }
}
