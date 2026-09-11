package com.zhihang.jobagent.agent.trace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentStepRepository extends JpaRepository<AgentStep, Long> {

    List<AgentStep> findByRunIdOrderByStepNoAsc(Long runId);
}
