package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.InterviewQuestionGenerationResult;
import com.zhihang.jobagent.dto.InterviewQuestionSelectionResult;
import com.zhihang.jobagent.dto.InterviewSessionSummary;
import com.zhihang.jobagent.entity.InterviewQuestion;
import com.zhihang.jobagent.entity.InterviewRecord;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.InterviewQuestionRepository;
import com.zhihang.jobagent.repository.InterviewRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InterviewService {

    private static final int SESSION_QUESTION_COUNT = 4;
    private static final int MIN_RELEVANT_POOL_SIZE = 8;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9.+#/_-]{1,18}|[\\u4E00-\\u9FFF]{2,8}");

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewRecordRepository interviewRecordRepository;
    private final InterviewQuestionGenerationService interviewQuestionGenerationService;

    public InterviewService(InterviewQuestionRepository interviewQuestionRepository,
                            InterviewRecordRepository interviewRecordRepository,
                            InterviewQuestionGenerationService interviewQuestionGenerationService) {
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.interviewRecordRepository = interviewRecordRepository;
        this.interviewQuestionGenerationService = interviewQuestionGenerationService;
    }

    public String createSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public InterviewQuestionSelectionResult selectQuestions(UserProfile userProfile, JobPost jobPost) {
        List<InterviewQuestion> allQuestions = interviewQuestionRepository.findAllByOrderByIdAsc();
        List<QuestionCandidate> rankedCandidates = rankQuestions(allQuestions, userProfile, jobPost);

        List<InterviewQuestion> relevantQuestions = new ArrayList<>();
        for (QuestionCandidate candidate : rankedCandidates) {
            if (candidate.score() > 0) {
                relevantQuestions.add(candidate.question());
            }
        }

        InterviewQuestionGenerationResult generationResult = interviewQuestionGenerationService.generateQuestionsIfNeeded(
                userProfile,
                jobPost,
                relevantQuestions,
                MIN_RELEVANT_POOL_SIZE
        );
        if (generationResult.isAiSupplemented()) {
            allQuestions = new ArrayList<>(allQuestions);
            allQuestions.addAll(generationResult.getAcceptedQuestions());
            rankedCandidates = rankQuestions(allQuestions, userProfile, jobPost);
            relevantQuestions = new ArrayList<>();
            for (QuestionCandidate candidate : rankedCandidates) {
                if (candidate.score() > 0) {
                    relevantQuestions.add(candidate.question());
                }
            }
        }

        List<InterviewQuestion> selectedQuestions = pickSessionQuestions(rankedCandidates);
        if (selectedQuestions.isEmpty() && !allQuestions.isEmpty()) {
            selectedQuestions = allQuestions.subList(0, Math.min(SESSION_QUESTION_COUNT, allQuestions.size()));
        }

        String noticeMessage = generationResult.getNoticeMessage();
        if (!StringUtils.hasText(noticeMessage) && relevantQuestions.size() < SESSION_QUESTION_COUNT) {
            noticeMessage = "当前可用题目较少，本轮已优先使用题库中的核心题目。";
        }

        return new InterviewQuestionSelectionResult(
                selectedQuestions,
                noticeMessage,
                relevantQuestions.size(),
                generationResult.getAcceptedQuestions().size()
        );
    }

    public List<InterviewRecord> saveInterviewRecords(String sessionId,
                                                      UserProfile userProfile,
                                                      JobPost jobPost,
                                                      List<Long> questionIds,
                                                      List<String> userAnswers) {
        List<InterviewQuestion> questions = interviewQuestionRepository.findAllById(questionIds);
        Map<Long, InterviewQuestion> questionMap = new LinkedHashMap<>();
        for (InterviewQuestion question : questions) {
            questionMap.put(question.getId(), question);
        }

        LocalDateTime now = LocalDateTime.now();
        List<InterviewRecord> records = new ArrayList<>();
        for (int index = 0; index < questionIds.size(); index++) {
            Long questionId = questionIds.get(index);
            InterviewQuestion question = questionMap.get(questionId);
            if (question == null) {
                continue;
            }

            String answer = index < userAnswers.size() ? userAnswers.get(index) : "";
            EvaluationResult evaluationResult = evaluateAnswer(question, answer);

            InterviewRecord record = new InterviewRecord();
            record.setUserAccountId(userProfile.getUserAccountId());
            record.setUserProfileId(userProfile.getId());
            record.setJobPostId(jobPost.getId());
            record.setQuestionId(question.getId());
            record.setQuestionText(question.getQuestionText());
            record.setUserAnswer(defaultText(answer));
            record.setAnswerScore(evaluationResult.score());
            record.setAnswerComment(evaluationResult.comment());
            record.setImprovementSuggestion(evaluationResult.suggestion());
            record.setSessionId(sessionId);
            record.setCreatedAt(now);
            records.add(record);
        }

        return interviewRecordRepository.saveAll(records);
    }

    public List<InterviewRecord> findSessionRecords(String sessionId) {
        return interviewRecordRepository.findBySessionIdOrderByCreatedAtAscIdAsc(sessionId);
    }

    public List<InterviewSessionSummary> buildSessionSummaries(List<InterviewRecord> records) {
        Map<String, List<InterviewRecord>> groupedRecords = new LinkedHashMap<>();
        for (InterviewRecord record : records) {
            groupedRecords.computeIfAbsent(record.getSessionId(), key -> new ArrayList<>()).add(record);
        }

        List<InterviewSessionSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<InterviewRecord>> entry : groupedRecords.entrySet()) {
            List<InterviewRecord> sessionRecords = entry.getValue();
            InterviewRecord firstRecord = sessionRecords.get(0);
            summaries.add(new InterviewSessionSummary(
                    entry.getKey(),
                    firstRecord.getUserProfileId(),
                    firstRecord.getJobPostId(),
                    calculateTotalScore(sessionRecords),
                    sessionRecords.size(),
                    firstRecord.getCreatedAt()
            ));
        }
        return summaries;
    }

    public int calculateTotalScore(List<InterviewRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (InterviewRecord record : records) {
            total += record.getAnswerScore() == null ? 0 : record.getAnswerScore();
        }
        return total / records.size();
    }

    public String buildOverallComment(List<InterviewRecord> records) {
        int totalScore = calculateTotalScore(records);
        if (totalScore >= 85) {
            return "这轮回答整体完整，岗位相关性和表达结构都比较稳，可以继续打磨项目细节和结果表达。";
        }
        if (totalScore >= 70) {
            return "这轮回答已经有不错基础，建议继续补充项目职责、关键取舍和量化成果，把答案讲得更具体。";
        }
        if (totalScore >= 55) {
            return "这轮回答还停留在基础层，建议优先补齐项目细节、问题处理过程和结果复盘。";
        }
        return "这轮回答整体偏弱，建议先整理常见题型答案，再补齐岗位相关项目和表达结构。";
    }

    private List<QuestionCandidate> rankQuestions(List<InterviewQuestion> questions,
                                                  UserProfile userProfile,
                                                  JobPost jobPost) {
        Set<String> focusTokens = buildFocusTokens(userProfile, jobPost);
        String targetDirection = resolveTargetDirection(userProfile, jobPost);
        String preferredDifficulty = inferPreferredDifficulty(userProfile, jobPost);
        boolean hasProjectExperience = StringUtils.hasText(userProfile == null ? null : userProfile.getProjectExperience());

        List<QuestionCandidate> rankedQuestions = new ArrayList<>();
        for (InterviewQuestion question : questions) {
            int score = 0;
            String questionDirection = normalizeRoleCategory(question.getRoleCategory(), question.getJobDirection(), question.getDisplayTags());

            if (targetDirection.equals(questionDirection)) {
                score += 28;
            } else if (isAdjacentDirection(targetDirection, questionDirection)) {
                score += 14;
            } else if (!"通用岗位".equals(questionDirection)) {
                score -= 4;
            }

            Set<String> questionTokens = tokenize(String.join(" ",
                    defaultText(question.getDisplayTitle()),
                    defaultText(question.getDisplayTags()),
                    defaultText(question.getKeyPoints()),
                    defaultText(question.getReferenceAnswer()),
                    defaultText(question.getScoringRubric())
            ));
            score += Math.min(24, intersectCount(focusTokens, questionTokens) * 4);

            String category = defaultText(question.getDisplayCategory());
            if ("项目经历".equals(category)) {
                score += hasProjectExperience ? 8 : 2;
            } else if ("场景题".equals(category) || "追问题".equals(category)) {
                score += 6;
            } else if ("自我介绍".equals(category)) {
                score += 4;
            } else {
                score += 5;
            }

            String questionDifficulty = defaultText(question.getDisplayDifficulty());
            if (preferredDifficulty.equals(questionDifficulty)) {
                score += 6;
            } else if ("中等".equals(questionDifficulty) && "基础".equals(preferredDifficulty)) {
                score += 3;
            } else if ("基础".equals(questionDifficulty) && "进阶".equals(preferredDifficulty)) {
                score += 2;
            }

            if ("AI_GENERATED".equalsIgnoreCase(question.getSourceType()) && targetDirection.equals(questionDirection)) {
                score += 3;
            }
            if (question.getUpdatedAt() != null) {
                score += 1;
            }

            rankedQuestions.add(new QuestionCandidate(question, score));
        }

        rankedQuestions.sort(Comparator
                .comparingInt(QuestionCandidate::score).reversed()
                .thenComparing(candidate -> defaultText(candidate.question().getDisplayTitle())));
        return rankedQuestions;
    }

    private List<InterviewQuestion> pickSessionQuestions(List<QuestionCandidate> rankedCandidates) {
        List<InterviewQuestion> selectedQuestions = new ArrayList<>();
        Set<Long> selectedIds = new LinkedHashSet<>();
        List<String> preferredCategories = Arrays.asList("自我介绍", "项目经历", "基础知识", "场景题", "追问题");

        for (String category : preferredCategories) {
            for (QuestionCandidate candidate : rankedCandidates) {
                InterviewQuestion question = candidate.question();
                if (selectedIds.contains(question.getId())) {
                    continue;
                }
                if (!category.equals(defaultText(question.getDisplayCategory()))) {
                    continue;
                }
                if (candidate.score() <= 0 && !selectedQuestions.isEmpty()) {
                    continue;
                }
                selectedQuestions.add(question);
                if (question.getId() != null) {
                    selectedIds.add(question.getId());
                }
                break;
            }
            if (selectedQuestions.size() >= SESSION_QUESTION_COUNT) {
                return selectedQuestions;
            }
        }

        for (QuestionCandidate candidate : rankedCandidates) {
            InterviewQuestion question = candidate.question();
            if (selectedIds.contains(question.getId())) {
                continue;
            }
            if (candidate.score() <= 0 && !selectedQuestions.isEmpty() && selectedQuestions.size() >= 2) {
                continue;
            }
            selectedQuestions.add(question);
            if (question.getId() != null) {
                selectedIds.add(question.getId());
            }
            if (selectedQuestions.size() >= SESSION_QUESTION_COUNT) {
                break;
            }
        }
        return selectedQuestions;
    }

    private EvaluationResult evaluateAnswer(InterviewQuestion question, String answerText) {
        String answer = defaultText(answerText);
        int score = 60;
        List<String> comments = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> keyPoints = extractKeywords(question.getKeyPoints());
        List<String> missingKeyPoints = new ArrayList<>();

        if (answer.length() < 30) {
            score -= 25;
            comments.add("回答太短，信息量不足。");
            suggestions.add("建议至少按背景、做法、结果三个层次展开回答。");
        } else if (answer.length() < 60) {
            score -= 10;
            comments.add("回答有基础，但细节仍然偏少。");
            suggestions.add("可以补充项目场景、关键动作和最终结果。");
        } else {
            score += 10;
            comments.add("回答长度和展开程度较好。");
        }

        int keywordScore = 0;
        for (String keyPoint : keyPoints) {
            if (containsIgnoreCase(answer, keyPoint)) {
                keywordScore += 6;
            } else {
                missingKeyPoints.add(keyPoint);
            }
        }
        score += Math.min(keywordScore, 24);

        if (!missingKeyPoints.isEmpty()) {
            suggestions.add("建议补充这些考察点：" + String.join("、", missingKeyPoints));
        }

        if (containsAny(answer, "项目", "负责", "实现", "优化", "问题", "解决", "分析", "结果")) {
            score += 10;
            comments.add("回答里体现了项目动作和解决过程。");
        } else {
            suggestions.add("建议多使用“负责、实现、优化、解决”这类动作表达。");
        }

        if (containsNumber(answer)) {
            score += 6;
            comments.add("有一定量化结果表达。");
        } else {
            suggestions.add("可以加入数字化结果，例如耗时、效率、规模或性能提升幅度。");
        }

        List<String> scoringRubric = question.getScoringRubricItems();
        if (!scoringRubric.isEmpty() && answer.length() >= 60) {
            score += 4;
            comments.add("回答结构已经覆盖了题目要求的关键评分点。");
        }

        score = clamp(score, 0, 100);
        if (score >= 85) {
            comments.add("整体回答较完整，岗位匹配度较高。");
        } else if (score >= 70) {
            comments.add("整体回答有基础，但还可以更具体。");
        } else if (score >= 55) {
            comments.add("整体回答偏概括，亮点还不够突出。");
        } else {
            comments.add("整体回答偏弱，需要补更多项目细节和岗位相关内容。");
        }

        String comment = String.join(" ", deduplicate(comments));
        String suggestion = suggestions.isEmpty()
                ? "建议继续打磨表达结构，突出项目细节、岗位术语和量化成果。"
                : String.join(" ", deduplicate(suggestions));
        return new EvaluationResult(score, comment, suggestion);
    }

    private List<String> extractKeywords(String text) {
        Set<String> keywords = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(defaultText(text));
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.length() >= 2 && token.length() <= 18) {
                keywords.add(token);
            }
            if (keywords.size() >= 8) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }

    private Set<String> buildFocusTokens(UserProfile userProfile, JobPost jobPost) {
        String jobFocusedText = String.join(" ",
                defaultText(jobPost == null ? null : jobPost.getJobDirection()),
                defaultText(jobPost == null ? null : jobPost.getJobRequirements()),
                defaultText(jobPost == null ? null : jobPost.getBonusPoints()),
                defaultText(jobPost == null ? null : jobPost.getDisplayTitle()),
                defaultText(jobPost == null ? null : jobPost.getJobDescription())
        );
        if (StringUtils.hasText(jobFocusedText)) {
            return tokenize(jobFocusedText);
        }

        String profileFocusedText = String.join(" ",
                defaultText(userProfile == null ? null : userProfile.getTargetDirection()),
                defaultText(userProfile == null ? null : userProfile.getSkills()),
                defaultText(userProfile == null ? null : userProfile.getProjectExperience()),
                defaultText(userProfile == null ? null : userProfile.getCareerGoal())
        );
        return tokenize(profileFocusedText);
    }

    private String resolveTargetDirection(UserProfile userProfile, JobPost jobPost) {
        String selectedJobDirection = normalizeRoleCategory(
                jobPost == null ? null : jobPost.getJobDirection(),
                jobPost == null ? null : jobPost.getDisplayTitle(),
                jobPost == null ? null : jobPost.getJobRequirements(),
                jobPost == null ? null : jobPost.getBonusPoints()
        );
        if (!"通用岗位".equals(selectedJobDirection)) {
            return selectedJobDirection;
        }

        return normalizeRoleCategory(
                userProfile == null ? null : userProfile.getTargetDirection(),
                userProfile == null ? null : userProfile.getSkills(),
                userProfile == null ? null : userProfile.getCareerGoal()
        );
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(defaultText(text).toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.length() <= 1) {
                continue;
            }
            if (containsAny(token, "岗位", "要求", "熟悉", "了解", "负责", "相关", "能力")) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private int intersectCount(Set<String> left, Set<String> right) {
        int count = 0;
        for (String token : left) {
            if (right.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private String inferPreferredDifficulty(UserProfile userProfile, JobPost jobPost) {
        String combinedText = String.join(" ",
                defaultText(userProfile == null ? null : userProfile.getGrade()),
                defaultText(userProfile == null ? null : userProfile.getProjectExperience()),
                defaultText(jobPost == null ? null : jobPost.getJobNature()),
                defaultText(jobPost == null ? null : jobPost.getDisplayStage())
        ).toLowerCase(Locale.ROOT);

        if (containsAny(combinedText, "大一", "大二", "基础")) {
            return "基础";
        }
        if (containsAny(combinedText, "实习", "校招", "课程", "大三", "大四")) {
            return "中等";
        }
        return "进阶";
    }

    private String normalizeRoleCategory(String... candidates) {
        String joinedText = String.join(" ", candidates).toLowerCase(Locale.ROOT);
        if (containsAny(joinedText, "后端", "backend", "java", "spring", "mysql", "微服务")) {
            return "Java后端";
        }
        if (containsAny(joinedText, "前端", "frontend", "react", "vue", "javascript", "typescript")) {
            return "前端开发";
        }
        if (containsAny(joinedText, "产品", "product", "prd", "需求", "原型")) {
            return "产品经理";
        }
        if (containsAny(joinedText, "测试", "qa", "自动化测试", "接口测试", "jmeter")) {
            return "测试开发";
        }
        if (containsAny(joinedText, "数据", "data", "analysis", "python", "bi", "pandas")) {
            return "数据分析";
        }
        if (containsAny(joinedText, "运营", "operation", "growth", "投放", "活动运营")) {
            return "运营";
        }
        if (containsAny(joinedText, "设计", "design", "ui", "ux", "交互")) {
            return "设计";
        }
        return "通用岗位";
    }

    private boolean isAdjacentDirection(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        adjacency.put("Java后端", Set.of("测试开发", "数据分析"));
        adjacency.put("前端开发", Set.of("测试开发", "设计"));
        adjacency.put("产品经理", Set.of("运营", "数据分析", "设计"));
        adjacency.put("测试开发", Set.of("Java后端", "前端开发"));
        adjacency.put("数据分析", Set.of("产品经理", "运营", "Java后端"));
        adjacency.put("运营", Set.of("产品经理", "数据分析"));
        adjacency.put("设计", Set.of("前端开发", "产品经理"));
        return adjacency.getOrDefault(left, Set.of()).contains(right)
                || adjacency.getOrDefault(right, Set.of()).contains(left);
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

    private boolean containsIgnoreCase(String source, String target) {
        String sourceText = defaultText(source).toLowerCase(Locale.ROOT);
        String targetText = defaultText(target).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(sourceText) || !StringUtils.hasText(targetText)) {
            return false;
        }
        return sourceText.contains(targetText);
    }

    private boolean containsNumber(String text) {
        for (char ch : defaultText(text).toCharArray()) {
            if (Character.isDigit(ch)) {
                return true;
            }
        }
        return false;
    }

    private List<String> deduplicate(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record QuestionCandidate(InterviewQuestion question, int score) {
    }

    private record EvaluationResult(int score, String comment, String suggestion) {
    }
}
