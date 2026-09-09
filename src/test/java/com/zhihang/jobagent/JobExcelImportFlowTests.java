package com.zhihang.jobagent;

import com.zhihang.jobagent.dto.JobExcelImportPreview;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.JobSourceSnapshot;
import com.zhihang.jobagent.entity.UpdateLog;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.JobSourceSnapshotRepository;
import com.zhihang.jobagent.repository.UpdateLogRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobExcelImportFlowTests {

    private static final List<String> HEADERS = List.of(
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JobSourceSnapshotRepository jobSourceSnapshotRepository;

    @Autowired
    private UpdateLogRepository updateLogRepository;

    @BeforeEach
    void setUp() {
        updateLogRepository.deleteAll();
        jobSourceSnapshotRepository.deleteAll();
        jobPostRepository.deleteAll();
    }

    @Test
    void jobsPageShouldRenderExcelImportEntryAndPreviewColumns() throws Exception {
        mockMvc.perform(get("/jobs")
                        .session(buildAdminSession())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Excel 导入岗位数据")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("岗位管理")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jobExcelImportTrigger")));
    }

    @Test
    void excelPreviewAndConfirmShouldStoreSnapshotsAndRenderJobDetail() throws Exception {
        JobPost existing = new JobPost();
        existing.setSourceJobId("JOB-EXIST-1");
        existing.setJobName("人工维护岗位名");
        existing.setJobDirection("后端开发");
        existing.setJobRequirements("人工技能要求");
        existing.setCompanyName("");
        existing.setCompanyAddress("");
        existing.setCompanyWebsite("");
        jobPostRepository.save(existing);

        MockHttpSession session = buildAdminSession();
        MockMultipartFile excelFile = new MockMultipartFile(
                "excelFile",
                "school-jobs.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildWorkbookBytes(List.of(
                        List.of("校招", "JOB-NEW-1", "Java后端工程师", "2026届 Java 后端工程师", "全职", "410100",
                                "本科", "计算机相关", "3", "DEV", "8000", "12000", "负责 Java 后端接口开发与维护。",
                                "https://new.example.com/job/1", "新增公司", "新增", "餐补,住房补贴", "91410000TEST001",
                                "IT", "民营", "500-999人", "410100", "郑州高新区", "https://new.example.com",
                                "是", "2026-04-01 18:00:00", "2026-03-01 10:00:00"),
                        List.of("校招", "JOB-EXIST-1", "Excel 职位名不应覆盖", "校招 Java 岗位", "全职", "410100",
                                "本科", "软件工程", "2", "DEV", "9000", "13000", "已有岗位只补空字段。",
                                "https://supplement.example.com/job/1", "补充公司", "补充", "六险一金", "91410000TEST002",
                                "IT", "上市", "1000-9999人", "410100", "郑州高新区科学大道 1 号", "https://supplement.example.com",
                                "是", "2026-04-10 18:00:00", "2026-03-02 09:00:00"),
                        List.of("校招", "", "缺少职位ID岗位", "无效岗位", "全职", "410100",
                                "本科", "不限", "1", "DEV", "7000", "9000", "该行应跳过。",
                                "https://skip.example.com/job/1", "跳过公司", "跳过", "", "", "",
                                "民营", "50-99人", "410100", "郑州", "https://skip.example.com",
                                "否", "2026-04-12", "2026-03-03"),
                        List.of("校招", "JOB-NEW-1", "重复职位", "重复职位", "全职", "410100",
                                "本科", "不限", "1", "DEV", "7000", "9000", "该行应因重复职位ID跳过。",
                                "https://duplicate.example.com/job/1", "重复公司", "重复", "", "", "",
                                "民营", "50-99人", "410100", "郑州", "https://duplicate.example.com",
                                "否", "2026-04-12", "2026-03-03"),
                        List.of("校招", "JOB-FAIL-1", "", "", "全职", "410100",
                                "本科", "不限", "1", "DEV", "7000", "9000", "该行因缺少职位名称和标题失败。",
                                "https://fail.example.com/job/1", "失败公司", "失败", "", "", "",
                                "民营", "50-99人", "410100", "郑州", "https://fail.example.com",
                                "否", "2026-04-12", "2026-03-03")
                ))
        );

        mockMvc.perform(multipart("/jobs/import/preview")
                        .file(excelFile)
                        .session(session)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));

        Object previewObject = session.getAttribute("jobExcelImportPreview");
        assertThat(previewObject).isInstanceOf(JobExcelImportPreview.class);
        JobExcelImportPreview preview = (JobExcelImportPreview) previewObject;
        assertThat(preview.getImportBatchNo()).startsWith("JOBEXCEL-");
        assertThat(preview.getTotalRows()).isEqualTo(5);
        assertThat(preview.getCreatableCount()).isEqualTo(1);
        assertThat(preview.getSupplementableCount()).isEqualTo(1);
        assertThat(preview.getSkippedCount()).isEqualTo(2);
        assertThat(preview.getFailedCount()).isEqualTo(1);
        assertThat(preview.getRecognizedKeyFieldCount()).isGreaterThanOrEqualTo(20);
        assertThat(preview.getSupplementFieldSummary()).contains("学历要求", "专业要求", "招聘人数", "校招原始快照");

        mockMvc.perform(get("/jobs")
                        .session(session)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("当前预览批次")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("未执行导入")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("仅统计已确认导入的数据")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("最近导入记录")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Excel 导入预览")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("学历要求")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("专业要求")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("招聘人数")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("薪资范围")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("公司地址摘要")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JOB-NEW-1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JOB-EXIST-1")));

        MvcResult confirmResult = mockMvc.perform(post("/jobs/import/confirm")
                        .session(session)
                        .param("previewToken", preview.getToken())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"))
                .andReturn();

        assertThat(confirmResult.getRequest().getSession(false).getAttribute("jobExcelImportPreview")).isNull();

        JobPost newJob = jobPostRepository.findBySourceJobId("JOB-NEW-1").orElseThrow();
        assertThat(newJob.getJobName()).isEqualTo("Java后端工程师");
        assertThat(newJob.getCompanyName()).isEqualTo("新增公司");
        assertThat(newJob.getSourceName()).isEqualTo("SCHOOL_JOB_EXCEL");

        JobPost reloadedExisting = jobPostRepository.findBySourceJobId("JOB-EXIST-1").orElseThrow();
        assertThat(reloadedExisting.getJobName()).isEqualTo("人工维护岗位名");
        assertThat(reloadedExisting.getJobRequirements()).isEqualTo("人工技能要求");
        assertThat(reloadedExisting.getCompanyName()).isEqualTo("补充公司");
        assertThat(reloadedExisting.getCompanyAddress()).isEqualTo("郑州高新区科学大道 1 号");
        assertThat(reloadedExisting.getCompanyWebsite()).isEqualTo("https://supplement.example.com/");

        UpdateLog importLog = updateLogRepository.findTopByUpdateTypeOrderByCreatedAtDescIdDesc("JOB_EXCEL_IMPORT").orElseThrow();
        assertThat(importLog.getFileName()).isEqualTo("school-jobs.xlsx");
        assertThat(importLog.getOperatorName()).isEqualTo("admin");
        assertThat(importLog.getImportBatchNo()).startsWith("JOBEXCEL-");
        assertThat(importLog.getTotalCount()).isEqualTo(5);
        assertThat(importLog.getAddedCount()).isEqualTo(1);
        assertThat(importLog.getUpdatedCount()).isEqualTo(1);
        assertThat(importLog.getSkippedCount()).isEqualTo(2);
        assertThat(importLog.getFailedCount()).isEqualTo(1);
        assertThat(importLog.getSnapshotCount()).isEqualTo(2);

        assertThat(jobSourceSnapshotRepository.countByImportBatchNo(importLog.getImportBatchNo())).isEqualTo(2);
        JobSourceSnapshot latestSnapshot = jobSourceSnapshotRepository.findTopByJobPostOrderByCreatedAtDescIdDesc(newJob).orElseThrow();
        assertThat(latestSnapshot.getImportFileName()).isEqualTo("school-jobs.xlsx");
        assertThat(latestSnapshot.getRecruitType()).isEqualTo("校招");
        assertThat(latestSnapshot.getCompanyShortName()).isEqualTo("新增");

        mockMvc.perform(get("/jobs/{id}", newJob.getId())
                        .session(buildAdminSession())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("校招来源信息")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("最近导入批次号")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("school-jobs.xlsx")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(importLog.getImportBatchNo())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JOB-NEW-1")));
    }

    @Test
    void existingCompleteJobShouldStillRefreshSnapshotWithoutOverwritingManualFields() throws Exception {
        JobPost existing = new JobPost();
        existing.setSourceJobId("JOB-SNAPSHOT-ONLY");
        existing.setJobName("人工维护岗位");
        existing.setJobTitle("人工维护标题");
        existing.setJobNature("全职");
        existing.setEducationRequirement("本科");
        existing.setMajorRequirement("软件工程");
        existing.setHeadCount(5);
        existing.setSalaryMin(new java.math.BigDecimal("10000"));
        existing.setSalaryMax(new java.math.BigDecimal("15000"));
        existing.setJobDescription("人工维护详情");
        existing.setJobUrl("https://manual.example.com/job");
        existing.setCompanyName("人工维护公司");
        existing.setCompanyType("民营");
        existing.setCompanySize("100-499人");
        existing.setCompanyAddress("人工维护地址");
        existing.setCompanyWebsite("https://manual.example.com");
        existing.setJobDirection("后端开发");
        existing.setJobRequirements("人工要求");
        existing.setBonusPoints("人工亮点");
        existing.setSuitableGrade("应届");
        existing.setJobLocation("郑州");
        existing.setSourceName("MANUAL");
        jobPostRepository.save(existing);

        MockHttpSession session = buildAdminSession();
        MockMultipartFile excelFile = new MockMultipartFile(
                "excelFile",
                "snapshot-only.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildWorkbookBytes(List.of(
                        List.of("校招", "JOB-SNAPSHOT-ONLY", "Excel 岗位名不应覆盖", "Excel 标题不应覆盖", "全职", "410100",
                                "本科", "计算机相关", "6", "DEV", "9000", "14000", "这次主要用于刷新原始快照。",
                                "https://snapshot.example.com/job/1", "快照公司", "快照简称", "双休", "91410000TEST099",
                                "IT", "上市", "1000-9999人", "410100", "郑州高新区快照路", "https://snapshot.example.com",
                                "是", "2026-05-01 18:00:00", "2026-03-05 12:00:00")
                ))
        );

        mockMvc.perform(multipart("/jobs/import/preview")
                        .file(excelFile)
                        .session(session)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));

        JobExcelImportPreview preview = (JobExcelImportPreview) session.getAttribute("jobExcelImportPreview");
        assertThat(preview.getCreatableCount()).isEqualTo(0);
        assertThat(preview.getSupplementableCount()).isEqualTo(1);

        mockMvc.perform(post("/jobs/import/confirm")
                        .session(session)
                        .param("previewToken", preview.getToken())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));

        JobPost reloaded = jobPostRepository.findBySourceJobId("JOB-SNAPSHOT-ONLY").orElseThrow();
        assertThat(reloaded.getJobName()).isEqualTo("人工维护岗位");
        assertThat(reloaded.getJobTitle()).isEqualTo("人工维护标题");
        assertThat(reloaded.getCompanyName()).isEqualTo("人工维护公司");

        UpdateLog importLog = updateLogRepository.findTopByUpdateTypeOrderByCreatedAtDescIdDesc("JOB_EXCEL_IMPORT").orElseThrow();
        assertThat(importLog.getAddedCount()).isEqualTo(0);
        assertThat(importLog.getUpdatedCount()).isEqualTo(1);
        assertThat(importLog.getSnapshotCount()).isEqualTo(1);

        JobSourceSnapshot latestSnapshot = jobSourceSnapshotRepository.findTopByJobPostOrderByCreatedAtDescIdDesc(reloaded).orElseThrow();
        assertThat(latestSnapshot.getImportFileName()).isEqualTo("snapshot-only.xlsx");
        assertThat(latestSnapshot.getCompanyNameRaw()).isEqualTo("快照公司");
        assertThat(latestSnapshot.getRecruitTitle()).isEqualTo("Excel 标题不应覆盖");
    }

    @Test
    void salaryNormalizationShouldInferMonthlyAndShortCycleRanges() throws Exception {
        MockHttpSession monthlySession = buildAdminSession();
        MockMultipartFile monthlyExcel = new MockMultipartFile(
                "excelFile",
                "salary-month.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildWorkbookBytes(List.of(
                        List.of("校招", "JOB-SALARY-MONTH", "测试科技有限公司", "Java 开发工程师", "全职", "440100",
                                "本科", "计算机相关", "2", "DEV", "7", "8", "负责后端接口开发", "https://salary.example.com/month",
                                "测试科技有限公司", "测试科技", "双休", "91440000TEST001", "IT", "民营", "100-499人",
                                "440100", "广州市天河区软件园", "https://salary.example.com", "是", "2026-05-01 18:00:00", "2026-03-08 12:00:00")
                ))
        );

        mockMvc.perform(multipart("/jobs/import/preview")
                        .file(monthlyExcel)
                        .session(monthlySession)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));

        JobExcelImportPreview monthlyPreview = (JobExcelImportPreview) monthlySession.getAttribute("jobExcelImportPreview");
        mockMvc.perform(post("/jobs/import/confirm")
                        .session(monthlySession)
                        .param("previewToken", monthlyPreview.getToken())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));

        JobPost monthlyJob = jobPostRepository.findBySourceJobId("JOB-SALARY-MONTH").orElseThrow();
        assertThat(monthlyJob.getJobName()).isEqualTo("Java 开发工程师");
        assertThat(monthlyJob.getSalaryMin()).isEqualByComparingTo("7000");
        assertThat(monthlyJob.getSalaryMax()).isEqualByComparingTo("8000");
        assertThat(monthlyJob.getSalaryUnitType()).isEqualTo("MONTH");
        assertThat(monthlyJob.getSalaryDisplayText()).isEqualTo("7k-8k/月");

        jobSourceSnapshotRepository.deleteAll();
        jobPostRepository.deleteAll();
        updateLogRepository.deleteAll();

        MockHttpSession shortCycleSession = buildAdminSession();
        MockMultipartFile shortCycleExcel = new MockMultipartFile(
                "excelFile",
                "salary-short-cycle.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildWorkbookBytes(List.of(
                        List.of("校招", "JOB-SALARY-SHORT", "短周期岗位", "短周期测试岗位", "兼职", "440100",
                                "本科", "不限", "1", "DEV", "150", "350", "周期类薪资岗位", "https://salary.example.com/short",
                                "短周期公司", "短周期", "", "91440000TEST002", "IT", "民营", "50-99人",
                                "440100", "广州市海珠区测试路", "https://salary.example.com", "是", "2026-05-03 18:00:00", "2026-03-08 12:30:00")
                ))
        );

        mockMvc.perform(multipart("/jobs/import/preview")
                        .file(shortCycleExcel)
                        .session(shortCycleSession)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));

        JobExcelImportPreview shortCyclePreview = (JobExcelImportPreview) shortCycleSession.getAttribute("jobExcelImportPreview");
        mockMvc.perform(post("/jobs/import/confirm")
                        .session(shortCycleSession)
                        .param("previewToken", shortCyclePreview.getToken())
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));

        JobPost shortCycleJob = jobPostRepository.findBySourceJobId("JOB-SALARY-SHORT").orElseThrow();
        assertThat(shortCycleJob.getSalaryMin()).isNull();
        assertThat(shortCycleJob.getSalaryMax()).isNull();
        assertThat(shortCycleJob.getSalaryUnitType()).isEqualTo("UNKNOWN");
        assertThat(shortCycleJob.getSalaryDisplayText()).contains("150-350").contains("短周期薪资");
    }

    private byte[] buildWorkbookBytes(List<List<String>> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("jobs");
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < HEADERS.size(); index++) {
                headerRow.createCell(index).setCellValue(HEADERS.get(index));
            }

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                createRow(sheet, rowIndex + 1, rows.get(rowIndex));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createRow(XSSFSheet sheet, int rowIndex, List<String> values) {
        Row row = sheet.createRow(rowIndex);
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
        }
    }

    private MockHttpSession buildAdminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedIn", true);
        session.setAttribute("role", "ADMIN");
        session.setAttribute("username", "excelAdmin");
        session.setAttribute("displayName", "管理员：excelAdmin");
        return session;
    }
}
