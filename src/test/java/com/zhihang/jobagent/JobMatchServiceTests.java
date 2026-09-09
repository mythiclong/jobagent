package com.zhihang.jobagent;

import com.zhihang.jobagent.dto.JobMatchResult;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.ResumeEnhanceRecordRepository;
import com.zhihang.jobagent.repository.ResumeReviewRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.JobMatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JobMatchServiceTests {

    @Autowired
    private JobMatchService jobMatchService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private ResumeReviewRepository resumeReviewRepository;

    @Autowired
    private ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;

    @BeforeEach
    void setUp() {
        resumeEnhanceRecordRepository.deleteAll();
        resumeReviewRepository.deleteAll();
        jobPostRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void shouldCapScoreWhenDirectionClearlyMismatched() {
        UserProfile profile = saveProfile("产品经理", "Java, Excel, PRD", "做过校园活动报名系统", "广州");
        JobPost backendJob = saveBackendJob();

        JobMatchResult result = jobMatchService.matchJobs(profile, List.of(backendJob)).get(0);

        assertThat(result.getCompatibilityLevel()).isEqualTo("MISMATCH");
        assertThat(result.getMatchScore()).isLessThanOrEqualTo(58);
        assertThat(result.getDeductionReasonItems()).anySatisfy(item -> assertThat(item).contains("方向"));
    }

    @Test
    void shouldPenalizeTechnicalJobWhenProjectExperienceMissing() {
        UserProfile withProject = saveProfile("Java 后端", "Java, Spring Boot, MySQL", "做过招聘管理系统，负责接口开发和 SQL 优化", "广州");
        UserProfile withoutProject = saveProfile("Java 后端", "Java, Spring Boot, MySQL", "", "广州");
        JobPost backendJob = saveBackendJob();

        JobMatchResult strongResult = jobMatchService.matchJobs(withProject, List.of(backendJob)).get(0);
        JobMatchResult weakResult = jobMatchService.matchJobs(withoutProject, List.of(backendJob)).get(0);

        assertThat(strongResult.getMatchScore()).isGreaterThan(weakResult.getMatchScore());
        assertThat(strongResult.getProjectScore()).isGreaterThan(weakResult.getProjectScore());
        assertThat(weakResult.getCoreWeaknesses()).anySatisfy(item -> assertThat(item).contains("项目"));
    }

    @Test
    void shouldUseResumeSignalsToDifferentiateSimilarProfiles() {
        UserProfile strongerProfile = saveProfile("Java 后端", "Java, Spring Boot, MySQL", "做过课程管理系统", "广州");
        UserProfile weakerProfile = saveProfile("Java 后端", "Java, Spring Boot, MySQL", "做过课程管理系统", "广州");
        JobPost backendJob = saveBackendJob();

        saveResumeSignals(strongerProfile.getId(), backendJob.getId(),
                "负责接口设计、性能优化，接口响应时间降低 35%，支撑 3 个模块上线。",
                88);
        saveResumeSignals(weakerProfile.getId(), backendJob.getId(),
                "参与项目开发，协助完成页面和接口联调。",
                55);

        JobMatchResult strongResult = jobMatchService.matchJobs(strongerProfile, List.of(backendJob)).get(0);
        JobMatchResult weakResult = jobMatchService.matchJobs(weakerProfile, List.of(backendJob)).get(0);

        assertThat(strongResult.getResumeMaturityScore()).isGreaterThan(weakResult.getResumeMaturityScore());
        assertThat(strongResult.getMatchScore()).isGreaterThan(weakResult.getMatchScore());
        assertThat(strongResult.getPositiveSignals()).anySatisfy(item -> assertThat(item).contains("简历"));
    }

    @Test
    void shouldDifferentiateOperationsDirectionFromProductAndBackendJobs() {
        UserProfile operationsProfile = saveProfile("用户增长运营", "Excel, 数据分析, 竞品分析, 沟通协作", "做过校园活动运营复盘和增长分析", "广州");
        JobPost operationsJob = saveOperationsJob();
        JobPost productJob = saveProductJob();
        JobPost backendJob = saveBackendJob();

        List<JobMatchResult> results = jobMatchService.matchJobs(operationsProfile, List.of(operationsJob, productJob, backendJob));

        assertThat(results.get(0).getJobPost().getJobName()).isEqualTo(operationsJob.getJobName());
        JobMatchResult productResult = results.stream()
                .filter(result -> result.getJobPost().getId().equals(productJob.getId()))
                .findFirst()
                .orElseThrow();
        JobMatchResult backendResult = results.stream()
                .filter(result -> result.getJobPost().getId().equals(backendJob.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(results.get(0).getCompatibilityLevel()).isIn("STRONG", "WEAK");
        assertThat(productResult.getCompatibilityLevel()).isEqualTo("WEAK");
        assertThat(backendResult.getCompatibilityLevel()).isEqualTo("MISMATCH");
        assertThat(results.get(0).getMatchScore()).isGreaterThan(productResult.getMatchScore());
        assertThat(productResult.getMatchScore()).isGreaterThan(backendResult.getMatchScore());
    }

    private UserProfile saveProfile(String targetDirection, String skills, String projectExperience, String city) {
        UserProfile profile = new UserProfile();
        profile.setStudentName("测试同学");
        profile.setGrade("大三");
        profile.setMajor("软件工程");
        profile.setTargetDirection(targetDirection);
        profile.setSkills(skills);
        profile.setProjectExperience(projectExperience);
        profile.setExpectedCity(city);
        profile.setCareerGoal("希望找到与 " + targetDirection + " 相关的实习或校招岗位");
        return userProfileRepository.save(profile);
    }

    private JobPost saveBackendJob() {
        JobPost jobPost = new JobPost();
        jobPost.setJobName("Java 后端实习生");
        jobPost.setJobDirection("Java 后端");
        jobPost.setJobRequirements("熟悉 Java、Spring Boot、MySQL，了解 Redis 和接口设计，要求有项目经历。");
        jobPost.setBonusPoints("有项目落地或实习经历优先。");
        jobPost.setCompanyName("示例科技");
        jobPost.setJobLocation("广州");
        jobPost.setEducationRequirement("本科及以上");
        jobPost.setJobNature("实习");
        jobPost.setDisplaySummary("负责后端服务开发、接口联调、性能优化和项目交付。");
        jobPost.setSalaryMin(new BigDecimal("7000"));
        jobPost.setSalaryMax(new BigDecimal("9000"));
        jobPost.setSalaryUnitType("MONTH");
        return jobPostRepository.save(jobPost);
    }

    private JobPost saveOperationsJob() {
        JobPost jobPost = new JobPost();
        jobPost.setJobName("用户增长运营实习生");
        jobPost.setJobDirection("用户增长运营");
        jobPost.setJobRequirements("需要做活动运营复盘、基础数据分析、竞品跟踪和跨团队协作。");
        jobPost.setBonusPoints("有校园活动策划或增长项目经验优先。");
        jobPost.setCompanyName("示例增长团队");
        jobPost.setJobLocation("广州");
        jobPost.setEducationRequirement("本科及以上");
        jobPost.setJobNature("实习");
        jobPost.setDisplaySummary("负责增长活动执行、复盘和基础数据分析。");
        return jobPostRepository.save(jobPost);
    }

    private JobPost saveProductJob() {
        JobPost jobPost = new JobPost();
        jobPost.setJobName("产品经理实习生");
        jobPost.setJobDirection("产品经理");
        jobPost.setJobRequirements("需要做需求分析、PRD 撰写、原型设计和跨团队沟通。");
        jobPost.setBonusPoints("有完整 PRD 和原型案例优先。");
        jobPost.setCompanyName("示例产品团队");
        jobPost.setJobLocation("广州");
        jobPost.setEducationRequirement("本科及以上");
        jobPost.setJobNature("实习");
        jobPost.setDisplaySummary("负责需求拆解、原型方案和产品迭代跟进。");
        return jobPostRepository.save(jobPost);
    }

    private void saveResumeSignals(Long userProfileId, Long jobPostId, String projectText, int reviewScore) {
        ResumeEnhanceRecord enhanceRecord = new ResumeEnhanceRecord();
        enhanceRecord.setUserProfileId(userProfileId);
        enhanceRecord.setJobPostId(jobPostId);
        enhanceRecord.setParsedProjects(projectText);
        enhanceRecord.setParsedExperience(projectText);
        enhanceRecord.setParsedSkills("Java, Spring Boot, MySQL");
        enhanceRecord.setReviewScore(reviewScore);
        resumeEnhanceRecordRepository.save(enhanceRecord);

        ResumeReview review = new ResumeReview();
        review.setUserProfileId(userProfileId);
        review.setJobPostId(jobPostId);
        review.setResumeText(projectText);
        review.setReviewScore(reviewScore);
        review.setSummary(projectText);
        review.setProblems(reviewScore >= 80 ? "暂无明显问题" : "缺少量化成果，项目描述偏泛");
        resumeReviewRepository.save(review);
    }
}
