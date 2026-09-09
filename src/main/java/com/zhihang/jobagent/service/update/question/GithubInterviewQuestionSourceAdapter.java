package com.zhihang.jobagent.service.update.question;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihang.jobagent.service.update.DirectionClassifier;
import com.zhihang.jobagent.service.update.ExternalFetchService;
import com.zhihang.jobagent.service.update.UpdateSourceSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GithubInterviewQuestionSourceAdapter implements InterviewQuestionSourceAdapter {

    private static final Pattern TABLE_OF_CONTENTS_PATTERN = Pattern.compile("\\|\\s*\\d+\\s*\\|\\s*\\[(.+?)]");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#{2,4}\\s+(.+)$");

    private final ExternalFetchService externalFetchService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final List<String> repos;
    private final String sourceName;

    public GithubInterviewQuestionSourceAdapter(
            ExternalFetchService externalFetchService,
            ObjectMapper objectMapper,
            @Value("${jobagent.update.questions.github.enabled:true}") boolean enabled,
            @Value("${jobagent.update.questions.github.source-name:GITHUB_PUBLIC_REPO}") String sourceName,
            @Value("${jobagent.update.questions.github.repos:sudheerj/javascript-interview-questions,sudheerj/reactjs-interview-questions,arialdomartini/Back-End-Developer-Interview-Questions,yangshun/tech-interview-handbook}") String reposValue) {
        this.externalFetchService = externalFetchService;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.sourceName = sourceName;
        this.repos = splitRepos(reposValue);
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public boolean isEnabled() {
        return enabled && !repos.isEmpty();
    }

    @Override
    public List<FetchedInterviewQuestionItem> fetchQuestions() throws IOException {
        List<FetchedInterviewQuestionItem> items = new ArrayList<>();
        for (String repo : repos) {
            String apiUrl = "https://api.github.com/repos/" + repo + "/contents/README.md";
            JsonNode root = objectMapper.readTree(externalFetchService.fetchText(apiUrl));
            String htmlUrl = root.path("html_url").asText("");
            String content = decodeContent(root.path("content").asText(""));
            items.addAll(parseMarkdownQuestions(repo, htmlUrl, content));
        }
        return items;
    }

    private List<FetchedInterviewQuestionItem> parseMarkdownQuestions(String repo, String htmlUrl, String markdown) {
        Set<String> seen = new LinkedHashSet<>();
        List<FetchedInterviewQuestionItem> items = new ArrayList<>();

        Matcher tocMatcher = TABLE_OF_CONTENTS_PATTERN.matcher(markdown);
        while (tocMatcher.find()) {
            appendQuestion(items, seen, repo, htmlUrl, tocMatcher.group(1));
            if (items.size() >= 80) {
                return items;
            }
        }

        if (items.size() >= 20) {
            return items;
        }

        String[] lines = markdown.split("\\R");
        for (String line : lines) {
            Matcher headingMatcher = HEADING_PATTERN.matcher(UpdateSourceSupport.normalizeText(line));
            if (!headingMatcher.matches()) {
                continue;
            }
            String question = headingMatcher.group(1);
            if (!looksLikeQuestion(question)) {
                continue;
            }
            appendQuestion(items, seen, repo, htmlUrl, question);
            if (items.size() >= 80) {
                break;
            }
        }
        return items;
    }

    private void appendQuestion(List<FetchedInterviewQuestionItem> items,
                                Set<String> seen,
                                String repo,
                                String htmlUrl,
                                String rawQuestion) {
        String question = UpdateSourceSupport.normalizeText(rawQuestion)
                .replace("`", "")
                .replace("*", "");
        if (!looksLikeQuestion(question)) {
            return;
        }
        String topicTag = DirectionClassifier.classifyQuestionTag(question, repo);
        String questionHash = UpdateSourceSupport.sha256(topicTag + "|" + question);
        if (!seen.add(questionHash)) {
            return;
        }
        items.add(new FetchedInterviewQuestionItem(
                sourceName + ":" + repo,
                htmlUrl,
                question,
                question,
                topicTag,
                "Curated from GitHub public interview repository: " + repo,
                questionHash
        ));
    }

    private boolean looksLikeQuestion(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.toLowerCase();
        return normalized.contains("?")
                || normalized.startsWith("what ")
                || normalized.startsWith("how ")
                || normalized.startsWith("why ")
                || normalized.startsWith("when ")
                || normalized.contains("什么")
                || normalized.contains("如何")
                || normalized.contains("区别");
    }

    private String decodeContent(String content) {
        String normalized = content.replace("\n", "");
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return new String(Base64.getDecoder().decode(normalized), StandardCharsets.UTF_8);
    }

    private List<String> splitRepos(String value) {
        List<String> results = new ArrayList<>();
        if (!StringUtils.hasText(value)) {
            return results;
        }
        for (String repo : value.split(",")) {
            String normalized = UpdateSourceSupport.normalizeText(repo);
            if (StringUtils.hasText(normalized)) {
                results.add(normalized);
            }
        }
        return results;
    }
}
