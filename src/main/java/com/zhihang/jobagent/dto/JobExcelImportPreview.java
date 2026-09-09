package com.zhihang.jobagent.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobExcelImportPreview implements Serializable {

    private String token;
    private String fileName;
    private String importBatchNo;
    private LocalDateTime generatedAt;
    private int totalRows;
    private int creatableCount;
    private int supplementableCount;
    private int skippedCount;
    private int failedCount;
    private int recognizedKeyFieldCount;
    private List<String> recognizedHeaders = new ArrayList<>();
    private List<String> missingHeaders = new ArrayList<>();
    private List<String> supplementFieldSummary = new ArrayList<>();
    private List<JobExcelImportPreviewItem> items = new ArrayList<>();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getImportBatchNo() {
        return importBatchNo;
    }

    public void setImportBatchNo(String importBatchNo) {
        this.importBatchNo = importBatchNo;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getCreatableCount() {
        return creatableCount;
    }

    public void setCreatableCount(int creatableCount) {
        this.creatableCount = creatableCount;
    }

    public int getSupplementableCount() {
        return supplementableCount;
    }

    public void setSupplementableCount(int supplementableCount) {
        this.supplementableCount = supplementableCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getRecognizedKeyFieldCount() {
        return recognizedKeyFieldCount;
    }

    public void setRecognizedKeyFieldCount(int recognizedKeyFieldCount) {
        this.recognizedKeyFieldCount = recognizedKeyFieldCount;
    }

    public List<String> getRecognizedHeaders() {
        return recognizedHeaders;
    }

    public void setRecognizedHeaders(List<String> recognizedHeaders) {
        this.recognizedHeaders = recognizedHeaders;
    }

    public List<String> getMissingHeaders() {
        return missingHeaders;
    }

    public void setMissingHeaders(List<String> missingHeaders) {
        this.missingHeaders = missingHeaders;
    }

    public List<String> getSupplementFieldSummary() {
        return supplementFieldSummary;
    }

    public void setSupplementFieldSummary(List<String> supplementFieldSummary) {
        this.supplementFieldSummary = supplementFieldSummary;
    }

    public List<JobExcelImportPreviewItem> getItems() {
        return items;
    }

    public void setItems(List<JobExcelImportPreviewItem> items) {
        this.items = items;
    }

    public boolean canConfirm() {
        return creatableCount > 0 || supplementableCount > 0;
    }
}
