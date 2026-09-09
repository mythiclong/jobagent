package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.LearningPathAdvice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningPathAdviceRepository extends JpaRepository<LearningPathAdvice, Long> {

    List<LearningPathAdvice> findAllByOrderByCreatedAtDescIdDesc();

    List<LearningPathAdvice> findTop10ByOrderByCreatedAtDescIdDesc();

    List<LearningPathAdvice> findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(Long userProfileId);

    List<LearningPathAdvice> findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(Long userAccountId);

    List<LearningPathAdvice> findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(List<Long> userProfileIds);
}
