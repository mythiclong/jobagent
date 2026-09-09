package com.zhihang.jobagent.service.update.resource;

import com.zhihang.jobagent.service.update.ExternalFetchService;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class FreeCodeCampResourceSourceAdapter extends AbstractDocumentResourceAdapter {

    public FreeCodeCampResourceSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.resources.freecodecamp.enabled:false}") boolean enabled,
            @Value("${jobagent.update.resources.freecodecamp.source-name:FREECODECAMP}") String sourceName,
            @Value("${jobagent.update.resources.freecodecamp.url:https://www.freecodecamp.org/news/tag/javascript/}") String url) {
        super(externalFetchService, enabled, sourceName, url);
    }

    @Override
    public List<FetchedLearningResourceItem> fetchResources() throws IOException {
        Document document = fetchDocument();
        return parseLinks(document, "a[href*='/news/']", 24);
    }
}
