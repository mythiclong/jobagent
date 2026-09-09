package com.zhihang.jobagent.dto;

import com.zhihang.jobagent.entity.UserProfile;

public class ProfileSyncApplyResult {

    private final UserProfile profile;
    private final boolean created;
    private final int updatedFieldCount;

    public ProfileSyncApplyResult(UserProfile profile, boolean created, int updatedFieldCount) {
        this.profile = profile;
        this.created = created;
        this.updatedFieldCount = updatedFieldCount;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public boolean isCreated() {
        return created;
    }

    public int getUpdatedFieldCount() {
        return updatedFieldCount;
    }
}
