package com.zhihang.jobagent.service.update.resource;

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
public class WhitelistBlogResourceSourceAdapter implements LearningResourceSourceAdapter {

    private final ExternalFetchService externalFetchService;
    private final boolean enabled;
    private final String sourceName;
    private final List<String> urls;

    public WhitelistBlogResourceSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.resources.blog-whitelist.enabled:false}") boolean enabled,
            @Value("${jobagent.update.resources.blog-whitelist.source-name:BLOG_WHITELIST}") String sourceName,
            @Value("${jobagent.update.resources.blog-whitelist.urls:}") String urlsValue) {
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
    public List<FetchedLearningResourceItem> fetchResources() throws IOException {
        List<FetchedLearningResourceItem> items = new ArrayList<>();
        for (String url : urls) {
            Document document = externalFetchService.fetchDocument(url);
            Elements links = document.select("a[href]");
            Set<String> seen = new LinkedHashSet<>();
            for (Element link : links) {
                String title = UpdateSourceSupport.normalizeText(link.text());
                String absUrl = UpdateSourceSupport.normalizeUrl(link.absUrl("href"));
                if (!StringUtils.hasText(title) || !StringUtils.hasText(absUrl) || !looksLikeLearningResource(title)) {
                    continue;
                }
                if (!seen.add(absUrl)) {
                    continue;
                }
                String summary = UpdateSourceSupport.normalizeText(link.parent() == null ? "" : link.parent().text());
                items.add(new FetchedLearningResourceItem(
                        sourceName,
                        title,
                        absUrl,
                        DirectionClassifier.classifyResourceType(title, summary),
                        DirectionClassifier.classifyDirection(title, summary, ""),
                        DirectionClassifier.classifyStageTag(title, summary),
                        summary
                ));
                if (items.size() >= 30) {
                    return items;
                }
            }
        }
        return items;
    }

    private boolean looksLikeLearningResource(String title) {
        String normalized = title.toLowerCase();
        return normalized.contains("教程")
                || normalized.contains("guide")
                || normalized.contains("learn")
                || normalized.contains("课程")
                || normalized.contains("spring")
                || normalized.contains("java")
                || normalized.contains("react")
                || normalized.contains("sql");
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
