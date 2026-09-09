package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.JobSourceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobSourceSnapshotRepository extends JpaRepository<JobSourceSnapshot, Long> {

    Optional<JobSourceSnapshot> findTopByJobPostOrderByCreatedAtDescIdDesc(JobPost jobPost);

    List<JobSourceSnapshot> findTop5ByJobPostOrderByCreatedAtDescIdDesc(JobPost jobPost);

    long countByImportBatchNo(String importBatchNo);

    void deleteByJobPost(JobPost jobPost);
}
