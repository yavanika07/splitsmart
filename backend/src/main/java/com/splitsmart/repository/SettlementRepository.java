package com.splitsmart.repository;

import com.splitsmart.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByGroupIdOrderByCreatedAtDesc(Long groupId);
    boolean existsByFromMemberIdOrToMemberId(Long fromId, Long toId);
}
