package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.UserAccessService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserProfileController {

    private final UserProfileRepository userProfileRepository;
    private final UserAccessService userAccessService;

    public UserProfileController(UserProfileRepository userProfileRepository,
                                 UserAccessService userAccessService) {
        this.userProfileRepository = userProfileRepository;
        this.userAccessService = userAccessService;
    }

    @GetMapping("/profiles")
    public String listProfiles(Model model) {
        model.addAttribute("profiles", userAccessService.findAccessibleProfiles());
        return "profiles/list";
    }

    @GetMapping("/profiles/new")
    public String showCreateForm(Model model) {
        model.addAttribute("userProfile", new UserProfile());
        return "profiles/form";
    }

    @PostMapping("/profiles")
    public String saveProfile(@ModelAttribute UserProfile userProfile) {
        UserProfile profileToSave = userProfile.getId() == null
                ? new UserProfile()
                : userAccessService.requireAccessibleProfile(userProfile.getId());
        profileToSave.setStudentName(userProfile.getStudentName());
        profileToSave.setGrade(userProfile.getGrade());
        profileToSave.setMajor(userProfile.getMajor());
        profileToSave.setTargetRole(userProfile.getTargetRole());
        profileToSave.setTargetDirection(userProfile.getTargetDirection());
        profileToSave.setSkills(userProfile.getSkills());
        profileToSave.setProjectExperience(userProfile.getProjectExperience());
        profileToSave.setTargetCity(userProfile.getTargetCity());
        profileToSave.setAcceptableCities(userProfile.getAcceptableCities());
        profileToSave.setExpectedSalary(userProfile.getExpectedSalary());
        profileToSave.setAcceptableJobNature(userProfile.getAcceptableJobNature());
        profileToSave.setTargetIndustry(userProfile.getTargetIndustry());
        profileToSave.setCareerGoal(userProfile.getCareerGoal());
        userAccessService.bindCurrentUser(profileToSave);
        UserProfile savedProfile = userProfileRepository.save(profileToSave);
        return "redirect:/career-profile?profileId=" + savedProfile.getId();
    }

    @GetMapping("/profiles/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("userProfile", userAccessService.requireAccessibleProfile(id));
        return "profiles/form";
    }

    @GetMapping("/profiles/delete/{id}")
    public String deleteProfile(@PathVariable Long id) {
        userProfileRepository.delete(userAccessService.requireAccessibleProfile(id));
        return "redirect:/career-profile";
    }
}
