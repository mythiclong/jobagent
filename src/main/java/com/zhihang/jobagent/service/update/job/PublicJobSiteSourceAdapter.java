package com.zhihang.jobagent.service.update.job;

import com.zhihang.jobagent.service.update.ExternalFetchService;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PublicJobSiteSourceAdapter extends AbstractWhitelistJobSourceAdapter {

    public PublicJobSiteSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.jobs.public.enabled:false}") boolean enabled,
            @Value("${jobagent.update.jobs.public.source-name:PUBLIC_RECRUIT_SOURCE}") String sourceName,
            @Value("${jobagent.update.jobs.public.urls:}") String urls) {
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
