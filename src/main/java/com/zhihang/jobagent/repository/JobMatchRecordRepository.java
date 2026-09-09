package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.JobMatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobMatchRecordRepository extends JpaRepository<JobMatchRecord, Long> {

    List<JobMatchRecord> findAllByOrderByCreatedAtDescIdDesc();

    List<JobMatchRecord> findTop10ByOrderByCreatedAtDescIdDesc();

    List<JobMatchRecord> findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(Long userProfileId);

    List<JobMatchRecord> findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(Long userAccountId);

    List<JobMatchRecord> findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(List<Long> userProfileIds);

    List<JobMatchRecord> findByUserProfileIdAndCreatedAtOrderByIdAsc(Long userProfileId, LocalDateTime createdAt);

    Optional<JobMatchRecord> findTopByUserProfileIdAndJobPostIdOrderByCreatedAtDescIdDesc(Long userProfileId, Long jobPostId);
}
