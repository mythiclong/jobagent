package com.zhihang.jobagent.dto;

import com.zhihang.jobagent.entity.UserProfile;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class JobMatchPreferenceInput {

    private String preferredCity;
    private String preferredSalary;
    private String preferredRole;
    private String preferredJobNature;
    private String preferredIndustry;
    private String acceptableCities;
    private boolean temporaryOverrideApplied;

    public static JobMatchPreferenceInput fromProfile(UserProfile profile) {
        JobMatchPreferenceInput input = new JobMatchPreferenceInput();
        if (profile == null) {
            return input;
        }
        input.setPreferredCity(profile.getTargetCity());
        input.setPreferredSalary(profile.getExpectedSalary());
        input.setPreferredRole(firstNonBlank(profile.getTargetRole(), profile.getTargetDirection()));
        input.setPreferredJobNature(profile.getAcceptableJobNature());
        input.setPreferredIndustry(profile.getTargetIndustry());
        input.setAcceptableCities(profile.getAcceptableCities());
        return input;
    }

    public void applyTemporaryOverrides(String preferredCity,
                                        String preferredSalary,
                                        String preferredRole,
                                        String preferredJobNature,
                                        String preferredIndustry) {
        if (StringUtils.hasText(preferredCity)) {
            setPreferredCity(preferredCity);
            temporaryOverrideApplied = true;
        }
        if (StringUtils.hasText(preferredSalary)) {
            setPreferredSalary(preferredSalary);
            temporaryOverrideApplied = true;
        }
        if (StringUtils.hasText(preferredRole)) {
            setPreferredRole(preferredRole);
            temporaryOverrideApplied = true;
        }
        if (StringUtils.hasText(preferredJobNature)) {
            setPreferredJobNature(preferredJobNature);
            temporaryOverrideApplied = true;
        }
        if (StringUtils.hasText(preferredIndustry)) {
            setPreferredIndustry(preferredIndustry);
            temporaryOverrideApplied = true;
        }
    }

    public boolean hasAnyPreference() {
        return StringUtils.hasText(preferredCity)
                || StringUtils.hasText(preferredSalary)
                || StringUtils.hasText(preferredRole)
                || StringUtils.hasText(preferredJobNature)
                || StringUtils.hasText(preferredIndustry)
                || StringUtils.hasText(acceptableCities);
    }

    public List<String> toSummaryItems() {
        List<String> items = new ArrayList<>();
        if (StringUtils.hasText(preferredCity)) {
            items.add("城市 " + preferredCity);
        }
        if (StringUtils.hasText(preferredSalary)) {
            items.add("薪资 " + preferredSalary);
        }
        if (StringUtils.hasText(preferredRole)) {
            items.add("岗位 " + preferredRole);
        }
        if (StringUtils.hasText(preferredJobNature)) {
            items.add("岗位性质 " + preferredJobNature);
        }
        if (StringUtils.hasText(preferredIndustry)) {
            items.add("行业 " + preferredIndustry);
        }
        return items;
    }

    public String toSummaryText() {
        List<String> items = toSummaryItems();
        return items.isEmpty() ? "当前未指定额外偏好" : String.join(" / ", items);
    }

    public String getPreferredCity() {
        return preferredCity;
    }

    public void setPreferredCity(String preferredCity) {
        this.preferredCity = normalize(preferredCity);
    }

    public String getPreferredSalary() {
        return preferredSalary;
    }

    public void setPreferredSalary(String preferredSalary) {
        this.preferredSalary = normalize(preferredSalary);
    }

    public String getPreferredRole() {
        return preferredRole;
    }

    public void setPreferredRole(String preferredRole) {
        this.preferredRole = normalize(preferredRole);
    }

    public String getPreferredJobNature() {
        return preferredJobNature;
    }

    public void setPreferredJobNature(String preferredJobNature) {
        this.preferredJobNature = normalize(preferredJobNature);
    }

    public String getPreferredIndustry() {
        return preferredIndustry;
    }

    public void setPreferredIndustry(String preferredIndustry) {
        this.preferredIndustry = normalize(preferredIndustry);
    }

    public String getAcceptableCities() {
        return acceptableCities;
    }

    public void setAcceptableCities(String acceptableCities) {
        this.acceptableCities = normalize(acceptableCities);
    }

    public boolean isTemporaryOverrideApplied() {
        return temporaryOverrideApplied;
    }

    public void setTemporaryOverrideApplied(boolean temporaryOverrideApplied) {
        this.temporaryOverrideApplied = temporaryOverrideApplied;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String firstNonBlank(String left, String right) {
        if (StringUtils.hasText(left)) {
            return left.trim();
        }
        return StringUtils.hasText(right) ? right.trim() : null;
    }
}
