package com.zhihang.jobagent.config;

import com.zhihang.jobagent.entity.InterviewQuestion;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.LearningResource;
import com.zhihang.jobagent.repository.InterviewQuestionRepository;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.LearningResourceRepository;
import com.zhihang.jobagent.service.ContentNormalizationService;
import com.zhihang.jobagent.service.InterviewQuestionTranslationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ContentNormalizationInitializer {

    @Bean
    public CommandLineRunner normalizeExternalContent(JobPostRepository jobPostRepository,
                                                      InterviewQuestionRepository interviewQuestionRepository,
                                                      LearningResourceRepository learningResourceRepository,
                                                      ContentNormalizationService contentNormalizationService,
                                                      InterviewQuestionTranslationService interviewQuestionTranslationService) {
        return args -> {
            List<JobPost> jobsToUpdate = new ArrayList<>();
            for (JobPost jobPost : jobPostRepository.findAll()) {
                if (!jobPost.isOrganized()) {
                    contentNormalizationService.normalizeJob(jobPost);
                    jobsToUpdate.add(jobPost);
                }
            }
            if (!jobsToUpdate.isEmpty()) {
                jobPostRepository.saveAll(jobsToUpdate);
            }

            List<InterviewQuestion> questionsToUpdate = new ArrayList<>();
            for (InterviewQuestion question : interviewQuestionRepository.findAll()) {
                if (!question.isOrganized() || contentNormalizationService.shouldRefreshInterviewQuestion(question)) {
                    questionsToUpdate.add(question);
                }
            }
            if (!questionsToUpdate.isEmpty()) {
                interviewQuestionTranslationService.translateTitlesIfNeeded(questionsToUpdate);
                for (InterviewQuestion question : questionsToUpdate) {
                    contentNormalizationService.normalizeInterviewQuestion(question);
                }
                interviewQuestionRepository.saveAll(questionsToUpdate);
            }

            List<LearningResource> resourcesToUpdate = new ArrayList<>();
            for (LearningResource resource : learningResourceRepository.findAll()) {
                if (!resource.isOrganized()) {
                    contentNormalizationService.normalizeLearningResource(resource);
                    resourcesToUpdate.add(resource);
                }
            }
            if (!resourcesToUpdate.isEmpty()) {
                learningResourceRepository.saveAll(resourcesToUpdate);
            }
        };
    }
}
