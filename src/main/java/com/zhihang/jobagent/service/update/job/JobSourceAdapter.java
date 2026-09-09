package com.zhihang.jobagent.service.update.job;

import java.io.IOException;
import java.util.List;

public interface JobSourceAdapter {

    String getSourceName();

    boolean isEnabled();

    List<FetchedJobItem> fetchJobs() throws IOException;
}
