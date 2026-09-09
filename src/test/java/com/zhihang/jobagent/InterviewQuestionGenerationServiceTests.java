package com.zhihang.jobagent;

import com.zhihang.jobagent.dto.AiGenerationMetadata;
import com.zhihang.jobagent.dto.AiTextGenerationResult;
import com.zhihang.jobagent.dto.InterviewQuestionSelectionResult;
import com.zhihang.jobagent.entity.InterviewQuestion;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.InterviewQuestionRepository;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.AiOrchestratorService;
import com.zhihang.jobagent.service.ContentNormalizationService;
import com.zhihang.jobagent.service.InterviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@SpringBootTest
class InterviewQuestionGenerationServiceTests {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private InterviewQuestionRepository interviewQuestionRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private ContentNormalizationService contentNormalizationService;

    @MockBean
    private AiOrchestratorService aiOrchestratorService;

    @BeforeEach
    void setUp() {
        interviewQuestionRepository.deleteAll();
        jobPostRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void shouldGenerateAndPersistAiQuestionsWhenRelevantBankIsTooSmall() {
        UserProfile profile = saveProfile();
        JobPost jobPost = saveBackendJob();
        saveLocalQuestion("Java后端", "基础知识", "什么是 Spring Boot 自动配置？");
        saveLocalQuestion("Java后端", "项目经历", "请介绍一个你做过的后端项目。");

        String aiJson = """
                {
                  "roleCategory": "Java后端",
                  "questions": [
                    {
                      "question": "请结合一个实际项目说明你如何设计后端接口并处理参数校验",
                      "category": "项目经历",
                      "difficulty": "中等",
                      "expectedPoints": ["接口设计思路", "参数校验与异常处理", "项目结果"],
                      "answerOutline": ["先交代项目背景", "说明接口设计和校验方案", "补充上线效果和问题复盘"],
                      "scoringRubric": ["是否结合真实项目", "是否说明技术取舍", "是否讲清最终结果"],
                      "tags": ["Java", "Spring Boot", "REST API"]
                    },
                    {
                      "question": "如果线上接口响应突然变慢，你会怎样定位和处理",
                      "category": "场景题",
                      "difficulty": "进阶",
                      "expectedPoints": ["监控排查路径", "日志与SQL定位", "优化与复盘"],
                      "answerOutline": ["先确认影响范围", "结合日志和监控定位瓶颈", "给出处理动作和复盘方案"],
                      "scoringRubric": ["是否有排查顺序", "是否说明监控指标", "是否有最终优化结果"],
                      "tags": ["性能优化", "SQL", "监控"]
                    },
                    {
                      "question": "请用一个例子说明你在项目里如何使用 Redis 提升接口效率",
                      "category": "基础知识",
                      "difficulty": "中等",
                      "expectedPoints": ["使用场景", "缓存策略", "效果说明"],
                      "answerOutline": ["先说明项目中的瓶颈", "解释 Redis 的使用方式", "补充优化前后差异"],
                      "scoringRubric": ["是否有真实场景", "是否说明缓存策略", "是否说明结果"],
                      "tags": ["Redis", "缓存", "Java"]
                    },
                    {
                      "question": "在追问项目细节时，面试官可能会继续问你哪些接口稳定性问题",
                      "category": "追问题",
                      "difficulty": "中等",
                      "expectedPoints": ["幂等性", "限流降级", "异常处理"],
                      "answerOutline": ["先从常见稳定性问题切入", "结合项目里的处理方式说明", "补充改进空间"],
                      "scoringRubric": ["是否贴近岗位场景", "是否说明稳定性方案", "是否有改进意识"],
                      "tags": ["接口稳定性", "限流", "幂等性"]
                    }
                  ]
                }
                """;
        given(aiOrchestratorService.generateJson(anyString(), eq("interview-question-generate"), anyFloat()))
                .willReturn(new AiTextGenerationResult(aiJson, AiGenerationMetadata.gemini("gemini-test")));

        InterviewQuestionSelectionResult result = interviewService.selectQuestions(profile, jobPost);

        assertThat(result.getGeneratedQuestionCount()).isGreaterThan(0);
        assertThat(result.getQuestions()).hasSize(4);
        assertThat(result.getNoticeMessage()).contains("自动补充");
        assertThat(interviewQuestionRepository.findAll())
                .anySatisfy(question -> {
                    assertThat(question.getSourceType()).isEqualTo("AI_GENERATED");
                    assertThat(question.getProviderName()).isEqualTo("GEMINI");
                    assertThat(question.getRoleCategory()).isEqualTo("Java后端");
                });
    }

    @Test
    void shouldFallbackToLocalCoreQuestionsWhenAiGenerationFails() {
        UserProfile profile = saveProfile();
        JobPost jobPost = saveBackendJob();
        saveLocalQuestion("Java后端", "基础知识", "什么是 Spring Boot 自动配置？");
        saveLocalQuestion("Java后端", "项目经历", "请介绍一个你做过的后端项目。");

        given(aiOrchestratorService.generateJson(anyString(), eq("interview-question-generate"), anyFloat()))
                .willReturn(new AiTextGenerationResult("", AiGenerationMetadata.fallback("local-rule", "ai failed")));

        InterviewQuestionSelectionResult result = interviewService.selectQuestions(profile, jobPost);

        assertThat(result.getQuestions()).isNotEmpty();
        assertThat(result.getGeneratedQuestionCount()).isZero();
        assertThat(result.getNoticeMessage()).contains("优先使用当前题库中的核心题目");
    }

    @Test
    void shouldPreferSelectedJobDirectionOverProfileTargetDirection() {
        UserProfile profile = saveProfile();
        JobPost productJob = saveProductJob();
        given(aiOrchestratorService.generateJson(anyString(), eq("interview-question-generate"), anyFloat()))
                .willReturn(new AiTextGenerationResult("", AiGenerationMetadata.fallback("local-rule", "ai disabled")));

        saveLocalQuestion("Java后端", "基础知识", "什么是 Spring Boot 自动配置？");
        saveLocalQuestion("Java后端", "场景题", "如果 SQL 查询变慢你会怎么排查？");
        saveLocalQuestion("Java后端", "项目经历", "请介绍一个你做过的后端项目。");
        saveLocalQuestion("Java后端", "追问题", "如何让接口具备幂等和限流能力？");

        saveLocalQuestion("产品经理", "自我介绍", "请结合一次需求分析经历做一个自我介绍。");
        saveLocalQuestion("产品经理", "基础知识", "你怎么判断一个新需求是否值得做？");
        saveLocalQuestion("产品经理", "项目经历", "请结合一次产品迭代说明你如何做需求拆解。");
        saveLocalQuestion("产品经理", "场景题", "如果业务方和开发对需求方向有分歧，你会怎么推进？");
        saveLocalQuestion("产品经理", "追问题", "如果数据表明用户转化下降，你会优先看哪些信息？");

        InterviewQuestionSelectionResult result = interviewService.selectQuestions(profile, productJob);

        assertThat(result.getQuestions()).hasSize(4);
        assertThat(result.getQuestions())
                .allSatisfy(question -> assertThat(question.getJobDirection()).contains("产品"));
    }

    private UserProfile saveProfile() {
        UserProfile profile = new UserProfile();
        profile.setStudentName("测试同学");
        profile.setGrade("大三");
        profile.setMajor("软件工程");
        profile.setTargetDirection("Java后端");
        profile.setSkills("Java、Spring Boot、MySQL、Redis");
        profile.setProjectExperience("做过选课系统和岗位推荐平台，负责接口设计、SQL 优化和缓存方案。");
        profile.setExpectedCity("广州");
        profile.setCareerGoal("优先投递 Java 后端实习或校招岗位");
        return userProfileRepository.save(profile);
    }

    private JobPost saveBackendJob() {
        JobPost jobPost = new JobPost();
        jobPost.setJobName("Java 后端实习生");
        jobPost.setJobDirection("Java后端");
        jobPost.setJobRequirements("熟悉 Java、Spring Boot、MySQL、Redis，要求有接口设计和项目经历。");
        jobPost.setBonusPoints("了解缓存、性能优化和接口稳定性更好。");
        jobPost.setEducationRequirement("本科及以上");
        jobPost.setJobNature("实习");
        jobPost.setCompanyName("示例科技");
        jobPost.setDisplayTitle("Java 后端实习生");
        jobPost.setDisplaySummary("负责后端接口开发、缓存优化和项目交付。");
        return jobPostRepository.save(jobPost);
    }

    private JobPost saveProductJob() {
        JobPost jobPost = new JobPost();
        jobPost.setJobName("产品经理实习生");
        jobPost.setJobDirection("产品经理");
        jobPost.setJobRequirements("需求分析、PRD、竞品调研、数据分析、跨部门沟通推进");
        jobPost.setBonusPoints("会原型设计和用户调研更好。");
        jobPost.setEducationRequirement("本科及以上");
        jobPost.setJobNature("实习");
        jobPost.setCompanyName("示例产品");
        jobPost.setDisplayTitle("产品经理实习生");
        jobPost.setDisplaySummary("负责需求拆解、PRD 输出、数据分析和跟进迭代。");
        return jobPostRepository.save(jobPost);
    }

    private void saveLocalQuestion(String direction, String category, String questionText) {
        InterviewQuestion question = new InterviewQuestion();
        question.setJobDirection(direction);
        question.setRoleCategory(direction);
        question.setQuestionType(category);
        question.setQuestionText(questionText);
        question.setReferenceAnswer("请结合真实项目回答，并说明关键动作和结果。");
        question.setKeyPoints("项目背景\n关键动作\n结果复盘");
        question.setDisplayDifficulty("中等");
        question.setDisplayTags(direction + " / 项目");
        contentNormalizationService.normalizeInterviewQuestion(question);
        interviewQuestionRepository.save(question);
    }
}
