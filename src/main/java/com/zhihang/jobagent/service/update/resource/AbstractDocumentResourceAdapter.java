package com.zhihang.jobagent.service.update.resource;

import com.zhihang.jobagent.service.update.DirectionClassifier;
import com.zhihang.jobagent.service.update.ExternalFetchService;
import com.zhihang.jobagent.service.update.UpdateSourceSupport;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

abstract class AbstractDocumentResourceAdapter implements LearningResourceSourceAdapter {

    private final ExternalFetchService externalFetchService;
    private final boolean enabled;
    private final String sourceName;
    private final String url;

    protected AbstractDocumentResourceAdapter(ExternalFetchService externalFetchService,
                                              boolean enabled,
                                              String sourceName,
                                              String url) {
        this.externalFetchService = externalFetchService;
        this.enabled = enabled;
        this.sourceName = sourceName;
        this.url = url;
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public boolean isEnabled() {
        return enabled && StringUtils.hasText(url);
    }

    protected Document fetchDocument() throws IOException {
        return externalFetchService.fetchDocument(url);
    }

    protected String getUrl() {
        return url;
    }

    protected List<FetchedLearningResourceItem> parseLinks(Document document, String cssSelector, int limit) {
        Set<String> seen = new LinkedHashSet<>();
        List<FetchedLearningResourceItem> items = new ArrayList<>();
        Elements links = document.select(cssSelector);
        for (Element link : links) {
            String title = UpdateSourceSupport.normalizeText(link.text());
            String absUrl = UpdateSourceSupport.normalizeUrl(link.absUrl("href"));
            if (!StringUtils.hasText(title) || !StringUtils.hasText(absUrl)) {
                continue;
            }
            if (!seen.add(absUrl)) {
                continue;
            }
            String summary = buildSummary(link);
            items.add(new FetchedLearningResourceItem(
                    sourceName,
                    title,
                    absUrl,
                    DirectionClassifier.classifyResourceType(title, summary),
                    DirectionClassifier.classifyDirection(title, summary, ""),
                    DirectionClassifier.classifyStageTag(title, summary),
                    summary
            ));
            if (items.size() >= limit) {
                break;
            }
        }
        return items;
    }

    private String buildSummary(Element link) {
        String parentText = link.parent() == null ? "" : UpdateSourceSupport.normalizeText(link.parent().text());
        if (!StringUtils.hasText(parentText) || parentText.equals(UpdateSourceSupport.normalizeText(link.text()))) {
            parentText = link.attr("title");
        }
        return UpdateSourceSupport.normalizeText(parentText);
    }
}
