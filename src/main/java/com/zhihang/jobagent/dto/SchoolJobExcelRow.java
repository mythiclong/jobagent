package com.zhihang.jobagent.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SchoolJobExcelRow implements Serializable {

    private Integer rowNumber;
    private String recruitmentType;
    private String sourceJobId;
    private String jobName;
    private String jobTitle;
    private String jobNature;
    private String workLocationCode;
    private String educationRequirement;
    private String majorRequirement;
    private Integer headCount;
    private String jobCategoryCode;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryRawText;
    private String salaryDisplayText;
    private String salaryUnitType;
    private String jobDescription;
    private String jobUrl;
    private String companyName;
    private String companyShortName;
    private String companyBenefitTags;
    private String unifiedSocialCreditCode;
    private String industryCode;
    private String companyType;
    private String companySize;
    private String companyRegionCode;
    private String companyAddress;
    private String companyWebsite;
    private Boolean isPublic;
    private LocalDateTime offlineTime;
    private LocalDateTime sourcePublishTime;
    private Map<String, String> sourceFieldMap = new LinkedHashMap<>();
    private List<String> warnings = new ArrayList<>();

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getRecruitmentType() {
        return recruitmentType;
    }

    public void setRecruitmentType(String recruitmentType) {
        this.recruitmentType = recruitmentType;
    }

    public String getSourceJobId() {
        return sourceJobId;
    }

    public void setSourceJobId(String sourceJobId) {
        this.sourceJobId = sourceJobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobNature() {
        return jobNature;
    }

    public void setJobNature(String jobNature) {
        this.jobNature = jobNature;
    }

    public String getWorkLocationCode() {
        return workLocationCode;
    }

    public void setWorkLocationCode(String workLocationCode) {
        this.workLocationCode = workLocationCode;
    }

    public String getEducationRequirement() {
        return educationRequirement;
    }

    public void setEducationRequirement(String educationRequirement) {
        this.educationRequirement = educationRequirement;
    }

    public String getMajorRequirement() {
        return majorRequirement;
    }

    public void setMajorRequirement(String majorRequirement) {
        this.majorRequirement = majorRequirement;
    }

    public Integer getHeadCount() {
        return headCount;
    }

    public void setHeadCount(Integer headCount) {
        this.headCount = headCount;
    }

    public String getJobCategoryCode() {
        return jobCategoryCode;
    }

    public void setJobCategoryCode(String jobCategoryCode) {
        this.jobCategoryCode = jobCategoryCode;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
    }

    public String getSalaryRawText() {
        return salaryRawText;
    }

    public void setSalaryRawText(String salaryRawText) {
        this.salaryRawText = salaryRawText;
    }

    public String getSalaryDisplayText() {
        return salaryDisplayText;
    }

    public void setSalaryDisplayText(String salaryDisplayText) {
        this.salaryDisplayText = salaryDisplayText;
    }

    public String getSalaryUnitType() {
        return salaryUnitType;
    }

    public void setSalaryUnitType(String salaryUnitType) {
        this.salaryUnitType = salaryUnitType;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyShortName() {
        return companyShortName;
    }

    public void setCompanyShortName(String companyShortName) {
        this.companyShortName = companyShortName;
    }

    public String getCompanyBenefitTags() {
        return companyBenefitTags;
    }

    public void setCompanyBenefitTags(String companyBenefitTags) {
        this.companyBenefitTags = companyBenefitTags;
    }

    public String getUnifiedSocialCreditCode() {
        return unifiedSocialCreditCode;
    }

    public void setUnifiedSocialCreditCode(String unifiedSocialCreditCode) {
        this.unifiedSocialCreditCode = unifiedSocialCreditCode;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public void setIndustryCode(String industryCode) {
        this.industryCode = industryCode;
    }

    public String getCompanyType() {
        return companyType;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public String getCompanySize() {
        return companySize;
    }

    public void setCompanySize(String companySize) {
        this.companySize = companySize;
    }

    public String getCompanyRegionCode() {
        return companyRegionCode;
    }

    public void setCompanyRegionCode(String companyRegionCode) {
        this.companyRegionCode = companyRegionCode;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getCompanyWebsite() {
        return companyWebsite;
    }

    public void setCompanyWebsite(String companyWebsite) {
        this.companyWebsite = companyWebsite;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public LocalDateTime getOfflineTime() {
        return offlineTime;
    }

    public void setOfflineTime(LocalDateTime offlineTime) {
        this.offlineTime = offlineTime;
    }

    public LocalDateTime getSourcePublishTime() {
        return sourcePublishTime;
    }

    public void setSourcePublishTime(LocalDateTime sourcePublishTime) {
        this.sourcePublishTime = sourcePublishTime;
    }

    public Map<String, String> getSourceFieldMap() {
        return sourceFieldMap;
    }

    public void setSourceFieldMap(Map<String, String> sourceFieldMap) {
        this.sourceFieldMap = sourceFieldMap;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
