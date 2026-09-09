package com.zhihang.jobagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_account_id")
    private Long userAccountId;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private String major;

    private String targetRole;

    @Column(nullable = false)
    private String targetDirection;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String projectExperience;

    private String targetCity;

    @Column(columnDefinition = "TEXT")
    private String acceptableCities;

    private String expectedSalary;

    private String acceptableJobNature;

    private String targetIndustry;

    private String expectedCity;

    @Column(columnDefinition = "TEXT")
    private String careerGoal;

    private LocalDateTime updatedAt;

    public UserProfile() {
    }

    @PrePersist
    @PreUpdate
    public void touchUpdatedAt() {
        if (!StringUtils.hasText(targetCity) && StringUtils.hasText(expectedCity)) {
            targetCity = normalizeNullable(expectedCity);
        }
        if (!StringUtils.hasText(expectedCity) && StringUtils.hasText(targetCity)) {
            expectedCity = normalizeNullable(targetCity);
        }
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserAccountId() {
        return userAccountId;
    }

    public void setUserAccountId(Long userAccountId) {
        this.userAccountId = userAccountId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getTargetDirection() {
        return targetDirection;
    }

    public void setTargetDirection(String targetDirection) {
        this.targetDirection = targetDirection;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getProjectExperience() {
        return projectExperience;
    }

    public void setProjectExperience(String projectExperience) {
        this.projectExperience = projectExperience;
    }

    public String getTargetCity() {
        return StringUtils.hasText(targetCity) ? targetCity : expectedCity;
    }

    public void setTargetCity(String targetCity) {
        String normalized = normalizeNullable(targetCity);
        this.targetCity = normalized;
        this.expectedCity = normalized;
    }

    public String getAcceptableCities() {
        return acceptableCities;
    }

    public void setAcceptableCities(String acceptableCities) {
        this.acceptableCities = acceptableCities;
    }

    public String getExpectedSalary() {
        return expectedSalary;
    }

    public void setExpectedSalary(String expectedSalary) {
        this.expectedSalary = expectedSalary;
    }

    public String getAcceptableJobNature() {
        return acceptableJobNature;
    }

    public void setAcceptableJobNature(String acceptableJobNature) {
        this.acceptableJobNature = acceptableJobNature;
    }

    public String getTargetIndustry() {
        return targetIndustry;
    }

    public void setTargetIndustry(String targetIndustry) {
        this.targetIndustry = targetIndustry;
    }

    public String getExpectedCity() {
        return StringUtils.hasText(expectedCity) ? expectedCity : targetCity;
    }

    public void setExpectedCity(String expectedCity) {
        String normalized = normalizeNullable(expectedCity);
        this.expectedCity = normalized;
        this.targetCity = normalized;
    }

    public String getCareerGoal() {
        return careerGoal;
    }

    public void setCareerGoal(String careerGoal) {
        this.careerGoal = careerGoal;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
