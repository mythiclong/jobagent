package com.zhihang.jobagent.config;

import com.zhihang.jobagent.entity.UserAccount;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.entity.UserRole;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.UserAccountService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Configuration
public class DemoAccountInitializer {

    private static final String DEMO_GRADE = "\u5927\u4e09";
    private static final String DEMO_MAJOR = "\u8f6f\u4ef6\u5de5\u7a0b";
    private static final String DEMO_DIRECTION = "Java \u540e\u7aef";
    private static final String DEMO_TARGET_ROLE = "Java \u540e\u7aef\u5f00\u53d1\u5b9e\u4e60\u751f";
    private static final String DEMO_SKILLS = "Java\u3001Spring Boot\u3001MySQL\u3001Git";
    private static final String DEMO_PROJECT_EXPERIENCE =
            "\u505a\u8fc7\u8bfe\u7a0b\u7ba1\u7406\u7cfb\u7edf\u548c\u6821\u62db\u5c97\u4f4d\u5206\u6790\u5e73\u53f0\uff0c" +
                    "\u8d1f\u8d23\u63a5\u53e3\u8bbe\u8ba1\u3001\u6743\u9650\u6d41\u7a0b\u548c\u6570\u636e\u770b\u677f\u3002";
    private static final String DEMO_EXPECTED_CITY = "\u4e0a\u6d77";
    private static final String DEMO_ACCEPTABLE_CITIES = "\u4e0a\u6d77\u3001\u676d\u5dde\u3001\u82cf\u5dde";
    private static final String DEMO_EXPECTED_SALARY = "6k-9k/\u6708";
    private static final String DEMO_ACCEPTABLE_JOB_NATURE = "\u5b9e\u4e60 / \u6821\u62db";
    private static final String DEMO_TARGET_INDUSTRY = "\u4f01\u4e1a\u670d\u52a1 / SaaS";
    private static final String DEMO_CAREER_GOAL =
            "\u5148\u62ff\u5230\u540e\u7aef\u5b9e\u4e60\uff0c\u518d\u6301\u7eed\u8865\u9f50\u9879\u76ee\u6df1\u5ea6\u548c\u9762\u8bd5\u8868\u8fbe\u3002";

    @Bean
    ApplicationRunner demoAccountRunner(UserAccountService userAccountService,
                                        UserProfileRepository userProfileRepository,
                                        DemoAccountProperties demoAccountProperties) {
        return args -> initializeDemoAccounts(userAccountService, userProfileRepository, demoAccountProperties);
    }

    @Transactional
    void initializeDemoAccounts(UserAccountService userAccountService,
                                UserProfileRepository userProfileRepository,
                                DemoAccountProperties demoAccountProperties) {
        userAccountService.upsertSeedAccount(
                demoAccountProperties.getAdminUsername(),
                demoAccountProperties.getAdminDisplayName(),
                demoAccountProperties.getAdminEmail(),
                demoAccountProperties.getAdminPassword(),
                UserRole.ADMIN
        );

        UserAccount demoUser = userAccountService.upsertSeedAccount(
                demoAccountProperties.getUserUsername(),
                demoAccountProperties.getUserDisplayName(),
                demoAccountProperties.getUserEmail(),
                demoAccountProperties.getUserPassword(),
                UserRole.USER
        );

        if (!demoAccountProperties.isSeedDemoProfile()) {
            return;
        }

        syncDemoProfile(userProfileRepository, demoUser, demoAccountProperties);
    }

    private void syncDemoProfile(UserProfileRepository userProfileRepository,
                                 UserAccount demoUser,
                                 DemoAccountProperties demoAccountProperties) {
        UserProfile profile = userProfileRepository.findFirstByUserAccountIdOrderByIdAsc(demoUser.getId())
                .orElseGet(UserProfile::new);

        if (profile.getId() != null && !shouldRepairDemoProfile(profile)) {
            return;
        }

        profile.setUserAccountId(demoUser.getId());
        profile.setStudentName(demoAccountProperties.getUserDisplayName());
        profile.setGrade(DEMO_GRADE);
        profile.setMajor(DEMO_MAJOR);
        profile.setTargetRole(DEMO_TARGET_ROLE);
        profile.setTargetDirection(DEMO_DIRECTION);
        profile.setSkills(DEMO_SKILLS);
        profile.setProjectExperience(DEMO_PROJECT_EXPERIENCE);
        profile.setTargetCity(DEMO_EXPECTED_CITY);
        profile.setAcceptableCities(DEMO_ACCEPTABLE_CITIES);
        profile.setExpectedSalary(DEMO_EXPECTED_SALARY);
        profile.setAcceptableJobNature(DEMO_ACCEPTABLE_JOB_NATURE);
        profile.setTargetIndustry(DEMO_TARGET_INDUSTRY);
        profile.setCareerGoal(DEMO_CAREER_GOAL);
        userProfileRepository.save(profile);
    }

    private boolean shouldRepairDemoProfile(UserProfile profile) {
        return !StringUtils.hasText(profile.getStudentName())
                || !StringUtils.hasText(profile.getGrade())
                || !StringUtils.hasText(profile.getMajor())
                || !StringUtils.hasText(profile.getTargetRole())
                || !StringUtils.hasText(profile.getTargetDirection())
                || containsBrokenText(profile.getStudentName())
                || containsBrokenText(profile.getGrade())
                || containsBrokenText(profile.getMajor())
                || containsBrokenText(profile.getTargetRole())
                || containsBrokenText(profile.getTargetDirection())
                || containsBrokenText(profile.getSkills())
                || containsBrokenText(profile.getProjectExperience())
                || containsBrokenText(profile.getTargetCity())
                || containsBrokenText(profile.getAcceptableCities())
                || containsBrokenText(profile.getExpectedSalary())
                || containsBrokenText(profile.getAcceptableJobNature())
                || containsBrokenText(profile.getTargetIndustry())
                || containsBrokenText(profile.getCareerGoal());
    }

    private boolean containsBrokenText(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.contains("\uFFFD")
                || value.contains("\u951b")
                || value.contains("\u9286")
                || value.contains("\u9225")
                || value.contains("\u6f84\u4f74\u7b01")
                || value.contains("\u676e\ue219\u4ef7")
                || value.contains("\u5a15\u65f7\u793a")
                || value.contains("\u7eef\u8354\u7cb8\u7edd")
                || value.contains("\u6d93\u5a1b\u6340");
    }
}
