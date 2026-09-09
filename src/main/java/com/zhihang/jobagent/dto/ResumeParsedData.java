package com.zhihang.jobagent.dto;

public class ResumeParsedData {

    private final String originalText;
    private final String inputType;
    private final String basicInfo;
    private final String education;
    private final String skills;
    private final String projects;
    private final String experience;
    private final String others;

    public ResumeParsedData(String originalText, String inputType, String basicInfo, String education,
                            String skills, String projects, String experience, String others) {
        this.originalText = originalText;
        this.inputType = inputType;
        this.basicInfo = basicInfo;
        this.education = education;
        this.skills = skills;
        this.projects = projects;
        this.experience = experience;
        this.others = others;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getInputType() {
        return inputType;
    }

    public String getBasicInfo() {
        return basicInfo;
    }

    public String getEducation() {
        return education;
    }

    public String getSkills() {
        return skills;
    }

    public String getProjects() {
        return projects;
    }

    public String getExperience() {
        return experience;
    }

    public String getOthers() {
        return others;
    }
}
