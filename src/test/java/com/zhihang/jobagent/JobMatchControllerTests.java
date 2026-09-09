package com.zhihang.jobagent;

import com.zhihang.jobagent.entity.JobMatchRecord;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.UserAccount;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.JobMatchRecordRepository;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.UserAccountRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobMatchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JobMatchRecordRepository jobMatchRecordRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserProfile savedProfile;
    private JobPost preferredJob;
    private Long demoUserId;

    @BeforeEach
    void setUp() {
        jobMatchRecordRepository.deleteAll();
        jobPostRepository.deleteAll();
        userProfileRepository.deleteAll();

        UserAccount demoUser = userAccountRepository.findByUsername("demo").orElseThrow();
        demoUserId = demoUser.getId();

        UserProfile userProfile = new UserProfile();
        userProfile.setUserAccountId(demoUserId);
        userProfile.setStudentName("张三");
        userProfile.setGrade("大三");
        userProfile.setMajor("软件工程");
        userProfile.setTargetDirection("Java 后端");
        userProfile.setSkills("Java、Spring Boot、MySQL");
        userProfile.setProjectExperience("做过课程管理系统和招聘数据平台");
        userProfile.setExpectedCity("广州");
        userProfile.setCareerGoal("先找后端实习，再冲校招");
        savedProfile = userProfileRepository.save(userProfile);

        preferredJob = new JobPost();
        preferredJob.setJobName("Java 开发实习生");
        preferredJob.setJobDirection("Java 后端");
        preferredJob.setJobRequirements("熟悉 Java、Spring Boot、MySQL，并具备良好的接口设计能力");
        preferredJob.setBonusPoints("有项目经验、了解 Redis 或消息队列更佳");
        preferredJob.setSuitableGrade("大三");
        preferredJob.setCompanyName("示例科技");
        preferredJob.setJobLocation("440100");
        preferredJob.setCompanyAddress("广东省广州市天河区科韵路 1 号");
        preferredJob.setSalaryMin(new BigDecimal("8000"));
        preferredJob.setSalaryMax(new BigDecimal("12000"));
        preferredJob.setEducationRequirement("本科及以上");
        preferredJob.setJobNature("实习");
        preferredJob.setSourceName("SCHOOL_JOB_EXCEL");
        preferredJob.setDisplaySummary("负责 Java 后端服务开发、接口联调、质量优化与性能排查。");
        preferredJob = jobPostRepository.save(preferredJob);

        JobPost secondJob = new JobPost();
        secondJob.setJobName("测试开发实习生");
        secondJob.setJobDirection("测试");
        secondJob.setJobRequirements("了解 Java、接口测试");
        secondJob.setBonusPoints("有自动化测试经验");
        secondJob.setSuitableGrade("大三");
        secondJob.setCompanyName("手动维护公司");
        secondJob.setJobLocation("深圳");
        secondJob.setSalaryMin(new BigDecimal("5000"));
        secondJob.setSalaryMax(new BigDecimal("7000"));
        secondJob.setEducationRequirement("本科");
        secondJob.setJobNature("全职");
        secondJob.setSourceName("MANUAL");
        secondJob.setDisplaySummary("适合测试方向同学的岗位。");
        jobPostRepository.save(secondJob);
    }

    @Test
    void shouldRenderMatchSelectPage() throws Exception {
        mockMvc.perform(get("/match").with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Job Matching")))
                .andExpect(content().string(containsString("开始匹配")));
    }

    @Test
    void shouldRenderFilterableMatchResultPageWithCollapsedCards() throws Exception {
        mockMvc.perform(get("/match/{profileId}", savedProfile.getId()).with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("顶部筛选栏")))
                .andExpect(content().string(containsString("全部城市")))
                .andExpect(content().string(containsString("清空筛选")))
                .andExpect(content().string(containsString("推荐解释面板")))
                .andExpect(content().string(containsString("岗位摘要")))
                .andExpect(content().string(containsString("示例科技")))
                .andExpect(content().string(containsString("手动维护")))
                .andExpect(content().string(containsString("广东省广州市天河区")))
                .andExpect(content().string(not(containsString("440100"))))
                .andExpect(content().string(containsString("生成成长路径")))
                .andExpect(content().string(containsString("Java 开发实习生")));
    }

    @Test
    void detailPageShouldReuseReadableLocationFormatting() throws Exception {
        mockMvc.perform(get("/match/{profileId}", savedProfile.getId()).with(user("demo").roles("USER")))
                .andExpect(status().isOk());

        JobMatchRecord record = jobMatchRecordRepository
                .findTopByUserProfileIdAndJobPostIdOrderByCreatedAtDescIdDesc(savedProfile.getId(), preferredJob.getId())
                .orElseThrow();

        mockMvc.perform(get("/match/record/{id}", record.getId()).with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("岗位匹配详情")))
                .andExpect(content().string(containsString("广东省广州市天河区")))
                .andExpect(content().string(not(containsString("440100"))))
                .andExpect(content().string(containsString("生成补强方案")));
    }
}
