package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.CareerProfileCenterView;
import com.zhihang.jobagent.dto.ProfileSyncApplyResult;
import com.zhihang.jobagent.dto.ResumeParsedData;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.ResumeEnhanceRecordRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.CareerProfileCenterService;
import com.zhihang.jobagent.service.ResumeProfileSyncService;
import com.zhihang.jobagent.service.UserAccessService;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
public class CareerProfileController {

    private final JobPostRepository jobPostRepository;
    private final ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;
    private final UserProfileRepository userProfileRepository;
    private final CareerProfileCenterService careerProfileCenterService;
    private final ResumeProfileSyncService resumeProfileSyncService;
    private final UserAccessService userAccessService;

    public CareerProfileController(JobPostRepository jobPostRepository,
                                   ResumeEnhanceRecordRepository resumeEnhanceRecordRepository,
                                   UserProfileRepository userProfileRepository,
                                   CareerProfileCenterService careerProfileCenterService,
                                   ResumeProfileSyncService resumeProfileSyncService,
                                   UserAccessService userAccessService) {
        this.jobPostRepository = jobPostRepository;
        this.resumeEnhanceRecordRepository = resumeEnhanceRecordRepository;
        this.userProfileRepository = userProfileRepository;
        this.careerProfileCenterService = careerProfileCenterService;
        this.resumeProfileSyncService = resumeProfileSyncService;
        this.userAccessService = userAccessService;
    }

    @GetMapping("/profile-center")
    public String redirectToCareerProfile() {
        return "redirect:/career-profile";
    }

    @GetMapping("/career-profile")
    public ModelAndView showCareerProfile(@RequestParam(required = false) Long profileId,
                                          @RequestParam(required = false) Long jobId,
                                          @RequestParam(required = false) String syncStatus,
                                          @RequestParam(required = false) Integer syncCount) {
        List<UserProfile> profiles = userAccessService.findAccessibleProfiles();
        UserProfile selectedProfile = profiles.isEmpty() ? null : userAccessService.resolveSelectedProfile(profileId);
        String syncNotice = buildSyncNotice(syncStatus, syncCount);
        CareerProfileCenterView centerView = careerProfileCenterService.build(selectedProfile, jobId, syncNotice);

        ModelAndView modelAndView = new ModelAndView("career-profile/index");
        modelAndView.addObject("profiles", profiles);
        modelAndView.addObject("jobs", jobPostRepository.findAll());
        modelAndView.addObject("selectedProfileId", selectedProfile == null ? null : selectedProfile.getId());
        modelAndView.addObject("selectedJobId", centerView.getSelectedJob() == null ? jobId : centerView.getSelectedJob().getId());
        modelAndView.addObject("selectedProfile", selectedProfile);
        modelAndView.addObject("centerView", centerView);
        return modelAndView;
    }

    @PostMapping("/career-profile/preferences")
    public String updatePreferences(@RequestParam Long profileId,
                                    @RequestParam(required = false) Long jobId,
                                    @RequestParam(required = false) String targetRole,
                                    @RequestParam(required = false) String targetDirection,
                                    @RequestParam(required = false) String targetCity,
                                    @RequestParam(required = false) String acceptableCities,
                                    @RequestParam(required = false) String expectedSalary,
                                    @RequestParam(required = false) String acceptableJobNature,
                                    @RequestParam(required = false) String targetIndustry,
                                    @RequestParam(required = false) String careerGoal) {
        UserProfile profile = userAccessService.requireAccessibleProfile(profileId);
        profile.setTargetRole(normalize(targetRole));
        if (StringUtils.hasText(targetDirection)) {
            profile.setTargetDirection(targetDirection.trim());
        }
        profile.setTargetCity(normalize(targetCity));
        profile.setAcceptableCities(normalize(acceptableCities));
        profile.setExpectedSalary(normalize(expectedSalary));
        profile.setAcceptableJobNature(normalize(acceptableJobNature));
        profile.setTargetIndustry(normalize(targetIndustry));
        profile.setCareerGoal(normalize(careerGoal));
        userAccessService.bindCurrentUser(profile);
        userProfileRepository.save(profile);
        String jobParam = jobId == null ? "" : "&jobId=" + jobId;
        return "redirect:/career-profile?profileId=" + profileId + jobParam;
    }

    @PostMapping("/career-profile/fill-from-resume")
    public String fillFromResume(@RequestParam(required = false) Long userProfileId,
                                 @RequestParam(required = false) Long jobPostId,
                                 @RequestParam(required = false) Long resumeRecordId,
                                 @RequestParam(required = false) String inputType,
                                 @RequestParam(required = false) String originalResumeText,
                                 @RequestParam(required = false) String parsedBasicInfo,
                                 @RequestParam(required = false) String parsedEducation,
                                 @RequestParam(required = false) String parsedSkills,
                                 @RequestParam(required = false) String parsedProjects,
                                 @RequestParam(required = false) String parsedExperience,
                                 @RequestParam(required = false) String parsedOthers) {
        if (userProfileId != null) {
            userAccessService.requireAccessibleProfile(userProfileId);
        }
        JobPost jobPost = jobPostId == null ? null : jobPostRepository.findById(jobPostId).orElse(null);
        ResumeParsedData parsedData;
        if (resumeRecordId != null) {
            ResumeEnhanceRecord record = resumeEnhanceRecordRepository.findById(resumeRecordId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid resume enhance record id: " + resumeRecordId));
            userAccessService.assertCanAccessRecord(record.getUserAccountId(), record.getUserProfileId());
            parsedData = resumeProfileSyncService.toParsedData(record);
        } else {
            parsedData = new ResumeParsedData(
                    originalResumeText,
                    inputType,
                    parsedBasicInfo,
                    parsedEducation,
                    parsedSkills,
                    parsedProjects,
                    parsedExperience,
                    parsedOthers
            );
        }

        ProfileSyncApplyResult applyResult = resumeProfileSyncService.applyFill(userProfileId, parsedData, jobPost);
        Long savedProfileId = applyResult.getProfile().getId();
        String status = applyResult.isCreated() ? "created" : "updated";
        String jobParam = jobPostId == null ? "" : "&jobId=" + jobPostId;
        return "redirect:/career-profile?profileId=" + savedProfileId + jobParam
                + "&syncStatus=" + status + "&syncCount=" + applyResult.getUpdatedFieldCount();
    }

    private String buildSyncNotice(String syncStatus, Integer syncCount) {
        if (!StringUtils.hasText(syncStatus)) {
            return null;
        }
        int count = syncCount == null ? 0 : syncCount;
        if ("created".equalsIgnoreCase(syncStatus)) {
            return "已根据最近一次简历识别创建草稿档案，你可以继续确认并补全偏好。";
        }
        if ("updated".equalsIgnoreCase(syncStatus)) {
            if (count > 0) {
                return "已从最近一次简历识别补入 " + count + " 项档案信息。";
            }
            return "最近一次简历识别没有发现可补空白字段，当前档案保持不变。";
        }
        return null;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
