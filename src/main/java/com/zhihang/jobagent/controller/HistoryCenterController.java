package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.InterviewSessionSummary;
import com.zhihang.jobagent.entity.JobMatchRecord;
import com.zhihang.jobagent.entity.LearningPathAdvice;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.service.HistoryCenterService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HistoryCenterController {

    private final HistoryCenterService historyCenterService;

    public HistoryCenterController(HistoryCenterService historyCenterService) {
        this.historyCenterService = historyCenterService;
    }

    @GetMapping({"/history-center", "/history-center/"})
    public String showCenter(Model model) {
        List<JobMatchRecord> matchRecords = historyCenterService.loadMatchRecords();
        List<ResumeReview> resumeReviews = historyCenterService.loadResumeReviews();
        List<ResumeEnhanceRecord> resumeEnhanceRecords = historyCenterService.loadResumeEnhanceRecords();
        List<InterviewSessionSummary> interviewSessions = historyCenterService.loadInterviewSessions();
        List<LearningPathAdvice> strengthPlanRecords = historyCenterService.loadStrengthPlanRecords();

        model.addAttribute("matchRecords", matchRecords);
        model.addAttribute("resumeReviews", resumeReviews);
        model.addAttribute("resumeEnhanceRecords", resumeEnhanceRecords);
        model.addAttribute("interviewSessions", interviewSessions.stream().limit(8).toList());
        model.addAttribute("learningPathRecords", strengthPlanRecords);
        model.addAttribute("strengthPlanRecords", strengthPlanRecords);
        model.addAttribute("latestJourney", historyCenterService.buildLatestJourney(
                matchRecords,
                resumeReviews,
                resumeEnhanceRecords,
                strengthPlanRecords,
                interviewSessions
        ));
        model.addAttribute("userNameMap", historyCenterService.buildUserNameMap());
        model.addAttribute("jobNameMap", historyCenterService.buildJobNameMap());
        model.addAttribute("strengthPlanRelatedMatchSummaryMap", historyCenterService.buildStrengthPlanRelatedMatchSummaryMap(strengthPlanRecords));
        model.addAttribute("strengthPlanRelatedMatchIdMap", historyCenterService.buildStrengthPlanRelatedMatchIdMap(strengthPlanRecords));
        model.addAttribute("strengthPlanRelatedResumeIdMap", historyCenterService.buildStrengthPlanRelatedResumeEnhanceIdMap(strengthPlanRecords));
        return "history-center";
    }
}
