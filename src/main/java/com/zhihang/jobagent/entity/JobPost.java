package com.zhihang.jobagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.zhihang.jobagent.support.JobDisplayFormatter;
import com.zhihang.jobagent.support.JobSalaryNormalizer;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "job_post")
public class JobPost {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobName;

    @Column(nullable = false)
    private String jobDirection;

    @Column(columnDefinition = "TEXT")
    private String jobRequirements;

    @Column(columnDefinition = "TEXT")
    private String bonusPoints;

    private String suitableGrade;

    @Column(unique = true, length = 120)
    private String sourceJobId;

    private String jobTitle;

    private String jobNature;

    private String educationRequirement;

    private String majorRequirement;

    private Integer headCount;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMax;

    private String salaryRawText;

    private String salaryDisplayText;

    private String salaryUnitType;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    private String jobUrl;

    private String companyName;

    private String companyType;

    private String companySize;

    private String companyAddress;

    private String companyWebsite;

    private Boolean isPublic;

    private LocalDateTime offlineTime;

    private LocalDateTime sourcePublishTime;

    @Column(columnDefinition = "TEXT")
    private String sourceRawData;

    private String jobLocation;

    private String sourceName;

    private String externalUrl;

    private String externalKey;

    private String publishDateText;

    private String rawTitle;

    @Column(columnDefinition = "TEXT")
    private String rawContent;

    private String rawUrl;

    private String sourceLanguage;

    private String displayTitle;

    @Column(columnDefinition = "TEXT")
    private String displaySummary;

    private String displayTags;

    private String displayCategory;

    private String displayLanguage;

    private String displayDifficulty;

    private String displayStage;

    private String normalizedStatus;

    private LocalDateTime updatedAt;

    public JobPost() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobDirection() {
        return jobDirection;
    }

    public void setJobDirection(String jobDirection) {
        this.jobDirection = jobDirection;
    }

    public String getJobRequirements() {
        return jobRequirements;
    }

    public void setJobRequirements(String jobRequirements) {
        this.jobRequirements = jobRequirements;
    }

    public String getBonusPoints() {
        return bonusPoints;
    }

    public void setBonusPoints(String bonusPoints) {
        this.bonusPoints = bonusPoints;
    }

    public String getSuitableGrade() {
        return suitableGrade;
    }

    public void setSuitableGrade(String suitableGrade) {
        this.suitableGrade = suitableGrade;
    }

    public String getSourceJobId() {
        return sourceJobId;
    }

    public void setSourceJobId(String sourceJobId) {
        this.sourceJobId = sourceJobId;
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
        if (StringUtils.hasText(salaryUnitType)) {
            return JobSalaryNormalizer.normalizeUnitType(salaryUnitType);
        }
        if (salaryMin != null || salaryMax != null) {
            return JobSalaryNormalizer.UNIT_MONTH;
        }
        return JobSalaryNormalizer.UNIT_UNKNOWN;
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

    public String getSourceRawData() {
        return sourceRawData;
    }

    public void setSourceRawData(String sourceRawData) {
        this.sourceRawData = sourceRawData;
    }

    public String getJobLocation() {
        return jobLocation;
    }

    public void setJobLocation(String jobLocation) {
        this.jobLocation = jobLocation;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public void setExternalKey(String externalKey) {
        this.externalKey = externalKey;
    }

    public String getPublishDateText() {
        if (StringUtils.hasText(publishDateText)) {
            return publishDateText;
        }
        if (sourcePublishTime != null) {
            return DATE_TIME_FORMATTER.format(sourcePublishTime);
        }
        return "";
    }

    public void setPublishDateText(String publishDateText) {
        this.publishDateText = publishDateText;
    }

    public String getRawTitle() {
        return firstNonBlank(rawTitle, jobTitle, jobName);
    }

    public void setRawTitle(String rawTitle) {
        this.rawTitle = rawTitle;
    }

    public String getRawContent() {
        return firstNonBlank(rawContent, jobDescription, joinNonBlank(jobRequirements, bonusPoints));
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getRawUrl() {
        return firstNonBlank(rawUrl, jobUrl, externalUrl);
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
    }

    public String getSourceLanguage() {
        if (StringUtils.hasText(sourceLanguage)) {
            return sourceLanguage;
        }
        return containsChinese(getRawTitle() + getRawContent()) ? "中文" : "未知";
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getDisplayTitle() {
        return JobDisplayFormatter.resolvePreferredJobTitle(displayTitle, jobName, jobTitle, rawTitle, companyName);
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public String getDisplaySummary() {
        return firstNonBlank(displaySummary, jobDescription, jobRequirements);
    }

    public void setDisplaySummary(String displaySummary) {
        this.displaySummary = displaySummary;
    }

    public String getDisplaySummaryPreview() {
        return JobDisplayFormatter.createSummaryPreview(getDisplaySummary());
    }

    public boolean isDisplaySummaryExpandable() {
        return JobDisplayFormatter.isSummaryExpandable(getDisplaySummary());
    }

    public String getDisplaySummaryFullText() {
        return JobDisplayFormatter.normalizeDisplayText(getDisplaySummary());
    }

    public String getDisplayTags() {
        return firstNonBlank(displayTags, jobDirection);
    }

    public void setDisplayTags(String displayTags) {
        this.displayTags = displayTags;
    }

    public String getDisplayCategory() {
        return firstNonBlank(displayCategory, "岗位");
    }

    public void setDisplayCategory(String displayCategory) {
        this.displayCategory = displayCategory;
    }

    public String getDisplayLanguage() {
        return firstNonBlank(displayLanguage, "中文");
    }

    public void setDisplayLanguage(String displayLanguage) {
        this.displayLanguage = displayLanguage;
    }

    public String getDisplayDifficulty() {
        return firstNonBlank(displayDifficulty, "标准");
    }

    public void setDisplayDifficulty(String displayDifficulty) {
        this.displayDifficulty = displayDifficulty;
    }

    public String getDisplayStage() {
        return firstNonBlank(displayStage, "通用岗位");
    }

    public void setDisplayStage(String displayStage) {
        this.displayStage = displayStage;
    }

    public String getNormalizedStatus() {
        return firstNonBlank(normalizedStatus, "待整理");
    }

    public void setNormalizedStatus(String normalizedStatus) {
        this.normalizedStatus = normalizedStatus;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getReadableLocationDisplay() {
        return JobDisplayFormatter.formatReadableLocation(jobLocation, companyAddress);
    }

    public String getReadableLocationFilterValue() {
        String readableLocation = getReadableLocationDisplay();
        return "-".equals(readableLocation) ? "" : readableLocation;
    }

    public String getSalaryRangeDisplay() {
        return JobDisplayFormatter.formatSalaryRange(salaryMin, salaryMax, salaryDisplayText, getSalaryUnitType());
    }

    public BigDecimal getMonthlySalaryMin() {
        return JobSalaryNormalizer.isMonthlyUnit(getSalaryUnitType())
                ? JobSalaryNormalizer.normalizeStoredMonthlyComparableAmount(salaryMin)
                : null;
    }

    public BigDecimal getMonthlySalaryMax() {
        return JobSalaryNormalizer.isMonthlyUnit(getSalaryUnitType())
                ? JobSalaryNormalizer.normalizeStoredMonthlyComparableAmount(salaryMax)
                : null;
    }

    public String getEducationRequirementDisplay() {
        return JobDisplayFormatter.normalizeDisplayText(educationRequirement);
    }

    public String getJobNatureDisplay() {
        return JobDisplayFormatter.normalizeDisplayText(jobNature);
    }

    public String getCompanyNameDisplay() {
        return JobDisplayFormatter.normalizeDisplayText(companyName);
    }

    public String getCompanyTypeDisplay() {
        return JobDisplayFormatter.normalizeDisplayText(companyType);
    }

    public String getCompanySizeDisplay() {
        return JobDisplayFormatter.normalizeDisplayText(companySize);
    }

    public String getCompanyAddressDisplay() {
        return JobDisplayFormatter.normalizeDisplayText(companyAddress);
    }

    public String getCompanyWebsiteDisplay() {
        return JobDisplayFormatter.normalizeDisplayText(companyWebsite);
    }

    public String getSourceTypeKey() {
        return JobDisplayFormatter.resolveSourceTypeKey(sourceName);
    }

    public String getSourceTypeLabel() {
        return JobDisplayFormatter.resolveSourceTypeLabel(sourceName);
    }

    public String getPrimaryJobLink() {
        return firstNonBlank(jobUrl, externalUrl, rawUrl);
    }

    public boolean isOrganized() {
        return !"待整理".equals(getNormalizedStatus());
    }

    public List<String> getDisplayTagList() {
        return splitItems(getDisplayTags(), "/", "、", ",", "，");
    }

    public List<String> getRequirementItemList() {
        return splitItems(jobRequirements, "\n", "；", ";");
    }

    public List<String> getBonusPointItemList() {
        return splitItems(bonusPoints, "\n", "；", ";");
    }

    private List<String> splitItems(String source, String... separators) {
        String working = source == null ? "" : source;
        for (String separator : separators) {
            working = working.replace(separator, "\n");
        }
        String[] parts = working.split("\\n");
        Set<String> result = new LinkedHashSet<>();
        for (String part : parts) {
            String item = part == null ? "" : part.trim();
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        return new ArrayList<>(result);
    }

    private String joinNonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                parts.add(value.trim());
            }
        }
        return String.join("\n", parts);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean containsChinese(String source) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        for (char ch : source.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
                return true;
            }
        }
        return false;
    }
}
