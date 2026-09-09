package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeEnhanceRecordRepository extends JpaRepository<ResumeEnhanceRecord, Long> {

    List<ResumeEnhanceRecord> findAllByOrderByCreatedAtDescIdDesc();

    List<ResumeEnhanceRecord> findTop10ByOrderByCreatedAtDescIdDesc();

    List<ResumeEnhanceRecord> findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(Long userProfileId);

    List<ResumeEnhanceRecord> findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(Long userAccountId);

    List<ResumeEnhanceRecord> findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(List<Long> userProfileIds);

    Optional<ResumeEnhanceRecord> findTopByUserProfileIdAndJobPostIdOrderByCreatedAtDescIdDesc(Long userProfileId, Long jobPostId);
}
