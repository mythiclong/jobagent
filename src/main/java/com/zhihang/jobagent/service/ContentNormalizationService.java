package com.zhihang.jobagent.service;

import com.zhihang.jobagent.entity.InterviewQuestion;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.LearningResource;
import com.zhihang.jobagent.service.update.DirectionClassifier;
import com.zhihang.jobagent.service.update.UpdateSourceSupport;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ContentNormalizationService {
    private static final Pattern LEADING_NOISE_PATTERN = Pattern.compile("^[\\p{Punct}·•、，。；：！？【】（）《》“”‘’]+\\s*");

    private static final Pattern QUESTION_MARK_PATTERN = Pattern.compile("[?？]+$");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern LIST_PREFIX_PATTERN = Pattern.compile("^(?:(?:question|q)\\s*\\d+\\s*[.)、:：-]?\\s*|\\d+\\s*[.)、:：-]\\s*|[\\-*•●▪▫■□◆◇►]+\\s*)+", Pattern.CASE_INSENSITIVE);

    public void normalizeJob(JobPost jobPost) {
        if (jobPost == null) {
            return;
        }

        String rawTitle = cleanPlainText(firstNonBlank(jobPost.getRawTitle(), jobPost.getJobName(), jobPost.getJobTitle()));
        String rawContent = cleanPlainText(joinNonBlank(jobPost.getRawContent(), jobPost.getJobDescription(), jobPost.getJobRequirements(), jobPost.getBonusPoints()));
        String rawUrl = UpdateSourceSupport.normalizeUrl(firstNonBlank(jobPost.getRawUrl(), jobPost.getJobUrl(), jobPost.getExternalUrl()));
        String direction = normalizeDirection(firstNonBlank(jobPost.getJobDirection(), DirectionClassifier.classifyDirection(rawTitle, rawContent, jobPost.getCompanyName())));
        List<String> requirementItems = extractItems(joinNonBlank(jobPost.getJobRequirements(), rawContent), 4);
        List<String> bonusItems = extractItems(joinNonBlank(jobPost.getBonusPoints(), rawContent), 3);
        String displayTitle = normalizeJobTitle(rawTitle, direction);
        String displaySummary = !requirementItems.isEmpty() ? truncateText("重点关注" + String.join("、", requirementItems) + "。", 120) : truncateText(firstNonBlank(rawContent, jobPost.getJobDescription(), jobPost.getJobRequirements(), "岗位信息已整理。"), 120);
        String displayStage = inferJobStage(rawTitle, rawContent, jobPost.getJobNature(), jobPost.getSuitableGrade());
        String displayDifficulty = inferJobDifficulty(displayStage, rawTitle, rawContent);

        jobPost.setRawTitle(UpdateSourceSupport.truncate(rawTitle, 255));
        jobPost.setRawContent(UpdateSourceSupport.truncate(rawContent, 6000));
        jobPost.setRawUrl(UpdateSourceSupport.truncate(rawUrl, 255));
        jobPost.setSourceLanguage(detectLanguage(rawTitle + " " + rawContent));
        jobPost.setDisplayTitle(UpdateSourceSupport.truncate(displayTitle, 255));
        jobPost.setDisplaySummary(UpdateSourceSupport.truncate(displaySummary, 1200));
        jobPost.setDisplayTags(UpdateSourceSupport.truncate(joinTagText(direction, jobPost.getSuitableGrade(), StringUtils.hasText(jobPost.getCompanyName()) ? "真实来源" : ""), 255));
        jobPost.setDisplayCategory("岗位");
        jobPost.setDisplayLanguage("中文");
        jobPost.setDisplayDifficulty(displayDifficulty);
        jobPost.setDisplayStage(displayStage);
        jobPost.setNormalizedStatus("已整理");
        jobPost.setJobName(displayTitle);
        jobPost.setJobDirection(direction);
        if (!requirementItems.isEmpty()) {
            jobPost.setJobRequirements(String.join("\n", requirementItems));
        } else {
            jobPost.setJobRequirements(UpdateSourceSupport.truncate(displaySummary, 1200));
        }
        if (!bonusItems.isEmpty()) {
            jobPost.setBonusPoints(String.join("\n", bonusItems));
        } else if (!StringUtils.hasText(jobPost.getBonusPoints())) {
            jobPost.setBonusPoints(buildJobBonusFallback(jobPost));
        }
        if (!StringUtils.hasText(jobPost.getSuitableGrade())) {
            jobPost.setSuitableGrade(defaultSuitableGrade(displayStage));
        }
    }

    public void normalizeInterviewQuestion(InterviewQuestion question) {
        if (question == null) {
            return;
        }

        String rawTitle = cleanPlainText(firstNonBlank(question.getRawTitle(), question.getQuestionText()));
        String rawContent = cleanPlainText(firstNonBlank(question.getRawContent(), question.getSourceSummary(), question.getReferenceAnswer(), question.getQuestionText()));
        String rawUrl = UpdateSourceSupport.normalizeUrl(firstNonBlank(question.getRawUrl(), question.getSourceUrl()));
        String direction = normalizeDirection(firstNonBlank(question.getJobDirection(), DirectionClassifier.classifyQuestionTag(rawTitle, question.getSourceName())));
        String displayTitle = resolvePreferredQuestionTitle(question, rawTitle);
        String displayCategory = normalizeInterviewCategory(firstNonBlank(question.getQuestionType(), question.getDisplayCategory()), rawTitle);
        String displayDifficulty = normalizeDifficulty(firstNonBlank(question.getDisplayDifficulty(), displayCategory));
        List<String> keyPoints = buildQuestionKeyPoints(question.getKeyPoints(), rawTitle, direction);
        List<String> displayTags = buildQuestionTags(direction, displayCategory, rawTitle, question.getDisplayTags());
        String displaySummary = truncateText("题型：" + displayCategory + "，重点关注" + (keyPoints.isEmpty() ? "关键知识点" : String.join("、", keyPoints)) + "。", 110);

        question.setRawTitle(UpdateSourceSupport.truncate(rawTitle, 255));
        question.setRawContent(UpdateSourceSupport.truncate(rawContent, 6000));
        question.setRawUrl(UpdateSourceSupport.truncate(rawUrl, 255));
        question.setSourceLanguage(detectLanguage(rawTitle + " " + rawContent));
        question.setDisplayTitle(UpdateSourceSupport.truncate(displayTitle, 255));
        question.setDisplaySummary(UpdateSourceSupport.truncate(displaySummary, 800));
        question.setDisplayTags(UpdateSourceSupport.truncate(String.join(" / ", displayTags), 255));
        question.setDisplayCategory(displayCategory);
        question.setDisplayLanguage("中文");
        question.setDisplayDifficulty(displayDifficulty);
        question.setDisplayStage("面试准备");
        question.setNormalizedStatus("已整理");
        question.setJobDirection(direction);
        question.setQuestionType(displayCategory);
        question.setQuestionText(displayTitle);
        question.setReferenceAnswer(StringUtils.hasText(question.getReferenceAnswer()) ? cleanPlainText(question.getReferenceAnswer()) : displaySummary);
        question.setKeyPoints(String.join("\n", keyPoints));
        question.setSourceSummary(displaySummary);
    }
    public void normalizeLearningResource(LearningResource resource) {
        if (resource == null) {
            return;
        }

        String rawTitle = cleanPlainText(firstNonBlank(resource.getRawTitle(), resource.getTitle()));
        String rawContent = cleanPlainText(firstNonBlank(resource.getRawContent(), resource.getSummary()));
        String rawUrl = UpdateSourceSupport.normalizeUrl(firstNonBlank(resource.getRawUrl(), resource.getUrl()));
        String direction = normalizeDirection(firstNonBlank(resource.getTargetDirection(), DirectionClassifier.classifyDirection(rawTitle, rawContent, resource.getSource())));
        String displayCategory = normalizeResourceCategory(firstNonBlank(resource.getDisplayCategory(), resource.getResourceType(), DirectionClassifier.classifyResourceType(rawTitle, rawContent)));
        String displayStage = normalizeResourceStage(firstNonBlank(resource.getDisplayStage(), resource.getStageTag(), DirectionClassifier.classifyStageTag(rawTitle, rawContent)));
        String displayTitle = normalizeResourceTitle(rawTitle, displayCategory, resource.getSource());
        String displaySummary = StringUtils.hasText(rawContent)
                ? truncateText(rawContent, 120)
                : truncateText("围绕" + defaultText(displayTitle) + "整理，适合" + defaultText(direction) + "方向学习。", 120);

        resource.setRawTitle(UpdateSourceSupport.truncate(rawTitle, 255));
        resource.setRawContent(UpdateSourceSupport.truncate(rawContent, 4000));
        resource.setRawUrl(UpdateSourceSupport.truncate(rawUrl, 255));
        resource.setSourceLanguage(detectLanguage(rawTitle + " " + rawContent));
        resource.setDisplayTitle(UpdateSourceSupport.truncate(displayTitle, 255));
        resource.setDisplaySummary(UpdateSourceSupport.truncate(displaySummary, 800));
        resource.setDisplayTags(UpdateSourceSupport.truncate(joinTagText(direction, displayCategory, displayStage), 255));
        resource.setDisplayCategory(displayCategory);
        resource.setDisplayLanguage("中文");
        resource.setDisplayDifficulty(normalizeResourceDifficulty(displayStage));
        resource.setDisplayStage(displayStage);
        resource.setNormalizedStatus("已整理");
        resource.setTitle(displayTitle);
        resource.setSummary(displaySummary);
        resource.setResourceType(displayCategory);
        resource.setTargetDirection(direction);
        resource.setStageTag(displayStage);
        resource.setUrl(UpdateSourceSupport.truncate(rawUrl, 255));
    }

    public String cleanImportedText(String text) {
        return cleanPlainText(text);
    }

    public boolean shouldRefreshInterviewQuestion(InterviewQuestion question) {
        if (question == null || !question.isOrganized()) {
            return question != null && !question.isOrganized();
        }
        String rawTitle = stripLeadingQuestionMarker(cleanPlainText(firstNonBlank(question.getRawTitle(), question.getQuestionText()))).toLowerCase(Locale.ROOT);
        String displayTitle = stripLeadingQuestionMarker(cleanPlainText(question.getDisplayTitle())).toLowerCase(Locale.ROOT);
        return rawTitle.matches("^(what|why|how|explain|describe|when)\\b.*")
                && (displayTitle.matches("^\\d+[.)、:：-]\\s*.*") || displayTitle.contains("you "));
    }

    private String resolvePreferredQuestionTitle(InterviewQuestion question, String rawTitle) {
        String preferredTitle = cleanPlainText(question.getDisplayTitle());
        if (StringUtils.hasText(preferredTitle) && !preferredTitle.equals(rawTitle)) {
            return ensureQuestionMark(stripLeadingQuestionMarker(preferredTitle));
        }
        return normalizeInterviewQuestionTitle(rawTitle);
    }

    private String normalizeInterviewQuestionTitle(String rawTitle) {
        String cleaned = stripLeadingQuestionMarker(cleanPlainText(rawTitle));
        if (!StringUtils.hasText(cleaned)) {
            return "请介绍一个与你目标岗位相关的经历？";
        }
        if (isChineseLike(cleaned)) {
            return ensureQuestionMark(cleaned);
        }

        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.startsWith("why ")) {
            return normalizeWhyQuestion(cleaned);
        }
        if (lower.startsWith("what is the difference between ")) {
            String tail = cleaned.substring("What is the difference between ".length());
            String[] parts = tail.split("(?i) and ");
            if (parts.length >= 2) {
                return ensureQuestionMark(localizeTerm(parts[0]) + "和" + localizeTerm(parts[1]) + "有什么区别");
            }
        }
        if (lower.startsWith("what is ") || lower.startsWith("what are ")) {
            return ensureQuestionMark("什么是" + localizeTerm(cleaned.replaceFirst("(?i)^what (is|are)\\s+", "")));
        }
        if (lower.startsWith("why ")) {
            return ensureQuestionMark("为什么要" + localizeActionTerm(cleaned.replaceFirst("(?i)^why (do we use|use|is|are|should we use)?\\s*", "")));
        }
        if (lower.startsWith("how do you ") || lower.startsWith("how would you ") || lower.startsWith("how can you ")) {
            return ensureQuestionMark("你会如何" + localizeActionTerm(cleaned.replaceFirst("(?i)^how (do|would|can) you\\s*", "")));
        }
        if (lower.startsWith("how does ") || lower.startsWith("how do ")) {
            String subject = cleaned.replaceFirst("(?i)^how (does|do)\\s*", "").replaceFirst("(?i)\\s+work(s)?$", "");
            return ensureQuestionMark(localizeTerm(subject) + "是如何工作的");
        }
        if (lower.startsWith("explain ")) {
            return ensureQuestionMark("请解释" + localizeTerm(cleaned.substring("Explain ".length())));
        }
        if (lower.startsWith("describe ")) {
            return ensureQuestionMark("请描述" + localizeTerm(cleaned.substring("Describe ".length())));
        }
        if (lower.startsWith("when would you ")) {
            return ensureQuestionMark("什么场景下你会" + localizeActionTerm(cleaned.replaceFirst("(?i)^when would you\\s*", "")));
        }
        return ensureQuestionMark(localizeTerm(cleaned));
    }

    private String normalizeWhyQuestion(String cleaned) {
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.startsWith("why do you need ") || lower.startsWith("why do we need ")) {
            return ensureQuestionMark("为什么需要" + localizeTerm(cleaned.replaceFirst("(?i)^why do (you|we) need\\s+", "")));
        }
        if (lower.startsWith("why need ")) {
            return ensureQuestionMark("为什么需要" + localizeTerm(cleaned.replaceFirst("(?i)^why need\\s+", "")));
        }
        if (lower.startsWith("why do you use ")
                || lower.startsWith("why do we use ")
                || lower.startsWith("why use ")
                || lower.startsWith("why should you use ")
                || lower.startsWith("why should we use ")) {
            return ensureQuestionMark("为什么要使用" + localizeTerm(cleaned.replaceFirst("(?i)^why (?:(?:do|should)\\s+)?(?:you|we)\\s+use\\s+|^why use\\s+", "")));
        }
        if (lower.startsWith("why is ") || lower.startsWith("why are ")) {
            return ensureQuestionMark("为什么" + localizeTerm(cleaned.replaceFirst("(?i)^why (is|are)\\s+", "")));
        }

        String action = cleaned.replaceFirst("(?i)^why\\s+", "");
        action = action.replaceFirst("(?i)^(do|does|did|can|could|should|would|is|are)\\s+", "");
        action = action.replaceFirst("(?i)^(you|we)\\s+", "");
        if (action.toLowerCase(Locale.ROOT).startsWith("need ")) {
            return ensureQuestionMark("为什么需要" + localizeTerm(action.substring("need ".length())));
        }
        if (action.toLowerCase(Locale.ROOT).startsWith("use ")) {
            return ensureQuestionMark("为什么要使用" + localizeTerm(action.substring("use ".length())));
        }
        return ensureQuestionMark("为什么" + localizeActionTerm(action));
    }

    private String normalizeInterviewCategory(String category, String rawTitle) {
        String value = (defaultText(category) + " " + defaultText(rawTitle)).toLowerCase(Locale.ROOT);
        if (containsAny(value, "自我介绍", "introduce yourself", "self introduction")) {
            return "自我介绍";
        }
        if (containsAny(value, "项目", "project", "experience")) {
            return "项目经历";
        }
        if (containsAny(value, "场景", "scenario", "case", "debug", "troubleshoot")) {
            return "场景题";
        }
        if (containsAny(value, "追问", "follow")) {
            return "追问题";
        }
        return "基础知识";
    }

    private String normalizeDifficulty(String difficulty) {
        String value = defaultText(difficulty).toLowerCase(Locale.ROOT);
        if (containsAny(value, "advanced", "hard", "困难", "进阶", "较难")) {
            return "进阶";
        }
        if (containsAny(value, "medium", "normal", "标准", "中等")) {
            return "中等";
        }
        return "基础";
    }

    private List<String> buildQuestionKeyPoints(String existingKeyPoints, String rawTitle, String direction) {
        List<String> keyPoints = extractItems(existingKeyPoints, 5);
        if (!keyPoints.isEmpty()) {
            return keyPoints;
        }
        String lower = defaultText(rawTitle).toLowerCase(Locale.ROOT);
        if (containsAny(lower, "difference", "区别")) {
            return List.of("核心差异", "适用场景");
        }
        if (containsAny(lower, "project", "项目")) {
            return List.of("项目背景", "关键动作", "结果复盘");
        }
        if (containsAny(lower, "thread", "process", "lock", "concurrency", "并发")) {
            return List.of("并发基础", "资源隔离");
        }
        if (containsAny(lower, "event loop", "promise", "closure", "javascript")) {
            return List.of("运行机制", "异步执行");
        }
        if (StringUtils.hasText(direction)) {
            return List.of(direction + "基础", "表达结构");
        }
        return List.of("关键知识点", "回答结构");
    }
    private List<String> buildQuestionTags(String direction, String category, String rawTitle, String existingTags) {
        List<String> tags = new ArrayList<>(extractItems(existingTags, 6));
        if (StringUtils.hasText(direction)) {
            tags.add(direction);
        }
        if (StringUtils.hasText(category)) {
            tags.add(category);
        }
        String lower = defaultText(rawTitle).toLowerCase(Locale.ROOT);
        if (containsAny(lower, "java", "spring")) {
            tags.add("Java");
        }
        if (containsAny(lower, "sql", "mysql", "database")) {
            tags.add("数据库");
        }
        if (containsAny(lower, "react", "javascript", "event loop", "web storage")) {
            tags.add("前端");
        }
        if (containsAny(lower, "product", "prd", "prototype")) {
            tags.add("产品");
        }
        return dedupe(tags, 6);
    }

    private String normalizeJobTitle(String rawTitle, String direction) {
        String cleaned = cleanPlainText(rawTitle);
        if (!StringUtils.hasText(cleaned)) {
            return StringUtils.hasText(direction) ? direction + "岗位" : "目标岗位";
        }
        if (isChineseLike(cleaned)) {
            return cleaned;
        }
        String localized = replaceEnglishTerms(cleaned);
        return StringUtils.hasText(direction) && !localized.contains(direction) ? direction + " · " + localized : localized;
    }

    private String inferJobStage(String rawTitle, String rawContent, String jobNature, String suitableGrade) {
        String value = (defaultText(rawTitle) + " " + defaultText(rawContent) + " " + defaultText(jobNature) + " " + defaultText(suitableGrade)).toLowerCase(Locale.ROOT);
        if (containsAny(value, "实习", "intern")) {
            return "实习投递";
        }
        if (containsAny(value, "校招", "应届", "graduate")) {
            return "校招准备";
        }
        return "通用岗位";
    }

    private String inferJobDifficulty(String displayStage, String rawTitle, String rawContent) {
        String value = (defaultText(displayStage) + " " + defaultText(rawTitle) + " " + defaultText(rawContent)).toLowerCase(Locale.ROOT);
        if (containsAny(value, "实习", "intern", "应届", "校招")) {
            return "入门";
        }
        if (containsAny(value, "高级", "资深", "senior", "架构")) {
            return "进阶";
        }
        return "标准";
    }

    private String defaultSuitableGrade(String displayStage) {
        if ("实习投递".equals(displayStage)) {
            return "大三 / 大四";
        }
        if ("校招准备".equals(displayStage)) {
            return "应届 / 研一至研三";
        }
        return "不限";
    }

    private String buildJobBonusFallback(JobPost jobPost) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(jobPost.getCompanyName())) {
            parts.add("来源单位：" + jobPost.getCompanyName().trim());
        }
        if (StringUtils.hasText(jobPost.getJobLocation())) {
            parts.add("工作地点：" + jobPost.getJobLocation().trim());
        }
        if (StringUtils.hasText(jobPost.getSourceName())) {
            parts.add("来源渠道：" + jobPost.getSourceName().trim());
        }
        return String.join("；", parts);
    }

    private String normalizeResourceCategory(String category) {
        String value = defaultText(category).toLowerCase(Locale.ROOT);
        if (containsAny(value, "docs", "documentation", "guide")) {
            return "官方文档";
        }
        if (containsAny(value, "course", "tutorial", "video")) {
            return "视频课程";
        }
        if (containsAny(value, "project", "demo")) {
            return "项目实战";
        }
        if (containsAny(value, "interview", "note", "handbook")) {
            return "面经 / 笔记";
        }
        return "教程文章";
    }

    private String normalizeResourceStage(String stage) {
        String value = defaultText(stage).toLowerCase(Locale.ROOT);
        if (containsAny(value, "beginner", "基础", "入门")) {
            return "入门阶段";
        }
        if (containsAny(value, "project", "实战")) {
            return "项目实战";
        }
        if (containsAny(value, "interview", "面试")) {
            return "面试冲刺";
        }
        return "进阶阶段";
    }

    private String normalizeResourceDifficulty(String stage) {
        if ("入门阶段".equals(stage)) {
            return "基础";
        }
        if ("项目实战".equals(stage) || "面试冲刺".equals(stage)) {
            return "进阶";
        }
        return "中等";
    }

    private String normalizeResourceTitle(String rawTitle, String category, String source) {
        String cleaned = cleanPlainText(rawTitle);
        if (!StringUtils.hasText(cleaned)) {
            return firstNonBlank(source, "学习资源");
        }
        if (isChineseLike(cleaned)) {
            return cleaned;
        }
        String localized = replaceEnglishTerms(cleaned);
        return StringUtils.hasText(category) && !localized.contains(category) ? localized + " · " + category : localized;
    }

    private String normalizeDirection(String value) {
        String text = defaultText(value).toLowerCase(Locale.ROOT);
        if (containsAny(text, "java", "backend", "后端", "spring", "mysql", "redis")) {
            return "Java后端";
        }
        if (containsAny(text, "frontend", "前端", "react", "vue", "javascript", "typescript")) {
            return "前端开发";
        }
        if (containsAny(text, "product", "prd", "pm", "产品")) {
            return "产品经理";
        }
        if (containsAny(text, "test", "qa", "测试")) {
            return "测试开发";
        }
        if (containsAny(text, "data", "analysis", "数据")) {
            return "数据分析";
        }
        if (containsAny(text, "operation", "运营", "增长")) {
            return "运营";
        }
        if (containsAny(text, "design", "设计", "ui", "ux")) {
            return "设计";
        }
        return StringUtils.hasText(value) ? value.trim() : "通用方向";
    }

    private List<String> extractItems(String text, int limit) {
        String cleaned = cleanPlainText(text);
        if (!StringUtils.hasText(cleaned)) {
            return List.of();
        }
        String normalized = cleaned.replace("；", "\n").replace(";", "\n").replace("。", "\n").replace(",", "\n").replace("，", "\n").replace("|", "\n");
        List<String> result = new ArrayList<>();
        for (String part : normalized.split("\\n")) {
            String item = stripLeadingQuestionMarker(cleanPlainText(part));
            if (!StringUtils.hasText(item)) {
                continue;
            }
            result.add(truncateText(item, 56));
            if (result.size() >= limit) {
                break;
            }
        }
        return dedupe(result, limit);
    }

    private List<String> dedupe(List<String> items, int limit) {
        Set<String> unique = new LinkedHashSet<>();
        for (String item : items) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            unique.add(item.trim());
            if (unique.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }
    private String joinTagText(String... items) {
        List<String> tags = new ArrayList<>();
        for (String item : items) {
            if (StringUtils.hasText(item)) {
                tags.add(item.trim());
            }
        }
        return String.join(" / ", dedupe(tags, 8));
    }

    private String localizeTerm(String term) {
        String value = replaceEnglishTerms(stripLeadingQuestionMarker(cleanPlainText(term)));
        value = value.replaceFirst("(?i)^the\\s+", "");
        value = value.replaceFirst("(?i)^(a|an)\\s+", "");
        value = QUESTION_MARK_PATTERN.matcher(value).replaceAll("");
        return value.trim();
    }

    private String localizeActionTerm(String term) {
        String value = localizeTerm(term);
        value = value.replaceFirst("(?i)^you\\s+", "");
        value = value.replaceFirst("(?i)^to\\s+", "");
        value = replaceIgnoreCase(value, "access ", "访问");
        value = replaceIgnoreCase(value, "retrieve ", "读取");
        value = replaceIgnoreCase(value, "store ", "存储");
        value = replaceIgnoreCase(value, "save ", "保存");
        value = replaceIgnoreCase(value, "handle ", "处理");
        value = replaceIgnoreCase(value, "debug ", "排查");
        value = replaceIgnoreCase(value, "test ", "测试");
        value = replaceIgnoreCase(value, "deploy ", "部署");
        value = replaceIgnoreCase(value, "build ", "构建");
        value = replaceIgnoreCase(value, "create ", "创建");
        value = replaceIgnoreCase(value, "manage ", "管理");
        value = replaceIgnoreCase(value, "use ", "使用");
        value = replaceIgnoreCase(value, "implement ", "实现");
        value = replaceIgnoreCase(value, "design ", "设计");
        value = replaceIgnoreCase(value, "optimize ", "优化");
        return value.trim();
    }

    private String replaceEnglishTerms(String text) {
        String localized = defaultText(text);
        localized = replaceIgnoreCase(localized, "Difference between", "区别");
        localized = replaceIgnoreCase(localized, "Guide", "指南");
        localized = replaceIgnoreCase(localized, "Tutorial", "教程");
        localized = replaceIgnoreCase(localized, "Course", "课程");
        localized = replaceIgnoreCase(localized, "Learn", "学习");
        localized = replaceIgnoreCase(localized, "Overview", "概览");
        localized = replaceIgnoreCase(localized, "Documentation", "官方文档");
        localized = replaceIgnoreCase(localized, "Docs", "文档");
        localized = replaceIgnoreCase(localized, "Project", "项目");
        localized = replaceIgnoreCase(localized, "Interview", "面试");
        localized = replaceIgnoreCase(localized, "Question", "问题");
        localized = replaceIgnoreCase(localized, "Questions", "问题");
        localized = replaceIgnoreCase(localized, "Backend", "后端");
        localized = replaceIgnoreCase(localized, "Front-end", "前端");
        localized = replaceIgnoreCase(localized, "Frontend", "前端");
        localized = replaceIgnoreCase(localized, "Product Manager", "产品经理");
        localized = replaceIgnoreCase(localized, "Data Analyst", "数据分析");
        localized = replaceIgnoreCase(localized, "Test Engineer", "测试工程师");
        localized = replaceIgnoreCase(localized, "Developer", "开发");
        localized = replaceIgnoreCase(localized, "Engineer", "工程师");
        localized = replaceIgnoreCase(localized, "Internship", "实习");
        localized = replaceIgnoreCase(localized, "Intern", "实习");
        localized = replaceIgnoreCase(localized, "Process", "进程");
        localized = replaceIgnoreCase(localized, "Thread", "线程");
        localized = replaceIgnoreCase(localized, "Closure", "闭包");
        localized = replaceIgnoreCase(localized, "Event Loop", "事件循环");
        localized = replaceIgnoreCase(localized, "State", "状态");
        localized = replaceIgnoreCase(localized, "Component", "组件");
        localized = replaceIgnoreCase(localized, "Index", "索引");
        localized = replaceIgnoreCase(localized, "Transaction", "事务");
        localized = replaceIgnoreCase(localized, "Dependency Injection", "依赖注入");
        localized = replaceIgnoreCase(localized, "Garbage Collection", "垃圾回收");
        localized = replaceIgnoreCase(localized, "Roadmap", "路线图");
        return localized;
    }

    private String stripLeadingQuestionMarker(String text) {
        String stripped = LIST_PREFIX_PATTERN.matcher(defaultText(text)).replaceFirst("").trim();
        return LEADING_NOISE_PATTERN.matcher(stripped).replaceFirst("").trim();
    }

    private String ensureQuestionMark(String text) {
        return QUESTION_MARK_PATTERN.matcher(defaultText(text)).replaceAll("") + "？";
    }

    private String cleanPlainText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalizedInput = defaultText(text).replace('\u00A0', ' ').replaceAll("(?i)<br\\s*/?>", "\n").replace('|', '\n');
        String stripped = Jsoup.parse(normalizedInput).text();
        String cleaned = UpdateSourceSupport.normalizeText(stripped).replace('\u3000', ' ');
        cleaned = cleaned.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");
        cleaned = cleaned.replaceAll("[\\r\\t\\f\\x0B]+", " ");
        return MULTI_SPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();
    }

    private String detectLanguage(String text) {
        String value = defaultText(text);
        if (!StringUtils.hasText(value)) {
            return "未知";
        }
        int chineseCount = 0;
        int letterCount = 0;
        for (char ch : value.toCharArray()) {
            if (isChineseChar(ch)) {
                chineseCount++;
            } else if (Character.isLetter(ch)) {
                letterCount++;
            }
        }
        if (chineseCount == 0 && letterCount > 0) {
            return "英文";
        }
        if (chineseCount > 0 && letterCount > 0) {
            return "中英混合";
        }
        return "中文";
    }

    private boolean isChineseLike(String text) {
        String language = detectLanguage(text);
        return "中文".equals(language) || "中英混合".equals(language);
    }

    private boolean isChineseChar(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    private boolean containsAny(String source, String... keywords) {
        String value = defaultText(source).toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (value.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String replaceIgnoreCase(String source, String target, String replacement) {
        return source.replaceAll("(?i)" + Pattern.quote(target), replacement);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
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

    private String truncateText(String value, int maxLength) {
        return UpdateSourceSupport.truncate(defaultText(value), maxLength);
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }
}
