package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    List<UserProfile> findAllByOrderByIdDesc();

    List<UserProfile> findAllByUserAccountIdOrderByIdDesc(Long userAccountId);

    Optional<UserProfile> findFirstByUserAccountIdOrderByIdAsc(Long userAccountId);
}
