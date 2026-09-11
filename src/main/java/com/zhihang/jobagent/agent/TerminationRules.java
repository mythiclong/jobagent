package com.zhihang.jobagent.agent;

/**
 * Runtime budgets that keep a single agent run bounded and observable.
 */
public final class TerminationRules {

    public static final int MAX_STEPS = 12;
    public static final int MAX_TOOL_CALLS = 8;
    public static final int MAX_RETRY_PER_TOOL = 2;
    public static final int MAX_TOKENS_PER_RUN = 30_000;
    public static final long MAX_LATENCY_MS = 60_000L;

    private TerminationRules() {
    }
}
