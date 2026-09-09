package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {

    List<LearningResource> findAllByOrderByUpdatedAtDescIdDesc();

    List<LearningResource> findTop20ByOrderByUpdatedAtDescIdDesc();

    List<LearningResource> findTop20ByTargetDirectionContainingIgnoreCaseOrderByUpdatedAtDescIdDesc(String targetDirection);

    Optional<LearningResource> findByTitleAndTargetDirection(String title, String targetDirection);

    Optional<LearningResource> findByUrl(String url);
}
