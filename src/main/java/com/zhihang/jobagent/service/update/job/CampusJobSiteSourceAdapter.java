package com.zhihang.jobagent.service.update.job;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CampusJobSiteSourceAdapter extends AbstractWhitelistJobSourceAdapter {

    private static final Pattern ONCLICK_PATTERN = Pattern.compile(
            "articleClick\\(\"[^\"]*\",\"([^\"]+)\",\"([^\"]+)\"(?:,\"([^\"]*)\",\"([^\"]*)\")?.*"
    );

    public CampusJobSiteSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.jobs.campus.enabled:true}") boolean enabled,
            @Value("${jobagent.update.jobs.campus.source-name:CAMPUS_JOB_SITE}") String sourceName,
            @Value("${jobagent.update.jobs.campus.urls:https://job.henu.edu.cn/}") String urls) {
        super(externalFetchService, enabled, sourceName, urls);
    }

    @Override
    public List<FetchedJobItem> fetchJobs() throws IOException {
        List<FetchedJobItem> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String url : getUrls()) {
            Document document = fetchDocument(url);
            Elements rows = document.select(".articlesimplelist-infoTitle");
            for (Element row : rows) {
                Element link = row.selectFirst("a");
                if (link == null) {
                    continue;
                }
                String title = UpdateSourceSupport.normalizeText(
                        StringUtils.hasText(link.attr("title")) ? link.attr("title") : link.text()
                );
                if (!StringUtils.hasText(title)) {
                    continue;
                }
                String publishDate = UpdateSourceSupport.normalizeText(row.select("span.articlesimplelist-infoTime").text());
                String externalUrl = resolveCampusDetailUrl(link, url);
                if (!StringUtils.hasText(externalUrl) || !seen.add(externalUrl)) {
                    continue;
                }
                String summary = title;
                String direction = DirectionClassifier.classifyDirection(title, summary, "");
                items.add(new FetchedJobItem(
                        getSourceName(),
                        title,
                        extractCompanyName(title),
                        extractLocation(title),
                        publishDate,
                        externalUrl,
                        summary,
                        direction,
                        UpdateSourceSupport.sha256(getSourceName() + "|" + title + "|" + publishDate + "|" + externalUrl)
                ));
                if (items.size() >= 40) {
                    break;
                }
            }
        }
        return items;
    }

    private String resolveCampusDetailUrl(Element link, String baseUrl) {
        String href = UpdateSourceSupport.normalizeText(link.absUrl("href"));
        if (StringUtils.hasText(href) && !href.startsWith("javascript")) {
            return href;
        }

        String onclick = link.attr("onclick");
        if (!StringUtils.hasText(onclick)) {
            return "";
        }
        Matcher matcher = ONCLICK_PATTERN.matcher(onclick);
        if (!matcher.matches()) {
            return "";
        }
        String articleId = matcher.group(1);
        String nid = matcher.group(2);
        String outerFlag = matcher.group(3);
        String externalUrl = matcher.group(4);
        if ("True".equalsIgnoreCase(outerFlag) && StringUtils.hasText(externalUrl)) {
            return UpdateSourceSupport.normalizeUrl(externalUrl);
        }
        return UpdateSourceSupport.normalizeUrl(baseUrl + (baseUrl.endsWith("/") ? "" : "/")
                + "module/newsdetail/id-" + articleId + "/nid-" + nid);
    }
}
