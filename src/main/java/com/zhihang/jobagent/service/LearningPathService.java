package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.AiGenerationMetadata;
import com.zhihang.jobagent.dto.AiTextGenerationResult;
import com.zhihang.jobagent.dto.GenerationSource;
import com.zhihang.jobagent.dto.JobMatchPreferenceInput;
import com.zhihang.jobagent.dto.LearningPathCoachAdvice;
import com.zhihang.jobagent.dto.LearningPathCoachGenerationResult;
import com.zhihang.jobagent.dto.LearningPathResourceView;
import com.zhihang.jobagent.dto.LearningPathResultView;
import com.zhihang.jobagent.entity.JobMatchRecord;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.LearningPathAdvice;
import com.zhihang.jobagent.entity.LearningResource;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.LearningResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LearningPathService {

    private static final int MAX_STEP_ITEMS = 8;
    private static final int MAX_PROJECT_ITEMS = 6;
    private static final int MAX_RESOURCE_ITEMS = 8;

    private final LearningResourceRepository learningResourceRepository;
    private final AiFacadeService aiFacadeService;

    public LearningPathService(LearningResourceRepository learningResourceRepository,
                               AiFacadeService aiFacadeService) {
        this.learningResourceRepository = learningResourceRepository;
        this.aiFacadeService = aiFacadeService;
    }

    public LearningPathAdvice generate(UserProfile userProfile, JobPost jobPost,
                                       JobMatchRecord latestMatchRecord, ResumeReview latestResumeReview,
                                       ResumeEnhanceRecord latestEnhanceRecord) {
        LearningPathAdvice advice = new LearningPathAdvice();
        advice.setUserAccountId(userProfile.getUserAccountId());
        advice.setUserProfileId(userProfile.getId());
        advice.setTargetJobId(jobPost.getId());
        advice.setTargetDirection(resolveDirection(userProfile, jobPost));
        JobMatchPreferenceInput preferenceInput = JobMatchPreferenceInput.fromProfile(userProfile);
        advice.setPreferenceSummary(preferenceInput.toSummaryText());

        WeaknessBundle weaknessBundle = collectWeaknessSources(
                userProfile, jobPost, latestMatchRecord, latestResumeReview, latestEnhanceRecord
        );
        advice.setCurrentWeaknesses(buildWeaknessSummaryDisplay(weaknessBundle));

        String weaknessPromptText = weaknessBundle.flattenForPrompt();
        String ruleSteps = buildLearningSteps(advice.getTargetDirection(), weaknessPromptText);
        String ruleProjects = buildRecommendedProjects(advice.getTargetDirection(), weaknessPromptText);
        String ruleSuggestedOrder = buildSuggestedOrder(advice.getTargetDirection());
        String ruleResources = buildRecommendedResourcesNormalized(advice.getTargetDirection());
        String ruleEstimatedDuration = buildEstimatedDuration(advice.getTargetDirection(), weaknessPromptText);
        String ruleSuggestedLevel = buildSuggestedLevel(weaknessPromptText);
        String ruleDeliveryReadiness = buildDeliveryReadiness(advice.getTargetDirection(), weaknessPromptText);
        AiTextGenerationResult polishedStepsResult = polishWithAi("学习步骤", ruleSteps, userProfile, jobPost);
        LearningPathCoachGenerationResult coachGenerationResult = aiFacadeService.generateLearningPathCoach(
                userProfile,
                jobPost,
                latestMatchRecord,
                latestResumeReview,
                latestEnhanceRecord,
                preferenceInput,
                advice.getCurrentWeaknesses(),
                ruleSteps,
                ruleProjects,
                ruleSuggestedOrder,
                ruleEstimatedDuration,
                ruleDeliveryReadiness
        );
        LearningPathCoachAdvice coachAdvice = mergeCoachAdvice(
                buildFallbackCoachAdvice(weaknessBundle, jobPost, preferenceInput, ruleEstimatedDuration, ruleDeliveryReadiness),
                coachGenerationResult.getAdvice()
        );

        advice.setLearningSteps(joinStructuredItems(
                coachAdvice.getLearningSteps(),
                defaultIfEmpty(polishedStepsResult.getText(), ruleSteps)
        ));
        advice.setLearningStepsGenerationSource(polishedStepsResult.getMetadata().getGenerationSource().name());
        advice.setLearningStepsAiUsed(polishedStepsResult.getMetadata().isAiUsed());
        advice.setLearningStepsFallbackReason(buildFallbackReason(
                polishedStepsResult.getMetadata(),
                "AI call failed, switched to local learning steps."
        ));
        advice.setLearningStepsModelName(polishedStepsResult.getMetadata().getModelName());

        advice.setRecommendedProjects(joinStructuredItems(coachAdvice.getRecommendedProjects(), ruleProjects));
        advice.setSuggestedOrder(joinStructuredItems(coachAdvice.getSuggestedOrder(), ruleSuggestedOrder));
        advice.setRecommendedResources(ruleResources);
        advice.setEstimatedDuration(defaultIfEmpty(coachAdvice.getEstimatedDuration(), ruleEstimatedDuration));
        advice.setSuggestedLevel(ruleSuggestedLevel);
        advice.setDeliveryReadiness(ruleDeliveryReadiness);
        advice.setCoachSummary(defaultIfEmpty(coachAdvice.getCoachSummary(), weaknessBundle.flattenForPrompt()));
        advice.setImpactExplanation(defaultIfEmpty(
                coachAdvice.getImpactExplanation(),
                "当前短板会直接影响岗位匹配分、简历说服力和投递时点，需要先补齐再提升投递效率。"
        ));
        advice.setPriorityFocus(defaultIfEmpty(coachAdvice.getPriorityFocus(), firstPriorityFocus(weaknessBundle)));
        advice.setSkillOrProjectFirst(defaultIfEmpty(coachAdvice.getSkillOrProjectFirst(), inferSkillOrProjectFirst(weaknessBundle)));
        advice.setDeliveryTimingAdvice(defaultIfEmpty(coachAdvice.getDeliveryTimingAdvice(), ruleDeliveryReadiness));
        advice.setNextActionSuggestion(defaultIfEmpty(
                coachAdvice.getNextActionSuggestion(),
                "先去简历优化，再进入模拟面试和重新匹配。"
        ));
        advice.setCoachResourceTypes(joinStructuredItems(
                coachAdvice.getResourceTypes(),
                String.join("\n", defaultResourceTypes(advice.getTargetDirection()))
        ));
        advice.setCoachGenerationSource(coachGenerationResult.getMetadata().getGenerationSource().name());
        advice.setCoachAiUsed(coachGenerationResult.getMetadata().isAiUsed());
        advice.setCoachFallbackReason(buildFallbackReason(
                coachGenerationResult.getMetadata(),
                "AI coach unavailable, switched to rule-based coaching view."
        ));
        advice.setCoachModelName(coachGenerationResult.getMetadata().getModelName());
        advice.setCreatedAt(LocalDateTime.now());
        return advice;
    }

    public LearningPathResultView buildResultView(UserProfile userProfile, JobPost jobPost, LearningPathAdvice advice,
                                                  JobMatchRecord latestMatchRecord, ResumeReview latestResumeReview,
                                                  ResumeEnhanceRecord latestEnhanceRecord) {
        WeaknessBundle weaknessBundle = collectWeaknessSources(
                userProfile, jobPost, latestMatchRecord, latestResumeReview, latestEnhanceRecord
        );

        List<String> learningSteps = splitStructuredItems(advice.getLearningSteps(), MAX_STEP_ITEMS);
        if (learningSteps.isEmpty()) {
            learningSteps = splitStructuredItems(
                    buildLearningSteps(advice.getTargetDirection(), weaknessBundle.flattenForPrompt()),
                    MAX_STEP_ITEMS
            );
        }

        List<String> recommendedProjects = splitStructuredItems(advice.getRecommendedProjects(), MAX_PROJECT_ITEMS);
        if (recommendedProjects.isEmpty()) {
            recommendedProjects = List.of("建议先完成 1 个可展示项目，再补 1 个与目标岗位强相关的实战项目。");
        }

        List<String> suggestedOrder = splitStructuredItems(advice.getSuggestedOrder(), 5);
        if (suggestedOrder.isEmpty()) {
            suggestedOrder = splitStructuredItems(buildSuggestedOrder(advice.getTargetDirection()), 5);
        }

        List<String> resourceTypeSuggestions = splitStructuredItems(advice.getCoachResourceTypes(), 5);
        if (resourceTypeSuggestions.isEmpty()) {
            resourceTypeSuggestions = defaultResourceTypes(advice.getTargetDirection());
        }

        List<LearningPathResourceView> resources = parseResourceItemsNormalized(advice.getRecommendedResources());
        if (resources.isEmpty()) {
            resources = parseResourceItemsNormalized(defaultResources(advice.getTargetDirection()));
        }

        List<String> deliveryChecklist = buildDeliveryChecklist(weaknessBundle);
        String weaknessSourceSummary = "岗位匹配 " + weaknessBundle.matchWeaknesses.size() + " 项 · "
                + "画像短板 " + weaknessBundle.profileWeaknesses.size() + " 项 · "
                + "简历诊断 " + weaknessBundle.resumeWeaknesses.size() + " 项";

        String preferenceSummary = defaultIfEmpty(advice.getPreferenceSummary(), JobMatchPreferenceInput.fromProfile(userProfile).toSummaryText());
        String coachSummary = defaultIfEmpty(advice.getCoachSummary(), weaknessBundle.flattenForPrompt());
        String impactExplanation = defaultIfEmpty(
                advice.getImpactExplanation(),
                "这些短板会同时影响规则匹配、简历表达和实际投递时点。"
        );
        String priorityFocus = defaultIfEmpty(advice.getPriorityFocus(), firstPriorityFocus(weaknessBundle));
        String skillOrProjectFirst = defaultIfEmpty(advice.getSkillOrProjectFirst(), inferSkillOrProjectFirst(weaknessBundle));
        String deliveryTimingAdvice = defaultIfEmpty(advice.getDeliveryTimingAdvice(), advice.getDeliveryReadiness());
        String nextActionSuggestion = defaultIfEmpty(
                advice.getNextActionSuggestion(),
                "先去简历优化，再进入模拟面试和重新匹配。"
        );

        return new LearningPathResultView(
                weaknessBundle.profileWeaknesses,
                weaknessBundle.matchWeaknesses,
                weaknessBundle.resumeWeaknesses,
                learningSteps,
                recommendedProjects,
                suggestedOrder,
                resourceTypeSuggestions,
                resources,
                deliveryChecklist,
                weaknessSourceSummary,
                preferenceSummary,
                coachSummary,
                impactExplanation,
                priorityFocus,
                skillOrProjectFirst,
                deliveryTimingAdvice,
                nextActionSuggestion,
                weaknessBundle.resumeWeaknesses.isEmpty()
        );
    }

    private WeaknessBundle collectWeaknessSources(UserProfile userProfile, JobPost jobPost,
                                                  JobMatchRecord latestMatchRecord, ResumeReview latestResumeReview,
                                                  ResumeEnhanceRecord latestEnhanceRecord) {
        List<String> profileWeaknesses = new ArrayList<>();
        List<String> matchActionSuggestions = new ArrayList<>();
        if (!StringUtils.hasText(userProfile.getProjectExperience())) {
            profileWeaknesses.add("项目经历偏少，缺少可展示的完整实战案例。");
        }
        if (!StringUtils.hasText(userProfile.getSkills())) {
            profileWeaknesses.add("技能描述不完整，建议补齐核心技术栈与熟练度。");
        }
        if (!directionAligned(userProfile, jobPost)) {
            profileWeaknesses.add("当前画像目标方向与目标岗位方向存在偏差。");
        }

        List<String> matchWeaknesses = new ArrayList<>();
        if (latestMatchRecord != null) {
            if (StringUtils.hasText(latestMatchRecord.getCompatibilityLevel())
                    && !"STRONG".equalsIgnoreCase(latestMatchRecord.getCompatibilityLevel())) {
                matchWeaknesses.add("当前岗位硬条件判断为" + latestMatchRecord.getCompatibilityLabel()
                        + "，需要先解决方向、学历、城市或岗位性质的基础适配问题。");
            }
            matchWeaknesses.addAll(splitStructuredItems(latestMatchRecord.getWeaknessAnalysis(), 5));
            matchWeaknesses.addAll(splitStructuredItems(latestMatchRecord.getDeductionReasons(), 4));
            if (!StringUtils.hasText(latestMatchRecord.getWeaknessAnalysis())
                    && StringUtils.hasText(latestMatchRecord.getImprovementSuggestion())) {
                matchWeaknesses.addAll(splitStructuredItems(latestMatchRecord.getImprovementSuggestion(), 3));
            }
            matchActionSuggestions.addAll(splitStructuredItems(latestMatchRecord.getImprovementSuggestion(), 4));
        }

        List<String> resumeWeaknesses = new ArrayList<>();
        if (latestResumeReview != null) {
            resumeWeaknesses.addAll(splitStructuredItems(latestResumeReview.getProblems(), 4));
            String missingKeywords = defaultText(latestResumeReview.getMissingKeywords());
            if (StringUtils.hasText(missingKeywords)
                    && !missingKeywords.contains("无明显缺失关键词")
                    && !missingKeywords.equalsIgnoreCase("none")) {
                resumeWeaknesses.add("简历缺失关键词：" + missingKeywords);
            }
        }
        if (latestEnhanceRecord != null) {
            resumeWeaknesses.addAll(splitStructuredItems(latestEnhanceRecord.getAiProblems(), 4));
            if (StringUtils.hasText(latestEnhanceRecord.getAiMissingKeywords())) {
                resumeWeaknesses.add("AI 诊断缺失关键词：" + latestEnhanceRecord.getAiMissingKeywords().trim());
            }
        }

        profileWeaknesses = dedupe(profileWeaknesses);
        matchWeaknesses = dedupe(matchWeaknesses);
        resumeWeaknesses = dedupe(resumeWeaknesses);

        if (profileWeaknesses.isEmpty() && matchWeaknesses.isEmpty() && resumeWeaknesses.isEmpty()) {
            profileWeaknesses = List.of("当前短板不明显，建议继续加强项目深度与量化成果表达。");
        }

        return new WeaknessBundle(profileWeaknesses, matchWeaknesses, resumeWeaknesses, dedupe(matchActionSuggestions));
    }

    private String buildWeaknessSummaryDisplay(WeaknessBundle weaknessBundle) {
        StringBuilder sb = new StringBuilder();
        appendSourceSection(sb, "来自岗位匹配的短板", weaknessBundle.matchWeaknesses);
        appendSourceSection(sb, "来自画像的短板", weaknessBundle.profileWeaknesses);
        if (weaknessBundle.resumeWeaknesses.isEmpty()) {
            appendSourceSection(sb, "来自简历诊断的问题",
                    List.of("当前未检测到简历诊断记录，以下建议主要基于岗位和画像生成。"));
        } else {
            appendSourceSection(sb, "来自简历诊断的问题", weaknessBundle.resumeWeaknesses);
        }
        return sb.toString().trim();
    }

    private void appendSourceSection(StringBuilder sb, String title, List<String> items) {
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(title).append(":\n");
        if (items == null || items.isEmpty()) {
            sb.append("- 暂无\n");
            return;
        }
        for (String item : items) {
            sb.append("- ").append(item).append('\n');
        }
    }

    private String buildLearningSteps(String direction, String weaknessSummary) {
        List<String> steps = new ArrayList<>();
        if (isBackend(direction)) {
            steps.add("第 1 周：补齐 Java 基础（集合、异常、并发基础）并梳理常见手写题。");
            steps.add("第 2-3 周：强化 MySQL 与 SQL（建模、索引、查询优化、事务）。");
            steps.add("第 4-5 周：系统学习 Spring Boot（接口设计、参数校验、异常处理、日志）。");
            steps.add("第 6-7 周：完成 1 个可部署后端项目并输出 README 与接口文档。");
            steps.add("第 8 周：完成简历优化、项目复盘和岗位模拟问答。");
        } else if (isFrontend(direction)) {
            steps.add("第 1 周：夯实 HTML/CSS/JavaScript 基础与页面语义化。");
            steps.add("第 2-3 周：强化组件化开发、状态管理和路由组织。");
            steps.add("第 4-5 周：完成页面还原 + 接口联调项目，补齐错误处理与性能优化。");
            steps.add("第 6-7 周：补充工程化能力（打包、部署、监控）。");
            steps.add("第 8 周：打磨作品集、简历表达与面试题复盘。");
        } else if (isProduct(direction)) {
            steps.add("第 1 周：补齐需求分析、用户场景拆解与竞品分析框架。");
            steps.add("第 2-3 周：完成 1 份完整 PRD + 原型并做评审复盘。");
            steps.add("第 4-5 周：补齐数据指标定义与方案可落地性验证。");
            steps.add("第 6-7 周：沉淀 2 个可讲述的项目案例，形成标准化讲述脚本。");
            steps.add("第 8 周：模拟产品面试并针对薄弱项迭代材料。");
        } else {
            steps.add("第 1-2 周：明确目标岗位能力模型并补齐基础技能。");
            steps.add("第 3-5 周：完成 1 个与岗位相关的完整项目。");
            steps.add("第 6-7 周：优化简历表达并做项目复盘演练。");
            steps.add("第 8 周：进行模拟面试并按反馈补强。");
        }

        if (containsAny(weaknessSummary, "关键词", "关键字")) {
            steps.add("并行任务：每周补齐 5-8 个岗位关键词，并在简历与项目描述中落地。");
        }
        return String.join("\n", steps);
    }

    private String buildRecommendedProjects(String direction, String weaknessSummary) {
        if (isBackend(direction)) {
            return """
                    1. 校园选课系统后端（用户、课程、选课、权限模块）
                    2. 岗位匹配 API 服务（规则评分、日志追踪、结果分析）
                    3. 面试题库管理后台（CRUD、检索、统计看板）
                    项目要求：至少 1 个项目可部署，包含接口文档与性能优化说明
                    """.trim();
        }
        if (isFrontend(direction)) {
            return """
                    1. 求职导航平台前端（首页、画像、记录中心）
                    2. 简历增强结果可视化页（分块对比、复制、二次改写）
                    3. 面试训练看板（分数趋势、薄弱点复盘）
                    项目要求：体现组件化、联调能力、响应式适配与性能优化
                    """.trim();
        }
        if (isProduct(direction)) {
            return """
                    1. 校园求职助手的需求拆解 + 原型设计
                    2. 学生求职训练闭环方案（匹配-诊断-优化-面试）
                    3. 题库更新中心流程与指标设计
                    项目要求：输出 PRD、原型、流程图、上线指标与复盘结论
                    """.trim();
        }
        String extra = StringUtils.hasText(weaknessSummary)
                ? "建议围绕当前短板设计 1 个主项目 + 1 个补强项目。"
                : "建议完成 1-2 个与目标岗位方向强相关的实战项目。";
        return extra;
    }

    private String buildRecommendedResources(String direction) {
        List<LearningResource> resources = queryResourcesByDirection(direction);
        if (resources.isEmpty()) {
            return defaultResources(direction);
        }

        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (LearningResource resource : resources) {
            sb.append(index++).append(". ")
                    .append(defaultIfEmpty(resource.getTitle(), "未命名资源"))
                    .append(" | 类型:").append(defaultIfEmpty(resource.getResourceType(), "学习资源"))
                    .append(" | 阶段:").append(defaultIfEmpty(resource.getStageTag(), "基础"))
                    .append(" | 链接:").append(defaultIfEmpty(resource.getUrl(), "-"))
                    .append('\n');
            if (index > MAX_RESOURCE_ITEMS) {
                break;
            }
        }
        return sb.toString().trim();
    }

    private String buildRecommendedResourcesNormalized(String direction) {
        List<LearningResource> resources = queryResourcesByDirection(direction);
        if (resources.isEmpty()) {
            return defaultResources(direction);
        }

        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (LearningResource resource : resources) {
            sb.append(index++).append(". ")
                    .append(sanitizeInlineValue(defaultIfEmpty(resource.getDisplayTitle(), "未命名资源")))
                    .append(" | 类型:").append(sanitizeInlineValue(defaultIfEmpty(resource.getDisplayCategory(), resource.getResourceType())))
                    .append(" | 阶段:").append(sanitizeInlineValue(defaultIfEmpty(resource.getDisplayStage(), resource.getStageTag())))
                    .append(" | 摘要:").append(sanitizeInlineValue(defaultIfEmpty(resource.getDisplaySummary(), resource.getSummary())))
                    .append(" | 原始标题:").append(sanitizeInlineValue(defaultIfEmpty(resource.getRawTitle(), resource.getTitle())))
                    .append(" | 链接:").append(sanitizeInlineValue(defaultIfEmpty(resource.getUrl(), "-")))
                    .append('\n');
            if (index > MAX_RESOURCE_ITEMS) {
                break;
            }
        }
        return sb.toString().trim();
    }

    private String buildSuggestedOrder(String direction) {
        if (isBackend(direction)) {
            return """
                    1. 先打牢 Java/SQL 基础
                    2. 再补齐 Spring Boot 工程化能力
                    3. 再输出可部署项目与项目文档
                    4. 最后做简历优化和模拟面试
                    """.trim();
        }
        if (isFrontend(direction)) {
            return """
                    1. 先补页面与脚本基础
                    2. 再提升框架与联调能力
                    3. 再做项目实战与性能优化
                    4. 最后做作品集与面试冲刺
                    """.trim();
        }
        if (isProduct(direction)) {
            return """
                    1. 先补需求分析与场景拆解
                    2. 再输出 PRD 与原型
                    3. 再做案例复盘与指标闭环
                    4. 最后做表达训练与模拟面试
                    """.trim();
        }
        return """
                1. 先补基础能力，再做项目
                2. 先补关键词覆盖，再优化表达
                3. 先形成可展示成果，再集中投递
                """.trim();
    }

    private String defaultResources(String direction) {
        if (isBackend(direction)) {
            return """
                    1. Spring Boot 官方文档 | 类型:官方文档 | 阶段:基础 | 链接:https://spring.io/projects/spring-boot
                    2. MySQL 官方手册 | 类型:官方文档 | 阶段:基础 | 链接:https://dev.mysql.com/doc/
                    3. Java 后端实战课程 | 类型:视频课程 | 阶段:进阶 | 链接:https://www.bilibili.com/
                    4. 后端项目模板仓库 | 类型:项目实战 | 阶段:实战 | 链接:https://github.com/
                    5. 后端面经合集 | 类型:面经/经验帖 | 阶段:冲刺 | 链接:https://www.nowcoder.com/
                    """.trim();
        }
        if (isFrontend(direction)) {
            return """
                    1. MDN Web Docs | 类型:官方文档 | 阶段:基础 | 链接:https://developer.mozilla.org/
                    2. 前端框架官方文档 | 类型:官方文档 | 阶段:进阶 | 链接:https://react.dev/
                    3. 前端实战课程 | 类型:视频课程 | 阶段:进阶 | 链接:https://www.bilibili.com/
                    4. 前端项目模板仓库 | 类型:项目实战 | 阶段:实战 | 链接:https://github.com/
                    5. 前端面经整理 | 类型:面经/经验帖 | 阶段:冲刺 | 链接:https://juejin.cn/
                    """.trim();
        }
        if (isProduct(direction)) {
            return """
                    1. 产品设计工具官方教程 | 类型:官方文档 | 阶段:基础 | 链接:https://www.axure.com/
                    2. 产品需求分析课程 | 类型:视频课程 | 阶段:进阶 | 链接:https://www.bilibili.com/
                    3. PRD 与案例模板库 | 类型:项目实战 | 阶段:实战 | 链接:https://github.com/
                    4. 产品经理经验帖集合 | 类型:面经/经验帖 | 阶段:冲刺 | 链接:https://www.nowcoder.com/
                    """.trim();
        }
        return """
                1. 通用官方文档集合 | 类型:官方文档 | 阶段:基础 | 链接:https://github.com/
                2. 岗位实战课程集合 | 类型:视频课程 | 阶段:进阶 | 链接:https://www.bilibili.com/
                3. 项目模板仓库 | 类型:项目实战 | 阶段:实战 | 链接:https://github.com/
                4. 面经与经验帖 | 类型:面经/经验帖 | 阶段:冲刺 | 链接:https://www.nowcoder.com/
                """.trim();
    }

    private String buildEstimatedDuration(String direction, String weaknessSummary) {
        if (isBackend(direction) || isFrontend(direction)) {
            if (containsAny(weaknessSummary, "偏少", "不足", "不完整", "偏差")) {
                return "建议 8-10 周（每周 10-14 小时）";
            }
            return "建议 6-8 周（每周 8-12 小时）";
        }
        if (isProduct(direction)) {
            return "建议 6-8 周（每周 8-12 小时）";
        }
        return "建议 6-10 周（每周 8-12 小时）";
    }

    private String buildSuggestedLevel(String weaknessSummary) {
        if (containsAny(weaknessSummary, "项目经历偏少", "关键词", "偏少")) {
            return "建议先补齐基础与项目，再进行集中投递。";
        }
        return "可并行进行投递与训练，每周固定复盘。";
    }

    private String buildDeliveryReadiness(String direction, String weaknessSummary) {
        String base = "当你能稳定完成 1-2 个岗位相关项目，关键词覆盖明显提升，且模拟面试达到中高分时，可开始集中投递。";
        if (isProduct(direction)) {
            base = "当你能输出完整 PRD 与原型，并清晰讲述需求拆解与复盘案例时，可开始集中投递。";
        }
        if (containsAny(weaknessSummary, "方向", "偏差", "不一致")) {
            base += " 当前还需补充与目标岗位方向一致的证明材料。";
        }
        return base;
    }

    private LearningPathCoachAdvice buildFallbackCoachAdvice(WeaknessBundle weaknessBundle,
                                                             JobPost jobPost,
                                                             JobMatchPreferenceInput preferenceInput,
                                                             String ruleEstimatedDuration,
                                                             String ruleDeliveryReadiness) {
        LearningPathCoachAdvice advice = new LearningPathCoachAdvice();
        advice.setCoachSummary(defaultIfEmpty(
                firstPriorityFocus(weaknessBundle),
                "先补齐最影响投递效率的短板，再集中推进目标岗位。"
        ));
        advice.setImpactExplanation("这些短板会直接影响你投递 "
                + defaultIfEmpty(jobPost == null ? null : jobPost.getDisplayTitle(), "目标岗位")
                + " 时的通过率、简历说服力和面试准备成本。");
        advice.setPriorityFocus(firstPriorityFocus(weaknessBundle));
        advice.setSkillOrProjectFirst(inferSkillOrProjectFirst(weaknessBundle));
        advice.setEstimatedDuration(ruleEstimatedDuration);
        advice.setDeliveryTimingAdvice(ruleDeliveryReadiness);
        advice.setNextActionSuggestion(preferenceInput != null && preferenceInput.isTemporaryOverrideApplied()
                ? "先按当前偏好重新看匹配结果，再去简历优化。"
                : "先去简历优化，再进入模拟面试和重新匹配。");
        advice.setResourceTypes(defaultResourceTypes(jobPost == null ? "" : jobPost.getJobDirection()));
        return advice;
    }

    private LearningPathCoachAdvice mergeCoachAdvice(LearningPathCoachAdvice fallbackAdvice,
                                                     LearningPathCoachAdvice aiAdvice) {
        if (fallbackAdvice == null) {
            return aiAdvice == null ? new LearningPathCoachAdvice() : aiAdvice;
        }
        if (aiAdvice == null) {
            return fallbackAdvice;
        }

        LearningPathCoachAdvice merged = new LearningPathCoachAdvice();
        merged.setCoachSummary(defaultIfEmpty(aiAdvice.getCoachSummary(), fallbackAdvice.getCoachSummary()));
        merged.setImpactExplanation(defaultIfEmpty(aiAdvice.getImpactExplanation(), fallbackAdvice.getImpactExplanation()));
        merged.setPriorityFocus(defaultIfEmpty(aiAdvice.getPriorityFocus(), fallbackAdvice.getPriorityFocus()));
        merged.setSkillOrProjectFirst(defaultIfEmpty(aiAdvice.getSkillOrProjectFirst(), fallbackAdvice.getSkillOrProjectFirst()));
        merged.setEstimatedDuration(defaultIfEmpty(aiAdvice.getEstimatedDuration(), fallbackAdvice.getEstimatedDuration()));
        merged.setDeliveryTimingAdvice(defaultIfEmpty(aiAdvice.getDeliveryTimingAdvice(), fallbackAdvice.getDeliveryTimingAdvice()));
        merged.setNextActionSuggestion(defaultIfEmpty(aiAdvice.getNextActionSuggestion(), fallbackAdvice.getNextActionSuggestion()));
        merged.setLearningSteps(aiAdvice.getLearningSteps().isEmpty() ? fallbackAdvice.getLearningSteps() : aiAdvice.getLearningSteps());
        merged.setRecommendedProjects(aiAdvice.getRecommendedProjects().isEmpty() ? fallbackAdvice.getRecommendedProjects() : aiAdvice.getRecommendedProjects());
        merged.setSuggestedOrder(aiAdvice.getSuggestedOrder().isEmpty() ? fallbackAdvice.getSuggestedOrder() : aiAdvice.getSuggestedOrder());
        merged.setResourceTypes(aiAdvice.getResourceTypes().isEmpty() ? fallbackAdvice.getResourceTypes() : aiAdvice.getResourceTypes());
        return merged;
    }

    private String joinStructuredItems(List<String> items, String fallbackText) {
        if (items != null && !items.isEmpty()) {
            return String.join("\n", items);
        }
        return defaultIfEmpty(fallbackText, "");
    }

    private List<String> defaultResourceTypes(String direction) {
        if (isBackend(direction) || isFrontend(direction)) {
            return List.of("官方文档", "项目实战", "面经复盘");
        }
        if (isProduct(direction)) {
            return List.of("案例拆解", "原型练习", "产品面经");
        }
        return List.of("官方文档", "项目实战", "模拟面试");
    }

    private String firstPriorityFocus(WeaknessBundle weaknessBundle) {
        if (!weaknessBundle.matchWeaknesses.isEmpty()) {
            return weaknessBundle.matchWeaknesses.get(0);
        }
        if (!weaknessBundle.profileWeaknesses.isEmpty()) {
            return weaknessBundle.profileWeaknesses.get(0);
        }
        if (!weaknessBundle.resumeWeaknesses.isEmpty()) {
            return weaknessBundle.resumeWeaknesses.get(0);
        }
        return "先补齐最影响投递效率的短板。";
    }

    private String inferSkillOrProjectFirst(WeaknessBundle weaknessBundle) {
        String combined = weaknessBundle.flattenForPrompt();
        if (containsAny(combined, "项目", "实习", "经历", "作品")) {
            return "PROJECT_FIRST";
        }
        if (containsAny(combined, "技能", "关键词", "技术", "知识")) {
            return "SKILL_FIRST";
        }
        return "BALANCED";
    }

    private AiTextGenerationResult polishWithAi(String sectionName, String text, UserProfile userProfile, JobPost jobPost) {
        if (!StringUtils.hasText(text)) {
            return new AiTextGenerationResult(
                    "",
                    AiGenerationMetadata.fallback(aiFacadeService.getConfiguredModelName(), "Learning steps are empty. AI generation was skipped.")
            );
        }

        AiTextGenerationResult aiResult = aiFacadeService.rewriteSection(sectionName, text, jobPost, userProfile);
        if (aiResult.getMetadata().isAiUsed() && StringUtils.hasText(aiResult.getText())) {
            return aiResult;
        }

        String fallbackReason = defaultIfEmpty(aiResult.getMetadata().getFallbackReason(), "AI call failed. Local strength plan steps were used.");
        String modelName = defaultIfEmpty(aiResult.getMetadata().getModelName(), "local-rule");
        return new AiTextGenerationResult(
                text,
                AiGenerationMetadata.fallback(
                        modelName,
                        buildFallbackReason(aiResult.getMetadata(), fallbackReason),
                        aiResult.getMetadata().getPrimaryFailureReason(),
                        aiResult.getMetadata().getSecondaryFailureReason(),
                        aiResult.getMetadata().isTimeoutOccurred()
                )
        );
    }

    private List<String> buildDeliveryChecklist(WeaknessBundle weaknessBundle) {
        List<String> checklist = new ArrayList<>();
        for (String item : weaknessBundle.matchActionSuggestions) {
            if (checklist.size() >= 5) break;
            checklist.add("优先执行：" + item);
        }
        for (String item : weaknessBundle.matchWeaknesses) {
            if (checklist.size() >= 5) break;
            checklist.add("补齐岗位匹配短板：" + item);
        }
        for (String item : weaknessBundle.profileWeaknesses) {
            if (checklist.size() >= 5) break;
            checklist.add("补齐画像能力项：" + item);
        }
        for (String item : weaknessBundle.resumeWeaknesses) {
            if (checklist.size() >= 5) break;
            checklist.add("修复简历问题：" + item);
        }
        if (checklist.isEmpty()) {
            checklist.add("完成 1 个可展示项目，并形成可量化成果描述。");
            checklist.add("完成简历优化并进行至少 1 次模拟面试。");
        }
        return checklist;
    }

    private List<LearningPathResourceView> parseResourceItems(String rawResources) {
        List<String> lines = splitStructuredItems(rawResources, MAX_RESOURCE_ITEMS);
        List<LearningPathResourceView> result = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split("\\|");
            String title = stripListPrefix(defaultText(parts.length > 0 ? parts[0] : ""));
            String type = "学习资源";
            String stage = "通用";
            String link = "-";

            for (int i = 1; i < parts.length; i++) {
                String part = defaultText(parts[i]);
                if (part.contains("类型")) {
                    type = valueAfterColon(part, "学习资源");
                } else if (part.contains("阶段")) {
                    stage = valueAfterColon(part, "通用");
                } else if (part.contains("链接") || part.contains("url") || part.contains("URL")) {
                    link = valueAfterColon(part, "-");
                } else if (part.startsWith("http")) {
                    link = part;
                }
            }

            if (!StringUtils.hasText(title)) {
                title = "未命名资源";
            }
            result.add(new LearningPathResourceView(title, defaultIfEmpty(type, "学习资源"), defaultIfEmpty(link, "-"),
                    defaultIfEmpty(stage, "通用")));
            if (result.size() >= MAX_RESOURCE_ITEMS) {
                break;
            }
        }
        return result;
    }

    private String valueAfterColon(String text, String defaultValue) {
        int idx = text.indexOf(':');
        if (idx < 0) idx = text.indexOf('：');
        if (idx < 0) return defaultValue;
        String value = text.substring(idx + 1).trim();
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private List<String> splitStructuredItems(String text, int maxItems) {
        Set<String> result = new LinkedHashSet<>();
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        String normalized = text.replace("\r", "");
        String[] lines = normalized.split("\n");
        for (String line : lines) {
            String cleaned = stripListPrefix(defaultText(line));
            if (!StringUtils.hasText(cleaned)) {
                continue;
            }
            result.add(cleaned);
            if (result.size() >= maxItems) {
                return new ArrayList<>(result);
            }
        }

        if (result.isEmpty()) {
            String[] parts = normalized.split("[；;。]");
            for (String part : parts) {
                String cleaned = stripListPrefix(defaultText(part));
                if (!StringUtils.hasText(cleaned)) {
                    continue;
                }
                result.add(cleaned);
                if (result.size() >= maxItems) {
                    break;
                }
            }
        }

        return new ArrayList<>(result);
    }

    private String stripListPrefix(String text) {
        return defaultText(text).replaceFirst("^([\\-•*]|\\d+[\\.、\\)]|[一二三四五六七八九十]+[、\\)])\\s*", "").trim();
    }

    private List<String> dedupe(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String item : source) {
            String cleaned = defaultText(item);
            if (StringUtils.hasText(cleaned)) {
                set.add(cleaned);
            }
        }
        return new ArrayList<>(set);
    }

    private List<LearningResource> queryResourcesByDirection(String direction) {
        String key = normalizeDirectionKey(direction);
        List<LearningResource> byDirection = learningResourceRepository
                .findTop20ByTargetDirectionContainingIgnoreCaseOrderByUpdatedAtDescIdDesc(key);
        if (!byDirection.isEmpty()) {
            return byDirection;
        }
        return learningResourceRepository.findTop20ByOrderByUpdatedAtDescIdDesc();
    }

    private String resolveDirection(UserProfile userProfile, JobPost jobPost) {
        if (StringUtils.hasText(jobPost.getJobDirection())) {
            return jobPost.getJobDirection().trim();
        }
        if (StringUtils.hasText(userProfile.getTargetDirection())) {
            return userProfile.getTargetDirection().trim();
        }
        return "通用方向";
    }

    private String normalizeDirectionKey(String direction) {
        String value = defaultText(direction).toLowerCase(Locale.ROOT);
        if (value.contains("后端")) {
            return "后端";
        }
        if (value.contains("前端")) {
            return "前端";
        }
        if (value.contains("产品")) {
            return "产品";
        }
        return defaultText(direction);
    }

    private boolean isBackend(String direction) {
        return defaultText(direction).contains("后端");
    }

    private boolean isFrontend(String direction) {
        return defaultText(direction).contains("前端");
    }

    private boolean isProduct(String direction) {
        return defaultText(direction).contains("产品");
    }

    private boolean directionAligned(UserProfile userProfile, JobPost jobPost) {
        String profileDirection = defaultText(userProfile.getTargetDirection()).toLowerCase(Locale.ROOT);
        String jobDirection = defaultText(jobPost.getJobDirection()).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(profileDirection) || !StringUtils.hasText(jobDirection)) {
            return true;
        }
        return profileDirection.contains(jobDirection) || jobDirection.contains(profileDirection);
    }

    private boolean containsAny(String text, String... keywords) {
        String source = defaultText(text);
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private List<LearningPathResourceView> parseResourceItemsNormalized(String rawResources) {
        List<String> lines = splitStructuredItems(rawResources, MAX_RESOURCE_ITEMS);
        List<LearningPathResourceView> result = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split("\\|");
            String title = stripListPrefix(defaultText(parts.length > 0 ? parts[0] : ""));
            String type = "学习资源";
            String stage = "通用";
            String link = "-";
            String summary = "";
            String originalTitle = "";

            for (int i = 1; i < parts.length; i++) {
                String part = defaultText(parts[i]);
                if (part.contains("类型")) {
                    type = valueAfterColon(part, "学习资源");
                } else if (part.contains("阶段")) {
                    stage = valueAfterColon(part, "通用");
                } else if (part.contains("摘要")) {
                    summary = valueAfterColon(part, "");
                } else if (part.contains("原始标题")) {
                    originalTitle = valueAfterColon(part, "");
                } else if (part.contains("链接") || part.contains("url") || part.contains("URL")) {
                    link = valueAfterColon(part, "-");
                } else if (part.startsWith("http")) {
                    link = part;
                }
            }

            if (!StringUtils.hasText(title)) {
                title = "未命名资源";
            }
            result.add(new LearningPathResourceView(
                    title,
                    defaultIfEmpty(type, "学习资源"),
                    defaultIfEmpty(link, "-"),
                    defaultIfEmpty(stage, "通用"),
                    summary,
                    originalTitle
            ));
            if (result.size() >= MAX_RESOURCE_ITEMS) {
                break;
            }
        }
        return result;
    }

    private String defaultIfEmpty(String text, String defaultValue) {
        return StringUtils.hasText(text) ? text.trim() : defaultValue;
    }

    private String sanitizeInlineValue(String value) {
        return defaultText(value).replace("|", "·").replace("\n", " / ");
    }

    private String buildFallbackReason(AiGenerationMetadata metadata, String defaultReason) {
        String fallback = defaultText(metadata.getFallbackReason());
        String primary = defaultText(metadata.getPrimaryFailureReason());
        String secondary = defaultText(metadata.getSecondaryFailureReason());
        boolean timeout = metadata.isTimeoutOccurred();

        if (!StringUtils.hasText(fallback)
                && !StringUtils.hasText(primary)
                && !StringUtils.hasText(secondary)
                && !timeout) {
            return "";
        }

        String baseReason = fallback;
        if (!StringUtils.hasText(baseReason)) {
            if (metadata.getGenerationSource() == GenerationSource.QWEN) {
                baseReason = "Gemini failed. Switched to the Qwen backup channel.";
            } else if (metadata.getGenerationSource() == GenerationSource.FALLBACK) {
                baseReason = defaultIfEmpty(defaultReason, "Gemini and Qwen both failed. Switched to local fallback.");
            }
        }

        StringBuilder builder = new StringBuilder(baseReason);
        if (StringUtils.hasText(primary)) {
            builder.append(" | Gemini(")
                    .append(metadata.getPrimaryFailureLayer())
                    .append("/")
                    .append(metadata.getPrimaryFailureType())
                    .append("): ")
                    .append(primary);
        }
        if (StringUtils.hasText(secondary)) {
            builder.append(" | Qwen(")
                    .append(metadata.getSecondaryFailureLayer())
                    .append("/")
                    .append(metadata.getSecondaryFailureType())
                    .append("): ")
                    .append(secondary);
        }
        if (timeout) {
            builder.append(" | timeout occurred");
        }
        return builder.toString();
    }

    private static final class WeaknessBundle {
        private final List<String> profileWeaknesses;
        private final List<String> matchWeaknesses;
        private final List<String> resumeWeaknesses;
        private final List<String> matchActionSuggestions;

        private WeaknessBundle(List<String> profileWeaknesses, List<String> matchWeaknesses,
                               List<String> resumeWeaknesses, List<String> matchActionSuggestions) {
            this.profileWeaknesses = profileWeaknesses;
            this.matchWeaknesses = matchWeaknesses;
            this.resumeWeaknesses = resumeWeaknesses;
            this.matchActionSuggestions = matchActionSuggestions;
        }

        private String flattenForPrompt() {
            List<String> all = new ArrayList<>();
            all.addAll(matchWeaknesses);
            all.addAll(profileWeaknesses);
            all.addAll(resumeWeaknesses);
            if (all.isEmpty()) {
                all.add("当前短板不明显，建议继续强化项目深度与成果量化。");
            }
            return String.join("；", all);
        }
    }
}
