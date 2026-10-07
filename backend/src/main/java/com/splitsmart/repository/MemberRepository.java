package com.splitsmart.repository;

import com.splitsmart.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByGroupIdOrderByIdAsc(Long groupId);
}
