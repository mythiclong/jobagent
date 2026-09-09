package com.zhihang.jobagent;

import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.UserAccount;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.ResumeReviewRepository;
import com.zhihang.jobagent.repository.UserAccountRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeReviewControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ResumeReviewRepository resumeReviewRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserProfile savedProfile;
    private JobPost savedJobPost;

    @BeforeEach
    void setUp() {
        resumeReviewRepository.deleteAll();
        jobPostRepository.deleteAll();
        userProfileRepository.deleteAll();

        UserAccount demoUser = userAccountRepository.findByUsername("demo").orElseThrow();

        UserProfile userProfile = new UserProfile();
        userProfile.setUserAccountId(demoUser.getId());
        userProfile.setStudentName("李四");
        userProfile.setGrade("大三");
        userProfile.setMajor("计算机科学与技术");
        userProfile.setTargetDirection("Java后端");
        userProfile.setSkills("Java、Spring Boot、MySQL");
        userProfile.setProjectExperience("做过图书管理系统和课程设计项目");
        userProfile.setExpectedCity("深圳");
        userProfile.setCareerGoal("希望先拿到后端实习，再准备校招");
        savedProfile = userProfileRepository.save(userProfile);

        JobPost jobPost = new JobPost();
        jobPost.setJobName("Java后端实习生");
        jobPost.setJobDirection("后端");
        jobPost.setJobRequirements("Java、Spring Boot、MySQL、Linux");
        jobPost.setBonusPoints("有项目经验");
        jobPost.setSuitableGrade("大三");
        savedJobPost = jobPostRepository.save(jobPost);
    }

    @Test
    void shouldRenderResumeFormPage() throws Exception {
        mockMvc.perform(get("/resume-review").with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Resume Review")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("开始诊断")));
    }

    @Test
    void shouldAnalyzeResumeAndShowHistory() throws Exception {
        mockMvc.perform(post("/resume-review/analyze")
                        .with(user("demo").roles("USER"))
                        .with(csrf())
                        .param("userProfileId", savedProfile.getId().toString())
                        .param("jobPostId", savedJobPost.getId().toString())
                        .param("resumeText", "我是大三学生，掌握Java、Spring Boot、MySQL，参与过2个项目开发，负责用户模块与接口联调，服务100+用户。"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Resume Review Result")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("总体评分")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("优化建议")));

        assertEquals(1, resumeReviewRepository.count());
        ResumeReview savedReview = resumeReviewRepository.findAll().get(0);

        mockMvc.perform(get("/resume-review/history").with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Resume Review History")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("查看详情")));

        mockMvc.perform(get("/resume-review/{id}", savedReview.getId()).with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Resume Review Result")));
    }
}
