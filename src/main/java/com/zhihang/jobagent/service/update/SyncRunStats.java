package com.zhihang.jobagent.service.update;

public record SyncRunStats(int addedCount, int updatedCount, int skippedCount, String message) {

    public static SyncRunStats empty(String message) {
        return new SyncRunStats(0, 0, 0, message);
    }

    public SyncRunStats plus(SyncRunStats other) {
        if (other == null) {
            return this;
        }
        String mergedMessage = message == null || message.isBlank()
                ? other.message
                : message + " | " + other.message;
        return new SyncRunStats(
                addedCount + other.addedCount,
                updatedCount + other.updatedCount,
                skippedCount + other.skippedCount,
                mergedMessage
        );
    }
}
