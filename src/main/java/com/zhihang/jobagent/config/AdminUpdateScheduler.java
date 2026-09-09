package com.zhihang.jobagent.config;

import com.zhihang.jobagent.service.InterviewQuestionUpdateService;
import com.zhihang.jobagent.service.JobUpdateService;
import com.zhihang.jobagent.service.LearningResourceUpdateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdminUpdateScheduler {

    private final JobUpdateService jobUpdateService;
    private final InterviewQuestionUpdateService interviewQuestionUpdateService;
    private final LearningResourceUpdateService learningResourceUpdateService;

    @Value("${jobagent.update-center.schedule-enabled:false}")
    private boolean scheduleEnabled;

    public AdminUpdateScheduler(JobUpdateService jobUpdateService,
                                InterviewQuestionUpdateService interviewQuestionUpdateService,
                                LearningResourceUpdateService learningResourceUpdateService) {
        this.jobUpdateService = jobUpdateService;
        this.interviewQuestionUpdateService = interviewQuestionUpdateService;
        this.learningResourceUpdateService = learningResourceUpdateService;
    }

    @Scheduled(cron = "${jobagent.update-center.schedule-cron:0 0 3 * * *}")
    public void runScheduledUpdates() {
        if (!scheduleEnabled) {
            return;
        }
        jobUpdateService.runScheduledUpdate();
        interviewQuestionUpdateService.runScheduledUpdate();
        learningResourceUpdateService.runScheduledUpdate();
    }
}
