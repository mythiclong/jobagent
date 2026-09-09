package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.UpdateLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UpdateLogRepository extends JpaRepository<UpdateLog, Long> {

    List<UpdateLog> findTop20ByOrderByCreatedAtDescIdDesc();

    Optional<UpdateLog> findTopByUpdateTypeOrderByCreatedAtDescIdDesc(String updateType);

    List<UpdateLog> findTop10ByUpdateTypeOrderByCreatedAtDescIdDesc(String updateType);
}
