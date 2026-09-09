package com.zhihang.jobagent;

import com.zhihang.jobagent.entity.InterviewQuestion;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.UserAccount;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.InterviewQuestionRepository;
import com.zhihang.jobagent.repository.InterviewRecordRepository;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.UserAccountRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InterviewControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InterviewQuestionRepository interviewQuestionRepository;

    @Autowired
    private InterviewRecordRepository interviewRecordRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserProfile savedProfile;
    private JobPost savedJobPost;

    @BeforeEach
    void setUp() {
        interviewRecordRepository.deleteAll();
        jobPostRepository.deleteAll();
        userProfileRepository.deleteAll();

        UserAccount demoUser = userAccountRepository.findByUsername("demo").orElseThrow();

        UserProfile userProfile = new UserProfile();
        userProfile.setUserAccountId(demoUser.getId());
        userProfile.setStudentName("王同学");
        userProfile.setGrade("大三");
        userProfile.setMajor("软件工程");
        userProfile.setTargetDirection("后端开发");
        userProfile.setSkills("Java、Spring Boot、MySQL");
        userProfile.setProjectExperience("负责课程管理系统的接口开发和数据库设计");
        userProfile.setExpectedCity("广州");
        userProfile.setCareerGoal("希望找到后端开发实习岗位");
        savedProfile = userProfileRepository.save(userProfile);

        JobPost jobPost = new JobPost();
        jobPost.setJobName("后端开发实习生");
        jobPost.setJobDirection("后端开发");
        jobPost.setJobRequirements("Java、Spring Boot、MySQL、Linux");
        jobPost.setBonusPoints("有完整项目经验");
        jobPost.setSuitableGrade("大三");
        savedJobPost = jobPostRepository.save(jobPost);
    }

    @Test
    void shouldRenderInterviewStartPage() throws Exception {
        mockMvc.perform(get("/interview").with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Interview Simulation")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("开始面试")));
    }

    @Test
    void shouldStartInterviewAndSubmitAnswers() throws Exception {
        mockMvc.perform(post("/interview/start")
                        .with(user("demo").roles("USER"))
                        .with(csrf())
                        .param("userProfileId", savedProfile.getId().toString())
                        .param("jobPostId", savedJobPost.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Interview Session")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("提交回答并查看点评")));

        List<InterviewQuestion> questions = interviewQuestionRepository.findAllByOrderByIdAsc();
        assertFalse(questions.isEmpty());

        String sessionId = "test-session-001";
        mockMvc.perform(post("/interview/submit")
                        .with(user("demo").roles("USER"))
                        .with(csrf())
                        .param("userProfileId", savedProfile.getId().toString())
                        .param("jobPostId", savedJobPost.getId().toString())
                        .param("sessionId", sessionId)
                        .param("questionIds",
                                questions.get(0).getId().toString(),
                                questions.get(1).getId().toString(),
                                questions.get(2).getId().toString())
                        .param("userAnswers",
                                "我是软件工程大三学生，负责过后端项目开发，使用 Java 和 Spring Boot 实现接口，并优化了数据库查询效率。",
                                "在项目中我负责接口设计、数据库建模和问题排查，通过 SQL 优化和日志分析解决性能问题。",
                                "我理解 RESTful API 要围绕资源设计，配合 HTTP 方法、状态码和接口规范，提高前后端协作效率。"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Interview Result")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("总评")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("改进建议")));

        assertEquals(3, interviewRecordRepository.count());

        mockMvc.perform(get("/interview/history").with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Interview History")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(sessionId)));

        mockMvc.perform(get("/interview/history/{sessionId}", sessionId).with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Interview Detail")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("整体点评")));
    }
}
