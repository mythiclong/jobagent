package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.ResumeReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeReviewRepository extends JpaRepository<ResumeReview, Long> {

    List<ResumeReview> findAllByOrderByIdDesc();

    List<ResumeReview> findTop10ByOrderByIdDesc();

    List<ResumeReview> findTop10ByUserProfileIdOrderByIdDesc(Long userProfileId);

    List<ResumeReview> findTop20ByUserAccountIdOrderByIdDesc(Long userAccountId);

    List<ResumeReview> findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByIdDesc(List<Long> userProfileIds);

    Optional<ResumeReview> findTopByUserProfileIdAndJobPostIdOrderByIdDesc(Long userProfileId, Long jobPostId);
}
