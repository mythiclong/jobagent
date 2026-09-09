package com.zhihang.jobagent.dto;

import java.util.ArrayList;
import java.util.List;

public class AiInterviewQuestionDraft {

    private String question;

    private String category;

    private String difficulty;

    private List<String> expectedPoints = new ArrayList<>();

    private List<String> answerOutline = new ArrayList<>();

    private List<String> scoringRubric = new ArrayList<>();

    private List<String> tags = new ArrayList<>();

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getExpectedPoints() {
        return expectedPoints;
    }

    public void setExpectedPoints(List<String> expectedPoints) {
        this.expectedPoints = expectedPoints == null ? new ArrayList<>() : new ArrayList<>(expectedPoints);
    }

    public List<String> getAnswerOutline() {
        return answerOutline;
    }

    public void setAnswerOutline(List<String> answerOutline) {
        this.answerOutline = answerOutline == null ? new ArrayList<>() : new ArrayList<>(answerOutline);
    }

    public List<String> getScoringRubric() {
        return scoringRubric;
    }

    public void setScoringRubric(List<String> scoringRubric) {
        this.scoringRubric = scoringRubric == null ? new ArrayList<>() : new ArrayList<>(scoringRubric);
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }
}
