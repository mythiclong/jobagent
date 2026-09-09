package com.zhihang.jobagent.dto;

public class ResumeRewriteBlock {

    private final String original;
    private final String rewritten;
    private final String reason;

    public ResumeRewriteBlock(String original, String rewritten, String reason) {
        this.original = original;
        this.rewritten = rewritten;
        this.reason = reason;
    }

    public String getOriginal() {
        return original;
    }

    public String getRewritten() {
        return rewritten;
    }

    public String getReason() {
        return reason;
    }
}
