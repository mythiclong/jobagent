package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.ProfileSyncApplyResult;
import com.zhihang.jobagent.dto.ProfileSyncFieldSuggestion;
import com.zhihang.jobagent.dto.ResumeParsedData;
import com.zhihang.jobagent.dto.ResumeProfileSyncSuggestion;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeProfileSyncService {

    private static final List<String> KNOWN_SKILLS = List.of(
            "Java", "Spring", "Spring Boot", "MySQL", "Redis", "SQL", "Linux", "Docker", "Git",
            "Python", "JavaScript", "TypeScript", "Vue", "React", "Node.js", "HTML", "CSS",
            "Pandas", "TensorFlow", "PyTorch", "Figma", "Axure", "PRD", "API", "HTTP",
            "自动化测试", "测试用例", "数据分析", "项目管理", "产品设计"
    );

    private static final Pattern NAME_PATTERN = Pattern.compile("(?:姓名|Name|name)\\s*[:：]?\\s*([\\u4e00-\\u9fa5A-Za-z·]{2,24})");
    private static final Pattern SCHOOL_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,24}(?:大学|学院|职业技术学院|学校))");
    private static final Pattern MAJOR_PATTERN = Pattern.compile("(?:专业|major|Major)\\s*[:：]?\\s*([\\u4e00-\\u9fa5A-Za-z0-9/()（）\\-]{2,30})");
    private static final Pattern GRADE_PATTERN = Pattern.compile("(大一|大二|大三|大四|研一|研二|研三|\\d{4}届)");
    private static final Pattern CITY_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,8})(?:市|区|县)");
    private static final Pattern TARGET_CITY_PATTERN = Pattern.compile("(?:意向城市|目标城市|期望城市|求职城市)\\s*[:：]?\\s*([^\\n]{2,40})");
    private static final Pattern EXPECTED_SALARY_PATTERN = Pattern.compile("(?:期望薪资|薪资期望|期望待遇)\\s*[:：]?\\s*([^\\n]{2,30})");
    private static final Pattern CAREER_GOAL_PATTERN = Pattern.compile("(?:求职意向|职业目标|目标岗位)\\s*[:：]?\\s*([^\\n]{4,80})");

    private final UserProfileRepository userProfileRepository;
    private final AiTextSanitizer aiTextSanitizer;
    private final CurrentUserService currentUserService;

    public ResumeProfileSyncService(UserProfileRepository userProfileRepository,
                                    AiTextSanitizer aiTextSanitizer,
                                    CurrentUserService currentUserService) {
        this.userProfileRepository = userProfileRepository;
        this.aiTextSanitizer = aiTextSanitizer;
        this.currentUserService = currentUserService;
    }

    public ResumeProfileSyncSuggestion buildSuggestion(UserProfile profile, ResumeParsedData parsedData, JobPost jobPost) {
        ExtractedProfileData extracted = extractProfileData(parsedData, jobPost);
        boolean draftMode = profile == null;
        List<ProfileSyncFieldSuggestion> fields = new ArrayList<>();

        addSimpleField(fields, "studentName", "姓名", profile == null ? "" : profile.getStudentName(), extracted.nameCandidate, draftMode);
        addSimpleField(fields, "major", "专业", profile == null ? "" : profile.getMajor(), extracted.majorCandidate, draftMode);
        addSimpleField(fields, "grade", "年级", profile == null ? "" : profile.getGrade(), extracted.gradeCandidate, draftMode);
        addSimpleField(fields, "targetRole", "目标岗位", profile == null ? "" : profile.getTargetRole(), extracted.targetRoleCandidate, draftMode);
        addSimpleField(fields, "targetDirection", "岗位方向", profile == null ? "" : profile.getTargetDirection(), extracted.directionCandidate, draftMode);
        addSimpleField(fields, "targetCity", "目标城市", profile == null ? "" : profile.getTargetCity(), extracted.targetCityCandidate, draftMode);
        addSimpleField(fields, "acceptableCities", "接受城市范围", profile == null ? "" : profile.getAcceptableCities(), extracted.acceptableCitiesCandidate, draftMode);
        addSimpleField(fields, "acceptableJobNature", "岗位性质", profile == null ? "" : profile.getAcceptableJobNature(), extracted.jobNatureCandidate, draftMode);
        addSimpleField(fields, "targetIndustry", "目标行业", profile == null ? "" : profile.getTargetIndustry(), extracted.industryCandidate, draftMode);
        addSimpleField(fields, "expectedSalary", "期望薪资", profile == null ? "" : profile.getExpectedSalary(), extracted.expectedSalaryCandidate, draftMode);

        String currentSkills = profile == null ? "" : profile.getSkills();
        String mergedSkills = mergeSkills(currentSkills, extracted.skillKeywords);
        fields.add(buildMergedField("skills", "技能关键词", currentSkills, mergedSkills, draftMode, "建议补入最近识别出的技能关键词"));

        String currentProjects = profile == null ? "" : profile.getProjectExperience();
        fields.add(buildAppendField("projectExperience", "项目经历摘要", currentProjects, extracted.projectSummary, draftMode));

        addSimpleField(fields, "careerGoal", "职业目标", profile == null ? "" : profile.getCareerGoal(), extracted.careerGoalCandidate, draftMode);

        int recommendedCount = (int) fields.stream().filter(ProfileSyncFieldSuggestion::isRecommended).count();
        return new ResumeProfileSyncSuggestion(
                draftMode,
                profile == null ? "未建立当前档案" : defaultDisplay(profile.getStudentName()),
                defaultDisplay(extracted.targetRoleCandidate),
                defaultDisplay(extracted.directionCandidate),
                defaultDisplay(extracted.targetCityCandidate),
                defaultDisplay(extracted.jobNatureCandidate),
                defaultDisplay(extracted.industryCandidate),
                defaultDisplay(extracted.expectedSalaryCandidate),
                defaultDisplay(extracted.careerGoalCandidate),
                defaultDisplay(String.join("、", extracted.skillKeywords)),
                defaultDisplay(extracted.projectSummary),
                fields,
                recommendedCount,
                recommendedCount > 0
        );
    }

    public ProfileSyncApplyResult applyFill(Long userProfileId, ResumeParsedData parsedData, JobPost jobPost) {
        ExtractedProfileData extracted = extractProfileData(parsedData, jobPost);
        if (userProfileId == null) {
            UserProfile created = createDraftProfile(extracted, jobPost);
            return new ProfileSyncApplyResult(created, true, countFilledFields(created));
        }

        UserProfile profile = userProfileRepository.findById(userProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user profile id: " + userProfileId));
        bindCurrentUserIfPossible(profile);

        int updated = 0;
        updated += fillIfBlank(profile.getStudentName(), extracted.nameCandidate, profile::setStudentName);
        updated += fillIfBlank(profile.getGrade(), extracted.gradeCandidate, profile::setGrade);
        updated += fillIfBlank(profile.getMajor(), extracted.majorCandidate, profile::setMajor);
        updated += fillIfBlank(profile.getTargetRole(), extracted.targetRoleCandidate, profile::setTargetRole);
        updated += fillIfBlank(profile.getTargetDirection(), extracted.directionCandidate, profile::setTargetDirection);
        updated += fillIfBlank(profile.getTargetCity(), extracted.targetCityCandidate, profile::setTargetCity);
        updated += fillIfBlank(profile.getAcceptableCities(), extracted.acceptableCitiesCandidate, profile::setAcceptableCities);
        updated += fillIfBlank(profile.getExpectedSalary(), extracted.expectedSalaryCandidate, profile::setExpectedSalary);
        updated += fillIfBlank(profile.getAcceptableJobNature(), extracted.jobNatureCandidate, profile::setAcceptableJobNature);
        updated += fillIfBlank(profile.getTargetIndustry(), extracted.industryCandidate, profile::setTargetIndustry);
        updated += fillIfBlank(profile.getCareerGoal(), extracted.careerGoalCandidate, profile::setCareerGoal);

        String mergedSkills = mergeSkills(profile.getSkills(), extracted.skillKeywords);
        if (StringUtils.hasText(mergedSkills) && !mergedSkills.equals(defaultText(profile.getSkills()))) {
            profile.setSkills(mergedSkills);
            updated++;
        }

        if (StringUtils.hasText(extracted.projectSummary)) {
            String existingProjects = defaultText(profile.getProjectExperience());
            if (!StringUtils.hasText(existingProjects)) {
                profile.setProjectExperience(extracted.projectSummary);
                updated++;
            } else if (!containsIgnoreCase(existingProjects, extracted.projectSummary)) {
                profile.setProjectExperience(existingProjects + "\n" + extracted.projectSummary);
                updated++;
            }
        }

        if (updated > 0) {
            profile = userProfileRepository.save(profile);
        }
        return new ProfileSyncApplyResult(profile, false, updated);
    }

    public ResumeParsedData toParsedData(ResumeEnhanceRecord record) {
        if (record == null) {
            return null;
        }
        return new ResumeParsedData(
                record.getOriginalResumeText(),
                record.getInputType(),
                record.getParsedBasicInfo(),
                record.getParsedEducation(),
                record.getParsedSkills(),
                record.getParsedProjects(),
                record.getParsedExperience(),
                record.getParsedOthers()
        );
    }

    private UserProfile createDraftProfile(ExtractedProfileData extracted, JobPost jobPost) {
        UserProfile draft = new UserProfile();
        bindCurrentUserIfPossible(draft);
        draft.setStudentName(defaultIfEmpty(extracted.nameCandidate, "简历候选人"));
        draft.setGrade(defaultIfEmpty(extracted.gradeCandidate, "待确认年级"));
        draft.setMajor(defaultIfEmpty(extracted.majorCandidate, "待确认专业"));
        draft.setTargetRole(defaultIfEmpty(extracted.targetRoleCandidate, inferDefaultRole(extracted.directionCandidate)));
        draft.setTargetDirection(defaultIfEmpty(extracted.directionCandidate,
                jobPost == null ? "待确认方向" : defaultIfEmpty(jobPost.getJobDirection(), "待确认方向")));
        draft.setSkills(mergeSkills("", extracted.skillKeywords));
        draft.setProjectExperience(extracted.projectSummary);
        draft.setTargetCity(extracted.targetCityCandidate);
        draft.setAcceptableCities(extracted.acceptableCitiesCandidate);
        draft.setExpectedSalary(extracted.expectedSalaryCandidate);
        draft.setAcceptableJobNature(extracted.jobNatureCandidate);
        draft.setTargetIndustry(extracted.industryCandidate);
        draft.setCareerGoal(extracted.careerGoalCandidate);
        return userProfileRepository.save(draft);
    }

    private void addSimpleField(List<ProfileSyncFieldSuggestion> fields,
                                String fieldKey,
                                String label,
                                String currentValue,
                                String extractedValue,
                                boolean draftMode) {
        fields.add(buildSimpleField(fieldKey, label, currentValue, extractedValue, draftMode));
    }

    private ProfileSyncFieldSuggestion buildSimpleField(String fieldKey,
                                                        String label,
                                                        String currentValue,
                                                        String extractedValue,
                                                        boolean draftMode) {
        boolean hasCurrent = StringUtils.hasText(currentValue);
        boolean hasRecognized = StringUtils.hasText(extractedValue);
        boolean recommended = draftMode ? hasRecognized : (!hasCurrent && hasRecognized);
        String actionLabel;
        if (!hasRecognized) {
            actionLabel = "暂无可回流候选值";
        } else if (draftMode) {
            actionLabel = "创建草稿时写入";
        } else if (!hasCurrent) {
            actionLabel = "建议补全";
        } else {
            actionLabel = "保留当前档案";
        }
        return new ProfileSyncFieldSuggestion(
                fieldKey,
                label,
                defaultDisplay(currentValue),
                defaultDisplay(extractedValue),
                actionLabel,
                recommended
        );
    }

    private ProfileSyncFieldSuggestion buildMergedField(String fieldKey,
                                                        String label,
                                                        String currentValue,
                                                        String mergedValue,
                                                        boolean draftMode,
                                                        String defaultAction) {
        boolean hasMergedValue = StringUtils.hasText(mergedValue);
        boolean recommended = draftMode
                ? hasMergedValue
                : hasMergedValue && !mergedValue.equals(defaultText(currentValue));
        String actionLabel;
        if (!hasMergedValue) {
            actionLabel = "暂无可回流候选值";
        } else if (draftMode) {
            actionLabel = "创建草稿时写入";
        } else if (recommended) {
            actionLabel = defaultAction;
        } else {
            actionLabel = "当前档案已覆盖";
        }
        return new ProfileSyncFieldSuggestion(
                fieldKey,
                label,
                defaultDisplay(currentValue),
                defaultDisplay(mergedValue),
                actionLabel,
                recommended
        );
    }

    private ProfileSyncFieldSuggestion buildAppendField(String fieldKey,
                                                        String label,
                                                        String currentValue,
                                                        String extractedValue,
                                                        boolean draftMode) {
        boolean hasRecognized = StringUtils.hasText(extractedValue);
        boolean recommended = draftMode
                ? hasRecognized
                : hasRecognized && !containsIgnoreCase(currentValue, extractedValue);
        String actionLabel;
        if (!hasRecognized) {
            actionLabel = "暂无可回流候选值";
        } else if (draftMode) {
            actionLabel = "创建草稿时写入";
        } else if (recommended) {
            actionLabel = "建议补入项目摘要";
        } else {
            actionLabel = "当前档案已覆盖";
        }
        return new ProfileSyncFieldSuggestion(
                fieldKey,
                label,
                defaultDisplay(currentValue),
                defaultDisplay(extractedValue),
                actionLabel,
                recommended
        );
    }

    private ExtractedProfileData extractProfileData(ResumeParsedData parsedData, JobPost jobPost) {
        String basicInfo = aiTextSanitizer.sanitizePlainText(defaultText(parsedData.getBasicInfo()));
        String education = aiTextSanitizer.sanitizePlainText(defaultText(parsedData.getEducation()));
        String skills = aiTextSanitizer.sanitizePlainText(defaultText(parsedData.getSkills()));
        String projects = aiTextSanitizer.sanitizeResumeText(defaultText(parsedData.getProjects()));
        String experience = aiTextSanitizer.sanitizeResumeText(defaultText(parsedData.getExperience()));
        String others = aiTextSanitizer.sanitizePlainText(defaultText(parsedData.getOthers()));
        String fullText = joinNonBlank(basicInfo, education, skills, projects, experience, others);

        String name = extractByRegex(basicInfo, NAME_PATTERN);
        if (!StringUtils.hasText(name)) {
            name = extractByRegex(fullText, NAME_PATTERN);
        }

        String school = extractByRegex(education, SCHOOL_PATTERN);
        String major = extractByRegex(education, MAJOR_PATTERN);
        if (!StringUtils.hasText(major)) {
            major = extractMajorFallback(fullText, school);
        }

        String grade = extractByRegex(fullText, GRADE_PATTERN);
        String direction = inferDirection(fullText, jobPost);
        String targetRole = inferTargetRole(fullText, direction, jobPost);
        String targetCity = inferTargetCity(fullText, jobPost);
        String acceptableCities = inferAcceptableCities(fullText, targetCity, jobPost);
        String expectedSalary = inferExpectedSalary(fullText);
        String jobNature = inferJobNature(fullText, jobPost);
        String industry = inferIndustry(fullText, jobPost);
        String careerGoal = inferCareerGoal(fullText, targetRole, direction, jobNature, school, jobPost);
        List<String> skillKeywords = extractSkillKeywords(joinNonBlank(skills, projects, experience, others));
        String projectSummary = summarizeProjects(projects, experience);

        return new ExtractedProfileData(
                sanitize(name),
                sanitize(school),
                sanitize(major),
                sanitize(grade),
                sanitize(targetRole),
                sanitize(direction),
                sanitize(targetCity),
                sanitize(acceptableCities),
                sanitize(expectedSalary),
                sanitize(jobNature),
                sanitize(industry),
                sanitize(careerGoal),
                skillKeywords,
                sanitizeResume(projectSummary)
        );
    }

    private String inferTargetRole(String fullText, String direction, JobPost jobPost) {
        if (jobPost != null && StringUtils.hasText(jobPost.getDisplayTitle())) {
            return jobPost.getDisplayTitle().trim();
        }
        String explicitGoal = extractByRegex(fullText, CAREER_GOAL_PATTERN);
        if (StringUtils.hasText(explicitGoal) && explicitGoal.length() <= 30) {
            return explicitGoal.trim();
        }
        return inferDefaultRole(direction);
    }

    private String inferDefaultRole(String direction) {
        String normalized = defaultText(direction);
        if (normalized.contains("后端")) {
            return "后端开发";
        }
        if (normalized.contains("前端")) {
            return "前端开发";
        }
        if (normalized.contains("产品")) {
            return "产品经理";
        }
        if (normalized.contains("测试")) {
            return "测试开发";
        }
        if (normalized.contains("数据")) {
            return "数据分析";
        }
        return normalized;
    }

    private String inferTargetCity(String fullText, JobPost jobPost) {
        String explicit = extractByRegex(fullText, TARGET_CITY_PATTERN);
        if (StringUtils.hasText(explicit)) {
            return firstCityFromText(explicit);
        }
        List<String> cityCandidates = extractCityCandidates(fullText);
        if (!cityCandidates.isEmpty()) {
            return cityCandidates.get(0);
        }
        if (jobPost != null && StringUtils.hasText(jobPost.getReadableLocationDisplay()) && !"-".equals(jobPost.getReadableLocationDisplay())) {
            return firstCityFromText(jobPost.getReadableLocationDisplay());
        }
        return "";
    }

    private String inferAcceptableCities(String fullText, String targetCity, JobPost jobPost) {
        LinkedHashSet<String> cities = new LinkedHashSet<>(extractCityCandidates(fullText));
        if (StringUtils.hasText(targetCity)) {
            cities.add(targetCity);
        }
        if (jobPost != null && StringUtils.hasText(jobPost.getReadableLocationDisplay()) && !"-".equals(jobPost.getReadableLocationDisplay())) {
            String city = firstCityFromText(jobPost.getReadableLocationDisplay());
            if (StringUtils.hasText(city)) {
                cities.add(city);
            }
        }
        return cities.stream().limit(3).reduce((left, right) -> left + "、" + right).orElse("");
    }

    private String inferExpectedSalary(String fullText) {
        String explicit = extractByRegex(fullText, EXPECTED_SALARY_PATTERN);
        if (StringUtils.hasText(explicit)) {
            return explicit.trim();
        }
        Matcher matcher = Pattern.compile("(\\d+\\s*(?:k|K|千)(?:\\s*[-~到]\\s*\\d+\\s*(?:k|K|千))?(?:\\s*/\\s*月)?)").matcher(fullText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String inferJobNature(String fullText, JobPost jobPost) {
        String normalized = fullText.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "实习", "intern")) {
            return "实习";
        }
        if (containsAny(normalized, "校招", "应届", "graduate")) {
            return "校招";
        }
        if (containsAny(normalized, "全职", "正式", "full-time", "fulltime")) {
            return "全职";
        }
        if (jobPost != null && StringUtils.hasText(jobPost.getJobNatureDisplay())) {
            return jobPost.getJobNatureDisplay().trim();
        }
        return "";
    }

    private String inferIndustry(String fullText, JobPost jobPost) {
        String normalized = fullText.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "saas", "企业服务")) {
            return "企业服务 / SaaS";
        }
        if (containsAny(normalized, "电商", "零售")) {
            return "电商零售";
        }
        if (containsAny(normalized, "金融")) {
            return "金融";
        }
        if (containsAny(normalized, "教育")) {
            return "教育";
        }
        if (containsAny(normalized, "游戏")) {
            return "游戏";
        }
        if (jobPost != null && StringUtils.hasText(jobPost.getCompanyTypeDisplay())) {
            return jobPost.getCompanyTypeDisplay().trim();
        }
        return "";
    }

    private String inferCareerGoal(String fullText,
                                   String targetRole,
                                   String direction,
                                   String jobNature,
                                   String school,
                                   JobPost jobPost) {
        String explicit = extractByRegex(fullText, CAREER_GOAL_PATTERN);
        if (StringUtils.hasText(explicit)) {
            return explicit.trim();
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(jobNature)) {
            parts.add("近期以" + jobNature + "机会为主");
        }
        if (StringUtils.hasText(targetRole)) {
            parts.add("聚焦" + targetRole);
        } else if (StringUtils.hasText(direction)) {
            parts.add("聚焦" + direction + "方向");
        }
        if (jobPost != null && StringUtils.hasText(jobPost.getCompanyNameDisplay())) {
            parts.add("优先准备与目标岗位匹配的投递材料");
        }
        if (StringUtils.hasText(school)) {
            parts.add("结合" + school + "背景补齐项目证明");
        }
        return String.join("，", parts);
    }

    private String inferDirection(String fullText, JobPost jobPost) {
        String normalized = defaultText(fullText).toLowerCase(Locale.ROOT);
        int backend = scoreDirection(normalized, List.of("后端", "java", "spring", "mysql", "redis", "api"));
        int frontend = scoreDirection(normalized, List.of("前端", "react", "vue", "javascript", "typescript", "css"));
        int product = scoreDirection(normalized, List.of("产品", "prd", "原型", "需求"));
        int test = scoreDirection(normalized, List.of("测试", "自动化测试", "qa", "jmeter"));
        int data = scoreDirection(normalized, List.of("数据", "analysis", "python", "pandas", "sql"));
        int max = Math.max(Math.max(backend, frontend), Math.max(product, Math.max(test, data)));

        if (max == 0 && jobPost != null && StringUtils.hasText(jobPost.getJobDirection())) {
            return jobPost.getJobDirection().trim();
        }
        if (max == backend) {
            return "后端开发";
        }
        if (max == frontend) {
            return "前端开发";
        }
        if (max == product) {
            return "产品方向";
        }
        if (max == test) {
            return "测试开发";
        }
        if (max == data) {
            return "数据分析";
        }
        return "";
    }

    private int scoreDirection(String source, List<String> keywords) {
        int score = 0;
        for (String keyword : keywords) {
            if (containsIgnoreCase(source, keyword)) {
                score++;
            }
        }
        return score;
    }

    private String summarizeProjects(String projects, String experience) {
        String source = StringUtils.hasText(projects) ? projects : experience;
        String sanitized = sanitizeResume(source);
        if (!StringUtils.hasText(sanitized)) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (String rawLine : sanitized.split("\n")) {
            String line = defaultText(rawLine);
            if (!StringUtils.hasText(line)) {
                continue;
            }
            lines.add(line);
            if (lines.size() >= 3) {
                break;
            }
        }
        return String.join("\n", lines);
    }

    private List<String> extractSkillKeywords(String text) {
        String sanitized = aiTextSanitizer.sanitizePlainText(defaultText(text));
        LinkedHashSet<String> results = new LinkedHashSet<>();
        for (String skill : KNOWN_SKILLS) {
            if (containsIgnoreCase(sanitized, skill)) {
                results.add(skill);
            }
            if (results.size() >= 14) {
                return new ArrayList<>(results);
            }
        }
        for (String token : sanitized.replace("/", " ").replace(",", " ").replace("，", " ").split("\\s+")) {
            String cleaned = token.trim();
            if (!StringUtils.hasText(cleaned) || cleaned.length() < 2 || cleaned.length() > 24) {
                continue;
            }
            if (cleaned.matches("^[0-9]+$") || cleaned.matches("^[\\p{Punct}]+$")) {
                continue;
            }
            results.add(cleaned);
            if (results.size() >= 14) {
                break;
            }
        }
        return new ArrayList<>(results);
    }

    private String mergeSkills(String currentSkills, List<String> extractedSkills) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(splitTokens(currentSkills));
        if (extractedSkills != null) {
            for (String skill : extractedSkills) {
                if (StringUtils.hasText(skill)) {
                    merged.add(skill.trim());
                }
            }
        }
        return merged.isEmpty() ? "" : String.join("、", merged);
    }

    private Set<String> splitTokens(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String token : defaultText(text).replace("/", " ").replace("、", " ").replace(",", " ").replace("，", " ").split("\\s+")) {
            if (StringUtils.hasText(token)) {
                result.add(token.trim());
            }
        }
        return result;
    }

    private List<String> extractCityCandidates(String text) {
        LinkedHashSet<String> cities = new LinkedHashSet<>();
        Matcher matcher = CITY_PATTERN.matcher(defaultText(text));
        while (matcher.find()) {
            String city = matcher.group(1).trim();
            if (StringUtils.hasText(city)) {
                cities.add(city);
            }
            if (cities.size() >= 3) {
                break;
            }
        }
        for (String city : List.of("北京", "上海", "广州", "深圳", "杭州", "苏州", "成都", "南京", "武汉")) {
            if (containsIgnoreCase(text, city)) {
                cities.add(city);
            }
        }
        return new ArrayList<>(cities);
    }

    private String firstCityFromText(String text) {
        List<String> cities = extractCityCandidates(text);
        return cities.isEmpty() ? "" : cities.get(0);
    }

    private String extractMajorFallback(String fullText, String school) {
        String major = extractByRegex(fullText, Pattern.compile("(软件工程|计算机[^\\s，。；]{0,12}|人工智能|数据科学|电子信息|信息管理)"));
        if (StringUtils.hasText(major)) {
            return major;
        }
        return school;
    }

    private String extractByRegex(String text, Pattern pattern) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return "";
        }
        return matcher.groupCount() >= 1 ? defaultText(matcher.group(1)) : defaultText(matcher.group());
    }

    private int fillIfBlank(String currentValue, String extractedValue, Consumer<String> consumer) {
        if (!StringUtils.hasText(currentValue) && StringUtils.hasText(extractedValue)) {
            consumer.accept(extractedValue.trim());
            return 1;
        }
        return 0;
    }

    private void bindCurrentUserIfPossible(UserProfile userProfile) {
        if (userProfile.getUserAccountId() == null && currentUserService.isAuthenticated()) {
            userProfile.setUserAccountId(currentUserService.requireCurrentUserId());
        }
    }

    private int countFilledFields(UserProfile profile) {
        int count = 0;
        if (StringUtils.hasText(profile.getStudentName())) count++;
        if (StringUtils.hasText(profile.getGrade())) count++;
        if (StringUtils.hasText(profile.getMajor())) count++;
        if (StringUtils.hasText(profile.getTargetRole())) count++;
        if (StringUtils.hasText(profile.getTargetDirection())) count++;
        if (StringUtils.hasText(profile.getSkills())) count++;
        if (StringUtils.hasText(profile.getProjectExperience())) count++;
        if (StringUtils.hasText(profile.getTargetCity())) count++;
        if (StringUtils.hasText(profile.getAcceptableCities())) count++;
        if (StringUtils.hasText(profile.getExpectedSalary())) count++;
        if (StringUtils.hasText(profile.getAcceptableJobNature())) count++;
        if (StringUtils.hasText(profile.getTargetIndustry())) count++;
        if (StringUtils.hasText(profile.getCareerGoal())) count++;
        return count;
    }

    private String sanitize(String text) {
        return aiTextSanitizer.sanitizePlainText(defaultText(text));
    }

    private String sanitizeResume(String text) {
        return aiTextSanitizer.sanitizeResumeText(defaultText(text));
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

    private String defaultDisplay(String text) {
        return StringUtils.hasText(text) ? text.trim() : "—";
    }

    private String defaultIfEmpty(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private boolean containsAny(String source, String... candidates) {
        String normalized = defaultText(source).toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (normalized.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(target)) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(target.toLowerCase(Locale.ROOT));
    }

    private static final class ExtractedProfileData {
        private final String nameCandidate;
        private final String schoolCandidate;
        private final String majorCandidate;
        private final String gradeCandidate;
        private final String targetRoleCandidate;
        private final String directionCandidate;
        private final String targetCityCandidate;
        private final String acceptableCitiesCandidate;
        private final String expectedSalaryCandidate;
        private final String jobNatureCandidate;
        private final String industryCandidate;
        private final String careerGoalCandidate;
        private final List<String> skillKeywords;
        private final String projectSummary;

        private ExtractedProfileData(String nameCandidate,
                                     String schoolCandidate,
                                     String majorCandidate,
                                     String gradeCandidate,
                                     String targetRoleCandidate,
                                     String directionCandidate,
                                     String targetCityCandidate,
                                     String acceptableCitiesCandidate,
                                     String expectedSalaryCandidate,
                                     String jobNatureCandidate,
                                     String industryCandidate,
                                     String careerGoalCandidate,
                                     List<String> skillKeywords,
                                     String projectSummary) {
            this.nameCandidate = nameCandidate;
            this.schoolCandidate = schoolCandidate;
            this.majorCandidate = majorCandidate;
            this.gradeCandidate = gradeCandidate;
            this.targetRoleCandidate = targetRoleCandidate;
            this.directionCandidate = directionCandidate;
            this.targetCityCandidate = targetCityCandidate;
            this.acceptableCitiesCandidate = acceptableCitiesCandidate;
            this.expectedSalaryCandidate = expectedSalaryCandidate;
            this.jobNatureCandidate = jobNatureCandidate;
            this.industryCandidate = industryCandidate;
            this.careerGoalCandidate = careerGoalCandidate;
            this.skillKeywords = skillKeywords;
            this.projectSummary = projectSummary;
        }
    }
}
