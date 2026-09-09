package com.zhihang.jobagent.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JobExcelImportPreviewItem implements Serializable {

    private Integer rowNumber;
    private String sourceJobId;
    private String jobName;
    private String companyName;
    private String educationRequirement;
    private String majorRequirement;
    private String headCountText;
    private String salaryRange;
    private String jobNature;
    private String companySize;
    private String companyAddressSummary;
    private String publicStatus;
    private String action;
    private String reason;
    private List<String> supplementFields = new ArrayList<>();
    private SchoolJobExcelRow rowData;

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
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

    public String getHeadCountText() {
        return headCountText;
    }

    public void setHeadCountText(String headCountText) {
        this.headCountText = headCountText;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getJobNature() {
        return jobNature;
    }

    public void setJobNature(String jobNature) {
        this.jobNature = jobNature;
    }

    public String getCompanySize() {
        return companySize;
    }

    public void setCompanySize(String companySize) {
        this.companySize = companySize;
    }

    public String getCompanyAddressSummary() {
        return companyAddressSummary;
    }

    public void setCompanyAddressSummary(String companyAddressSummary) {
        this.companyAddressSummary = companyAddressSummary;
    }

    public String getPublicStatus() {
        return publicStatus;
    }

    public void setPublicStatus(String publicStatus) {
        this.publicStatus = publicStatus;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getSupplementFields() {
        return supplementFields;
    }

    public void setSupplementFields(List<String> supplementFields) {
        this.supplementFields = supplementFields;
    }

    public SchoolJobExcelRow getRowData() {
        return rowData;
    }

    public void setRowData(SchoolJobExcelRow rowData) {
        this.rowData = rowData;
    }
}
