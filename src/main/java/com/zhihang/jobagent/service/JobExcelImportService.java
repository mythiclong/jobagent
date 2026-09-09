package com.zhihang.jobagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihang.jobagent.dto.JobExcelImportPreview;
import com.zhihang.jobagent.dto.JobExcelImportPreviewItem;
import com.zhihang.jobagent.dto.SchoolJobExcelRow;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.JobSourceSnapshot;
import com.zhihang.jobagent.entity.UpdateLog;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.JobSourceSnapshotRepository;
import com.zhihang.jobagent.repository.UpdateLogRepository;
import com.zhihang.jobagent.support.JobDisplayFormatter;
import com.zhihang.jobagent.support.JobSalaryNormalizer;
import com.zhihang.jobagent.service.update.DirectionClassifier;
import com.zhihang.jobagent.service.update.UpdateSourceSupport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class JobExcelImportService {

    public static final String UPDATE_TYPE = "JOB_EXCEL_IMPORT";
    public static final String SOURCE_NAME = "SCHOOL_JOB_EXCEL";
    public static final String ACTION_CREATE = "新增";
    public static final String ACTION_SUPPLEMENT = "补充";
    public static final String ACTION_SKIP = "跳过";
    public static final String ACTION_FAIL = "失败";
    private static final String PREVIEW_SNAPSHOT_LABEL = "校招原始快照";

    private static final List<String> SUPPORTED_HEADERS = List.of(
            "招聘类型",
            "职位ID",
            "职位名称",
            "招聘标题",
            "职位性质",
            "工作地点代码",
            "学历要求",
            "专业",
            "招聘人数",
            "职位类别代码",
            "最低月薪",
            "最高月薪",
            "职位详情",
            "职位访问链接",
            "公司名称",
            "公司简称",
            "公司福利标签",
            "统一社会信用代码",
            "所属行业代码",
            "公司性质",
            "公司规模",
            "公司地区代码",
            "公司详细地址",
            "公司网址",
            "是否公开发布",
            "下线时间",
            "职位原始发布时间"
    );

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:m:s"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd H:m:s"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-M-d H:m"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:m"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd H:m")
    );
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy年M月d日")
    );
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter BATCH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final TypeReference<LinkedHashMap<String, String>> RAW_DATA_TYPE = new TypeReference<>() {
    };
    private static final Map<String, String> HEADER_LOOKUP = SUPPORTED_HEADERS.stream()
            .collect(Collectors.toMap(JobExcelImportService::normalizeHeaderKey, header -> header, (left, right) -> left, LinkedHashMap::new));

    private final JobPostRepository jobPostRepository;
    private final JobSourceSnapshotRepository jobSourceSnapshotRepository;
    private final UpdateLogRepository updateLogRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final ContentNormalizationService contentNormalizationService;

    public JobExcelImportService(JobPostRepository jobPostRepository,
                                 JobSourceSnapshotRepository jobSourceSnapshotRepository,
                                 UpdateLogRepository updateLogRepository,
                                 ObjectMapper objectMapper,
                                 PlatformTransactionManager transactionManager,
                                 ContentNormalizationService contentNormalizationService) {
        this.jobPostRepository = jobPostRepository;
        this.jobSourceSnapshotRepository = jobSourceSnapshotRepository;
        this.updateLogRepository = updateLogRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.contentNormalizationService = contentNormalizationService;
    }

    public JobExcelImportPreview buildPreview(MultipartFile file) {
        validateFile(file);

        JobExcelImportPreview preview = new JobExcelImportPreview();
        preview.setToken(UUID.randomUUID().toString());
        preview.setFileName(resolveFileName(file));
        preview.setImportBatchNo(generateImportBatchNo());
        preview.setGeneratedAt(LocalDateTime.now());

        try (InputStream inputStream = file.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel 文件没有可读取的 sheet。");
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel 第一行缺少表头，无法解析。");
            }

            Map<String, Integer> headerIndexes = extractHeaderIndexes(headerRow, formatter);
            if (!headerIndexes.containsKey("职位ID")) {
                throw new IllegalArgumentException("Excel 缺少必需表头：职位ID。");
            }

            preview.setRecognizedHeaders(new ArrayList<>(headerIndexes.keySet()));
            preview.setMissingHeaders(SUPPORTED_HEADERS.stream()
                    .filter(header -> !headerIndexes.containsKey(header))
                    .toList());
            preview.setRecognizedKeyFieldCount(headerIndexes.size());

            Set<String> seenJobIds = new LinkedHashSet<>();
            List<SchoolJobExcelRow> validRows = new ArrayList<>();
            List<JobExcelImportPreviewItem> items = new ArrayList<>();
            Set<String> supplementSummary = new LinkedHashSet<>();
            int skippedCount = 0;
            int failedCount = 0;
            int totalRows = 0;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row, formatter, headerIndexes.values())) {
                    continue;
                }
                totalRows++;

                try {
                    SchoolJobExcelRow parsedRow = parseRow(row, formatter, headerIndexes);
                    JobExcelImportPreviewItem item = baseItem(parsedRow);
                    item.setRowData(parsedRow);

                    if (!StringUtils.hasText(parsedRow.getSourceJobId())) {
                        item.setAction(ACTION_SKIP);
                        item.setReason("职位ID 为空，已跳过该行。");
                        items.add(item);
                        skippedCount++;
                        continue;
                    }

                    if (!seenJobIds.add(parsedRow.getSourceJobId())) {
                        item.setAction(ACTION_SKIP);
                        item.setReason("Excel 内存在重复的职位ID，当前重复行已跳过。");
                        items.add(item);
                        skippedCount++;
                        continue;
                    }

                    items.add(item);
                    validRows.add(parsedRow);
                } catch (IllegalArgumentException ex) {
                    JobExcelImportPreviewItem item = new JobExcelImportPreviewItem();
                    item.setRowNumber(rowIndex + 1);
                    item.setAction(ACTION_FAIL);
                    item.setReason(ex.getMessage());
                    items.add(item);
                    failedCount++;
                } catch (Exception ex) {
                    JobExcelImportPreviewItem item = new JobExcelImportPreviewItem();
                    item.setRowNumber(rowIndex + 1);
                    item.setAction(ACTION_FAIL);
                    item.setReason("该行解析失败：" + safeExceptionMessage(ex));
                    items.add(item);
                    failedCount++;
                }
            }

            Map<String, JobPost> existingJobMap = loadExistingJobMap(validRows.stream()
                    .map(SchoolJobExcelRow::getSourceJobId)
                    .filter(StringUtils::hasText)
                    .toList());

            int creatableCount = 0;
            int supplementableCount = 0;
            for (JobExcelImportPreviewItem item : items) {
                if (StringUtils.hasText(item.getAction()) || item.getRowData() == null) {
                    continue;
                }
                SchoolJobExcelRow row = item.getRowData();
                JobPost existingJob = existingJobMap.get(row.getSourceJobId());
                String warningText = joinWarnings(row.getWarnings());
                if (existingJob == null) {
                    List<String> createFields = collectCreateFields(row);
                    item.setSupplementFields(createFields);
                    item.setAction(ACTION_CREATE);
                    item.setReason(appendWarning(buildCreateReason(createFields), warningText));
                    supplementSummary.addAll(createFields);
                    creatableCount++;
                    continue;
                }

                List<String> supplementFields = collectSupplementableFields(existingJob, row);
                item.setSupplementFields(supplementFields);
                item.setAction(ACTION_SUPPLEMENT);
                item.setReason(appendWarning(buildSupplementReason(supplementFields, hasWritableStandardField(existingJob, row)), warningText));
                supplementSummary.addAll(supplementFields);
                supplementableCount++;
            }

            preview.setTotalRows(totalRows);
            preview.setCreatableCount(creatableCount);
            preview.setSupplementableCount(supplementableCount);
            preview.setSkippedCount(skippedCount);
            preview.setFailedCount(failedCount);
            preview.setSupplementFieldSummary(new ArrayList<>(supplementSummary));
            preview.setItems(items);
            return preview;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Excel 文件读取失败：" + safeExceptionMessage(ex), ex);
        }
    }

    public UpdateLog confirmImport(JobExcelImportPreview preview, String operatorName) {
        if (preview == null) {
            throw new IllegalArgumentException("导入预览不存在，请先重新解析 Excel。");
        }

        AtomicInteger addedCount = new AtomicInteger();
        AtomicInteger updatedCount = new AtomicInteger();
        AtomicInteger snapshotCount = new AtomicInteger();
        int skippedCount = preview.getSkippedCount();
        AtomicInteger failedCount = new AtomicInteger(preview.getFailedCount());
        List<String> failureNotes = new ArrayList<>();

        for (JobExcelImportPreviewItem item : preview.getItems()) {
            if (!ACTION_CREATE.equals(item.getAction()) && !ACTION_SUPPLEMENT.equals(item.getAction())) {
                continue;
            }

            SchoolJobExcelRow row = item.getRowData();
            if (row == null || !StringUtils.hasText(row.getSourceJobId())) {
                skippedCount++;
                continue;
            }

            try {
                transactionTemplate.executeWithoutResult(status -> {
                    JobPost existingJob = jobPostRepository.findBySourceJobId(row.getSourceJobId()).orElse(null);
                    if (existingJob == null) {
                        JobPost jobPost = new JobPost();
                        applyNewJob(jobPost, row);
                        JobPost savedJob = jobPostRepository.save(jobPost);
                        jobSourceSnapshotRepository.save(buildSnapshot(savedJob, row, preview.getImportBatchNo(), preview.getFileName()));
                        addedCount.incrementAndGet();
                        snapshotCount.incrementAndGet();
                        return;
                    }

                    List<String> supplementedFields = new ArrayList<>();
                    applySupplement(existingJob, row, supplementedFields);
                    existingJob.setUpdatedAt(LocalDateTime.now());
                    JobPost savedJob = jobPostRepository.save(existingJob);
                    jobSourceSnapshotRepository.save(buildSnapshot(savedJob, row, preview.getImportBatchNo(), preview.getFileName()));
                    updatedCount.incrementAndGet();
                    snapshotCount.incrementAndGet();
                });
            } catch (Exception ex) {
                failedCount.incrementAndGet();
                failureNotes.add("第" + defaultRowNumber(row) + "行（职位ID: " + row.getSourceJobId() + "）失败：" + safeExceptionMessage(ex));
            }
        }

        UpdateLog log = new UpdateLog();
        log.setUpdateType(UPDATE_TYPE);
        log.setSourceName(SOURCE_NAME);
        log.setTriggerMode("MANUAL_CONFIRM");
        log.setFileName(preview.getFileName());
        log.setImportBatchNo(preview.getImportBatchNo());
        log.setOperatorName(StringUtils.hasText(operatorName) ? operatorName.trim() : "admin");
        log.setTotalCount(preview.getTotalRows());
        log.setAddedCount(addedCount.get());
        log.setUpdatedCount(updatedCount.get());
        log.setSkippedCount(skippedCount);
        log.setFailedCount(failedCount.get());
        log.setSnapshotCount(snapshotCount.get());
        log.setStatus(resolveStatus(addedCount.get(), updatedCount.get(), skippedCount, failedCount.get()));
        log.setMessage(buildLogMessage(preview, addedCount.get(), updatedCount.get(), skippedCount, failedCount.get(), snapshotCount.get(), failureNotes));
        log.setCreatedAt(LocalDateTime.now());
        return updateLogRepository.save(log);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请先选择一个 .xlsx 文件。");
        }
        String fileName = resolveFileName(file).toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".xlsx")) {
            throw new IllegalArgumentException("当前只支持导入 .xlsx 文件。");
        }
    }

    private String resolveFileName(MultipartFile file) {
        if (file == null || !StringUtils.hasText(file.getOriginalFilename())) {
            return "job-import.xlsx";
        }
        return file.getOriginalFilename().trim();
    }

    private String generateImportBatchNo() {
        return "JOBEXCEL-" + BATCH_TIME_FORMATTER.format(LocalDateTime.now()) + "-"
                + Integer.toHexString(ThreadLocalRandom.current().nextInt(0x1000, 0x10000)).toUpperCase(Locale.ROOT);
    }

    private Map<String, Integer> extractHeaderIndexes(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String headerText = formatter.formatCellValue(cell);
            String normalizedHeader = HEADER_LOOKUP.get(normalizeHeaderKey(headerText));
            if (normalizedHeader != null && !result.containsKey(normalizedHeader)) {
                result.put(normalizedHeader, cell.getColumnIndex());
            }
        }
        return result;
    }

    private SchoolJobExcelRow parseRow(Row row, DataFormatter formatter, Map<String, Integer> headerIndexes) {
        SchoolJobExcelRow result = new SchoolJobExcelRow();
        result.setRowNumber(row.getRowNum() + 1);
        result.setSourceFieldMap(buildSourceFieldMap(row, formatter, headerIndexes));

        result.setRecruitmentType(readString(row, formatter, headerIndexes, "招聘类型"));
        result.setSourceJobId(readString(row, formatter, headerIndexes, "职位ID"));
        result.setJobName(readString(row, formatter, headerIndexes, "职位名称"));
        result.setJobTitle(readString(row, formatter, headerIndexes, "招聘标题"));
        result.setJobNature(readString(row, formatter, headerIndexes, "职位性质"));
        result.setWorkLocationCode(readString(row, formatter, headerIndexes, "工作地点代码"));
        result.setEducationRequirement(readString(row, formatter, headerIndexes, "学历要求"));
        result.setMajorRequirement(readString(row, formatter, headerIndexes, "专业"));
        result.setHeadCount(parseIntegerCell(row, formatter, headerIndexes, "招聘人数", result.getWarnings()));
        result.setJobCategoryCode(readString(row, formatter, headerIndexes, "职位类别代码"));
        String salaryMinRaw = readString(row, formatter, headerIndexes, "最低月薪");
        String salaryMaxRaw = readString(row, formatter, headerIndexes, "最高月薪");
        result.setSalaryMin(parseDecimalCell(row, formatter, headerIndexes, "最低月薪", result.getWarnings()));
        result.setSalaryMax(parseDecimalCell(row, formatter, headerIndexes, "最高月薪", result.getWarnings()));
        JobSalaryNormalizer.SalaryNormalization salaryNormalization = JobSalaryNormalizer.normalize(
                salaryMinRaw,
                salaryMaxRaw,
                result.getSalaryMin(),
                result.getSalaryMax()
        );
        result.setSalaryRawText(salaryNormalization.rawText());
        result.setSalaryDisplayText(salaryNormalization.displayText());
        result.setSalaryUnitType(salaryNormalization.unitType());
        result.setSalaryMin(salaryNormalization.monthlyComparableMin());
        result.setSalaryMax(salaryNormalization.monthlyComparableMax());
        result.setJobDescription(readString(row, formatter, headerIndexes, "职位详情"));
        result.setJobUrl(readString(row, formatter, headerIndexes, "职位访问链接"));
        result.setCompanyName(readString(row, formatter, headerIndexes, "公司名称"));
        result.setCompanyShortName(readString(row, formatter, headerIndexes, "公司简称"));
        result.setCompanyBenefitTags(readString(row, formatter, headerIndexes, "公司福利标签"));
        result.setUnifiedSocialCreditCode(readString(row, formatter, headerIndexes, "统一社会信用代码"));
        result.setIndustryCode(readString(row, formatter, headerIndexes, "所属行业代码"));
        result.setCompanyType(readString(row, formatter, headerIndexes, "公司性质"));
        result.setCompanySize(readString(row, formatter, headerIndexes, "公司规模"));
        result.setCompanyRegionCode(readString(row, formatter, headerIndexes, "公司地区代码"));
        result.setCompanyAddress(readString(row, formatter, headerIndexes, "公司详细地址"));
        result.setCompanyWebsite(readString(row, formatter, headerIndexes, "公司网址"));
        result.setIsPublic(parseBooleanCell(row, formatter, headerIndexes, "是否公开发布", result.getWarnings()));
        result.setOfflineTime(parseDateTimeCell(row, formatter, headerIndexes, "下线时间", result.getWarnings()));
        result.setSourcePublishTime(parseDateTimeCell(row, formatter, headerIndexes, "职位原始发布时间", result.getWarnings()));

        if (!StringUtils.hasText(resolveJobName(result))) {
            throw new IllegalArgumentException("第 " + result.getRowNumber() + " 行缺少职位名称/招聘标题，无法生成岗位。");
        }
        return result;
    }

    private Map<String, String> buildSourceFieldMap(Row row,
                                                    DataFormatter formatter,
                                                    Map<String, Integer> headerIndexes) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String header : SUPPORTED_HEADERS) {
            String value = readString(row, formatter, headerIndexes, header);
            if (StringUtils.hasText(value)) {
                result.put(header, value);
            }
        }
        return result;
    }

    private String readString(Row row,
                              DataFormatter formatter,
                              Map<String, Integer> headerIndexes,
                              String header) {
        Integer columnIndex = headerIndexes.get(header);
        if (columnIndex == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }
        return normalizeCellText(formatter.formatCellValue(cell));
    }

    private Integer parseIntegerCell(Row row,
                                     DataFormatter formatter,
                                     Map<String, Integer> headerIndexes,
                                     String header,
                                     List<String> warnings) {
        BigDecimal decimal = parseDecimalCell(row, formatter, headerIndexes, header, warnings);
        if (decimal == null) {
            return null;
        }
        return decimal.setScale(0, RoundingMode.DOWN).intValue();
    }

    private BigDecimal parseDecimalCell(Row row,
                                        DataFormatter formatter,
                                        Map<String, Integer> headerIndexes,
                                        String header,
                                        List<String> warnings) {
        Integer columnIndex = headerIndexes.get(header);
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
        }
        String text = normalizeCellText(formatter.formatCellValue(cell));
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(text.replace(",", ""));
        if (matcher.find()) {
            return new BigDecimal(matcher.group()).stripTrailingZeros();
        }
        warnings.add(header + " 无法解析为数字，已置空。");
        return null;
    }

    private Boolean parseBooleanCell(Row row,
                                     DataFormatter formatter,
                                     Map<String, Integer> headerIndexes,
                                     String header,
                                     List<String> warnings) {
        String text = readString(row, formatter, headerIndexes, header);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (List.of("是", "true", "1", "y", "yes", "公开", "已公开").contains(normalized)) {
            return Boolean.TRUE;
        }
        if (List.of("否", "false", "0", "n", "no", "不公开", "未公开").contains(normalized)) {
            return Boolean.FALSE;
        }
        warnings.add(header + " 无法识别为布尔值，已置空。");
        return null;
    }

    private LocalDateTime parseDateTimeCell(Row row,
                                            DataFormatter formatter,
                                            Map<String, Integer> headerIndexes,
                                            String header,
                                            List<String> warnings) {
        Integer columnIndex = headerIndexes.get(header);
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }

        String text = normalizeCellText(formatter.formatCellValue(cell));
        if (!StringUtils.hasText(text)) {
            return null;
        }

        for (DateTimeFormatter formatterItem : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatterItem);
            } catch (DateTimeParseException ignored) {
                // Try next formatter.
            }
        }
        for (DateTimeFormatter formatterItem : DATE_FORMATTERS) {
            try {
                LocalDate localDate = LocalDate.parse(text, formatterItem);
                return localDate.atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // Try next formatter.
            }
        }
        warnings.add(header + " 无法解析为时间，已置空。");
        return null;
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter, Collection<Integer> relevantColumns) {
        for (Integer columnIndex : relevantColumns) {
            if (columnIndex == null) {
                continue;
            }
            Cell cell = row.getCell(columnIndex);
            if (cell != null && StringUtils.hasText(normalizeCellText(formatter.formatCellValue(cell)))) {
                return false;
            }
        }
        return true;
    }

    private Map<String, JobPost> loadExistingJobMap(List<String> sourceJobIds) {
        if (sourceJobIds == null || sourceJobIds.isEmpty()) {
            return Map.of();
        }
        return jobPostRepository.findBySourceJobIdIn(sourceJobIds).stream()
                .collect(Collectors.toMap(JobPost::getSourceJobId, job -> job, (left, right) -> left, LinkedHashMap::new));
    }

    private List<String> collectCreateFields(SchoolJobExcelRow row) {
        List<String> fields = new ArrayList<>();
        addCreateFieldIfPresent(fields, "职位名称", resolveJobName(row));
        addCreateFieldIfPresent(fields, "招聘标题", resolveJobTitle(row));
        addCreateFieldIfPresent(fields, "职位性质", row.getJobNature());
        addCreateFieldIfPresent(fields, "学历要求", row.getEducationRequirement());
        addCreateFieldIfPresent(fields, "专业要求", row.getMajorRequirement());
        addCreateFieldIfPresent(fields, "招聘人数", row.getHeadCount());
        addCreateFieldIfPresent(fields, "最低月薪", row.getSalaryMin());
        addCreateFieldIfPresent(fields, "最高月薪", row.getSalaryMax());
        addCreateFieldIfPresent(fields, "薪资展示", row.getSalaryDisplayText());
        addCreateFieldIfPresent(fields, "职位详情", contentNormalizationService.cleanImportedText(row.getJobDescription()));
        addCreateFieldIfPresent(fields, "原始链接", normalizeUrl(row.getJobUrl()));
        addCreateFieldIfPresent(fields, "公司名称", row.getCompanyName());
        addCreateFieldIfPresent(fields, "公司性质", row.getCompanyType());
        addCreateFieldIfPresent(fields, "公司规模", row.getCompanySize());
        addCreateFieldIfPresent(fields, "公司地址", row.getCompanyAddress());
        addCreateFieldIfPresent(fields, "公司网址", normalizeUrl(row.getCompanyWebsite()));
        addCreateFieldIfPresent(fields, "是否公开", row.getIsPublic());
        addCreateFieldIfPresent(fields, "下线时间", row.getOfflineTime());
        addCreateFieldIfPresent(fields, "原始发布时间", row.getSourcePublishTime());
        addCreateFieldIfPresent(fields, "岗位方向", inferJobDirection(row));
        addCreateFieldIfPresent(fields, "岗位要求", buildRequirementText(row));
        addCreateFieldIfPresent(fields, "展示摘要", buildDisplaySummary(row));
        if (!fields.contains(PREVIEW_SNAPSHOT_LABEL)) {
            fields.add(PREVIEW_SNAPSHOT_LABEL);
        }
        return fields;
    }

    private boolean hasWritableStandardField(JobPost existingJob, SchoolJobExcelRow row) {
        List<String> fields = collectSupplementableFields(existingJob, row);
        fields.remove(PREVIEW_SNAPSHOT_LABEL);
        return !fields.isEmpty();
    }

    private void addCreateFieldIfPresent(List<String> fields, String label, Object value) {
        if (value instanceof String textValue) {
            if (StringUtils.hasText(textValue)) {
                fields.add(label);
            }
            return;
        }
        if (value != null) {
            fields.add(label);
        }
    }

    private List<String> collectSupplementableFields(JobPost existingJob, SchoolJobExcelRow row) {
        List<String> fields = new ArrayList<>();
        addFieldIfSupplementable(fields, "岗位名称", existingJob.getJobName(), resolveJobName(row));
        addFieldIfSupplementable(fields, "招聘标题", existingJob.getJobTitle(), resolveJobTitle(row));
        addFieldIfSupplementable(fields, "职位性质", existingJob.getJobNature(), row.getJobNature());
        addFieldIfSupplementable(fields, "学历要求", existingJob.getEducationRequirement(), row.getEducationRequirement());
        addFieldIfSupplementable(fields, "专业要求", existingJob.getMajorRequirement(), row.getMajorRequirement());
        addFieldIfSupplementable(fields, "招聘人数", existingJob.getHeadCount(), row.getHeadCount());
        addFieldIfSupplementable(fields, "最低月薪", existingJob.getSalaryMin(), row.getSalaryMin());
        addFieldIfSupplementable(fields, "最高月薪", existingJob.getSalaryMax(), row.getSalaryMax());
        addFieldIfSupplementable(fields, "薪资展示", existingJob.getSalaryDisplayText(), row.getSalaryDisplayText());
        addFieldIfSupplementable(fields, "职位详情", existingJob.getJobDescription(), contentNormalizationService.cleanImportedText(row.getJobDescription()));
        addFieldIfSupplementable(fields, "职位链接", existingJob.getJobUrl(), row.getJobUrl());
        addFieldIfSupplementable(fields, "公司名称", existingJob.getCompanyName(), row.getCompanyName());
        addFieldIfSupplementable(fields, "公司性质", existingJob.getCompanyType(), row.getCompanyType());
        addFieldIfSupplementable(fields, "公司规模", existingJob.getCompanySize(), row.getCompanySize());
        addFieldIfSupplementable(fields, "公司地址", existingJob.getCompanyAddress(), row.getCompanyAddress());
        addFieldIfSupplementable(fields, "公司网址", existingJob.getCompanyWebsite(), row.getCompanyWebsite());
        addFieldIfSupplementable(fields, "是否公开", existingJob.getIsPublic(), row.getIsPublic());
        addFieldIfSupplementable(fields, "下线时间", existingJob.getOfflineTime(), row.getOfflineTime());
        addFieldIfSupplementable(fields, "原始发布时间", existingJob.getSourcePublishTime(), row.getSourcePublishTime());
        addFieldIfSupplementable(fields, "岗位方向", existingJob.getJobDirection(), inferJobDirection(row));
        addFieldIfSupplementable(fields, "岗位要求", existingJob.getJobRequirements(), buildRequirementText(row));
        addFieldIfSupplementable(fields, "加分项", existingJob.getBonusPoints(), buildBonusText(row));
        addFieldIfSupplementable(fields, "适合年级", existingJob.getSuitableGrade(), inferSuitableGrade(row));
        addFieldIfSupplementable(fields, "工作地点", existingJob.getJobLocation(), resolveJobLocation(row));
        addFieldIfSupplementable(fields, "来源渠道", existingJob.getSourceName(), SOURCE_NAME);
        addFieldIfSupplementable(fields, "外部链接", existingJob.getExternalUrl(), normalizeUrl(row.getJobUrl()));
        addFieldIfSupplementable(fields, "外部键", existingJob.getExternalKey(), buildExternalKey(row.getSourceJobId()));
        addFieldIfSupplementable(fields, "发布时间文本", existingJob.getPublishDateText(), resolvePublishDateText(row));
        if (hasMissingRawData(existingJob.getSourceRawData(), row.getSourceFieldMap())) {
            fields.add("原始来源字段");
        }
        fields.add(PREVIEW_SNAPSHOT_LABEL);
        return new ArrayList<>(new LinkedHashSet<>(fields));
    }

    private void applyNewJob(JobPost jobPost, SchoolJobExcelRow row) {
        LocalDateTime now = LocalDateTime.now();
        jobPost.setSourceJobId(truncate(row.getSourceJobId(), 120));
        jobPost.setJobName(truncate(resolveJobName(row), 255));
        jobPost.setJobTitle(truncate(resolveJobTitle(row), 255));
        jobPost.setJobNature(truncate(row.getJobNature(), 120));
        jobPost.setEducationRequirement(truncate(row.getEducationRequirement(), 120));
        jobPost.setMajorRequirement(truncate(row.getMajorRequirement(), 255));
        jobPost.setHeadCount(row.getHeadCount());
        jobPost.setSalaryMin(row.getSalaryMin());
        jobPost.setSalaryMax(row.getSalaryMax());
        jobPost.setSalaryRawText(truncate(row.getSalaryRawText(), 255));
        jobPost.setSalaryDisplayText(truncate(row.getSalaryDisplayText(), 255));
        jobPost.setSalaryUnitType(truncate(row.getSalaryUnitType(), 32));
        jobPost.setJobDescription(truncate(contentNormalizationService.cleanImportedText(row.getJobDescription()), 6000));
        jobPost.setJobUrl(truncate(normalizeUrl(row.getJobUrl()), 255));
        jobPost.setCompanyName(truncate(row.getCompanyName(), 255));
        jobPost.setCompanyType(truncate(row.getCompanyType(), 120));
        jobPost.setCompanySize(truncate(row.getCompanySize(), 120));
        jobPost.setCompanyAddress(truncate(row.getCompanyAddress(), 255));
        jobPost.setCompanyWebsite(truncate(normalizeUrl(row.getCompanyWebsite()), 255));
        jobPost.setIsPublic(row.getIsPublic());
        jobPost.setOfflineTime(row.getOfflineTime());
        jobPost.setSourcePublishTime(row.getSourcePublishTime());
        jobPost.setSourceRawData(serializeRawData(row.getSourceFieldMap()));
        jobPost.setJobDirection(truncate(inferJobDirection(row), 120));
        jobPost.setJobRequirements(truncate(buildRequirementText(row), 6000));
        jobPost.setBonusPoints(truncate(buildBonusText(row), 6000));
        jobPost.setSuitableGrade(truncate(inferSuitableGrade(row), 120));
        jobPost.setJobLocation(truncate(resolveJobLocation(row), 120));
        jobPost.setSourceName(SOURCE_NAME);
        jobPost.setExternalUrl(truncate(normalizeUrl(row.getJobUrl()), 255));
        jobPost.setExternalKey(truncate(buildExternalKey(row.getSourceJobId()), 255));
        jobPost.setPublishDateText(truncate(resolvePublishDateText(row), 80));
        jobPost.setRawTitle(truncate(resolveJobTitle(row), 255));
        jobPost.setRawContent(truncate(buildRawContent(row), 6000));
        jobPost.setRawUrl(truncate(normalizeUrl(row.getJobUrl()), 255));
        jobPost.setSourceLanguage(truncate(detectLanguage(resolveJobTitle(row), buildRawContent(row)), 80));
        jobPost.setDisplayTitle(truncate(resolveJobName(row), 255));
        jobPost.setDisplaySummary(truncate(buildDisplaySummary(row), 1200));
        jobPost.setDisplayTags(truncate(buildDisplayTags(row), 255));
        jobPost.setDisplayCategory("岗位");
        jobPost.setDisplayLanguage("中文");
        jobPost.setDisplayDifficulty(truncate(inferDisplayDifficulty(row), 120));
        jobPost.setDisplayStage(truncate(inferDisplayStage(row), 120));
        jobPost.setNormalizedStatus("已导入 / 待整理");
        jobPost.setUpdatedAt(now);
    }

    private void applySupplement(JobPost jobPost, SchoolJobExcelRow row, List<String> supplementedFields) {
        fillStringIfBlank(jobPost.getJobName(), resolveJobName(row), value -> jobPost.setJobName(truncate(value, 255)), "岗位名称", supplementedFields);
        fillStringIfBlank(jobPost.getJobTitle(), resolveJobTitle(row), value -> jobPost.setJobTitle(truncate(value, 255)), "招聘标题", supplementedFields);
        fillStringIfBlank(jobPost.getJobNature(), row.getJobNature(), value -> jobPost.setJobNature(truncate(value, 120)), "职位性质", supplementedFields);
        fillStringIfBlank(jobPost.getEducationRequirement(), row.getEducationRequirement(), value -> jobPost.setEducationRequirement(truncate(value, 120)), "学历要求", supplementedFields);
        fillStringIfBlank(jobPost.getMajorRequirement(), row.getMajorRequirement(), value -> jobPost.setMajorRequirement(truncate(value, 255)), "专业要求", supplementedFields);
        fillObjectIfBlank(jobPost.getHeadCount(), row.getHeadCount(), jobPost::setHeadCount, "招聘人数", supplementedFields);
        fillObjectIfBlank(jobPost.getSalaryMin(), row.getSalaryMin(), jobPost::setSalaryMin, "最低月薪", supplementedFields);
        fillObjectIfBlank(jobPost.getSalaryMax(), row.getSalaryMax(), jobPost::setSalaryMax, "最高月薪", supplementedFields);
        fillStringIfBlank(jobPost.getSalaryRawText(), row.getSalaryRawText(), value -> jobPost.setSalaryRawText(truncate(value, 255)), "原始薪资文本", supplementedFields);
        fillStringIfBlank(jobPost.getSalaryDisplayText(), row.getSalaryDisplayText(), value -> jobPost.setSalaryDisplayText(truncate(value, 255)), "薪资展示值", supplementedFields);
        fillStringIfBlank(jobPost.getSalaryUnitType(), row.getSalaryUnitType(), value -> jobPost.setSalaryUnitType(truncate(value, 32)), "薪资单位类型", supplementedFields);
        fillStringIfBlank(jobPost.getJobDescription(),
                contentNormalizationService.cleanImportedText(row.getJobDescription()),
                value -> jobPost.setJobDescription(truncate(value, 6000)),
                "职位详情",
                supplementedFields);
        fillStringIfBlank(jobPost.getJobUrl(), normalizeUrl(row.getJobUrl()), value -> jobPost.setJobUrl(truncate(value, 255)), "职位链接", supplementedFields);
        fillStringIfBlank(jobPost.getCompanyName(), row.getCompanyName(), value -> jobPost.setCompanyName(truncate(value, 255)), "公司名称", supplementedFields);
        fillStringIfBlank(jobPost.getCompanyType(), row.getCompanyType(), value -> jobPost.setCompanyType(truncate(value, 120)), "公司性质", supplementedFields);
        fillStringIfBlank(jobPost.getCompanySize(), row.getCompanySize(), value -> jobPost.setCompanySize(truncate(value, 120)), "公司规模", supplementedFields);
        fillStringIfBlank(jobPost.getCompanyAddress(), row.getCompanyAddress(), value -> jobPost.setCompanyAddress(truncate(value, 255)), "公司地址", supplementedFields);
        fillStringIfBlank(jobPost.getCompanyWebsite(), normalizeUrl(row.getCompanyWebsite()), value -> jobPost.setCompanyWebsite(truncate(value, 255)), "公司网址", supplementedFields);
        fillObjectIfBlank(jobPost.getIsPublic(), row.getIsPublic(), jobPost::setIsPublic, "是否公开", supplementedFields);
        fillObjectIfBlank(jobPost.getOfflineTime(), row.getOfflineTime(), jobPost::setOfflineTime, "下线时间", supplementedFields);
        fillObjectIfBlank(jobPost.getSourcePublishTime(), row.getSourcePublishTime(), jobPost::setSourcePublishTime, "原始发布时间", supplementedFields);
        fillStringIfBlank(jobPost.getJobDirection(), inferJobDirection(row), value -> jobPost.setJobDirection(truncate(value, 120)), "岗位方向", supplementedFields);
        fillStringIfBlank(jobPost.getJobRequirements(), buildRequirementText(row), value -> jobPost.setJobRequirements(truncate(value, 6000)), "岗位要求", supplementedFields);
        fillStringIfBlank(jobPost.getBonusPoints(), buildBonusText(row), value -> jobPost.setBonusPoints(truncate(value, 6000)), "加分项", supplementedFields);
        fillStringIfBlank(jobPost.getSuitableGrade(), inferSuitableGrade(row), value -> jobPost.setSuitableGrade(truncate(value, 120)), "适合年级", supplementedFields);
        fillStringIfBlank(jobPost.getJobLocation(), resolveJobLocation(row), value -> jobPost.setJobLocation(truncate(value, 120)), "工作地点", supplementedFields);
        fillStringIfBlank(jobPost.getSourceName(), SOURCE_NAME, jobPost::setSourceName, "来源渠道", supplementedFields);
        fillStringIfBlank(jobPost.getExternalUrl(), normalizeUrl(row.getJobUrl()), value -> jobPost.setExternalUrl(truncate(value, 255)), "外部链接", supplementedFields);
        fillStringIfBlank(jobPost.getExternalKey(), buildExternalKey(row.getSourceJobId()), value -> jobPost.setExternalKey(truncate(value, 255)), "外部键", supplementedFields);
        fillStringIfBlank(jobPost.getPublishDateText(), resolvePublishDateText(row), value -> jobPost.setPublishDateText(truncate(value, 80)), "发布时间文本", supplementedFields);

        String mergedRawData = mergeRawData(jobPost.getSourceRawData(), row.getSourceFieldMap());
        if (!equalsTrimmed(jobPost.getSourceRawData(), mergedRawData)) {
            jobPost.setSourceRawData(mergedRawData);
            supplementedFields.add("原始来源字段");
        }

        fillStringIfBlank(jobPost.getRawTitle(), resolveJobTitle(row), value -> jobPost.setRawTitle(truncate(value, 255)), "原始标题", supplementedFields);
        fillStringIfBlank(jobPost.getRawContent(), buildRawContent(row), value -> jobPost.setRawContent(truncate(value, 6000)), "原始详情", supplementedFields);
        fillStringIfBlank(jobPost.getRawUrl(), normalizeUrl(row.getJobUrl()), value -> jobPost.setRawUrl(truncate(value, 255)), "原始链接", supplementedFields);
        fillStringIfBlank(jobPost.getSourceLanguage(), detectLanguage(resolveJobTitle(row), buildRawContent(row)), value -> jobPost.setSourceLanguage(truncate(value, 80)), "来源语言", supplementedFields);
        fillStringIfBlank(jobPost.getDisplayTitle(), resolveJobName(row), value -> jobPost.setDisplayTitle(truncate(value, 255)), "展示标题", supplementedFields);
        fillStringIfBlank(jobPost.getDisplaySummary(), buildDisplaySummary(row), value -> jobPost.setDisplaySummary(truncate(value, 1200)), "展示摘要", supplementedFields);
        fillStringIfBlank(jobPost.getDisplayTags(), buildDisplayTags(row), value -> jobPost.setDisplayTags(truncate(value, 255)), "展示标签", supplementedFields);
        fillStringIfBlank(jobPost.getDisplayCategory(), "岗位", jobPost::setDisplayCategory, "展示分类", supplementedFields);
        fillStringIfBlank(jobPost.getDisplayLanguage(), "中文", jobPost::setDisplayLanguage, "展示语言", supplementedFields);
        fillStringIfBlank(jobPost.getDisplayDifficulty(), inferDisplayDifficulty(row), value -> jobPost.setDisplayDifficulty(truncate(value, 120)), "展示难度", supplementedFields);
        fillStringIfBlank(jobPost.getDisplayStage(), inferDisplayStage(row), value -> jobPost.setDisplayStage(truncate(value, 120)), "展示阶段", supplementedFields);
        fillStringIfBlank(jobPost.getNormalizedStatus(), "已导入 / 待整理", jobPost::setNormalizedStatus, "整理状态", supplementedFields);
    }

    private JobSourceSnapshot buildSnapshot(JobPost jobPost,
                                            SchoolJobExcelRow row,
                                            String importBatchNo,
                                            String fileName) {
        JobSourceSnapshot snapshot = new JobSourceSnapshot();
        snapshot.setJobPost(jobPost);
        snapshot.setSourceJobId(truncate(row.getSourceJobId(), 120));
        snapshot.setRecruitType(truncate(rawValue(row, "招聘类型"), 120));
        snapshot.setJobNameRaw(truncate(rawValue(row, "职位名称"), 255));
        snapshot.setRecruitTitle(truncate(rawValue(row, "招聘标题"), 255));
        snapshot.setJobNatureRaw(truncate(rawValue(row, "职位性质"), 120));
        snapshot.setWorkLocationCode(truncate(rawValue(row, "工作地点代码"), 120));
        snapshot.setEducationRequirementRaw(truncate(rawValue(row, "学历要求"), 120));
        snapshot.setMajorRequirementRaw(truncate(rawValue(row, "专业"), 255));
        snapshot.setHeadCountRaw(truncate(rawValue(row, "招聘人数"), 120));
        snapshot.setJobCategoryCode(truncate(rawValue(row, "职位类别代码"), 120));
        snapshot.setSalaryMinRaw(truncate(rawValue(row, "最低月薪"), 120));
        snapshot.setSalaryMaxRaw(truncate(rawValue(row, "最高月薪"), 120));
        snapshot.setSalaryRawText(truncate(row.getSalaryRawText(), 255));
        snapshot.setSalaryDisplayText(truncate(row.getSalaryDisplayText(), 255));
        snapshot.setSalaryUnitType(truncate(row.getSalaryUnitType(), 32));
        snapshot.setJobDetailRaw(truncate(rawValue(row, "职位详情"), 12000));
        snapshot.setJobUrlRaw(truncate(normalizeUrl(rawValue(row, "职位访问链接")), 500));
        snapshot.setCompanyNameRaw(truncate(rawValue(row, "公司名称"), 255));
        snapshot.setCompanyShortName(truncate(rawValue(row, "公司简称"), 255));
        snapshot.setWelfareTags(truncate(rawValue(row, "公司福利标签"), 2000));
        snapshot.setUnifiedCreditCode(truncate(rawValue(row, "统一社会信用代码"), 120));
        snapshot.setIndustryCode(truncate(rawValue(row, "所属行业代码"), 120));
        snapshot.setCompanyTypeRaw(truncate(rawValue(row, "公司性质"), 120));
        snapshot.setCompanySizeRaw(truncate(rawValue(row, "公司规模"), 120));
        snapshot.setCompanyRegionCode(truncate(rawValue(row, "公司地区代码"), 120));
        snapshot.setCompanyAddressRaw(truncate(rawValue(row, "公司详细地址"), 1000));
        snapshot.setCompanyWebsiteRaw(truncate(normalizeUrl(rawValue(row, "公司网址")), 500));
        snapshot.setIsPublicRaw(truncate(rawValue(row, "是否公开发布"), 120));
        snapshot.setOfflineTimeRaw(truncate(rawValue(row, "下线时间"), 120));
        snapshot.setSourcePublishTimeRaw(truncate(rawValue(row, "职位原始发布时间"), 120));
        snapshot.setImportBatchNo(truncate(importBatchNo, 120));
        snapshot.setImportFileName(truncate(fileName, 255));
        snapshot.setSourceRowNumber(row.getRowNumber());
        snapshot.setRawFieldJson(serializeRawData(row.getSourceFieldMap()));
        snapshot.setCreatedAt(LocalDateTime.now());
        return snapshot;
    }

    private String rawValue(SchoolJobExcelRow row, String header) {
        if (row == null || row.getSourceFieldMap() == null) {
            return "";
        }
        return firstNonBlank(row.getSourceFieldMap().get(header));
    }

    private JobExcelImportPreviewItem baseItem(SchoolJobExcelRow row) {
        JobExcelImportPreviewItem item = new JobExcelImportPreviewItem();
        item.setRowNumber(row.getRowNumber());
        item.setSourceJobId(row.getSourceJobId());
        item.setJobName(resolveJobName(row));
        item.setCompanyName(row.getCompanyName());
        item.setEducationRequirement(firstNonBlank(row.getEducationRequirement(), rawValue(row, "学历要求")));
        item.setMajorRequirement(firstNonBlank(row.getMajorRequirement(), rawValue(row, "专业")));
        item.setHeadCountText(formatHeadCount(row));
        item.setSalaryRange(formatSalaryRange(row));
        item.setJobNature(firstNonBlank(row.getJobNature(), rawValue(row, "职位性质")));
        item.setCompanySize(firstNonBlank(row.getCompanySize(), rawValue(row, "公司规模")));
        item.setCompanyAddressSummary(summarizeAddress(firstNonBlank(row.getCompanyAddress(), rawValue(row, "公司详细地址"))));
        item.setPublicStatus(formatPublicStatus(row));
        return item;
    }

    private String resolveJobName(SchoolJobExcelRow row) {
        String resolved = JobDisplayFormatter.resolvePreferredJobTitle(
                row.getJobName(),
                row.getJobName(),
                row.getJobTitle(),
                row.getJobTitle(),
                row.getCompanyName()
        );
        return "-".equals(resolved) ? "" : resolved;
    }

    private String resolveJobTitle(SchoolJobExcelRow row) {
        return firstNonBlank(row.getJobTitle(), row.getJobName());
    }

    private String inferJobDirection(SchoolJobExcelRow row) {
        return DirectionClassifier.classifyDirection(
                resolveJobName(row),
                buildRawContent(row),
                firstNonBlank(row.getMajorRequirement(), row.getCompanyName(), row.getRecruitmentType())
        );
    }

    private String buildRequirementText(SchoolJobExcelRow row) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(row.getEducationRequirement())) {
            parts.add("学历要求：" + row.getEducationRequirement().trim());
        }
        if (StringUtils.hasText(row.getMajorRequirement())) {
            parts.add("专业要求：" + row.getMajorRequirement().trim());
        }
        if (StringUtils.hasText(row.getJobNature())) {
            parts.add("职位性质：" + row.getJobNature().trim());
        }
        if (parts.isEmpty() && StringUtils.hasText(row.getJobDescription())) {
            parts.add(shorten(contentNormalizationService.cleanImportedText(row.getJobDescription()), 220));
        }
        return String.join("\n", parts);
    }

    private String buildBonusText(SchoolJobExcelRow row) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(row.getCompanyBenefitTags())) {
            parts.add("福利标签：" + row.getCompanyBenefitTags().trim());
        }
        if (StringUtils.hasText(row.getCompanyType())) {
            parts.add("公司性质：" + row.getCompanyType().trim());
        }
        if (StringUtils.hasText(row.getCompanySize())) {
            parts.add("公司规模：" + row.getCompanySize().trim());
        }
        if (StringUtils.hasText(row.getCompanyAddress())) {
            parts.add("公司地址：" + row.getCompanyAddress().trim());
        }
        return String.join("\n", parts);
    }

    private String inferSuitableGrade(SchoolJobExcelRow row) {
        String text = firstNonBlank(row.getRecruitmentType(), row.getEducationRequirement(), row.getJobNature(), resolveJobName(row)).toLowerCase(Locale.ROOT);
        if (text.contains("实习")) {
            return "大三 / 大四";
        }
        if (text.contains("校招") || text.contains("应届")) {
            return "应届 / 在校生";
        }
        if (text.contains("硕士") || text.contains("博士")) {
            return "应届硕士 / 博士";
        }
        return "";
    }

    private String resolveJobLocation(SchoolJobExcelRow row) {
        return firstNonBlank(row.getCompanyAddress(), row.getCompanyRegionCode(), row.getWorkLocationCode());
    }

    private String resolvePublishDateText(SchoolJobExcelRow row) {
        if (row.getSourcePublishTime() != null) {
            return LOG_TIME_FORMATTER.format(row.getSourcePublishTime());
        }
        return firstNonBlank(row.getSourceFieldMap().get("职位原始发布时间"), row.getSourceFieldMap().get("下线时间"));
    }

    private String buildRawContent(SchoolJobExcelRow row) {
        return contentNormalizationService.cleanImportedText(firstNonBlank(
                row.getJobDescription(),
                joinNonBlank(buildRequirementText(row), buildBonusText(row))
        ));
    }

    private String buildDisplaySummary(SchoolJobExcelRow row) {
        if (StringUtils.hasText(row.getJobDescription())) {
            return shorten(contentNormalizationService.cleanImportedText(row.getJobDescription()), 180);
        }
        return shorten(contentNormalizationService.cleanImportedText(
                firstNonBlank(buildRequirementText(row), buildBonusText(row), "Excel 导入岗位")
        ), 180);
    }

    private String buildDisplayTags(SchoolJobExcelRow row) {
        List<String> tags = new ArrayList<>();
        addTag(tags, inferJobDirection(row));
        addTag(tags, row.getRecruitmentType());
        addTag(tags, row.getEducationRequirement());
        if (StringUtils.hasText(row.getCompanyName())) {
            tags.add("真实岗位");
        }
        return String.join(" / ", tags);
    }

    private String inferDisplayStage(SchoolJobExcelRow row) {
        String text = firstNonBlank(row.getRecruitmentType(), row.getJobNature(), resolveJobName(row)).toLowerCase(Locale.ROOT);
        if (text.contains("实习")) {
            return "实习岗位";
        }
        if (text.contains("校招") || text.contains("应届")) {
            return "校招岗位";
        }
        return "通用岗位";
    }

    private String inferDisplayDifficulty(SchoolJobExcelRow row) {
        String text = firstNonBlank(row.getEducationRequirement(), row.getJobNature(), row.getRecruitmentType()).toLowerCase(Locale.ROOT);
        if (text.contains("硕士") || text.contains("博士")) {
            return "进阶";
        }
        if (text.contains("实习") || text.contains("校招") || text.contains("应届")) {
            return "入门";
        }
        return "标准";
    }

    private String detectLanguage(String title, String content) {
        String text = firstNonBlank(title, content, title + " " + content);
        for (char ch : text.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
                return "中文";
            }
        }
        return "未知";
    }

    private String buildExternalKey(String sourceJobId) {
        if (!StringUtils.hasText(sourceJobId)) {
            return "";
        }
        return "excel:" + sourceJobId.trim();
    }

    private String formatHeadCount(SchoolJobExcelRow row) {
        if (row.getHeadCount() != null) {
            return String.valueOf(row.getHeadCount());
        }
        return firstNonBlank(rawValue(row, "招聘人数"), "-");
    }

    private String formatSalaryRange(SchoolJobExcelRow row) {
        if (StringUtils.hasText(row.getSalaryDisplayText())) {
            return row.getSalaryDisplayText().trim();
        }
        if (StringUtils.hasText(row.getSalaryRawText())) {
            return row.getSalaryRawText().trim();
        }
        return "-";
    }

    private String formatPublicStatus(SchoolJobExcelRow row) {
        if (row.getIsPublic() != null) {
            return row.getIsPublic() ? "公开" : "不公开";
        }
        return firstNonBlank(rawValue(row, "是否公开发布"), "-");
    }

    private String summarizeAddress(String address) {
        String readableSummary = JobDisplayFormatter.extractReadableLocationFromAddress(address);
        if (StringUtils.hasText(readableSummary)) {
            return readableSummary;
        }
        return shorten(firstNonBlank(address), 40);
    }

    private String buildCreateReason(List<String> createFields) {
        if (createFields == null || createFields.isEmpty()) {
            return "数据库中不存在该职位ID，将新增岗位并保存完整校招原始快照。";
        }
        List<String> sampledFields = createFields.stream().limit(8).toList();
        String suffix = createFields.size() > sampledFields.size()
                ? " 等" + createFields.size() + "项"
                : "";
        return "数据库中不存在该职位ID，将新增岗位，并带入字段：" + String.join("、", sampledFields) + suffix + "。";
    }

    private String buildSupplementReason(List<String> supplementFields, boolean hasWritableStandardField) {
        if (!hasWritableStandardField) {
            return "该职位ID已存在，标准化字段已完整；本次将刷新校招原始快照并记录最新批次。";
        }
        List<String> sampledFields = supplementFields.stream()
                .filter(label -> !PREVIEW_SNAPSHOT_LABEL.equals(label))
                .limit(8)
                .toList();
        String suffix = supplementFields.size() > sampledFields.size()
                ? " 等" + supplementFields.size() + "项"
                : "";
        return "将补充空字段：" + String.join("、", sampledFields) + suffix + "，并保存最新校招原始快照。";
    }

    private String joinWarnings(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return "";
        }
        return String.join("；", warnings);
    }

    private String appendWarning(String reason, String warningText) {
        if (!StringUtils.hasText(warningText)) {
            return reason;
        }
        return reason + " 警告：" + warningText;
    }

    private String buildLogMessage(JobExcelImportPreview preview,
                                   int addedCount,
                                   int updatedCount,
                                   int skippedCount,
                                   int failedCount,
                                   int snapshotCount,
                                   List<String> failureNotes) {
        List<String> parts = new ArrayList<>();
        parts.add("Excel 导入完成");
        parts.add("批次：" + preview.getImportBatchNo());
        parts.add("文件：" + preview.getFileName());
        parts.add("总行数：" + preview.getTotalRows());
        parts.add("新增：" + addedCount);
        parts.add("补充：" + updatedCount);
        parts.add("跳过：" + skippedCount);
        parts.add("失败：" + failedCount);
        parts.add("原始快照：" + snapshotCount);
        if (!failureNotes.isEmpty()) {
            parts.add("异常：" + String.join(" | ", failureNotes.stream().limit(2).toList()));
        }
        return truncate(String.join("；", parts), 500);
    }

    private String resolveStatus(int addedCount, int updatedCount, int skippedCount, int failedCount) {
        if (failedCount == 0) {
            return "SUCCESS";
        }
        if (addedCount > 0 || updatedCount > 0 || skippedCount > 0) {
            return "PARTIAL_SUCCESS";
        }
        return "FAILED";
    }

    private int countAction(List<JobExcelImportPreviewItem> items, String action) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (JobExcelImportPreviewItem item : items) {
            if (action.equals(item.getAction())) {
                count++;
            }
        }
        return count;
    }

    private boolean hasMissingRawData(String existingRawData, Map<String, String> newFieldMap) {
        if (newFieldMap == null || newFieldMap.isEmpty()) {
            return false;
        }
        Map<String, String> existingMap = parseRawData(existingRawData);
        for (Map.Entry<String, String> entry : newFieldMap.entrySet()) {
            if (!StringUtils.hasText(existingMap.get(entry.getKey())) && StringUtils.hasText(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private String mergeRawData(String existingRawData, Map<String, String> newFieldMap) {
        Map<String, String> existingMap = parseRawData(existingRawData);
        if (newFieldMap != null) {
            for (Map.Entry<String, String> entry : newFieldMap.entrySet()) {
                if (!StringUtils.hasText(existingMap.get(entry.getKey())) && StringUtils.hasText(entry.getValue())) {
                    existingMap.put(entry.getKey(), entry.getValue().trim());
                }
            }
        }
        return serializeRawData(existingMap);
    }

    private Map<String, String> parseRawData(String rawData) {
        if (!StringUtils.hasText(rawData)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(rawData, RAW_DATA_TYPE);
            return parsed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parsed);
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }

    private String serializeRawData(Map<String, String> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(rawData);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("原始来源字段序列化失败", ex);
        }
    }

    private void addFieldIfSupplementable(List<String> fields, String label, String existingValue, String newValue) {
        if (!StringUtils.hasText(existingValue) && StringUtils.hasText(newValue)) {
            fields.add(label);
        }
    }

    private void addFieldIfSupplementable(List<String> fields, String label, Object existingValue, Object newValue) {
        if (existingValue == null && newValue != null) {
            fields.add(label);
        }
    }

    private void fillStringIfBlank(String existingValue,
                                   String newValue,
                                   java.util.function.Consumer<String> setter,
                                   String label,
                                   List<String> supplementedFields) {
        if (!StringUtils.hasText(existingValue) && StringUtils.hasText(newValue)) {
            setter.accept(newValue.trim());
            supplementedFields.add(label);
        }
    }

    private <T> void fillObjectIfBlank(T existingValue,
                                       T newValue,
                                       java.util.function.Consumer<T> setter,
                                       String label,
                                       List<String> supplementedFields) {
        if (existingValue == null && newValue != null) {
            setter.accept(newValue);
            supplementedFields.add(label);
        }
    }

    private void addTag(List<String> tags, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String cleaned = value.trim();
        if (!tags.contains(cleaned)) {
            tags.add(cleaned);
        }
    }

    private String normalizeUrl(String value) {
        return StringUtils.hasText(value) ? UpdateSourceSupport.normalizeUrl(value.trim()) : "";
    }

    private String normalizeCellText(String value) {
        return UpdateSourceSupport.normalizeText(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
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

    private String shorten(String value, int maxLength) {
        String normalized = normalizeCellText(value);
        if (!StringUtils.hasText(normalized) || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }

    private String truncate(String value, int maxLength) {
        return UpdateSourceSupport.truncate(value, maxLength);
    }

    private String safeExceptionMessage(Exception ex) {
        if (ex == null || !StringUtils.hasText(ex.getMessage())) {
            return "未知错误";
        }
        return ex.getMessage().trim();
    }

    private int defaultRowNumber(SchoolJobExcelRow row) {
        return row == null || row.getRowNumber() == null ? -1 : row.getRowNumber();
    }

    private boolean equalsTrimmed(String left, String right) {
        return normalizeCellText(left).equals(normalizeCellText(right));
    }

    private static String normalizeHeaderKey(String header) {
        return UpdateSourceSupport.normalizeText(header).replace(" ", "");
    }
}
