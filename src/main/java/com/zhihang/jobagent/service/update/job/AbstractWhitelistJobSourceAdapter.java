package com.zhihang.jobagent.service.update.job;

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

abstract class AbstractWhitelistJobSourceAdapter implements JobSourceAdapter {

    private final ExternalFetchService externalFetchService;
    private final boolean enabled;
    private final String sourceName;
    private final List<String> urls;

    protected AbstractWhitelistJobSourceAdapter(ExternalFetchService externalFetchService,
                                                boolean enabled,
                                                String sourceName,
                                                String urlsValue) {
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

    protected List<String> getUrls() {
        return urls;
    }

    protected Document fetchDocument(String url) throws IOException {
        return externalFetchService.fetchDocument(url);
    }

    protected List<FetchedJobItem> parseGenericJobLinks(Document document, String sourceName) {
        Set<String> seen = new LinkedHashSet<>();
        List<FetchedJobItem> items = new ArrayList<>();
        Elements links = document.select("a[href]");
        for (Element link : links) {
            String title = UpdateSourceSupport.normalizeText(link.text());
            if (!looksLikeJobTitle(title)) {
                continue;
            }
            String absUrl = link.absUrl("href");
            if (!StringUtils.hasText(absUrl) || !seen.add(absUrl)) {
                continue;
            }
            String summary = title;
            String direction = DirectionClassifier.classifyDirection(title, summary, "");
            items.add(new FetchedJobItem(
                    sourceName,
                    title,
                    extractCompanyName(title),
                    extractLocation(title),
                    "",
                    absUrl,
                    summary,
                    direction,
                    UpdateSourceSupport.sha256(sourceName + "|" + title + "|" + absUrl)
            ));
            if (items.size() >= 30) {
                break;
            }
        }
        return items;
    }

    protected String extractCompanyName(String title) {
        String normalized = UpdateSourceSupport.normalizeText(title);
        int split = normalized.indexOf("招聘");
        if (split > 0) {
            return normalized.substring(0, split).trim();
        }
        split = normalized.indexOf("宣讲");
        if (split > 0) {
            return normalized.substring(0, split).trim();
        }
        return "";
    }

    protected String extractLocation(String title) {
        String normalized = UpdateSourceSupport.normalizeText(title);
        if (normalized.contains("广州")) {
            return "广州";
        }
        if (normalized.contains("深圳")) {
            return "深圳";
        }
        if (normalized.contains("上海")) {
            return "上海";
        }
        if (normalized.contains("北京")) {
            return "北京";
        }
        if (normalized.contains("郑州")) {
            return "郑州";
        }
        return "";
    }

    private boolean looksLikeJobTitle(String title) {
        String value = title.toLowerCase();
        return StringUtils.hasText(title)
                && (value.contains("招聘")
                || value.contains("校招")
                || value.contains("宣讲")
                || value.contains("实习")
                || value.contains("工程师")
                || value.contains("产品经理"));
    }

    private List<String> splitUrls(String urlsValue) {
        if (!StringUtils.hasText(urlsValue)) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        for (String item : urlsValue.split(",")) {
            String normalized = UpdateSourceSupport.normalizeUrl(item);
            if (StringUtils.hasText(normalized)) {
                urls.add(normalized);
            }
        }
        return urls;
    }
}
