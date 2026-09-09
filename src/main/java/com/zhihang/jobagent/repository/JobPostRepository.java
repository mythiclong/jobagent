package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    Optional<JobPost> findBySourceNameAndExternalUrl(String sourceName, String externalUrl);

    Optional<JobPost> findByExternalKey(String externalKey);

    Optional<JobPost> findBySourceJobId(String sourceJobId);

    List<JobPost> findBySourceJobIdIn(Collection<String> sourceJobIds);

    List<JobPost> findTop20ByOrderByUpdatedAtDescIdDesc();
}
