package com.zhihang.jobagent.agent;

/**
 * States used by the job-search assistant agent workflow.
 */
public enum AgentState {

    S0_IDLE,
    S1_PROFILE,
    S2_MATCH,
    S3_OPTIMIZE,
    S4_ROADMAP,
    S5_INTERVIEW,
    S6_SUMMARY,
    S7_DONE,
    S8_FAILED
}
