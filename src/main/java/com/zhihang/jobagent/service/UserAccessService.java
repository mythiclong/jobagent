package com.zhihang.jobagent.service;

import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.UserProfileRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class UserAccessService {

    private final CurrentUserService currentUserService;
    private final UserProfileRepository userProfileRepository;

    public UserAccessService(CurrentUserService currentUserService,
                             UserProfileRepository userProfileRepository) {
        this.currentUserService = currentUserService;
        this.userProfileRepository = userProfileRepository;
    }

    public List<UserProfile> findAccessibleProfiles() {
        if (currentUserService.isAdmin()) {
            return userProfileRepository.findAllByOrderByIdDesc();
        }
        if (!currentUserService.isAuthenticated()) {
            return Collections.emptyList();
        }
        return userProfileRepository.findAllByUserAccountIdOrderByIdDesc(currentUserService.requireCurrentUserId());
    }

    public List<Long> findAccessibleProfileIds() {
        return findAccessibleProfiles().stream()
                .map(UserProfile::getId)
                .toList();
    }

    public UserProfile resolveSelectedProfile(Long requestedProfileId) {
        List<UserProfile> profiles = findAccessibleProfiles();
        if (profiles.isEmpty()) {
            return null;
        }
        if (requestedProfileId == null) {
            return profiles.get(0);
        }
        return profiles.stream()
                .filter(profile -> Objects.equals(profile.getId(), requestedProfileId))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("无权访问该档案。"));
    }

    public UserProfile requireAccessibleProfile(Long profileId) {
        UserProfile profile = userProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("用户档案不存在: " + profileId));
        if (canAccessProfile(profile)) {
            return profile;
        }
        throw new AccessDeniedException("无权访问该档案。");
    }

    public void assertCanAccessRecord(Long userAccountId, Long userProfileId) {
        if (currentUserService.isAdmin()) {
            return;
        }
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (userAccountId != null) {
            if (!Objects.equals(userAccountId, currentUserId)) {
                throw new AccessDeniedException("无权访问该记录。");
            }
            return;
        }
        if (userProfileId != null && findAccessibleProfileIds().contains(userProfileId)) {
            return;
        }
        throw new AccessDeniedException("无权访问该记录。");
    }

    public UserProfile bindCurrentUser(UserProfile userProfile) {
        if (currentUserService.isAuthenticated()) {
            Long currentUserId = currentUserService.requireCurrentUserId();
            userProfile.setUserAccountId(currentUserId);
        }
        return userProfile;
    }

    private boolean canAccessProfile(UserProfile profile) {
        if (currentUserService.isAdmin()) {
            return true;
        }
        return Objects.equals(profile.getUserAccountId(), currentUserService.requireCurrentUserId());
    }
}
