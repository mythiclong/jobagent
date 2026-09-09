package com.zhihang.jobagent.service.update.resource;

import com.zhihang.jobagent.service.update.ExternalFetchService;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class SpringGuideResourceSourceAdapter extends AbstractDocumentResourceAdapter {

    public SpringGuideResourceSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.resources.spring.enabled:true}") boolean enabled,
            @Value("${jobagent.update.resources.spring.source-name:SPRING_GUIDES}") String sourceName,
            @Value("${jobagent.update.resources.spring.url:https://spring.io/guides}") String url) {
        super(externalFetchService, enabled, sourceName, url);
    }

    @Override
    public List<FetchedLearningResourceItem> fetchResources() throws IOException {
        Document document = fetchDocument();
        return parseLinks(document, "a[href*='/guides/gs/']", 24);
    }
}
