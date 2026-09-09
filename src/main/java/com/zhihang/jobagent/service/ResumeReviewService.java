package com.zhihang.jobagent.service;

import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ResumeReviewService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*\\d+.*");

    private static final List<String> COMMON_SKILL_KEYWORDS = Arrays.asList(
            "Java", "Spring Boot", "Spring", "MySQL", "SQL", "Python",
            "JavaScript", "HTML", "CSS", "Linux", "测试", "自动化测试",
            "接口测试", "数据分析", "Excel", "产品", "运营", "UI", "设计"
    );

    public ResumeReview analyze(UserProfile userProfile, JobPost jobPost, String resumeText) {
        ResumeReview resumeReview = new ResumeReview();
        resumeReview.setUserAccountId(userProfile.getUserAccountId());
        resumeReview.setUserProfileId(userProfile.getId());
        resumeReview.setJobPostId(jobPost.getId());
        resumeReview.setResumeText(defaultText(resumeText));

        int score = 100;
        List<String> problems = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> missingKeywords = new ArrayList<>();

        String text = defaultText(resumeText);

        if (text.length() < 100) {
            score -= 25;
            problems.add("简历内容偏短，信息量不足");
            suggestions.add("建议补充教育背景、项目经历、技能说明和成果描述，正文尽量达到 100 字以上");
        } else if (text.length() < 180) {
            score -= 10;
            problems.add("简历内容略短，亮点表达还不够充分");
            suggestions.add("可以进一步补充项目职责、使用技术和完成结果");
        }

        if (!containsAny(text, "项目", "实习", "经历", "实践")) {
            score -= 20;
            problems.add("缺少项目或实习经历表达");
            suggestions.add("建议增加课程设计、比赛项目、社团实践或实习内容");
        }

        if (!NUMBER_PATTERN.matcher(text).matches()) {
            score -= 15;
            problems.add("缺少量化描述");
            suggestions.add("建议加入数字化成果，例如“提升 20%”“负责 3 个模块”“服务 100+ 用户”");
        }

        List<String> jobKeywords = extractKeywords(jobPost.getJobRequirements());
        for (String keyword : jobKeywords) {
            if (!containsIgnoreCase(text, keyword)) {
                missingKeywords.add(keyword);
                score -= 6;
            }
        }

        if (!missingKeywords.isEmpty()) {
            suggestions.add("建议在技能或项目经历中自然补充这些岗位关键词：" + String.join("、", missingKeywords));
        }

        boolean resumeMatchesDirection = containsEither(text, jobPost.getJobDirection())
                || containsEither(text, userProfile.getTargetDirection());
        if (!resumeMatchesDirection) {
            score -= 15;
            problems.add("简历内容与目标岗位方向关联不强");
            suggestions.add("建议突出与“" + jobPost.getJobDirection() + "”相关的技能、项目和成果");
        }

        if (!containsEither(userProfile.getTargetDirection(), jobPost.getJobDirection())) {
            score -= 10;
            problems.add("用户画像目标方向与岗位方向不完全一致");
            suggestions.add("如继续投递该岗位，建议在简历中补充与岗位方向相关的转向理由和能力证明");
        }

        score = clamp(score, 0, 100);

        resumeReview.setReviewScore(score);
        resumeReview.setSummary(buildSummary(score));
        resumeReview.setProblems(joinOrDefault(problems, "暂未发现明显问题，简历基础较完整。"));
        resumeReview.setMissingKeywords(joinOrDefault(missingKeywords, "无明显缺失关键词"));
        resumeReview.setSuggestions(joinOrDefault(suggestions, "建议继续优化项目描述，突出技能、成果和岗位相关性。"));

        return resumeReview;
    }

    private String buildSummary(int score) {
        if (score >= 85) {
            return "这份简历与目标岗位较匹配，已经具备基本投递条件，可重点优化表达细节。";
        }
        if (score >= 70) {
            return "这份简历有一定匹配度，但还需要补充岗位关键词和项目亮点。";
        }
        if (score >= 55) {
            return "这份简历基础一般，建议先完善关键信息后再投递。";
        }
        return "这份简历与目标岗位匹配度较低，建议先系统补强内容，再进行投递。";
    }

    private List<String> extractKeywords(String jobRequirements) {
        Set<String> keywords = new LinkedHashSet<>();
        String text = defaultText(jobRequirements);

        for (String keyword : COMMON_SKILL_KEYWORDS) {
            if (containsIgnoreCase(text, keyword)) {
                keywords.add(keyword);
            }
        }

        if (keywords.isEmpty() && !text.isEmpty()) {
            String[] parts = text.split("[，,。；;、/\\s\\n]+");
            for (String part : parts) {
                String keyword = part.trim();
                if (keyword.length() >= 2 && keyword.length() <= 12) {
                    keywords.add(keyword);
                }
                if (keywords.size() >= 6) {
                    break;
                }
            }
        }

        return new ArrayList<>(keywords);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (containsIgnoreCase(text, keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsEither(String left, String right) {
        String leftText = defaultText(left).toLowerCase();
        String rightText = defaultText(right).toLowerCase();
        if (leftText.isEmpty() || rightText.isEmpty()) {
            return false;
        }
        return leftText.contains(rightText) || rightText.contains(leftText);
    }

    private boolean containsIgnoreCase(String source, String target) {
        String sourceText = defaultText(source).toLowerCase();
        String targetText = defaultText(target).toLowerCase();
        if (sourceText.isEmpty() || targetText.isEmpty()) {
            return false;
        }
        return sourceText.contains(targetText);
    }

    private String joinOrDefault(List<String> values, String defaultValue) {
        if (values.isEmpty()) {
            return defaultValue;
        }
        return String.join("；", values);
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
