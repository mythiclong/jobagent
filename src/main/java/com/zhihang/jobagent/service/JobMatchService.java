package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.JobMatchPreferenceInput;
import com.zhihang.jobagent.dto.JobMatchResult;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.ResumeEnhanceRecordRepository;
import com.zhihang.jobagent.repository.ResumeReviewRepository;
import com.zhihang.jobagent.support.JobSalaryNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobMatchService {

    private static final int MAX_DIRECTION_SCORE = 28;
    private static final int MAX_SKILL_SCORE = 24;
    private static final int MAX_PROJECT_SCORE = 16;
    private static final int MAX_EDUCATION_SCORE = 12;
    private static final int MAX_CITY_SCORE = 10;
    private static final int MAX_RESUME_MATURITY_SCORE = 6;
    private static final int MAX_DATA_TRUST_SCORE = 4;
    private static final int MAX_SALARY_SUPPORT_SCORE = 2;
    private static final Pattern SALARY_RANGE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([kK千]?)\\s*[-~到至]\\s*(\\d+(?:\\.\\d+)?)\\s*([kK千]?)");
    private static final Pattern SALARY_SINGLE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([kK千]?)");

    private static final Pattern FALLBACK_SKILL_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9.+#/_-]{1,18}|[\\u4E00-\\u9FFF]{2,8}");
    private static final Pattern QUANTIFIED_RESULT_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?%|\\d+\\+?|\\d+个|\\d+项|\\d+次|\\d+天|\\d+周|\\d+月|\\d+万|\\d+千)");
    private static final Pattern CITY_PATTERN = Pattern.compile("([\\u4E00-\\u9FFF]{2,8})(市|州|盟)");
    private static final Pattern PROVINCE_PATTERN = Pattern.compile("([\\u4E00-\\u9FFF]{2,8})(省|自治区|特别行政区|市)");

    private static final Set<String> GENERIC_SKILLS = Set.of(
            "沟通协作", "文档表达", "Excel", "Office", "学习能力"
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "熟悉", "了解", "负责", "岗位", "工作", "能力", "经验", "优先", "具备", "相关", "掌握",
            "基础", "良好", "团队", "同学", "方向", "实习", "校招", "招聘", "职位", "要求", "参与",
            "公司", "表达", "学习", "执行", "推进", "支持", "能够", "具有"
    );

    private static final Map<String, List<String>> SKILL_SYNONYMS = new LinkedHashMap<>();
    private static final Map<DirectionKey, Set<String>> CORE_SKILLS_BY_DIRECTION = new LinkedHashMap<>();
    private static final Map<DirectionKey, Set<DirectionKey>> ADJACENT_DIRECTIONS = new LinkedHashMap<>();

    static {
        registerSkill("Java", "java", "jdk", "jvm");
        registerSkill("Spring Boot", "spring boot", "springboot");
        registerSkill("Spring", "spring", "spring mvc", "spring cloud");
        registerSkill("MySQL", "mysql");
        registerSkill("SQL", "sql", "数据库");
        registerSkill("Redis", "redis");
        registerSkill("微服务", "微服务", "microservice", "microservices");
        registerSkill("Linux", "linux", "shell");
        registerSkill("REST API", "api", "rest", "接口设计", "接口开发");
        registerSkill("Docker", "docker", "容器");
        registerSkill("Git", "git", "github", "gitlab");
        registerSkill("JavaScript", "javascript", "js");
        registerSkill("TypeScript", "typescript", "ts");
        registerSkill("React", "react");
        registerSkill("Vue", "vue");
        registerSkill("HTML/CSS", "html", "css", "html5", "css3");
        registerSkill("Node.js", "node", "node.js");
        registerSkill("前端工程化", "webpack", "vite", "前端工程化", "构建工具");
        registerSkill("浏览器原理", "浏览器", "dom", "bom");
        registerSkill("Python", "python");
        registerSkill("Pandas", "pandas");
        registerSkill("数据分析", "数据分析", "analysis", "分析");
        registerSkill("Tableau/BI", "tableau", "power bi", "bi");
        registerSkill("Excel", "excel", "office");
        registerSkill("PRD", "prd");
        registerSkill("需求分析", "需求分析", "需求拆解", "需求管理");
        registerSkill("原型设计", "原型", "axure", "figma", "墨刀");
        registerSkill("用户研究", "用户研究", "用户访谈", "用户洞察");
        registerSkill("竞品分析", "竞品", "竞品分析");
        registerSkill("测试用例", "测试用例", "test case");
        registerSkill("接口测试", "接口测试", "api test");
        registerSkill("自动化测试", "自动化测试", "automation", "selenium");
        registerSkill("JMeter", "jmeter", "性能测试");
        registerSkill("Selenium", "selenium");
        registerSkill("沟通协作", "沟通", "协作", "团队合作");
        registerSkill("文档表达", "文档", "表达", "汇报");
        registerSkill("学习能力", "学习能力", "自驱", "自学");

        CORE_SKILLS_BY_DIRECTION.put(DirectionKey.BACKEND, Set.of(
                "Java", "Spring Boot", "Spring", "MySQL", "SQL", "Redis", "微服务", "Linux", "REST API", "Docker", "Git"
        ));
        CORE_SKILLS_BY_DIRECTION.put(DirectionKey.FRONTEND, Set.of(
                "JavaScript", "TypeScript", "React", "Vue", "HTML/CSS", "Node.js", "前端工程化", "浏览器原理", "Git"
        ));
        CORE_SKILLS_BY_DIRECTION.put(DirectionKey.PRODUCT, Set.of(
                "PRD", "需求分析", "原型设计", "用户研究", "竞品分析", "数据分析", "Excel", "沟通协作", "文档表达"
        ));
        CORE_SKILLS_BY_DIRECTION.put(DirectionKey.TEST, Set.of(
                "测试用例", "接口测试", "自动化测试", "JMeter", "Selenium", "Java", "Python", "SQL", "Git"
        ));
        CORE_SKILLS_BY_DIRECTION.put(DirectionKey.DATA, Set.of(
                "Python", "SQL", "Pandas", "数据分析", "Tableau/BI", "Excel", "沟通协作"
        ));

        ADJACENT_DIRECTIONS.put(DirectionKey.BACKEND, Set.of(DirectionKey.TEST, DirectionKey.DATA));
        ADJACENT_DIRECTIONS.put(DirectionKey.FRONTEND, Set.of(DirectionKey.TEST));
        ADJACENT_DIRECTIONS.put(DirectionKey.PRODUCT, Set.of(DirectionKey.DATA));
        ADJACENT_DIRECTIONS.put(DirectionKey.TEST, Set.of(DirectionKey.BACKEND, DirectionKey.FRONTEND));
        ADJACENT_DIRECTIONS.put(DirectionKey.DATA, Set.of(DirectionKey.BACKEND, DirectionKey.PRODUCT));
        CORE_SKILLS_BY_DIRECTION.put(DirectionKey.OPERATIONS, Set.of("PRD", "Excel"));
        CORE_SKILLS_BY_DIRECTION.put(DirectionKey.DESIGN, Set.of("PRD"));
        ADJACENT_DIRECTIONS.put(DirectionKey.OPERATIONS, Set.of(DirectionKey.PRODUCT, DirectionKey.DATA));
        ADJACENT_DIRECTIONS.put(DirectionKey.DESIGN, Set.of(DirectionKey.FRONTEND, DirectionKey.PRODUCT));
        ADJACENT_DIRECTIONS.put(DirectionKey.OTHER, Set.of());
        ADJACENT_DIRECTIONS.put(DirectionKey.UNKNOWN, Set.of());
    }

    private final ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;
    private final ResumeReviewRepository resumeReviewRepository;

    public JobMatchService(ResumeEnhanceRecordRepository resumeEnhanceRecordRepository,
                           ResumeReviewRepository resumeReviewRepository) {
        this.resumeEnhanceRecordRepository = resumeEnhanceRecordRepository;
        this.resumeReviewRepository = resumeReviewRepository;
    }

    public List<JobMatchResult> matchJobs(UserProfile userProfile, List<JobPost> jobPosts) {
        return matchJobs(userProfile, jobPosts, JobMatchPreferenceInput.fromProfile(userProfile));
    }

    public List<JobMatchResult> matchJobs(UserProfile userProfile,
                                          List<JobPost> jobPosts,
                                          JobMatchPreferenceInput preferenceInput) {
        ResumeEnhanceRecord latestEnhance = null;
        ResumeReview latestReview = null;
        if (userProfile != null && userProfile.getId() != null) {
            List<ResumeEnhanceRecord> enhanceRecords = resumeEnhanceRecordRepository
                    .findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(userProfile.getId());
            if (!enhanceRecords.isEmpty()) {
                latestEnhance = enhanceRecords.get(0);
            }
            List<ResumeReview> reviewRecords = resumeReviewRepository
                    .findTop10ByUserProfileIdOrderByIdDesc(userProfile.getId());
            if (!reviewRecords.isEmpty()) {
                latestReview = reviewRecords.get(0);
            }
        }

        UserProfile effectiveProfile = buildEffectiveProfile(userProfile, preferenceInput);
        CandidateProfileFeatures profileFeatures = buildProfileFeatures(effectiveProfile, latestEnhance, latestReview);
        List<JobMatchResult> results = new ArrayList<>();
        for (JobPost jobPost : jobPosts) {
            JobMatchResult result = buildMatchResult(effectiveProfile, profileFeatures, jobPost);
            applyPreferenceRuleAdjustments(result, preferenceInput);
            results.add(result);
        }
        results.sort(Comparator
                .comparingInt((JobMatchResult result) -> compatibilityRank(result.getCompatibilityLevel()))
                .reversed()
                .thenComparing(JobMatchResult::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(result -> safeText(result.getJobPost() == null ? "" : result.getJobPost().getDisplayTitle())));
        return results;
    }

    private JobMatchResult buildMatchResult(UserProfile userProfile,
                                            CandidateProfileFeatures profile,
                                            JobPost jobPost) {
        JobFeatures job = buildJobFeatures(jobPost);
        HardConditionAssessment hardCondition = assessHardConditions(profile, job);
        DimensionScores scores = scoreDimensions(profile, job, hardCondition);

        List<String> positives = new ArrayList<>();
        positives.add(buildCompatibilityHeadline(hardCondition));
        positives.addAll(scores.positiveSignals());
        positives = dedupeAndLimit(positives, 5);

        List<String> weaknesses = new ArrayList<>();
        weaknesses.addAll(scores.weaknesses());
        if (hardCondition.compatibilityLevel() == CompatibilityLevel.WEAK) {
            weaknesses.add("硬条件判断为弱适配，建议先补齐基础适配项再集中投递。");
        } else if (hardCondition.compatibilityLevel() == CompatibilityLevel.MISMATCH) {
            weaknesses.add("硬条件判断为明显不适合，当前更适合作为补强参考岗而非优先投递岗。");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("当前未发现明显结构化短板，可以优先进入简历优化与面试准备。");
        }
        weaknesses = dedupeAndLimit(weaknesses, 6);

        List<String> deductions = new ArrayList<>();
        deductions.addAll(hardCondition.deductionReasons());
        deductions.addAll(scores.deductions());
        deductions = dedupeAndLimit(deductions, 6);
        if (deductions.isEmpty()) {
            deductions.add("当前未触发明显扣分项，结果主要由方向、技能和项目匹配驱动。");
        }

        List<String> nextActions = buildNextActions(userProfile, jobPost, profile, job, scores, hardCondition);
        int totalScore = clamp(scores.rawTotal() + hardCondition.basePenalty(), 0, 100);
        totalScore = Math.min(totalScore, hardCondition.scoreCap());

        JobMatchResult result = new JobMatchResult();
        result.setJobPost(jobPost);
        result.setMatchScore(totalScore);
        result.setCompatibilityLevel(hardCondition.compatibilityLevel().name());
        result.setHardConditionSummary(joinLines(hardCondition.summaryItems()));
        result.setScoreBreakdown(buildScoreBreakdown(scores));
        result.setMatchReason(joinLines(positives));
        result.setWeaknessAnalysis(joinLines(weaknesses));
        result.setDeductionReasons(joinLines(deductions));
        result.setImprovementSuggestion(joinLines(nextActions));
        result.setDirectionScore(scores.directionScore());
        result.setSkillScore(scores.skillScore());
        result.setProjectScore(scores.projectScore());
        result.setEducationScore(scores.educationScore());
        result.setCityScore(scores.cityScore());
        result.setResumeMaturityScore(scores.resumeMaturityScore());
        result.setDataTrustScore(scores.dataTrustScore());
        result.setSalarySupportScore(scores.salarySupportScore());
        result.setPositiveSignals(positives);
        result.setCoreWeaknesses(weaknesses);
        result.setDeductionReasonItems(deductions);
        result.setNextActionItems(nextActions);
        result.setCompositeRankScore(totalScore);
        return result;
    }

    private UserProfile buildEffectiveProfile(UserProfile userProfile, JobMatchPreferenceInput preferenceInput) {
        if (userProfile == null || preferenceInput == null || !preferenceInput.hasAnyPreference()) {
            return userProfile;
        }

        UserProfile effectiveProfile = new UserProfile();
        effectiveProfile.setId(userProfile.getId());
        effectiveProfile.setUserAccountId(userProfile.getUserAccountId());
        effectiveProfile.setStudentName(userProfile.getStudentName());
        effectiveProfile.setGrade(userProfile.getGrade());
        effectiveProfile.setMajor(userProfile.getMajor());
        effectiveProfile.setTargetRole(userProfile.getTargetRole());
        effectiveProfile.setTargetDirection(userProfile.getTargetDirection());
        effectiveProfile.setSkills(userProfile.getSkills());
        effectiveProfile.setProjectExperience(userProfile.getProjectExperience());
        effectiveProfile.setTargetCity(userProfile.getTargetCity());
        effectiveProfile.setAcceptableCities(userProfile.getAcceptableCities());
        effectiveProfile.setExpectedSalary(userProfile.getExpectedSalary());
        effectiveProfile.setAcceptableJobNature(userProfile.getAcceptableJobNature());
        effectiveProfile.setTargetIndustry(userProfile.getTargetIndustry());
        effectiveProfile.setCareerGoal(userProfile.getCareerGoal());

        if (StringUtils.hasText(preferenceInput.getPreferredCity())) {
            effectiveProfile.setTargetCity(preferenceInput.getPreferredCity());
        }
        if (StringUtils.hasText(preferenceInput.getAcceptableCities())) {
            effectiveProfile.setAcceptableCities(preferenceInput.getAcceptableCities());
        }
        if (StringUtils.hasText(preferenceInput.getPreferredSalary())) {
            effectiveProfile.setExpectedSalary(preferenceInput.getPreferredSalary());
        }
        if (StringUtils.hasText(preferenceInput.getPreferredRole())) {
            effectiveProfile.setTargetRole(preferenceInput.getPreferredRole());
        }
        if (StringUtils.hasText(preferenceInput.getPreferredJobNature())) {
            effectiveProfile.setAcceptableJobNature(preferenceInput.getPreferredJobNature());
        }
        if (StringUtils.hasText(preferenceInput.getPreferredIndustry())) {
            effectiveProfile.setTargetIndustry(preferenceInput.getPreferredIndustry());
        }
        return effectiveProfile;
    }

    private void applyPreferenceRuleAdjustments(JobMatchResult result, JobMatchPreferenceInput preferenceInput) {
        if (result == null) {
            return;
        }
        result.setPreferenceSummary(preferenceInput == null ? "" : preferenceInput.toSummaryText());
        if (preferenceInput == null || !preferenceInput.hasAnyPreference() || result.getJobPost() == null) {
            result.setCompositeRankScore(result.getMatchScore());
            return;
        }

        List<String> positives = new ArrayList<>(result.getPositiveSignals());
        List<String> weaknesses = new ArrayList<>(result.getCoreWeaknesses());
        List<String> deductions = new ArrayList<>(result.getDeductionReasonItems());
        List<String> nextActions = new ArrayList<>(result.getNextActionItems());
        List<String> breakdownItems = new ArrayList<>(result.getScoreBreakdownItems());

        int scoreDelta = 0;
        int salaryDelta = applySalaryPreferenceAdjustment(result, preferenceInput, positives, weaknesses, deductions, nextActions);
        if (salaryDelta != 0) {
            breakdownItems.add("期望薪资修正 " + formatSigned(salaryDelta));
            scoreDelta += salaryDelta;
        }

        int roleDelta = applyRolePreferenceAdjustment(result.getJobPost(), preferenceInput, positives, deductions);
        if (roleDelta != 0) {
            breakdownItems.add("目标岗位偏好修正 " + formatSigned(roleDelta));
            scoreDelta += roleDelta;
        }

        int natureDelta = applyNaturePreferenceAdjustment(result.getJobPost(), preferenceInput, positives, weaknesses, deductions);
        if (natureDelta != 0) {
            breakdownItems.add("岗位性质偏好修正 " + formatSigned(natureDelta));
            scoreDelta += natureDelta;
        }

        int cityDelta = applyAcceptableCityAdjustment(result.getJobPost(), preferenceInput, positives, deductions);
        if (cityDelta != 0) {
            breakdownItems.add("城市偏好修正 " + formatSigned(cityDelta));
            scoreDelta += cityDelta;
        }

        int industryDelta = applyIndustryPreferenceAdjustment(result.getJobPost(), preferenceInput, positives);
        if (industryDelta != 0) {
            breakdownItems.add("行业偏好修正 " + formatSigned(industryDelta));
            scoreDelta += industryDelta;
        }

        if (scoreDelta < 0) {
            nextActions.add("按当前偏好重新匹配：优先锁定更接近你城市、薪资和岗位目标的候选岗位。");
        }

        int updatedScore = clamp(defaultInt(result.getMatchScore()) + scoreDelta, 0, 100);
        updatedScore = capScoreByCompatibility(updatedScore, result.getCompatibilityLevel());

        positives = dedupeAndLimit(positives, 6);
        weaknesses = dedupeAndLimit(weaknesses, 6);
        deductions = dedupeAndLimit(deductions, 6);
        nextActions = dedupeAndLimit(nextActions, 5);
        breakdownItems = dedupeAndLimit(breakdownItems, 10);

        result.setMatchScore(updatedScore);
        result.setCompositeRankScore(updatedScore);
        result.setPositiveSignals(positives);
        result.setCoreWeaknesses(weaknesses);
        result.setDeductionReasonItems(deductions);
        result.setNextActionItems(nextActions);
        result.setMatchReason(joinLines(positives));
        result.setWeaknessAnalysis(joinLines(weaknesses));
        result.setDeductionReasons(joinLines(deductions));
        result.setImprovementSuggestion(joinLines(nextActions));
        result.setScoreBreakdown(joinLines(breakdownItems));
    }

    private int applySalaryPreferenceAdjustment(JobMatchResult result,
                                                JobMatchPreferenceInput preferenceInput,
                                                List<String> positives,
                                                List<String> weaknesses,
                                                List<String> deductions,
                                                List<String> nextActions) {
        SalaryPreference salaryPreference = parseSalaryPreference(preferenceInput.getPreferredSalary());
        if (salaryPreference == null || result.getJobPost() == null) {
            return 0;
        }

        SalaryPreferenceRelation relation = compareSalaryPreference(salaryPreference, result.getJobPost());
        int originalSalaryScore = defaultInt(result.getSalarySupportScore());
        int updatedSalaryScore = originalSalaryScore;
        switch (relation) {
            case MATCH -> {
                updatedSalaryScore = clamp(originalSalaryScore + 1, -2, MAX_SALARY_SUPPORT_SCORE);
                positives.add("薪资区间更接近你本次的期望范围。");
            }
            case ABOVE_EXPECTATION -> {
                updatedSalaryScore = clamp(originalSalaryScore + 1, -2, MAX_SALARY_SUPPORT_SCORE);
                positives.add("岗位薪资高于你的期望上沿，可作为优先准备岗位。");
            }
            case BELOW_EXPECTATION -> {
                updatedSalaryScore = clamp(originalSalaryScore - 2, -2, MAX_SALARY_SUPPORT_SCORE);
                weaknesses.add("岗位薪资低于你当前的期望区间，本轮不建议排在优先投递前列。");
                deductions.add("期望薪资为 " + preferenceInput.getPreferredSalary() + "，当前岗位薪资为 "
                        + defaultIfEmpty(result.getJobPost().getSalaryRangeDisplay(), "-") + "。");
                nextActions.add("按当前薪资偏好重新筛选岗位，或先补强更高薪资段所需的能力证明。");
            }
            case UNKNOWN_WEAK -> deductions.add("岗位薪资单位待确认，本轮仅做弱参与判断，没有让薪资偏好强行影响总分。");
            case UNKNOWN -> {
            }
        }
        result.setSalarySupportScore(updatedSalaryScore);
        return updatedSalaryScore - originalSalaryScore;
    }

    private int applyRolePreferenceAdjustment(JobPost jobPost,
                                              JobMatchPreferenceInput preferenceInput,
                                              List<String> positives,
                                              List<String> deductions) {
        if (jobPost == null || !StringUtils.hasText(preferenceInput.getPreferredRole())) {
            return 0;
        }
        List<String> roleTokens = splitPreferenceTokens(preferenceInput.getPreferredRole());
        if (roleTokens.isEmpty()) {
            return 0;
        }

        String jobText = normalizeText(joinNonBlank(
                jobPost.getDisplayTitle(),
                jobPost.getJobName(),
                jobPost.getJobDirection(),
                jobPost.getDisplayTags()
        )).toLowerCase(Locale.ROOT);
        for (String token : roleTokens) {
            if (jobText.contains(token.toLowerCase(Locale.ROOT))) {
                positives.add("岗位名称更贴近你本次的目标岗位：" + token + "。");
                return 2;
            }
        }

        DirectionKey preferredDirection = normalizeDirectionKey(preferenceInput.getPreferredRole());
        DirectionKey jobDirection = normalizeDirectionKey(joinNonBlank(jobPost.getJobDirection(), jobPost.getDisplayTitle()));
        if (preferredDirection != DirectionKey.UNKNOWN && preferredDirection == jobDirection) {
            positives.add("岗位方向与本次目标岗位保持一致。");
            return 1;
        }

        deductions.add("岗位名称与本次目标岗位偏好不完全一致，优先级会略降。");
        return -1;
    }

    private int applyNaturePreferenceAdjustment(JobPost jobPost,
                                                JobMatchPreferenceInput preferenceInput,
                                                List<String> positives,
                                                List<String> weaknesses,
                                                List<String> deductions) {
        if (jobPost == null || !StringUtils.hasText(preferenceInput.getPreferredJobNature())) {
            return 0;
        }
        String jobNature = normalizeText(jobPost.getJobNatureDisplay()).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(jobNature)) {
            return 0;
        }

        for (String token : splitPreferenceTokens(preferenceInput.getPreferredJobNature())) {
            if (jobNature.contains(token.toLowerCase(Locale.ROOT))) {
                positives.add("岗位性质符合你当前可接受的范围。");
                return 1;
            }
        }

        weaknesses.add("岗位性质与本次接受范围不完全一致。");
        deductions.add("你当前接受的岗位性质为 " + preferenceInput.getPreferredJobNature()
                + "，该岗位为 " + jobPost.getJobNatureDisplay() + "。");
        return -1;
    }

    private int applyAcceptableCityAdjustment(JobPost jobPost,
                                              JobMatchPreferenceInput preferenceInput,
                                              List<String> positives,
                                              List<String> deductions) {
        if (jobPost == null || StringUtils.hasText(preferenceInput.getPreferredCity())
                || !StringUtils.hasText(preferenceInput.getAcceptableCities())) {
            return 0;
        }

        String locationText = normalizeText(jobPost.getReadableLocationDisplay()).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(locationText)) {
            return 0;
        }

        for (String token : splitPreferenceTokens(preferenceInput.getAcceptableCities())) {
            if (locationText.contains(token.toLowerCase(Locale.ROOT))) {
                positives.add("岗位地点落在你可接受的城市范围内。");
                return 1;
            }
        }

        deductions.add("岗位地点不在你当前可接受的城市范围内。");
        return -1;
    }

    private int applyIndustryPreferenceAdjustment(JobPost jobPost,
                                                  JobMatchPreferenceInput preferenceInput,
                                                  List<String> positives) {
        if (jobPost == null || !StringUtils.hasText(preferenceInput.getPreferredIndustry())) {
            return 0;
        }

        String jobText = normalizeText(joinNonBlank(
                jobPost.getCompanyNameDisplay(),
                jobPost.getDisplaySummaryFullText(),
                jobPost.getDisplayTags(),
                jobPost.getJobDescription()
        )).toLowerCase(Locale.ROOT);
        for (String token : splitPreferenceTokens(preferenceInput.getPreferredIndustry())) {
            if (jobText.contains(token.toLowerCase(Locale.ROOT))) {
                positives.add("岗位/公司信息与当前行业偏好有重合。");
                return 1;
            }
        }
        return 0;
    }

    private SalaryPreference parseSalaryPreference(String text) {
        String normalized = defaultText(text).replace(" ", "").toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized) || normalized.contains("面议")) {
            return null;
        }

        Matcher rangeMatcher = SALARY_RANGE_PATTERN.matcher(normalized);
        if (rangeMatcher.find()) {
            return new SalaryPreference(
                    toComparableSalary(rangeMatcher.group(1), rangeMatcher.group(2)),
                    toComparableSalary(rangeMatcher.group(3), rangeMatcher.group(4))
            );
        }

        Matcher singleMatcher = SALARY_SINGLE_PATTERN.matcher(normalized);
        if (!singleMatcher.find()) {
            return null;
        }

        BigDecimal value = toComparableSalary(singleMatcher.group(1), singleMatcher.group(2));
        if (value == null) {
            return null;
        }
        if (containsAny(normalized, "以下", "封顶", "以内")) {
            return new SalaryPreference(null, value);
        }
        if (containsAny(normalized, "以上", "起", "至少", "+")) {
            return new SalaryPreference(value, null);
        }
        return new SalaryPreference(value, value);
    }

    private SalaryPreferenceRelation compareSalaryPreference(SalaryPreference salaryPreference, JobPost jobPost) {
        if (salaryPreference == null || jobPost == null) {
            return SalaryPreferenceRelation.UNKNOWN;
        }
        if (!JobSalaryNormalizer.isMonthlyUnit(jobPost.getSalaryUnitType())) {
            return SalaryPreferenceRelation.UNKNOWN_WEAK;
        }

        BigDecimal jobMin = jobPost.getMonthlySalaryMin();
        BigDecimal jobMax = jobPost.getMonthlySalaryMax();
        if (jobMin == null && jobMax == null) {
            return SalaryPreferenceRelation.UNKNOWN_WEAK;
        }
        if (jobMin == null) {
            jobMin = jobMax;
        }
        if (jobMax == null) {
            jobMax = jobMin;
        }

        if (salaryPreference.min() != null && jobMax != null && jobMax.compareTo(salaryPreference.min()) < 0) {
            return SalaryPreferenceRelation.BELOW_EXPECTATION;
        }
        if (salaryPreference.max() != null && jobMin != null && jobMin.compareTo(salaryPreference.max()) > 0) {
            return SalaryPreferenceRelation.ABOVE_EXPECTATION;
        }
        return SalaryPreferenceRelation.MATCH;
    }

    private BigDecimal toComparableSalary(String rawValue, String unitToken) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        BigDecimal value = new BigDecimal(rawValue);
        if ("k".equalsIgnoreCase(defaultText(unitToken))
                || "千".equals(defaultText(unitToken))
                || value.compareTo(new BigDecimal("100")) <= 0) {
            return value.multiply(new BigDecimal("1000"));
        }
        return JobSalaryNormalizer.normalizeStoredMonthlyComparableAmount(value);
    }

    private List<String> splitPreferenceTokens(String text) {
        List<String> tokens = new ArrayList<>();
        for (String raw : defaultText(text).split("[/|,，、;；\\s]+")) {
            String value = raw.trim();
            if (StringUtils.hasText(value)) {
                tokens.add(value);
            }
        }
        return tokens;
    }

    private int capScoreByCompatibility(int score, String compatibilityLevel) {
        String normalized = defaultText(compatibilityLevel).toUpperCase(Locale.ROOT);
        if ("MISMATCH".equals(normalized)) {
            return Math.min(score, 58);
        }
        if ("WEAK".equals(normalized)) {
            return Math.min(score, 82);
        }
        return score;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String formatSigned(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private CandidateProfileFeatures buildProfileFeatures(UserProfile userProfile,
                                                          ResumeEnhanceRecord latestEnhance,
                                                          ResumeReview latestReview) {
        String targetDirectionText = joinNonBlank(
                userProfile == null ? null : userProfile.getTargetRole(),
                userProfile == null ? null : userProfile.getTargetDirection(),
                userProfile == null ? null : userProfile.getTargetIndustry(),
                userProfile == null ? null : userProfile.getCareerGoal(),
                latestEnhance == null ? null : latestEnhance.getParsedSkills(),
                latestEnhance == null ? null : latestEnhance.getParsedProjects()
        );
        String combinedResumeText = joinNonBlank(
                userProfile == null ? null : userProfile.getSkills(),
                userProfile == null ? null : userProfile.getProjectExperience(),
                userProfile == null ? null : userProfile.getCareerGoal(),
                latestEnhance == null ? null : latestEnhance.getParsedSkills(),
                latestEnhance == null ? null : latestEnhance.getParsedProjects(),
                latestEnhance == null ? null : latestEnhance.getParsedExperience(),
                latestEnhance == null ? null : latestEnhance.getOptimizedProjects(),
                latestEnhance == null ? null : latestEnhance.getOptimizedExperience(),
                latestEnhance == null ? null : latestEnhance.getFullOptimizedResume(),
                latestReview == null ? null : latestReview.getResumeText(),
                latestReview == null ? null : latestReview.getSummary()
        );
        String projectText = joinNonBlank(
                userProfile == null ? null : userProfile.getProjectExperience(),
                latestEnhance == null ? null : latestEnhance.getParsedProjects(),
                latestEnhance == null ? null : latestEnhance.getParsedExperience(),
                latestEnhance == null ? null : latestEnhance.getOptimizedProjects(),
                latestEnhance == null ? null : latestEnhance.getOptimizedExperience(),
                latestReview == null ? null : latestReview.getResumeText()
        );
        ResumeMaturity resumeMaturity = inferResumeMaturity(combinedResumeText, userProfile, latestEnhance, latestReview);

        return new CandidateProfileFeatures(
                normalizeDirectionKey(targetDirectionText),
                normalizeText(targetDirectionText),
                extractSkillTags(combinedResumeText),
                extractSkillTags(projectText),
                inferProjectStrength(projectText),
                inferEducationLevel(joinNonBlank(
                        userProfile == null ? null : userProfile.getGrade(),
                        latestEnhance == null ? null : latestEnhance.getParsedEducation(),
                        latestReview == null ? null : latestReview.getResumeText()
                )),
                normalizeLocationText(userProfile == null ? null : userProfile.getTargetCity()),
                extractProvinceToken(userProfile == null ? null : userProfile.getTargetCity()),
                extractCityToken(userProfile == null ? null : userProfile.getTargetCity()),
                inferNaturePreference(joinNonBlank(
                        userProfile == null ? null : userProfile.getAcceptableJobNature(),
                        userProfile == null ? null : userProfile.getGrade(),
                        userProfile == null ? null : userProfile.getCareerGoal(),
                        latestEnhance == null ? null : latestEnhance.getParsedBasicInfo()
                )),
                resumeMaturity,
                normalizeText(userProfile == null ? null : userProfile.getCareerGoal()),
                normalizeText(projectText),
                normalizeText(combinedResumeText)
        );
    }

    private JobFeatures buildJobFeatures(JobPost jobPost) {
        String fullText = joinNonBlank(
                jobPost.getJobName(),
                jobPost.getDisplayTitle(),
                jobPost.getJobDirection(),
                jobPost.getJobRequirements(),
                jobPost.getBonusPoints(),
                jobPost.getJobDescription(),
                jobPost.getDisplaySummary(),
                jobPost.getEducationRequirement(),
                jobPost.getJobNature(),
                jobPost.getCompanyName()
        );
        Set<String> extractedSkills = new LinkedHashSet<>(extractSkillTags(fullText));
        if (extractedSkills.isEmpty()) {
            extractedSkills.addAll(extractFallbackKeywords(fullText, 5));
        }

        DirectionKey directionKey = normalizeDirectionKey(joinNonBlank(
                jobPost.getJobDirection(),
                jobPost.getDisplayTitle(),
                jobPost.getJobRequirements(),
                jobPost.getDisplayTags(),
                jobPost.getJobDescription()
        ));
        Set<String> directionCoreSkills = new LinkedHashSet<>(CORE_SKILLS_BY_DIRECTION.getOrDefault(directionKey, Set.of()));
        Set<String> coreSkills = new LinkedHashSet<>();
        Set<String> secondarySkills = new LinkedHashSet<>();
        Set<String> genericSkills = new LinkedHashSet<>();
        for (String skill : extractedSkills) {
            if (GENERIC_SKILLS.contains(skill)) {
                genericSkills.add(skill);
            } else if (directionCoreSkills.contains(skill)) {
                coreSkills.add(skill);
            } else {
                secondarySkills.add(skill);
            }
        }
        if (coreSkills.isEmpty() && !secondarySkills.isEmpty() && !directionCoreSkills.isEmpty()) {
            for (String skill : new ArrayList<>(secondarySkills)) {
                if (directionCoreSkills.contains(skill)) {
                    coreSkills.add(skill);
                }
            }
            secondarySkills.removeAll(coreSkills);
        }

        return new JobFeatures(
                directionKey,
                normalizeText(jobPost.getJobDirection()),
                coreSkills,
                secondarySkills,
                genericSkills,
                inferEducationLevel(joinNonBlank(jobPost.getEducationRequirement(), jobPost.getSuitableGrade())),
                normalizeLocationText(jobPost.getReadableLocationDisplay()),
                extractProvinceToken(jobPost.getReadableLocationDisplay()),
                extractCityToken(jobPost.getReadableLocationDisplay()),
                inferJobNature(joinNonBlank(jobPost.getJobNatureDisplay(), jobPost.getDisplayStage(), jobPost.getJobName(), jobPost.getDisplaySummary())),
                needsProjectEvidence(fullText, directionKey),
                computeDataCompleteness(jobPost),
                JobSalaryNormalizer.isMonthlyUnit(jobPost.getSalaryUnitType()),
                averageMonthlySalary(jobPost),
                normalizeText(fullText)
        );
    }

    private HardConditionAssessment assessHardConditions(CandidateProfileFeatures profile, JobFeatures job) {
        CompatibilityLevel level = CompatibilityLevel.STRONG;
        int scoreCap = 100;
        int penalty = 0;
        List<String> summaryItems = new ArrayList<>();
        List<String> deductions = new ArrayList<>();

        DirectionRelation directionRelation = compareDirections(profile.directionKey(), job.directionKey());
        if (directionRelation == DirectionRelation.EXACT) {
            summaryItems.add("方向强适配：目标方向与岗位方向一致，允许完整参与后续打分。");
        } else if (directionRelation == DirectionRelation.ADJACENT) {
            level = CompatibilityLevel.WEAK;
            scoreCap = Math.min(scoreCap, 82);
            penalty -= 10;
            summaryItems.add("方向弱适配：方向存在迁移空间，但总分已按弱适配封顶。");
            deductions.add("岗位方向与当前目标方向不完全一致，不能只靠关键词命中拿高分。");
        } else if (directionRelation == DirectionRelation.MISMATCH) {
            level = CompatibilityLevel.MISMATCH;
            scoreCap = Math.min(scoreCap, 58);
            penalty -= 20;
            summaryItems.add("方向明显不适合：目标方向与岗位方向偏差较大，总分已按硬条件封顶。");
            deductions.add("方向不匹配，即使撞上部分技能关键词，总分也不会进入高分段。");
        } else {
            summaryItems.add("方向信息不完整：本轮按中性处理，但不会额外加方向强适配分。");
        }

        if (job.educationLevel() != EducationLevel.UNKNOWN && profile.educationLevel() != EducationLevel.UNKNOWN) {
            if (profile.educationLevel().level() >= job.educationLevel().level()) {
                summaryItems.add("学历满足：当前学历阶段满足岗位要求。");
            } else if (profile.educationLevel().level() + 1 == job.educationLevel().level()) {
                if (level == CompatibilityLevel.STRONG) {
                    level = CompatibilityLevel.WEAK;
                }
                scoreCap = Math.min(scoreCap, 72);
                penalty -= 8;
                summaryItems.add("学历弱适配：当前学历略低于岗位要求。");
                deductions.add("学历仅弱匹配，结果排序会明显下调。");
            } else {
                level = CompatibilityLevel.MISMATCH;
                scoreCap = Math.min(scoreCap, 60);
                penalty -= 14;
                summaryItems.add("学历明显不适合：岗位要求的学历层级更高。");
                deductions.add("学历不满足岗位要求，建议先优先投递更匹配的岗位。");
            }
        } else {
            summaryItems.add("学历信息不足：暂按中性处理。");
        }

        CityRelation cityRelation = compareCities(profile, job);
        if (cityRelation == CityRelation.EXACT) {
            summaryItems.add("城市一致：岗位地点与目标城市一致。");
        } else if (cityRelation == CityRelation.PROVINCE_MATCH) {
            scoreCap = Math.min(scoreCap, 94);
            penalty -= 2;
            summaryItems.add("城市弱适配：岗位地点与目标城市同省，可接受但不是最优。");
        } else if (cityRelation == CityRelation.MISMATCH) {
            if (level == CompatibilityLevel.STRONG) {
                level = CompatibilityLevel.WEAK;
            }
            scoreCap = Math.min(scoreCap, 88);
            penalty -= 8;
            summaryItems.add("城市偏差：岗位地点与目标城市不一致。");
            deductions.add("目标城市与岗位地点完全不符，排序已做明显惩罚。");
        } else {
            summaryItems.add("城市未知：岗位或用户城市信息不足，不做强惩罚。");
        }

        NatureRelation natureRelation = compareNature(profile.naturePreference(), job.natureKey());
        if (natureRelation == NatureRelation.ALIGNED) {
            summaryItems.add("岗位性质匹配：当前阶段与岗位性质基本一致。");
        } else if (natureRelation == NatureRelation.UNRELATED) {
            if (level == CompatibilityLevel.STRONG) {
                level = CompatibilityLevel.WEAK;
            }
            scoreCap = Math.min(scoreCap, 84);
            penalty -= 5;
            summaryItems.add("岗位性质弱适配：当前阶段偏好与岗位性质存在偏差。");
            deductions.add("岗位性质与当前求职阶段不完全一致，建议谨慎看待排序结果。");
        } else {
            summaryItems.add("岗位性质未知：暂按中性处理。");
        }

        return new HardConditionAssessment(level, scoreCap, penalty, dedupe(summaryItems), dedupe(deductions), directionRelation, cityRelation, natureRelation);
    }

    private DimensionScores scoreDimensions(CandidateProfileFeatures profile,
                                           JobFeatures job,
                                           HardConditionAssessment hardCondition) {
        List<String> positives = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> deductions = new ArrayList<>();

        int directionScore = scoreDirection(profile, job, hardCondition.directionRelation(), positives, deductions);
        SkillMatch skillMatch = scoreSkills(profile, job, positives, weaknesses, deductions);
        int projectScore = scoreProjectExperience(profile, job, positives, weaknesses, deductions);
        int educationScore = scoreEducation(profile, job, positives, deductions);
        int cityScore = scoreCity(profile, job, hardCondition.cityRelation(), positives, deductions);
        int resumeMaturityScore = scoreResumeMaturity(profile.resumeMaturity(), positives, weaknesses);
        int dataTrustScore = scoreDataTrust(job, positives, deductions);
        int salarySupportScore = scoreSalarySupport(job, positives, deductions);

        return new DimensionScores(
                directionScore,
                skillMatch.totalScore(),
                projectScore,
                educationScore,
                cityScore,
                resumeMaturityScore,
                dataTrustScore,
                salarySupportScore,
                skillMatch.matchedCoreSkills(),
                skillMatch.matchedSecondarySkills(),
                skillMatch.missingCoreSkills(),
                dedupeAndLimit(positives, 6),
                dedupeAndLimit(weaknesses, 6),
                dedupeAndLimit(deductions, 6)
        );
    }

    private int scoreDirection(CandidateProfileFeatures profile,
                               JobFeatures job,
                               DirectionRelation relation,
                               List<String> positives,
                               List<String> deductions) {
        return switch (relation) {
            case EXACT -> {
                positives.add("方向匹配度高：目标方向与岗位方向高度一致。");
                yield MAX_DIRECTION_SCORE;
            }
            case ADJACENT -> {
                positives.add("方向有迁移空间：当前方向与岗位方向存在部分能力重叠。");
                yield 13;
            }
            case UNKNOWN -> 14;
            case MISMATCH -> {
                deductions.add("方向匹配分为 0，岗位方向与当前求职目标差异明显。");
                yield 0;
            }
        };
    }

    private SkillMatch scoreSkills(CandidateProfileFeatures profile,
                                   JobFeatures job,
                                   List<String> positives,
                                   List<String> weaknesses,
                                   List<String> deductions) {
        Set<String> matchedCoreSkills = intersect(profile.skillTags(), job.coreSkills());
        Set<String> matchedSecondarySkills = intersect(profile.skillTags(), job.secondarySkills());
        Set<String> matchedGenericSkills = intersect(profile.skillTags(), job.genericSkills());
        Set<String> missingCoreSkills = subtract(job.coreSkills(), profile.skillTags());

        int coreScore = Math.min(14, matchedCoreSkills.size() * 7);
        int secondaryScore = Math.min(6, matchedSecondarySkills.size() * 3);
        int genericScore = Math.min(4, matchedGenericSkills.size() * 2);
        int totalScore = coreScore + secondaryScore + genericScore;

        if (!matchedCoreSkills.isEmpty()) {
            positives.add("核心技能命中：" + joinNaturalList(matchedCoreSkills, 3) + "。");
        }
        if (!matchedSecondarySkills.isEmpty()) {
            positives.add("次要技能也有覆盖：" + joinNaturalList(matchedSecondarySkills, 2) + "。");
        }
        if (!missingCoreSkills.isEmpty()) {
            weaknesses.add("缺少核心技能：" + joinNaturalList(missingCoreSkills, 3) + "。");
        }
        if (!job.coreSkills().isEmpty() && matchedCoreSkills.isEmpty()) {
            deductions.add("核心技能命中不足，技能分不会被泛技能关键词拉高。");
            totalScore = Math.min(totalScore, 8);
        }
        if (job.coreSkills().isEmpty() && job.secondarySkills().isEmpty()) {
            totalScore = Math.max(totalScore, 10);
            positives.add("岗位技能描述较少，本轮主要参考方向、学历和项目经历。");
        }

        return new SkillMatch(
                clamp(totalScore, 0, MAX_SKILL_SCORE),
                toOrderedList(matchedCoreSkills),
                toOrderedList(matchedSecondarySkills),
                toOrderedList(missingCoreSkills)
        );
    }

    private int scoreProjectExperience(CandidateProfileFeatures profile,
                                       JobFeatures job,
                                       List<String> positives,
                                       List<String> weaknesses,
                                       List<String> deductions) {
        int score = switch (profile.projectStrength()) {
            case INTERNSHIP -> 16;
            case COMPETITION -> 13;
            case CAMPUS -> 10;
            case COURSE -> 6;
            case NONE -> 1;
        };

        if (!intersect(profile.projectSkillTags(), job.coreSkills()).isEmpty()) {
            score = Math.min(MAX_PROJECT_SCORE, score + 2);
        }

        if (job.directionKey().isTechnical() && profile.projectStrength() == ProjectStrength.NONE) {
            weaknesses.add("技术岗缺少项目或实习证明，项目经历分被明显拉低。");
            deductions.add("技术岗无项目经历，总分不会进入高分区间。");
            return 1;
        }

        if ((job.directionKey() == DirectionKey.OPERATIONS || job.directionKey() == DirectionKey.DESIGN)
                && profile.projectStrength() == ProjectStrength.NONE) {
            weaknesses.add("当前方向缺少可展示的活动案例、作品集或项目复盘，项目经历分会持续偏低。");
            deductions.add("运营或设计岗位通常需要案例证明，当前缺少可展示作品会影响排序。");
            return 3;
        }

        if (job.projectPreferred() && profile.projectStrength().level() <= ProjectStrength.COURSE.level()) {
            weaknesses.add("岗位偏好项目/实习经历，当前项目强度仍偏弱。");
            score = Math.max(0, score - 3);
        }

        if (profile.projectStrength() == ProjectStrength.INTERNSHIP) {
            positives.add("已有实习级项目/业务经历，能提供较强的岗位证明。");
        } else if (profile.projectStrength() == ProjectStrength.COMPETITION || profile.projectStrength() == ProjectStrength.CAMPUS) {
            positives.add("具备可讲述的项目经历，能够支撑岗位评估。");
        } else if (profile.projectStrength() == ProjectStrength.COURSE) {
            weaknesses.add("当前项目更多停留在课程/课设层，建议补强真实业务或完整项目。");
        }

        return clamp(score, 0, MAX_PROJECT_SCORE);
    }

    private int scoreEducation(CandidateProfileFeatures profile,
                               JobFeatures job,
                               List<String> positives,
                               List<String> deductions) {
        if (job.educationLevel() == EducationLevel.UNKNOWN || profile.educationLevel() == EducationLevel.UNKNOWN) {
            return 6;
        }
        if (profile.educationLevel().level() >= job.educationLevel().level()) {
            positives.add("学历满足岗位要求。");
            return MAX_EDUCATION_SCORE;
        }
        if (profile.educationLevel().level() + 1 == job.educationLevel().level()) {
            deductions.add("学历与岗位要求仅弱匹配。");
            return 4;
        }
        deductions.add("学历与岗位要求差距较大。");
        return 0;
    }

    private int scoreCity(CandidateProfileFeatures profile,
                          JobFeatures job,
                          CityRelation cityRelation,
                          List<String> positives,
                          List<String> deductions) {
        return switch (cityRelation) {
            case EXACT -> {
                positives.add("目标城市与岗位地点一致。");
                yield MAX_CITY_SCORE;
            }
            case PROVINCE_MATCH -> 6;
            case UNKNOWN -> 4;
            case MISMATCH -> {
                deductions.add("岗位地点与目标城市不一致，城市分被明显压低。");
                yield 0;
            }
        };
    }

    private int scoreResumeMaturity(ResumeMaturity resumeMaturity,
                                    List<String> positives,
                                    List<String> weaknesses) {
        int score = clamp(resumeMaturity.score(), 0, MAX_RESUME_MATURITY_SCORE);
        if (score >= 5) {
            positives.add("简历表达成熟度较好，已有量化结果或完整项目叙述。");
        } else if (score <= 2) {
            weaknesses.add("简历表达成熟度偏弱，缺少量化结果或完整项目闭环。");
        }
        return score;
    }

    private int scoreDataTrust(JobFeatures job,
                               List<String> positives,
                               List<String> deductions) {
        if (job.dataCompleteness() >= 7) {
            positives.add("岗位字段较完整，结构化判断更可靠。");
            return MAX_DATA_TRUST_SCORE;
        }
        if (job.dataCompleteness() >= 5) {
            return 2;
        }
        if (job.dataCompleteness() >= 3) {
            return 0;
        }
        deductions.add("岗位数据字段较少，本轮做了轻度可信度修正。");
        return -4;
    }

    private int scoreSalarySupport(JobFeatures job,
                                   List<String> positives,
                                   List<String> deductions) {
        if (!job.monthlySalaryComparable() || job.averageMonthlySalary() == null) {
            return 0;
        }

        BigDecimal average = job.averageMonthlySalary();
        if (average.compareTo(new BigDecimal("3000")) < 0) {
            deductions.add("薪资明显偏低，薪资辅助分不加分。");
            return -1;
        }
        if (average.compareTo(new BigDecimal("12000")) >= 0) {
            positives.add("薪资信息完整，且位于较有竞争力的区间。");
            return 2;
        }
        if (average.compareTo(new BigDecimal("5000")) >= 0) {
            return 1;
        }
        return 0;
    }

    private List<String> buildNextActions(UserProfile userProfile,
                                          JobPost jobPost,
                                          CandidateProfileFeatures profile,
                                          JobFeatures job,
                                          DimensionScores scores,
                                          HardConditionAssessment hardCondition) {
        List<String> actions = new ArrayList<>();
        List<String> missingCoreSkills = scores.missingCoreSkills();

        if (scores.resumeMaturityScore() <= 3 || hardCondition.compatibilityLevel() != CompatibilityLevel.STRONG) {
            actions.add("去简历优化：把目标方向、项目职责和量化结果写得更明确，先修复当前排序里被扣分的表达问题。");
        } else if (!missingCoreSkills.isEmpty()) {
            actions.add("去简历优化：在简历项目中补齐 " + joinNaturalList(missingCoreSkills, 3) + " 等岗位关键词与对应成果。");
        } else {
            actions.add("去简历优化：继续强化项目结果、业务指标和技术取舍，让高分岗位更容易转化为面试机会。");
        }

        if (!missingCoreSkills.isEmpty()) {
            actions.add("生成补强方案：围绕 " + joinNaturalList(missingCoreSkills, 3) + " 以及岗位短板，生成更聚焦的学习与项目补强路径。");
        } else if (profile.projectStrength() == ProjectStrength.NONE || (job.directionKey().isTechnical() && scores.projectScore() <= 6)) {
            actions.add("生成补强方案：优先补一段能证明岗位能力的项目/实习经历，再安排后续投递节奏。");
        } else {
            actions.add("生成补强方案：把当前弱项拆成学习步骤、项目安排和投递时点，避免只停留在泛泛准备。");
        }

        String interviewFocus;
        if (!missingCoreSkills.isEmpty()) {
            interviewFocus = "优先准备 " + joinNaturalList(missingCoreSkills, 2) + " 和项目追问。";
        } else if (job.directionKey().isTechnical()) {
            interviewFocus = "优先准备项目细节、核心技术栈和常见场景题。";
        } else if (job.directionKey() == DirectionKey.PRODUCT) {
            interviewFocus = "优先准备需求拆解、PRD 取舍和案例表达。";
        } else if (job.directionKey() == DirectionKey.OPERATIONS) {
            interviewFocus = "优先准备活动复盘、增长策略和数据分析相关问题。";
        } else if (job.directionKey() == DirectionKey.DESIGN) {
            interviewFocus = "优先准备作品集讲解、设计取舍和用户体验相关追问。";
        } else {
            interviewFocus = "优先准备岗位高频问题和自我介绍表达。";
        }
        actions.add("开始模拟面试：" + interviewFocus);

        return dedupeAndLimit(actions, 4);
    }

    private String buildCompatibilityHeadline(HardConditionAssessment hardCondition) {
        return switch (hardCondition.compatibilityLevel()) {
            case STRONG -> "当前属于强适配岗位，可优先进入简历优化和投递准备。";
            case WEAK -> "当前属于弱适配岗位，需要先补齐关键短板后再提高投递优先级。";
            case MISMATCH -> "当前岗位更适合作为参考方向岗，暂不建议排在优先投递队列前列。";
        };
    }

    private String buildScoreBreakdown(DimensionScores scores) {
        List<String> lines = new ArrayList<>();
        lines.add("方向匹配分 " + scores.directionScore() + "/" + MAX_DIRECTION_SCORE);
        lines.add("技能匹配分 " + scores.skillScore() + "/" + MAX_SKILL_SCORE);
        lines.add("项目经历分 " + scores.projectScore() + "/" + MAX_PROJECT_SCORE);
        lines.add("学历匹配分 " + scores.educationScore() + "/" + MAX_EDUCATION_SCORE);
        lines.add("城市匹配分 " + scores.cityScore() + "/" + MAX_CITY_SCORE);
        lines.add("简历表达成熟度分 " + scores.resumeMaturityScore() + "/" + MAX_RESUME_MATURITY_SCORE);
        lines.add("数据可信度修正分 " + scores.dataTrustScore() + "/" + MAX_DATA_TRUST_SCORE);
        lines.add("薪资辅助分 " + scores.salarySupportScore() + "/" + MAX_SALARY_SUPPORT_SCORE);
        return joinLines(lines);
    }

    private DirectionKey normalizeDirectionKey(String text) {
        String normalized = normalizeText(text).toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "java", "backend", "后端", "spring", "mysql", "接口", "微服务")) {
            return DirectionKey.BACKEND;
        }
        if (containsAny(normalized, "frontend", "前端", "react", "vue", "javascript", "typescript", "浏览器")) {
            return DirectionKey.FRONTEND;
        }
        if (containsAny(normalized, "product", "prd", "产品", "原型", "需求", "竞品", "pm")) {
            return DirectionKey.PRODUCT;
        }
        if (containsAny(normalized, "test", "qa", "测试", "自动化测试", "接口测试", "jmeter", "selenium")) {
            return DirectionKey.TEST;
        }
        if (containsAny(normalized, "data", "python", "pandas", "bi", "数据", "analysis", "sql")) {
            return DirectionKey.DATA;
        }
        if (containsAny(normalized, "运营", "operation", "growth", "用户增长", "活动运营", "内容运营", "投放")) {
            return DirectionKey.OPERATIONS;
        }
        if (containsAny(normalized, "design", "ui", "ux", "设计")) {
            return DirectionKey.DESIGN;
        }
        return DirectionKey.UNKNOWN;
    }

    private DirectionRelation compareDirections(DirectionKey profileDirection, DirectionKey jobDirection) {
        if (profileDirection == DirectionKey.UNKNOWN || jobDirection == DirectionKey.UNKNOWN) {
            return DirectionRelation.UNKNOWN;
        }
        if (profileDirection == jobDirection) {
            return DirectionRelation.EXACT;
        }
        if (ADJACENT_DIRECTIONS.getOrDefault(profileDirection, Set.of()).contains(jobDirection)
                || ADJACENT_DIRECTIONS.getOrDefault(jobDirection, Set.of()).contains(profileDirection)) {
            return DirectionRelation.ADJACENT;
        }
        return DirectionRelation.MISMATCH;
    }

    private EducationLevel inferEducationLevel(String text) {
        String normalized = normalizeText(text).toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "博士", "phd", "doctor")) {
            return EducationLevel.DOCTOR;
        }
        if (containsAny(normalized, "硕士", "研究生", "研一", "研二", "master")) {
            return EducationLevel.MASTER;
        }
        if (containsAny(normalized, "本科", "学士", "大一", "大二", "大三", "大四", "应届", "校招")) {
            return EducationLevel.UNDERGRAD;
        }
        if (containsAny(normalized, "大专", "专科")) {
            return EducationLevel.COLLEGE;
        }
        return EducationLevel.UNKNOWN;
    }

    private NatureKey inferNaturePreference(String text) {
        String normalized = normalizeText(text).toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "实习", "intern")) {
            return NatureKey.INTERN;
        }
        if (containsAny(normalized, "校招", "应届", "graduate")) {
            return NatureKey.CAMPUS;
        }
        if (containsAny(normalized, "全职", "正式", "fulltime", "full-time")) {
            return NatureKey.FULLTIME;
        }
        if (containsAny(normalized, "兼职", "part-time", "小时工")) {
            return NatureKey.PARTTIME;
        }
        return NatureKey.UNKNOWN;
    }

    private NatureKey inferJobNature(String text) {
        return inferNaturePreference(text);
    }

    private NatureRelation compareNature(NatureKey profileNature, NatureKey jobNature) {
        if (profileNature == NatureKey.UNKNOWN || jobNature == NatureKey.UNKNOWN) {
            return NatureRelation.UNKNOWN;
        }
        if (profileNature == jobNature) {
            return NatureRelation.ALIGNED;
        }
        if ((profileNature == NatureKey.INTERN && jobNature == NatureKey.CAMPUS)
                || (profileNature == NatureKey.CAMPUS && jobNature == NatureKey.FULLTIME)
                || (profileNature == NatureKey.CAMPUS && jobNature == NatureKey.INTERN)) {
            return NatureRelation.UNRELATED;
        }
        return NatureRelation.UNRELATED;
    }

    private CityRelation compareCities(CandidateProfileFeatures profile, JobFeatures job) {
        if (!StringUtils.hasText(profile.targetCity()) || !StringUtils.hasText(job.city())) {
            return CityRelation.UNKNOWN;
        }
        if (Objects.equals(profile.targetCityToken(), job.cityToken())
                || profile.targetCity().contains(job.city())
                || job.city().contains(profile.targetCity())) {
            return CityRelation.EXACT;
        }
        if (StringUtils.hasText(profile.targetProvinceToken())
                && StringUtils.hasText(job.provinceToken())
                && profile.targetProvinceToken().equals(job.provinceToken())) {
            return CityRelation.PROVINCE_MATCH;
        }
        return CityRelation.MISMATCH;
    }

    private ResumeMaturity inferResumeMaturity(String combinedResumeText,
                                               UserProfile userProfile,
                                               ResumeEnhanceRecord latestEnhance,
                                               ResumeReview latestReview) {
        int score = 0;
        List<String> signals = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        if (StringUtils.hasText(userProfile == null ? null : userProfile.getSkills())) {
            score += 1;
        } else {
            weaknesses.add("技能栈描述不完整");
        }
        if (StringUtils.hasText(userProfile == null ? null : userProfile.getProjectExperience())
                || StringUtils.hasText(latestEnhance == null ? null : latestEnhance.getParsedProjects())
                || StringUtils.hasText(latestEnhance == null ? null : latestEnhance.getParsedExperience())) {
            score += 2;
        } else {
            weaknesses.add("缺少完整项目经历");
        }
        if (QUANTIFIED_RESULT_PATTERN.matcher(defaultText(combinedResumeText)).find()
                || containsAny(defaultText(combinedResumeText).toLowerCase(Locale.ROOT),
                "提升", "优化", "降低", "上线", "落地", "转化", "用户", "qps", "pv", "uv")) {
            score += 2;
            signals.add("存在量化结果或业务影响");
        } else {
            weaknesses.add("缺少量化成果描述");
        }

        Integer reviewScore = latestEnhance != null && latestEnhance.getReviewScore() != null
                ? latestEnhance.getReviewScore()
                : latestReview == null ? null : latestReview.getReviewScore();
        if (reviewScore != null && reviewScore >= 80) {
            score += 1;
            signals.add("最近一次简历评估表现较好");
        } else if (reviewScore != null && reviewScore < 60) {
            weaknesses.add("最近一次简历评估仍偏弱");
        }

        return new ResumeMaturity(clamp(score, 0, MAX_RESUME_MATURITY_SCORE), dedupe(signals), dedupe(weaknesses));
    }

    private ProjectStrength inferProjectStrength(String text) {
        String normalized = normalizeText(text).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            return ProjectStrength.NONE;
        }
        if (containsAny(normalized, "实习", "intern", "业务", "上线", "客户", "生产环境")) {
            return ProjectStrength.INTERNSHIP;
        }
        if (containsAny(normalized, "比赛", "竞赛", "challenge", "hackathon")) {
            return ProjectStrength.COMPETITION;
        }
        if (containsAny(normalized, "校园", "实验室", "社团", "学校项目")) {
            return ProjectStrength.CAMPUS;
        }
        if (containsAny(normalized, "课程", "课设", "作业")) {
            return ProjectStrength.COURSE;
        }
        if (containsAny(normalized, "项目", "系统", "平台", "app", "网站")) {
            return ProjectStrength.CAMPUS;
        }
        return ProjectStrength.COURSE;
    }

    private boolean needsProjectEvidence(String text, DirectionKey directionKey) {
        String normalized = normalizeText(text).toLowerCase(Locale.ROOT);
        if (directionKey.isTechnical()
                || directionKey == DirectionKey.PRODUCT
                || directionKey == DirectionKey.OPERATIONS
                || directionKey == DirectionKey.DESIGN) {
            return true;
        }
        return containsAny(normalized, "项目", "实习", "落地", "经验", "作品");
    }

    private int computeDataCompleteness(JobPost jobPost) {
        int completeness = 0;
        if (StringUtils.hasText(jobPost.getJobDirection())) completeness++;
        if (StringUtils.hasText(jobPost.getJobRequirements())) completeness++;
        if (StringUtils.hasText(jobPost.getCompanyName())) completeness++;
        if (!"-".equals(jobPost.getReadableLocationDisplay())) completeness++;
        if (StringUtils.hasText(jobPost.getEducationRequirement())) completeness++;
        if (StringUtils.hasText(jobPost.getJobNature())) completeness++;
        if (StringUtils.hasText(jobPost.getDisplaySummary())) completeness++;
        if (StringUtils.hasText(jobPost.getSourceName())) completeness++;
        if (JobSalaryNormalizer.isMonthlyUnit(jobPost.getSalaryUnitType())
                && (jobPost.getSalaryMin() != null || jobPost.getSalaryMax() != null)) {
            completeness++;
        }
        return completeness;
    }

    private BigDecimal averageMonthlySalary(JobPost jobPost) {
        if (!JobSalaryNormalizer.isMonthlyUnit(jobPost.getSalaryUnitType())) {
            return null;
        }
        BigDecimal salaryMin = jobPost.getMonthlySalaryMin();
        BigDecimal salaryMax = jobPost.getMonthlySalaryMax();
        if (salaryMin == null && salaryMax == null) {
            return null;
        }
        if (salaryMin == null) {
            return salaryMax;
        }
        if (salaryMax == null) {
            return salaryMin;
        }
        return salaryMin.add(salaryMax).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    private Set<String> extractSkillTags(String text) {
        String normalized = normalizeText(text).toLowerCase(Locale.ROOT);
        Set<String> results = new LinkedHashSet<>();
        if (!StringUtils.hasText(normalized)) {
            return results;
        }

        for (Map.Entry<String, List<String>> entry : SKILL_SYNONYMS.entrySet()) {
            for (String alias : entry.getValue()) {
                if (normalized.contains(alias.toLowerCase(Locale.ROOT))) {
                    results.add(entry.getKey());
                    break;
                }
            }
        }
        return results;
    }

    private Set<String> extractFallbackKeywords(String text, int limit) {
        String normalized = normalizeText(text);
        Set<String> keywords = new LinkedHashSet<>();
        Matcher matcher = FALLBACK_SKILL_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group();
            String lower = token.toLowerCase(Locale.ROOT);
            if (token.length() < 2 || STOP_WORDS.contains(token) || STOP_WORDS.contains(lower)) {
                continue;
            }
            keywords.add(token);
            if (keywords.size() >= limit) {
                break;
            }
        }
        return keywords;
    }

    private Set<String> intersect(Set<String> left, Set<String> right) {
        Set<String> results = new LinkedHashSet<>();
        for (String value : left) {
            if (right.contains(value)) {
                results.add(value);
            }
        }
        return results;
    }

    private Set<String> subtract(Set<String> left, Set<String> right) {
        Set<String> results = new LinkedHashSet<>();
        for (String value : left) {
            if (!right.contains(value)) {
                results.add(value);
            }
        }
        return results;
    }

    private List<String> toOrderedList(Set<String> values) {
        return new ArrayList<>(values);
    }

    private int compatibilityRank(String compatibilityLevel) {
        String normalized = defaultText(compatibilityLevel).toUpperCase(Locale.ROOT);
        if ("STRONG".equals(normalized)) {
            return 3;
        }
        if ("WEAK".equals(normalized)) {
            return 2;
        }
        if ("MISMATCH".equals(normalized)) {
            return 1;
        }
        return 0;
    }

    private String normalizeLocationText(String text) {
        String normalized = normalizeText(text);
        if (!StringUtils.hasText(normalized) || "-".equals(normalized)) {
            return "";
        }
        return normalized;
    }

    private String extractProvinceToken(String text) {
        String normalized = normalizeLocationText(text);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        if (containsAny(normalized, "北京", "上海", "天津", "重庆")) {
            if (normalized.contains("北京")) return "北京";
            if (normalized.contains("上海")) return "上海";
            if (normalized.contains("天津")) return "天津";
            if (normalized.contains("重庆")) return "重庆";
        }
        Matcher matcher = PROVINCE_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractCityToken(String text) {
        String normalized = normalizeLocationText(text);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        if (containsAny(normalized, "北京", "上海", "天津", "重庆")) {
            if (normalized.contains("北京")) return "北京";
            if (normalized.contains("上海")) return "上海";
            if (normalized.contains("天津")) return "天津";
            if (normalized.contains("重庆")) return "重庆";
        }
        Matcher matcher = CITY_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String joinNaturalList(Set<String> values, int limit) {
        return joinNaturalList(new ArrayList<>(values), limit);
    }

    private String joinNaturalList(List<String> values, int limit) {
        List<String> selected = new ArrayList<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            selected.add(value.trim());
            if (selected.size() >= limit) {
                break;
            }
        }
        return selected.isEmpty() ? "-" : String.join("、", selected);
    }

    private List<String> dedupe(List<String> source) {
        Set<String> values = new LinkedHashSet<>();
        for (String item : source) {
            String cleaned = defaultText(item);
            if (StringUtils.hasText(cleaned)) {
                values.add(cleaned);
            }
        }
        return new ArrayList<>(values);
    }

    private List<String> dedupeAndLimit(List<String> source, int limit) {
        List<String> deduped = dedupe(source);
        if (deduped.size() <= limit) {
            return deduped;
        }
        return new ArrayList<>(deduped.subList(0, limit));
    }

    private String joinLines(List<String> lines) {
        return String.join("\n", dedupe(lines));
    }

    private boolean containsAny(String source, String... keywords) {
        String normalized = defaultText(source).toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String joinNonBlank(String... values) {
        List<String> items = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                items.add(value.trim());
            }
        }
        return String.join("\n", items);
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace('\u3000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : "";
    }

    private String defaultIfEmpty(String text, String defaultValue) {
        return StringUtils.hasText(text) ? text.trim() : defaultValue;
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void registerSkill(String canonicalName, String... aliases) {
        SKILL_SYNONYMS.put(canonicalName, Arrays.asList(aliases));
    }

    private enum CompatibilityLevel {
        STRONG,
        WEAK,
        MISMATCH
    }

    private enum DirectionKey {
        BACKEND,
        FRONTEND,
        PRODUCT,
        TEST,
        DATA,
        OPERATIONS,
        DESIGN,
        OTHER,
        UNKNOWN;

        private boolean isTechnical() {
            return this == BACKEND || this == FRONTEND || this == TEST || this == DATA;
        }
    }

    private enum DirectionRelation {
        EXACT,
        ADJACENT,
        MISMATCH,
        UNKNOWN
    }

    private enum CityRelation {
        EXACT,
        PROVINCE_MATCH,
        MISMATCH,
        UNKNOWN
    }

    private enum NatureRelation {
        ALIGNED,
        UNRELATED,
        UNKNOWN
    }

    private enum NatureKey {
        INTERN,
        CAMPUS,
        FULLTIME,
        PARTTIME,
        UNKNOWN
    }

    private enum EducationLevel {
        UNKNOWN(0),
        COLLEGE(1),
        UNDERGRAD(2),
        MASTER(3),
        DOCTOR(4);

        private final int level;

        EducationLevel(int level) {
            this.level = level;
        }

        private int level() {
            return level;
        }
    }

    private enum ProjectStrength {
        NONE(0),
        COURSE(1),
        CAMPUS(2),
        COMPETITION(3),
        INTERNSHIP(4);

        private final int level;

        ProjectStrength(int level) {
            this.level = level;
        }

        private int level() {
            return level;
        }
    }

    private enum SalaryPreferenceRelation {
        MATCH,
        ABOVE_EXPECTATION,
        BELOW_EXPECTATION,
        UNKNOWN_WEAK,
        UNKNOWN
    }

    private record ResumeMaturity(int score, List<String> signals, List<String> weaknesses) {
    }

    private record SalaryPreference(BigDecimal min, BigDecimal max) {
    }

    private record CandidateProfileFeatures(DirectionKey directionKey,
                                            String rawDirection,
                                            Set<String> skillTags,
                                            Set<String> projectSkillTags,
                                            ProjectStrength projectStrength,
                                            EducationLevel educationLevel,
                                            String targetCity,
                                            String targetProvinceToken,
                                            String targetCityToken,
                                            NatureKey naturePreference,
                                            ResumeMaturity resumeMaturity,
                                            String careerGoal,
                                            String projectText,
                                            String combinedResumeText) {
    }

    private record JobFeatures(DirectionKey directionKey,
                               String rawDirection,
                               Set<String> coreSkills,
                               Set<String> secondarySkills,
                               Set<String> genericSkills,
                               EducationLevel educationLevel,
                               String city,
                               String provinceToken,
                               String cityToken,
                               NatureKey natureKey,
                               boolean projectPreferred,
                               int dataCompleteness,
                               boolean monthlySalaryComparable,
                               BigDecimal averageMonthlySalary,
                               String combinedText) {
    }

    private record HardConditionAssessment(CompatibilityLevel compatibilityLevel,
                                           int scoreCap,
                                           int basePenalty,
                                           List<String> summaryItems,
                                           List<String> deductionReasons,
                                           DirectionRelation directionRelation,
                                           CityRelation cityRelation,
                                           NatureRelation natureRelation) {
    }

    private record SkillMatch(int totalScore,
                              List<String> matchedCoreSkills,
                              List<String> matchedSecondarySkills,
                              List<String> missingCoreSkills) {
    }

    private record DimensionScores(int directionScore,
                                   int skillScore,
                                   int projectScore,
                                   int educationScore,
                                   int cityScore,
                                   int resumeMaturityScore,
                                   int dataTrustScore,
                                   int salarySupportScore,
                                   List<String> matchedCoreSkills,
                                   List<String> matchedSecondarySkills,
                                   List<String> missingCoreSkills,
                                   List<String> positiveSignals,
                                   List<String> weaknesses,
                                   List<String> deductions) {
        private int rawTotal() {
            return directionScore + skillScore + projectScore + educationScore + cityScore
                    + resumeMaturityScore + dataTrustScore + salarySupportScore;
        }
    }
}
