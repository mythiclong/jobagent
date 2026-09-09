package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.InterviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRecordRepository extends JpaRepository<InterviewRecord, Long> {

    List<InterviewRecord> findAllByOrderByCreatedAtDescIdDesc();

    List<InterviewRecord> findBySessionIdOrderByCreatedAtAscIdAsc(String sessionId);

    List<InterviewRecord> findTop30ByOrderByCreatedAtDescIdDesc();

    List<InterviewRecord> findTop30ByUserProfileIdOrderByCreatedAtDescIdDesc(Long userProfileId);

    List<InterviewRecord> findTop50ByUserAccountIdOrderByCreatedAtDescIdDesc(Long userAccountId);

    List<InterviewRecord> findTop50ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(List<Long> userProfileIds);
}
