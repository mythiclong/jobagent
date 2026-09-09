package com.zhihang.jobagent.service.update.question;

import com.zhihang.jobagent.service.update.DirectionClassifier;
import com.zhihang.jobagent.service.update.ExternalFetchService;
import com.zhihang.jobagent.service.update.UpdateSourceSupport;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PublicInterviewArticleSourceAdapter implements InterviewQuestionSourceAdapter {

    private final ExternalFetchService externalFetchService;
    private final boolean enabled;
    private final String sourceName;
    private final List<String> urls;

    public PublicInterviewArticleSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.questions.public.enabled:false}") boolean enabled,
            @Value("${jobagent.update.questions.public.source-name:PUBLIC_INTERVIEW_ARTICLE}") String sourceName,
            @Value("${jobagent.update.questions.public.urls:}") String urlsValue) {
        this.externalFetchService = externalFetchService;
        this.enabled = enabled;
        this.sourceName = sourceName;
        this.urls = splitUrls(urlsValue);
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public boolean isEnabled() {
        return enabled && !urls.isEmpty();
    }

    @Override
    public List<FetchedInterviewQuestionItem> fetchQuestions() throws IOException {
        List<FetchedInterviewQuestionItem> items = new ArrayList<>();
        for (String url : urls) {
            Document document = externalFetchService.fetchDocument(url);
            Elements candidates = document.select("h1, h2, h3, li, strong");
            Set<String> seen = new LinkedHashSet<>();
            for (Element candidate : candidates) {
                String text = UpdateSourceSupport.normalizeText(candidate.text());
                if (!looksLikeQuestion(text)) {
                    continue;
                }
                String topicTag = DirectionClassifier.classifyQuestionTag(text, sourceName);
                String hash = UpdateSourceSupport.sha256(topicTag + "|" + text);
                if (!seen.add(hash)) {
                    continue;
                }
                items.add(new FetchedInterviewQuestionItem(
                        sourceName,
                        url,
                        text,
                        text,
                        topicTag,
                        "Extracted from whitelist interview article",
                        hash
                ));
                if (items.size() >= 60) {
                    return items;
                }
            }
        }
        return items;
    }

    private boolean looksLikeQuestion(String text) {
        return StringUtils.hasText(text)
                && (text.contains("?")
                || text.contains("？")
                || text.contains("什么")
                || text.contains("如何")
                || text.contains("为什么")
                || text.contains("区别"));
    }

    private List<String> splitUrls(String value) {
        List<String> results = new ArrayList<>();
        if (!StringUtils.hasText(value)) {
            return results;
        }
        for (String url : value.split(",")) {
            String normalized = UpdateSourceSupport.normalizeUrl(url);
            if (StringUtils.hasText(normalized)) {
                results.add(normalized);
            }
        }
        return results;
    }
}
