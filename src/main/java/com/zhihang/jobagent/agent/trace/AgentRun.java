package com.zhihang.jobagent.agent.trace;

import com.zhihang.jobagent.agent.AgentState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One complete execution of the agent workflow.
 */
@Entity
@Table(name = "agent_run")
public class AgentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AgentState status = AgentState.S0_IDLE;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private Integer totalTokens = 0;

    @Column(nullable = false)
    private Long totalLatencyMs = 0L;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public AgentRun() {
    }

    @PrePersist
    void initializeDefaults() {
        if (status == null) {
            status = AgentState.S0_IDLE;
        }
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (totalTokens == null) {
            totalTokens = 0;
        }
        if (totalLatencyMs == null) {
            totalLatencyMs = 0L;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AgentState getStatus() {
        return status;
    }

    public void setStatus(AgentState status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Long getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public void setTotalLatencyMs(Long totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
