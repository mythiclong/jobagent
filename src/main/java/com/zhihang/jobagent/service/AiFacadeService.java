package com.zhihang.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihang.jobagent.dto.AiFailureLayer;
import com.zhihang.jobagent.dto.AiFailureType;
import com.zhihang.jobagent.dto.AiGenerationMetadata;
import com.zhihang.jobagent.dto.AiTextGenerationResult;
import com.zhihang.jobagent.dto.GenerationSource;
import com.zhihang.jobagent.dto.JobMatchAiEnhancementResult;
import com.zhihang.jobagent.dto.JobMatchAiInsight;
import com.zhihang.jobagent.dto.JobMatchPreferenceInput;
import com.zhihang.jobagent.dto.JobMatchResult;
import com.zhihang.jobagent.dto.LearningPathCoachAdvice;
import com.zhihang.jobagent.dto.LearningPathCoachGenerationResult;
import com.zhihang.jobagent.dto.ResumeAiGenerationResult;
import com.zhihang.jobagent.dto.ResumeAiReviewResult;
import com.zhihang.jobagent.dto.ResumeSectionRewriteGenerationResult;
import com.zhihang.jobagent.entity.JobMatchRecord;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AiFacadeService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiOrchestratorService aiOrchestratorService;
    private final AiTextSanitizer aiTextSanitizer;

    public AiFacadeService(AiOrchestratorService aiOrchestratorService, AiTextSanitizer aiTextSanitizer) {
        this.aiOrchestratorService = aiOrchestratorService;
        this.aiTextSanitizer = aiTextSanitizer;
    }

    public boolean isGeminiEnabled() {
        return aiOrchestratorService.isGeminiConfigured();
    }

    public boolean isQwenEnabled() {
        return aiOrchestratorService.isQwenConfigured();
    }

    public String getConfiguredModelName() {
        return aiOrchestratorService.getGeminiModelName();
    }

    public String getQwenConfiguredModelName() {
        return aiOrchestratorService.getQwenModelName();
    }

    public String getApiKeySource() {
        return aiOrchestratorService.getGeminiKeySource();
    }

    public String getQwenApiKeySource() {
        return aiOrchestratorService.getQwenKeySource();
    }

    public int getGeminiTimeoutSeconds() {
        return aiOrchestratorService.getGeminiTimeoutSeconds();
    }

    public int getQwenTimeoutSeconds() {
        return aiOrchestratorService.getQwenTimeoutSeconds();
    }

    public ResumeAiGenerationResult generateReview(String resumeText, JobPost jobPost, UserProfile userProfile) {
        String prompt = """
                你是简历诊断助手。必须只返回一个 JSON 对象，不允许 markdown，不允许额外解释。
                JSON 结构如下：
                {
                  "summary": "一句话总体诊断",
                  "coreProblems": ["问题1","问题2","问题3"],
                  "missingKeywords": ["关键词1","关键词2","关键词3"],
                  "improvementSuggestions": ["建议1","建议2","建议3"]
                }
                约束：
                1. summary 1-3句。
                2. coreProblems 输出2-4条。
                3. missingKeywords 输出3-12条。
                4. improvementSuggestions 输出3-5条短句。
                5. 字段必须保留，缺失时返回空字符串或空数组。
                目标岗位：%s（方向：%s）
                岗位要求：%s
                用户目标方向：%s
                简历正文：
                %s
                """.formatted(
                defaultText(jobPost.getJobName()),
                defaultText(jobPost.getJobDirection()),
                defaultText(jobPost.getJobRequirements()),
                defaultText(userProfile.getTargetDirection()),
                defaultText(resumeText)
        );

        AiTextGenerationResult jsonResult = generateJson(prompt, "resume-review", 0.3f);
        if (jsonResult.getMetadata().isAiUsed()) {
            JsonNode json = parseJsonObject(jsonResult.getText());
            if (json != null) {
                String summary = readText(json, "summary");
                List<String> problems = readArray(json, "coreProblems", 4);
                List<String> missingKeywords = readArray(json, "missingKeywords", 12);
                List<String> suggestions = readArray(json, "improvementSuggestions", 5);

                if (StringUtils.hasText(summary) || !problems.isEmpty() || !missingKeywords.isEmpty() || !suggestions.isEmpty()) {
                    ResumeAiReviewResult review = new ResumeAiReviewResult(
                            defaultIfEmpty(summary, "简历已完成基础诊断，建议继续强化岗位关键词与成果表达。"),
                            toJsonArrayString(problems),
                            toJsonArrayString(missingKeywords),
                            toJsonArrayString(suggestions)
                    );
                    return new ResumeAiGenerationResult(review, jsonResult.getMetadata());
                }
            }
        }

        String invalidReason = jsonResult.getMetadata().isAiUsed()
                ? "AI returned invalid review JSON, switched to local rules."
                : defaultIfEmpty(jsonResult.getMetadata().getFallbackReason(), "AI call failed.");
        AiGenerationMetadata metadata = toFallbackMetadata(jsonResult.getMetadata(), invalidReason);
        ResumeAiReviewResult fallbackReview = buildFallbackReview(resumeText, jobPost, userProfile);
        return new ResumeAiGenerationResult(fallbackReview, metadata);
    }

    public ResumeSectionRewriteGenerationResult rewriteSectionStructured(String sectionName,
                                                                        String sectionText,
                                                                        JobPost jobPost,
                                                                        UserProfile userProfile) {
        if (!StringUtils.hasText(sectionText)) {
            return new ResumeSectionRewriteGenerationResult(
                    "",
                    "输入为空，无法重写该模块。",
                    AiGenerationMetadata.fallback("local-rule", "Section text is empty.")
            );
        }

        String prompt = """
                你是简历改写助手。必须只返回一个 JSON 对象，不允许 markdown，不允许额外解释。
                JSON 结构如下：
                {
                  "rewritten": "重写后的内容",
                  "reason": "改写理由"
                }
                约束：
                1. rewritten 必须可直接粘贴到简历里。
                2. reason 用1-2句说明改写重点。
                3. 字段必须保留，缺失时返回空字符串。
                模块名称：%s
                目标岗位：%s（方向：%s）
                岗位要求：%s
                用户目标方向：%s
                模块原文：
                %s
                """.formatted(
                defaultText(sectionName),
                defaultText(jobPost.getJobName()),
                defaultText(jobPost.getJobDirection()),
                defaultText(jobPost.getJobRequirements()),
                defaultText(userProfile.getTargetDirection()),
                defaultText(sectionText)
        );

        AiTextGenerationResult jsonResult = generateJson(prompt, "resume-rewrite-section", 0.35f);
        if (jsonResult.getMetadata().isAiUsed()) {
            JsonNode json = parseJsonObject(jsonResult.getText());
            if (json != null) {
                String rewritten = readText(json, "rewritten");
                String reason = readText(json, "reason");
                if (StringUtils.hasText(rewritten)) {
                    return new ResumeSectionRewriteGenerationResult(
                            aiTextSanitizer.sanitizeResumeText(rewritten),
                            defaultIfEmpty(aiTextSanitizer.sanitizePlainText(reason), "已强化岗位关键词覆盖与结果表达。"),
                            jsonResult.getMetadata()
                    );
                }
            }
        }

        String fallbackReason = jsonResult.getMetadata().isAiUsed()
                ? "AI returned invalid section JSON, switched to local rules."
                : defaultIfEmpty(jsonResult.getMetadata().getFallbackReason(), "AI rewrite failed.");
        String fallbackText = fallbackRewriteSection(sectionName, sectionText, jobPost, userProfile);
        return new ResumeSectionRewriteGenerationResult(
                fallbackText,
                "当前模块已按本地规则生成。"
                        + (StringUtils.hasText(fallbackReason) ? " 原因：" + fallbackReason : ""),
                toFallbackMetadata(jsonResult.getMetadata(), fallbackReason)
        );
    }

    public AiTextGenerationResult rewriteSection(String sectionName, String sectionText, JobPost jobPost, UserProfile userProfile) {
        ResumeSectionRewriteGenerationResult structured = rewriteSectionStructured(sectionName, sectionText, jobPost, userProfile);
        return new AiTextGenerationResult(structured.getRewritten(), structured.getMetadata());
    }

    public AiTextGenerationResult rewriteFull(String originalText,
                                              Map<String, String> optimizedSections,
                                              JobPost jobPost,
                                              UserProfile userProfile) {
        String merged = mergeSections(optimizedSections);
        if (!StringUtils.hasText(merged)) {
            merged = defaultText(originalText);
        }

        String prompt = """
                你是简历整合助手。必须只返回一个 JSON 对象，不允许 markdown，不允许额外解释。
                JSON 结构如下：
                {
                  "fullOptimizedResume": "整份优化版简历全文"
                }
                约束：
                1. fullOptimizedResume 必须是可直接投递的中文简历文本。
                2. 保持结构清晰：基本信息、教育经历、技能描述、项目经历、实习/校园经历。
                3. 尽量融入目标岗位关键词和量化结果。
                4. 字段必须保留，失败时可返回空字符串。
                目标岗位：%s（方向：%s）
                岗位要求：%s
                用户目标方向：%s
                输入内容：
                %s
                """.formatted(
                defaultText(jobPost.getJobName()),
                defaultText(jobPost.getJobDirection()),
                defaultText(jobPost.getJobRequirements()),
                defaultText(userProfile.getTargetDirection()),
                merged
        );

        AiTextGenerationResult jsonResult = generateJson(prompt, "resume-rewrite-full", 0.35f);
        if (jsonResult.getMetadata().isAiUsed()) {
            JsonNode json = parseJsonObject(jsonResult.getText());
            if (json != null) {
                String fullText = readText(json, "fullOptimizedResume");
                if (StringUtils.hasText(fullText)) {
                    return new AiTextGenerationResult(aiTextSanitizer.sanitizeResumeText(fullText), jsonResult.getMetadata());
                }
            }
        }

        String fallbackReason = jsonResult.getMetadata().isAiUsed()
                ? "AI returned invalid full-resume JSON, switched to local rules."
                : defaultIfEmpty(jsonResult.getMetadata().getFallbackReason(), "AI full rewrite failed.");
        return new AiTextGenerationResult(
                fallbackRewriteFull(merged, jobPost, userProfile),
                toFallbackMetadata(jsonResult.getMetadata(), fallbackReason)
        );
    }

    public JobMatchAiEnhancementResult enhanceMatchCandidates(UserProfile userProfile,
                                                              JobMatchPreferenceInput preferenceInput,
                                                              ResumeEnhanceRecord latestEnhance,
                                                              ResumeReview latestReview,
                                                              List<JobMatchResult> candidateResults) {
        if (candidateResults == null || candidateResults.isEmpty()) {
            return new JobMatchAiEnhancementResult(
                    Map.of(),
                    "",
                    AiGenerationMetadata.fallback("local-rule", "No rule candidates available for AI enhancement.")
            );
        }

        String prompt = buildJobMatchEnhancementPrompt(
                userProfile,
                preferenceInput,
                latestEnhance,
                latestReview,
                candidateResults
        );
        AiTextGenerationResult jsonResult = generateJson(prompt, "job-match-enhancement", 0.25f);
        if (jsonResult.getMetadata().isAiUsed()) {
            JsonNode json = parseJsonObject(jsonResult.getText());
            if (json != null) {
                String summary = readText(json, "summary");
                Map<Long, JobMatchAiInsight> insights = readJobMatchInsights(json.get("candidates"));
                if (!insights.isEmpty() || StringUtils.hasText(summary)) {
                    return new JobMatchAiEnhancementResult(
                            insights,
                            defaultIfEmpty(summary, "已完成候选岗位的偏好增强分析。"),
                            jsonResult.getMetadata()
                    );
                }
            }
        }

        String fallbackReason = jsonResult.getMetadata().isAiUsed()
                ? "AI returned invalid job-match JSON, switched to rule-only ranking."
                : defaultIfEmpty(jsonResult.getMetadata().getFallbackReason(), "AI match enhancement failed.");
        return new JobMatchAiEnhancementResult(
                Map.of(),
                "AI 增强暂不可用，当前先按规则匹配结果展示。",
                toFallbackMetadata(jsonResult.getMetadata(), fallbackReason)
        );
    }

    public LearningPathCoachGenerationResult generateLearningPathCoach(UserProfile userProfile,
                                                                       JobPost jobPost,
                                                                       JobMatchRecord latestMatchRecord,
                                                                       ResumeReview latestResumeReview,
                                                                       ResumeEnhanceRecord latestEnhanceRecord,
                                                                       JobMatchPreferenceInput preferenceInput,
                                                                       String ruleWeaknessSummary,
                                                                       String ruleLearningSteps,
                                                                       String ruleRecommendedProjects,
                                                                       String ruleSuggestedOrder,
                                                                       String ruleEstimatedDuration,
                                                                       String ruleDeliveryReadiness) {
        String prompt = buildLearningPathCoachPrompt(
                userProfile,
                jobPost,
                latestMatchRecord,
                latestResumeReview,
                latestEnhanceRecord,
                preferenceInput,
                ruleWeaknessSummary,
                ruleLearningSteps,
                ruleRecommendedProjects,
                ruleSuggestedOrder,
                ruleEstimatedDuration,
                ruleDeliveryReadiness
        );
        AiTextGenerationResult jsonResult = generateJson(prompt, "learning-path-coach", 0.3f);
        if (jsonResult.getMetadata().isAiUsed()) {
            JsonNode json = parseJsonObject(jsonResult.getText());
            if (json != null) {
                LearningPathCoachAdvice advice = readLearningPathCoachAdvice(json);
                if (advice != null && hasCoachAdviceContent(advice)) {
                    return new LearningPathCoachGenerationResult(advice, jsonResult.getMetadata());
                }
            }
        }

        String fallbackReason = jsonResult.getMetadata().isAiUsed()
                ? "AI returned invalid strength-plan JSON, switched to rule plan."
                : defaultIfEmpty(jsonResult.getMetadata().getFallbackReason(), "AI strength plan enhancement failed.");
        return new LearningPathCoachGenerationResult(
                null,
                toFallbackMetadata(jsonResult.getMetadata(), fallbackReason)
        );
    }

    public AiTextGenerationResult testConnectivity(String inputText) {
        String text = defaultIfEmpty(inputText, "请返回：AI service is reachable.");
        String prompt = "请用一句中文总结以下内容，并保持在20字以内：\n" + text;

        AiTextGenerationResult result = generateText(prompt, "admin-ai-test", 0.2f);
        if (result.getMetadata().isAiUsed() && StringUtils.hasText(result.getText())) {
            return result;
        }

        String fallbackReason = defaultIfEmpty(result.getMetadata().getFallbackReason(), "AI test response is empty.");
        AiGenerationMetadata fallbackMetadata = AiGenerationMetadata.fallback(
                "local-rule",
                fallbackReason,
                result.getMetadata().getPrimaryFailureReason(),
                result.getMetadata().getSecondaryFailureReason(),
                result.getMetadata().isTimeoutOccurred(),
                result.getMetadata().getPrimaryFailureLayer(),
                result.getMetadata().getPrimaryFailureType(),
                result.getMetadata().getSecondaryFailureLayer(),
                result.getMetadata().getSecondaryFailureType()
        );
        return new AiTextGenerationResult(
                "规则回退：AI服务当前不可用，请检查 GEMINI_API_KEY / DASHSCOPE_API_KEY 和网络后重试。",
                fallbackMetadata
        );
    }

    public AiTextGenerationResult testConnectivityMinimalPlainText(String inputText) {
        String plainInput = defaultIfEmpty(inputText, "Please reply with: AI_TEST_OK");
        AiTextGenerationResult result = generateText(plainInput, "admin-ai-test", 0.0f);
        if (result.getMetadata().isAiUsed() && StringUtils.hasText(result.getText())) {
            return result;
        }

        String fallbackReason = defaultIfEmpty(result.getMetadata().getFallbackReason(), "AI test response is empty.");
        AiGenerationMetadata fallbackMetadata = AiGenerationMetadata.fallback(
                "local-rule",
                fallbackReason,
                result.getMetadata().getPrimaryFailureReason(),
                result.getMetadata().getSecondaryFailureReason(),
                result.getMetadata().isTimeoutOccurred(),
                result.getMetadata().getPrimaryFailureLayer(),
                result.getMetadata().getPrimaryFailureType(),
                result.getMetadata().getSecondaryFailureLayer(),
                result.getMetadata().getSecondaryFailureType()
        );
        return new AiTextGenerationResult(
                "Rule fallback: AI service is unavailable. Please check GEMINI_API_KEY / DASHSCOPE_API_KEY and network.",
                fallbackMetadata
        );
    }

    private AiTextGenerationResult generateJson(String prompt, String action, float temperature) {
        return aiOrchestratorService.generateJson(prompt, action, temperature);
    }

    private AiTextGenerationResult generateText(String prompt, String action, float temperature) {
        return aiOrchestratorService.generateText(prompt, action, temperature);
    }

    private AiGenerationMetadata toFallbackMetadata(AiGenerationMetadata metadata, String fallbackReason) {
        String primaryReason = metadata.getPrimaryFailureReason();
        String secondaryReason = metadata.getSecondaryFailureReason();
        AiFailureLayer primaryLayer = metadata.getPrimaryFailureLayer();
        AiFailureType primaryType = metadata.getPrimaryFailureType();
        AiFailureLayer secondaryLayer = metadata.getSecondaryFailureLayer();
        AiFailureType secondaryType = metadata.getSecondaryFailureType();

        if (metadata.isAiUsed()) {
            if (metadata.getGenerationSource() == GenerationSource.GEMINI) {
                primaryReason = defaultIfEmpty(primaryReason, "Gemini response is not usable.");
                primaryLayer = AiFailureLayer.MODEL;
                primaryType = AiFailureType.RESPONSE_PARSE_FAILED;
            } else if (metadata.getGenerationSource() == GenerationSource.QWEN) {
                secondaryReason = defaultIfEmpty(secondaryReason, "Qwen response is not usable.");
                secondaryLayer = AiFailureLayer.MODEL;
                secondaryType = AiFailureType.RESPONSE_PARSE_FAILED;
            }
        } else {
            primaryReason = defaultIfEmpty(primaryReason, metadata.getFallbackReason());
            secondaryReason = defaultIfEmpty(secondaryReason, "");
            if (primaryLayer == AiFailureLayer.NONE) {
                primaryLayer = AiFailureLayer.UNKNOWN;
            }
            if (primaryType == AiFailureType.NONE) {
                primaryType = AiFailureType.UNKNOWN_EXCEPTION;
            }
        }

        return AiGenerationMetadata.fallback(
                "local-rule",
                fallbackReason,
                defaultText(primaryReason),
                defaultText(secondaryReason),
                metadata.isTimeoutOccurred(),
                primaryLayer,
                primaryType,
                secondaryLayer,
                secondaryType
        );
    }

    private String buildJobMatchEnhancementPrompt(UserProfile userProfile,
                                                  JobMatchPreferenceInput preferenceInput,
                                                  ResumeEnhanceRecord latestEnhance,
                                                  ResumeReview latestReview,
                                                  List<JobMatchResult> candidateResults) {
        StringBuilder candidatesBuilder = new StringBuilder();
        int index = 1;
        for (JobMatchResult result : candidateResults) {
            JobPost jobPost = result.getJobPost();
            if (jobPost == null || jobPost.getId() == null) {
                continue;
            }
            if (candidatesBuilder.length() > 0) {
                candidatesBuilder.append('\n').append('\n');
            }
            candidatesBuilder.append(index++).append(". ")
                    .append("jobPostId=").append(jobPost.getId()).append('\n')
                    .append("岗位=").append(defaultIfEmpty(jobPost.getDisplayTitle(), jobPost.getJobName())).append('\n')
                    .append("公司=").append(defaultIfEmpty(jobPost.getCompanyNameDisplay(), "-")).append('\n')
                    .append("方向=").append(defaultIfEmpty(jobPost.getJobDirection(), "-")).append('\n')
                    .append("城市=").append(defaultIfEmpty(jobPost.getReadableLocationDisplay(), "-")).append('\n')
                    .append("岗位性质=").append(defaultIfEmpty(jobPost.getJobNatureDisplay(), "-")).append('\n')
                    .append("薪资=").append(defaultIfEmpty(jobPost.getSalaryRangeDisplay(), "-")).append('\n')
                    .append("规则匹配分=").append(result.getMatchScore() == null ? "-" : result.getMatchScore()).append('\n')
                    .append("硬条件结论=").append(defaultIfEmpty(result.getCompatibilityLabel(), "-")).append('\n')
                    .append("规则推荐理由=").append(joinForPrompt(result.getPositiveSignals(), 4)).append('\n')
                    .append("规则短板=").append(joinForPrompt(result.getCoreWeaknesses(), 4)).append('\n')
                    .append("岗位要求=").append(joinForPrompt(jobPost.getRequirementItemList(), 5)).append('\n')
                    .append("岗位标签=").append(joinForPrompt(jobPost.getDisplayTagList(), 5));
        }

        return """
                你是岗位匹配增强助手。你只能基于“规则匹配已经筛出的候选岗位”做二次判断，不能扫描候选池外的岗位。
                你的任务是：综合规则匹配分、岗位语义、用户偏好、简历最近状态，对候选岗位做重排序建议并给出简洁说明。
                必须只返回一个 JSON 对象，不要 markdown，不要额外解释。
                JSON 结构如下：
                {
                  "summary": "一句话总结本轮增强判断",
                  "candidates": [
                    {
                      "jobPostId": 1,
                      "rerankScore": 0,
                      "preferenceFitLevel": "HIGH|MEDIUM|LOW",
                      "aiMatchReason": "一句简洁说明",
                      "aiRiskPoints": ["风险1", "风险2"],
                      "recommendedPriority": "HIGH|MEDIUM|LOW",
                      "suggestedNextAction": "下一步动作",
                      "shortTagSummary": ["标签1", "标签2", "标签3"]
                    }
                  ]
                }
                约束：
                1. 只能评估下面给出的候选岗位，不能虚构新的岗位。
                2. rerankScore 取 0-100，且要尊重规则匹配分；规则明显不适合的岗位不能被抬成高优先级。
                3. preferenceFitLevel 只输出 HIGH / MEDIUM / LOW。
                4. recommendedPriority 只输出 HIGH / MEDIUM / LOW。
                5. aiMatchReason 保持 1-2 句中文短句。
                6. aiRiskPoints 输出 1-3 条。
                7. shortTagSummary 输出 2-4 个简短标签。
                8. 如果城市、薪资、岗位性质更接近用户偏好，要明确体现；如果项目或技能短板明显，也要明确指出。

                用户档案摘要：
                姓名：%s
                年级/专业：%s / %s
                目标方向：%s
                目标岗位：%s
                技能：%s
                项目经历：%s
                职业目标：%s

                当前偏好：
                %s

                最近一次简历优化摘要：
                技能提取：%s
                项目提取：%s
                最近一次简历诊断摘要：%s

                候选岗位列表：
                %s
                """.formatted(
                defaultIfEmpty(userProfile == null ? null : userProfile.getStudentName(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getGrade(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getMajor(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getTargetDirection(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getTargetRole(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getSkills(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getProjectExperience(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getCareerGoal(), "未填写"),
                preferenceInput == null ? "未填写额外偏好" : preferenceInput.toSummaryText(),
                defaultIfEmpty(latestEnhance == null ? null : latestEnhance.getParsedSkills(), "暂无"),
                defaultIfEmpty(latestEnhance == null ? null : latestEnhance.getParsedProjects(), "暂无"),
                defaultIfEmpty(latestReview == null ? null : latestReview.getSummary(), "暂无"),
                defaultIfEmpty(candidatesBuilder.toString(), "无候选岗位")
        );
    }

    private String buildLearningPathCoachPrompt(UserProfile userProfile,
                                                JobPost jobPost,
                                                JobMatchRecord latestMatchRecord,
                                                ResumeReview latestResumeReview,
                                                ResumeEnhanceRecord latestEnhanceRecord,
                                                JobMatchPreferenceInput preferenceInput,
                                                String ruleWeaknessSummary,
                                                String ruleLearningSteps,
                                                String ruleRecommendedProjects,
                                                String ruleSuggestedOrder,
                                                String ruleEstimatedDuration,
                                                String ruleDeliveryReadiness) {
        return """
                你是求职补强教练。你需要把规则总结升级成“更像真人教练”的定制化建议。
                你必须结合：目标岗位、最近一次岗位匹配短板、当前求职档案、最近一次简历问题、用户偏好。
                只能返回一个 JSON 对象，不要 markdown，不要额外解释。
                JSON 结构如下：
                {
                  "coachSummary": "当前短板总结",
                  "impactExplanation": "为什么这些短板会影响投递这个岗位",
                  "priorityFocus": "建议优先补什么",
                  "skillOrProjectFirst": "SKILL_FIRST|PROJECT_FIRST|BALANCED",
                  "estimatedDuration": "建议时长",
                  "deliveryTimingAdvice": "投递时点建议",
                  "nextActionSuggestion": "下一步建议动作",
                  "learningSteps": ["步骤1", "步骤2", "步骤3"],
                  "recommendedProjects": ["项目方向1", "项目方向2"],
                  "suggestedOrder": ["顺序1", "顺序2", "顺序3"],
                  "resourceTypes": ["资源类型1", "资源类型2", "资源类型3"]
                }
                约束：
                1. 所有输出都要贴合当前目标岗位和用户偏好，不能泛泛而谈。
                2. learningSteps 输出 4-6 条，recommendedProjects 输出 2-4 条，suggestedOrder 输出 3-5 条，resourceTypes 输出 3-5 条。
                3. nextActionSuggestion 要明确引导到“去简历优化 / 去模拟面试 / 重新匹配”等具体动作之一。
                4. skillOrProjectFirst 只能输出 SKILL_FIRST / PROJECT_FIRST / BALANCED。
                5. 内容要简洁、可执行、面向求职者，不要写开发者视角的话。

                用户档案：
                姓名：%s
                年级/专业：%s / %s
                目标方向：%s
                目标岗位：%s
                技能：%s
                项目经历：%s
                职业目标：%s

                当前偏好：
                %s

                目标岗位：
                岗位：%s
                方向：%s
                城市：%s
                岗位性质：%s
                薪资：%s
                要求：%s

                最近一次岗位匹配：
                规则分：%s
                适配结论：%s
                规则短板：%s
                扣分原因：%s
                已有下一步建议：%s

                最近一次简历诊断：
                诊断摘要：%s
                问题：%s
                缺失关键词：%s

                最近一次简历优化：
                AI 问题：%s
                缺失关键词：%s
                技能提取：%s
                项目提取：%s

                规则补强底稿：
                当前短板总结：%s
                学习步骤：%s
                推荐项目：%s
                推荐顺序：%s
                预计时长：%s
                当前投递建议：%s
                """.formatted(
                defaultIfEmpty(userProfile == null ? null : userProfile.getStudentName(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getGrade(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getMajor(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getTargetDirection(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getTargetRole(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getSkills(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getProjectExperience(), "未填写"),
                defaultIfEmpty(userProfile == null ? null : userProfile.getCareerGoal(), "未填写"),
                preferenceInput == null ? "未填写额外偏好" : preferenceInput.toSummaryText(),
                defaultIfEmpty(jobPost == null ? null : jobPost.getDisplayTitle(), "未填写"),
                defaultIfEmpty(jobPost == null ? null : jobPost.getJobDirection(), "未填写"),
                defaultIfEmpty(jobPost == null ? null : jobPost.getReadableLocationDisplay(), "未填写"),
                defaultIfEmpty(jobPost == null ? null : jobPost.getJobNatureDisplay(), "未填写"),
                defaultIfEmpty(jobPost == null ? null : jobPost.getSalaryRangeDisplay(), "未填写"),
                joinForPrompt(jobPost == null ? List.of() : jobPost.getRequirementItemList(), 5),
                latestMatchRecord == null || latestMatchRecord.getMatchScore() == null ? "-" : String.valueOf(latestMatchRecord.getMatchScore()),
                defaultIfEmpty(latestMatchRecord == null ? null : latestMatchRecord.getCompatibilityLabel(), "暂无"),
                joinForPrompt(latestMatchRecord == null ? List.of() : latestMatchRecord.getWeaknessItems(), 6),
                joinForPrompt(latestMatchRecord == null ? List.of() : latestMatchRecord.getDeductionReasonItems(), 6),
                joinForPrompt(latestMatchRecord == null ? List.of() : latestMatchRecord.getImprovementItems(), 4),
                defaultIfEmpty(latestResumeReview == null ? null : latestResumeReview.getSummary(), "暂无"),
                defaultIfEmpty(latestResumeReview == null ? null : latestResumeReview.getProblems(), "暂无"),
                defaultIfEmpty(latestResumeReview == null ? null : latestResumeReview.getMissingKeywords(), "暂无"),
                defaultIfEmpty(latestEnhanceRecord == null ? null : latestEnhanceRecord.getAiProblems(), "暂无"),
                defaultIfEmpty(latestEnhanceRecord == null ? null : latestEnhanceRecord.getAiMissingKeywords(), "暂无"),
                defaultIfEmpty(latestEnhanceRecord == null ? null : latestEnhanceRecord.getParsedSkills(), "暂无"),
                defaultIfEmpty(latestEnhanceRecord == null ? null : latestEnhanceRecord.getParsedProjects(), "暂无"),
                defaultIfEmpty(ruleWeaknessSummary, "暂无"),
                defaultIfEmpty(ruleLearningSteps, "暂无"),
                defaultIfEmpty(ruleRecommendedProjects, "暂无"),
                defaultIfEmpty(ruleSuggestedOrder, "暂无"),
                defaultIfEmpty(ruleEstimatedDuration, "暂无"),
                defaultIfEmpty(ruleDeliveryReadiness, "暂无")
        );
    }

    private Map<Long, JobMatchAiInsight> readJobMatchInsights(JsonNode candidatesNode) {
        Map<Long, JobMatchAiInsight> insights = new LinkedHashMap<>();
        if (candidatesNode == null || !candidatesNode.isArray()) {
            return insights;
        }

        for (JsonNode item : candidatesNode) {
            Long jobPostId = readLong(item, "jobPostId");
            if (jobPostId == null) {
                continue;
            }

            JobMatchAiInsight insight = new JobMatchAiInsight();
            insight.setJobPostId(jobPostId);
            insight.setRerankScore(readInteger(item, "rerankScore", 0, 100));
            insight.setPreferenceFitLevel(normalizeChoice(readText(item, "preferenceFitLevel"), "MEDIUM", "HIGH", "MEDIUM", "LOW"));
            insight.setAiMatchReason(readText(item, "aiMatchReason"));
            insight.setSuggestedNextAction(readText(item, "suggestedNextAction"));
            insight.setRecommendedPriority(normalizeChoice(readText(item, "recommendedPriority"), "MEDIUM", "HIGH", "MEDIUM", "LOW"));
            insight.setAiRiskPoints(aiTextSanitizer.sanitizeListItems(readArray(item, "aiRiskPoints", 3), 3));
            insight.setShortTagSummary(aiTextSanitizer.sanitizeListItems(readArray(item, "shortTagSummary", 4), 4));
            insights.put(jobPostId, insight);
        }
        return insights;
    }

    private LearningPathCoachAdvice readLearningPathCoachAdvice(JsonNode json) {
        if (json == null || !json.isObject()) {
            return null;
        }
        LearningPathCoachAdvice advice = new LearningPathCoachAdvice();
        advice.setCoachSummary(readText(json, "coachSummary"));
        advice.setImpactExplanation(readText(json, "impactExplanation"));
        advice.setPriorityFocus(readText(json, "priorityFocus"));
        advice.setSkillOrProjectFirst(normalizeChoice(
                readText(json, "skillOrProjectFirst"),
                "BALANCED",
                "SKILL_FIRST",
                "PROJECT_FIRST",
                "BALANCED"
        ));
        advice.setEstimatedDuration(readText(json, "estimatedDuration"));
        advice.setDeliveryTimingAdvice(readText(json, "deliveryTimingAdvice"));
        advice.setNextActionSuggestion(readText(json, "nextActionSuggestion"));
        advice.setLearningSteps(aiTextSanitizer.sanitizeListItems(readArray(json, "learningSteps", 6), 6));
        advice.setRecommendedProjects(aiTextSanitizer.sanitizeListItems(readArray(json, "recommendedProjects", 4), 4));
        advice.setSuggestedOrder(aiTextSanitizer.sanitizeListItems(readArray(json, "suggestedOrder", 5), 5));
        advice.setResourceTypes(aiTextSanitizer.sanitizeListItems(readArray(json, "resourceTypes", 5), 5));
        return advice;
    }

    private boolean hasCoachAdviceContent(LearningPathCoachAdvice advice) {
        if (advice == null) {
            return false;
        }
        return StringUtils.hasText(advice.getCoachSummary())
                || StringUtils.hasText(advice.getPriorityFocus())
                || !advice.getLearningSteps().isEmpty()
                || !advice.getRecommendedProjects().isEmpty();
    }

    private JsonNode parseJsonObject(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String raw = text.trim();
        try {
            JsonNode direct = OBJECT_MAPPER.readTree(raw);
            if (direct != null && direct.isObject()) {
                return direct;
            }
        } catch (Exception ignored) {
        }

        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonNode embedded = OBJECT_MAPPER.readTree(raw.substring(start, end + 1));
            return embedded != null && embedded.isObject() ? embedded : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String readText(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "";
        }
        JsonNode valueNode = node.get(field);
        if (valueNode.isTextual()) {
            return aiTextSanitizer.sanitizePlainText(valueNode.asText(""));
        }
        if (valueNode.isNumber() || valueNode.isBoolean()) {
            return valueNode.asText("").trim();
        }
        return aiTextSanitizer.sanitizePlainText(valueNode.toString());
    }

    private Integer readInteger(JsonNode node, String field, int min, int max) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        JsonNode valueNode = node.get(field);
        Integer value = null;
        if (valueNode.isInt() || valueNode.isLong()) {
            value = valueNode.asInt();
        } else if (valueNode.isTextual()) {
            try {
                value = Integer.parseInt(valueNode.asText("").trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (value == null) {
            return null;
        }
        return Math.max(min, Math.min(max, value));
    }

    private Long readLong(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        JsonNode valueNode = node.get(field);
        if (valueNode.isLong() || valueNode.isInt()) {
            return valueNode.asLong();
        }
        if (valueNode.isTextual()) {
            try {
                return Long.parseLong(valueNode.asText("").trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<String> readArray(JsonNode node, String field, int limit) {
        Set<String> items = new LinkedHashSet<>();
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return new ArrayList<>();
        }
        JsonNode arrayNode = node.get(field);
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String text = item == null ? "" : item.asText("").trim();
                if (isValidItem(text)) {
                    items.add(aiTextSanitizer.sanitizePlainText(text));
                }
                if (items.size() >= limit) {
                    break;
                }
            }
            return new ArrayList<>(items);
        }

        if (arrayNode.isTextual()) {
            String text = arrayNode.asText("").trim();
            for (String token : text.split("[,，。；;\\n]")) {
                String cleaned = token.trim();
                if (isValidItem(cleaned)) {
                    items.add(aiTextSanitizer.sanitizePlainText(cleaned));
                }
                if (items.size() >= limit) {
                    break;
                }
            }
        }
        return new ArrayList<>(items);
    }

    private String joinForPrompt(List<String> items, int limit) {
        if (items == null || items.isEmpty()) {
            return "暂无";
        }
        List<String> cleaned = aiTextSanitizer.sanitizeListItems(items, limit);
        return cleaned.isEmpty() ? "暂无" : String.join("；", cleaned);
    }

    private String normalizeChoice(String value, String defaultValue, String... allowedValues) {
        String normalized = defaultText(value).toUpperCase(Locale.ROOT);
        for (String allowedValue : allowedValues) {
            if (allowedValue.equalsIgnoreCase(normalized)) {
                return allowedValue;
            }
        }
        return defaultValue;
    }

    private boolean isValidItem(String text) {
        return StringUtils.hasText(text) && text.trim().length() >= 2 && text.trim().length() <= 90;
    }

    private String toJsonArrayString(List<String> items) {
        try {
            return OBJECT_MAPPER.writeValueAsString(items == null ? List.of() : items);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private ResumeAiReviewResult buildFallbackReview(String resumeText, JobPost jobPost, UserProfile userProfile) {
        String text = defaultText(resumeText);
        List<String> problems = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (text.length() < 140) {
            problems.add("简历正文偏短，关键信息覆盖不足");
            suggestions.add("先补齐教育、技能、项目、实习四个核心模块，每个模块至少 2-3 条要点。");
        } else if (text.length() < 260) {
            problems.add("简历信息密度偏低，亮点表达不够集中");
            suggestions.add("优先补充最相关项目的职责、技术栈和结果指标。");
        }

        if (!containsAnyIgnoreCase(text, "项目", "实习", "经历", "实践")) {
            problems.add("缺少项目或实习经历描述");
            suggestions.add("增加至少 1 个可讲述项目，按“背景-动作-结果”组织描述。");
        }

        if (!containsDigit(text)) {
            problems.add("缺少量化结果，成果说服力不足");
            suggestions.add("每段经历补充 1 个量化指标，如耗时、效率、规模、准确率。");
        }

        if (!containsAnyIgnoreCase(text, "负责", "实现", "优化", "设计", "推进")) {
            problems.add("动作型动词不足，贡献表达不清晰");
            suggestions.add("多用“负责/设计/实现/优化/推进”等动作词开头。");
        }

        List<String> missingKeywords = new ArrayList<>();
        for (String keyword : extractKeywords(defaultText(jobPost.getJobRequirements()), 12)) {
            if (!containsIgnoreCase(text, keyword)) {
                missingKeywords.add(keyword);
            }
            if (missingKeywords.size() >= 8) {
                break;
            }
        }
        if (missingKeywords.isEmpty()) {
            missingKeywords = new ArrayList<>(List.of("岗位核心技能", "项目量化成果", "技术栈关键词"));
        } else {
            suggestions.add("在技能和项目中自然补齐关键词：" + String.join("、", missingKeywords));
        }

        if (!directionAligned(userProfile.getTargetDirection(), jobPost.getJobDirection())) {
            problems.add("画像方向与目标岗位方向存在偏差");
            suggestions.add("在简历开头补充目标岗位动机和相关能力迁移说明。");
        }

        if (suggestions.size() < 3) {
            suggestions.add("按目标岗位 JD 调整关键词排序，优先展示高匹配能力。");
        }

        problems = limitUnique(problems, 4);
        suggestions = limitUnique(suggestions, 5);
        String summary = buildRuleSummary(problems);
        return new ResumeAiReviewResult(
                summary,
                toJsonArrayString(problems),
                toJsonArrayString(missingKeywords),
                toJsonArrayString(suggestions)
        );
    }

    private List<String> extractKeywords(String source, int limit) {
        Set<String> set = new LinkedHashSet<>();
        if (!StringUtils.hasText(source)) {
            return new ArrayList<>();
        }
        String normalized = source.replace("\n", ",")
                .replace("，", ",")
                .replace("。", ",")
                .replace("；", ",")
                .replace(";", ",")
                .replace("/", ",");
        for (String token : normalized.split(",")) {
            String cleaned = token.trim();
            if (!StringUtils.hasText(cleaned)) {
                continue;
            }
            if (cleaned.length() > 24) {
                continue;
            }
            set.add(cleaned);
            if (set.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(set);
    }

    private boolean directionAligned(String profileDirection, String jobDirection) {
        String left = defaultText(profileDirection).toLowerCase(Locale.ROOT);
        String right = defaultText(jobDirection).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return true;
        }
        return left.contains(right) || right.contains(left);
    }

    private String fallbackRewriteSection(String sectionName, String sectionText, JobPost jobPost, UserProfile userProfile) {
        String focusDirection = defaultIfEmpty(jobPost.getJobDirection(), userProfile.getTargetDirection());
        List<String> keywords = extractKeywords(defaultText(jobPost.getJobRequirements()), 4);
        List<String> lines = new ArrayList<>();
        for (String raw : defaultText(sectionText).split("\\n")) {
            String line = raw.replaceFirst("^[\\-•*\\d\\s\\.、\\)]+", "").trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }
            if (!containsDigit(line) && line.length() > 8) {
                line = line + "（建议补充量化结果）";
            }
            lines.add(line);
        }
        if (lines.isEmpty()) {
            lines.add(defaultText(sectionText));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【").append(defaultIfEmpty(sectionName, "简历模块")).append("（本地规则优化版）】\n")
                .append("目标方向：").append(defaultIfEmpty(focusDirection, "目标岗位")).append('\n')
                .append("表达模板：动作 + 技术 + 结果（尽量量化）");
        if (!keywords.isEmpty()) {
            sb.append("\n优先关键词：").append(String.join("、", keywords));
        }
        for (String line : lines) {
            sb.append("\n- ").append(line);
        }
        return aiTextSanitizer.sanitizeResumeText(sb.toString().trim());
    }

    private String fallbackRewriteFull(String merged, JobPost jobPost, UserProfile userProfile) {
        String focusDirection = defaultIfEmpty(jobPost.getJobDirection(), defaultText(userProfile.getTargetDirection()));
        List<String> keywords = extractKeywords(defaultText(jobPost.getJobRequirements()), 6);
        StringBuilder sb = new StringBuilder();
        sb.append("【优化版简历（本地规则）】\n")
                .append("目标岗位：").append(defaultIfEmpty(jobPost.getJobName(), "目标岗位"))
                .append("（").append(defaultIfEmpty(focusDirection, "目标方向")).append("）\n")
                .append("优化策略：关键词对齐、项目量化、结果前置");
        if (!keywords.isEmpty()) {
            sb.append("\n推荐关键词：").append(String.join("、", keywords));
        }
        sb.append("\n\n").append(formatFallbackResumeBody(merged));
        return aiTextSanitizer.sanitizeResumeText(sb.toString().trim());
    }

    private String mergeSections(Map<String, String> sections) {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "基本信息", sections.get("basicInfo"));
        appendSection(sb, "教育经历", sections.get("education"));
        appendSection(sb, "技能描述", sections.get("skills"));
        appendSection(sb, "项目经历", sections.get("projects"));
        appendSection(sb, "实习/校园经历", sections.get("experience"));
        appendSection(sb, "其他补充", sections.get("others"));
        return sb.toString().trim();
    }

    private void appendSection(StringBuilder sb, String title, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append("\n\n");
        }
        sb.append(title).append("：\n").append(text.trim());
    }

    private String formatFallbackResumeBody(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return "暂无可用简历正文，请先补充基础信息与项目经历。";
        }
        StringBuilder body = new StringBuilder();
        for (String rawLine : rawText.replace("\r", "").split("\n")) {
            String line = rawLine.trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }
            if (line.endsWith("：") || line.endsWith(":")) {
                if (!body.isEmpty()) {
                    body.append('\n');
                }
                body.append('\n').append(line).append('\n');
                continue;
            }
            String cleaned = line.replaceFirst("^[\\-•*\\d\\s\\.、\\)]+", "").trim();
            if (!StringUtils.hasText(cleaned)) {
                continue;
            }
            body.append("- ").append(cleaned).append('\n');
        }
        return body.toString().trim();
    }

    private String buildRuleSummary(List<String> problems) {
        int count = problems == null ? 0 : problems.size();
        if (count <= 1) {
            return "简历基础可用，建议补齐关键词和量化成果后投递。";
        }
        if (count <= 3) {
            return "简历有一定匹配度，建议先完成关键补强再进入集中投递。";
        }
        return "当前简历与目标岗位存在明显差距，建议先按补强清单完成结构化优化。";
    }

    private List<String> limitUnique(List<String> source, int limit) {
        Set<String> set = new LinkedHashSet<>();
        if (source != null) {
            for (String item : source) {
                String cleaned = defaultText(item);
                if (StringUtils.hasText(cleaned)) {
                    set.add(cleaned);
                }
                if (set.size() >= limit) {
                    break;
                }
            }
        }
        return new ArrayList<>(set);
    }

    private boolean containsAnyIgnoreCase(String source, String... targets) {
        for (String target : targets) {
            if (containsIgnoreCase(source, target)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String source, String target) {
        String left = defaultText(source).toLowerCase(Locale.ROOT);
        String right = defaultText(target).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return left.contains(right);
    }

    private boolean containsDigit(String text) {
        return defaultText(text).matches(".*\\d+.*");
    }

    private String defaultText(String text) {
        return text == null ? "" : text.replace("\u0000", "").trim();
    }

    private String defaultIfEmpty(String text, String defaultValue) {
        return StringUtils.hasText(text) ? text.trim() : defaultValue;
    }
}
