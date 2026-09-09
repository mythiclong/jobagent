package com.zhihang.jobagent.service.update.job;

import com.zhihang.jobagent.service.update.ExternalFetchService;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class GuangdongPublicJobSourceAdapter extends AbstractWhitelistJobSourceAdapter {

    public GuangdongPublicJobSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.jobs.guangdong.enabled:false}") boolean enabled,
            @Value("${jobagent.update.jobs.guangdong.source-name:GD_PUBLIC_JOB}") String sourceName,
            @Value("${jobagent.update.jobs.guangdong.urls:}") String urls) {
        super(externalFetchService, enabled, sourceName, urls);
    }

    @Override
    public List<FetchedJobItem> fetchJobs() throws IOException {
        List<FetchedJobItem> items = new ArrayList<>();
        for (String url : getUrls()) {
            Document document = fetchDocument(url);
            items.addAll(parseGenericJobLinks(document, getSourceName()));
        }
        return items;
    }
}
