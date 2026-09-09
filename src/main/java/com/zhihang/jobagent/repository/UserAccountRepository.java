package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.UserAccount;
import com.zhihang.jobagent.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRole(UserRole role);
}
