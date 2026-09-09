package com.zhihang.jobagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_source_snapshot")
public class JobSourceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_post_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private JobPost jobPost;

    private String sourceJobId;

    private String recruitType;

    private String jobNameRaw;

    private String recruitTitle;

    private String jobNatureRaw;

    private String workLocationCode;

    private String educationRequirementRaw;

    private String majorRequirementRaw;

    private String headCountRaw;

    private String jobCategoryCode;

    private String salaryMinRaw;

    private String salaryMaxRaw;

    private String salaryRawText;

    private String salaryDisplayText;

    private String salaryUnitType;

    @Column(columnDefinition = "TEXT")
    private String jobDetailRaw;

    @Column(length = 500)
    private String jobUrlRaw;

    private String companyNameRaw;

    private String companyShortName;

    @Column(columnDefinition = "TEXT")
    private String welfareTags;

    private String unifiedCreditCode;

    private String industryCode;

    private String companyTypeRaw;

    private String companySizeRaw;

    private String companyRegionCode;

    @Column(length = 1000)
    private String companyAddressRaw;

    @Column(length = 500)
    private String companyWebsiteRaw;

    private String isPublicRaw;

    private String offlineTimeRaw;

    private String sourcePublishTimeRaw;

    private String importBatchNo;

    private String importFileName;

    private Integer sourceRowNumber;

    @Column(columnDefinition = "TEXT")
    private String rawFieldJson;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JobPost getJobPost() {
        return jobPost;
    }

    public void setJobPost(JobPost jobPost) {
        this.jobPost = jobPost;
    }

    public String getSourceJobId() {
        return sourceJobId;
    }

    public void setSourceJobId(String sourceJobId) {
        this.sourceJobId = sourceJobId;
    }

    public String getRecruitType() {
        return recruitType;
    }

    public void setRecruitType(String recruitType) {
        this.recruitType = recruitType;
    }

    public String getJobNameRaw() {
        return jobNameRaw;
    }

    public void setJobNameRaw(String jobNameRaw) {
        this.jobNameRaw = jobNameRaw;
    }

    public String getRecruitTitle() {
        return recruitTitle;
    }

    public void setRecruitTitle(String recruitTitle) {
        this.recruitTitle = recruitTitle;
    }

    public String getJobNatureRaw() {
        return jobNatureRaw;
    }

    public void setJobNatureRaw(String jobNatureRaw) {
        this.jobNatureRaw = jobNatureRaw;
    }

    public String getWorkLocationCode() {
        return workLocationCode;
    }

    public void setWorkLocationCode(String workLocationCode) {
        this.workLocationCode = workLocationCode;
    }

    public String getEducationRequirementRaw() {
        return educationRequirementRaw;
    }

    public void setEducationRequirementRaw(String educationRequirementRaw) {
        this.educationRequirementRaw = educationRequirementRaw;
    }

    public String getMajorRequirementRaw() {
        return majorRequirementRaw;
    }

    public void setMajorRequirementRaw(String majorRequirementRaw) {
        this.majorRequirementRaw = majorRequirementRaw;
    }

    public String getHeadCountRaw() {
        return headCountRaw;
    }

    public void setHeadCountRaw(String headCountRaw) {
        this.headCountRaw = headCountRaw;
    }

    public String getJobCategoryCode() {
        return jobCategoryCode;
    }

    public void setJobCategoryCode(String jobCategoryCode) {
        this.jobCategoryCode = jobCategoryCode;
    }

    public String getSalaryMinRaw() {
        return salaryMinRaw;
    }

    public void setSalaryMinRaw(String salaryMinRaw) {
        this.salaryMinRaw = salaryMinRaw;
    }

    public String getSalaryMaxRaw() {
        return salaryMaxRaw;
    }

    public void setSalaryMaxRaw(String salaryMaxRaw) {
        this.salaryMaxRaw = salaryMaxRaw;
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

    public String getJobDetailRaw() {
        return jobDetailRaw;
    }

    public void setJobDetailRaw(String jobDetailRaw) {
        this.jobDetailRaw = jobDetailRaw;
    }

    public String getJobUrlRaw() {
        return jobUrlRaw;
    }

    public void setJobUrlRaw(String jobUrlRaw) {
        this.jobUrlRaw = jobUrlRaw;
    }

    public String getCompanyNameRaw() {
        return companyNameRaw;
    }

    public void setCompanyNameRaw(String companyNameRaw) {
        this.companyNameRaw = companyNameRaw;
    }

    public String getCompanyShortName() {
        return companyShortName;
    }

    public void setCompanyShortName(String companyShortName) {
        this.companyShortName = companyShortName;
    }

    public String getWelfareTags() {
        return welfareTags;
    }

    public void setWelfareTags(String welfareTags) {
        this.welfareTags = welfareTags;
    }

    public String getUnifiedCreditCode() {
        return unifiedCreditCode;
    }

    public void setUnifiedCreditCode(String unifiedCreditCode) {
        this.unifiedCreditCode = unifiedCreditCode;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public void setIndustryCode(String industryCode) {
        this.industryCode = industryCode;
    }

    public String getCompanyTypeRaw() {
        return companyTypeRaw;
    }

    public void setCompanyTypeRaw(String companyTypeRaw) {
        this.companyTypeRaw = companyTypeRaw;
    }

    public String getCompanySizeRaw() {
        return companySizeRaw;
    }

    public void setCompanySizeRaw(String companySizeRaw) {
        this.companySizeRaw = companySizeRaw;
    }

    public String getCompanyRegionCode() {
        return companyRegionCode;
    }

    public void setCompanyRegionCode(String companyRegionCode) {
        this.companyRegionCode = companyRegionCode;
    }

    public String getCompanyAddressRaw() {
        return companyAddressRaw;
    }

    public void setCompanyAddressRaw(String companyAddressRaw) {
        this.companyAddressRaw = companyAddressRaw;
    }

    public String getCompanyWebsiteRaw() {
        return companyWebsiteRaw;
    }

    public void setCompanyWebsiteRaw(String companyWebsiteRaw) {
        this.companyWebsiteRaw = companyWebsiteRaw;
    }

    public String getIsPublicRaw() {
        return isPublicRaw;
    }

    public void setIsPublicRaw(String isPublicRaw) {
        this.isPublicRaw = isPublicRaw;
    }

    public String getOfflineTimeRaw() {
        return offlineTimeRaw;
    }

    public void setOfflineTimeRaw(String offlineTimeRaw) {
        this.offlineTimeRaw = offlineTimeRaw;
    }

    public String getSourcePublishTimeRaw() {
        return sourcePublishTimeRaw;
    }

    public void setSourcePublishTimeRaw(String sourcePublishTimeRaw) {
        this.sourcePublishTimeRaw = sourcePublishTimeRaw;
    }

    public String getImportBatchNo() {
        return importBatchNo;
    }

    public void setImportBatchNo(String importBatchNo) {
        this.importBatchNo = importBatchNo;
    }

    public String getImportFileName() {
        return importFileName;
    }

    public void setImportFileName(String importFileName) {
        this.importFileName = importFileName;
    }

    public Integer getSourceRowNumber() {
        return sourceRowNumber;
    }

    public void setSourceRowNumber(Integer sourceRowNumber) {
        this.sourceRowNumber = sourceRowNumber;
    }

    public String getRawFieldJson() {
        return rawFieldJson;
    }

    public void setRawFieldJson(String rawFieldJson) {
        this.rawFieldJson = rawFieldJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSalaryRangeText() {
        if (StringUtils.hasText(salaryDisplayText)) {
            return salaryDisplayText.trim();
        }
        if (StringUtils.hasText(salaryRawText)) {
            return salaryRawText.trim();
        }
        String min = normalize(salaryMinRaw);
        String max = normalize(salaryMaxRaw);
        if (StringUtils.hasText(min) && StringUtils.hasText(max)) {
            return min + " ~ " + max;
        }
        if (StringUtils.hasText(min)) {
            return min;
        }
        if (StringUtils.hasText(max)) {
            return max;
        }
        return "-";
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
