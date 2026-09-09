package com.zhihang.jobagent.service.update.resource;

import java.io.IOException;
import java.util.List;

public interface LearningResourceSourceAdapter {

    String getSourceName();

    boolean isEnabled();

    List<FetchedLearningResourceItem> fetchResources() throws IOException;
}
