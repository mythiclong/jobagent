package com.zhihang.jobagent.service.update.resource;

import com.zhihang.jobagent.service.update.ExternalFetchService;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class MdnLearnResourceSourceAdapter extends AbstractDocumentResourceAdapter {

    public MdnLearnResourceSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.resources.mdn.enabled:true}") boolean enabled,
            @Value("${jobagent.update.resources.mdn.source-name:MDN_LEARN}") String sourceName,
            @Value("${jobagent.update.resources.mdn.url:https://developer.mozilla.org/en-US/docs/Learn_web_development}") String url) {
        super(externalFetchService, enabled, sourceName, url);
    }

    @Override
    public List<FetchedLearningResourceItem> fetchResources() throws IOException {
        Document document = fetchDocument();
        return parseLinks(document, "main a[href*='/docs/Learn']", 24);
    }
}
