package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.entity.InterviewQuestion;
import com.zhihang.jobagent.repository.InterviewQuestionRepository;
import com.zhihang.jobagent.service.ContentNormalizationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminQuestionController {

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final ContentNormalizationService contentNormalizationService;

    public AdminQuestionController(InterviewQuestionRepository interviewQuestionRepository,
                                   ContentNormalizationService contentNormalizationService) {
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.contentNormalizationService = contentNormalizationService;
    }

    @GetMapping("/admin/questions")
    public String list(Model model) {
        model.addAttribute("questions", interviewQuestionRepository.findAllByOrderByIdAsc());
        return "admin/questions";
    }

    @GetMapping("/admin/questions/new")
    public String showCreateForm(Model model) {
        model.addAttribute("question", new InterviewQuestion());
        return "admin/question-form";
    }

    @PostMapping("/admin/questions")
    public String save(@ModelAttribute("question") InterviewQuestion formQuestion) {
        InterviewQuestion question = formQuestion.getId() == null
                ? new InterviewQuestion()
                : interviewQuestionRepository.findById(formQuestion.getId()).orElseGet(InterviewQuestion::new);

        question.setJobDirection(formQuestion.getJobDirection());
        question.setQuestionType(formQuestion.getQuestionType());
        question.setQuestionText(formQuestion.getQuestionText());
        question.setReferenceAnswer(formQuestion.getReferenceAnswer());
        question.setKeyPoints(formQuestion.getKeyPoints());
        question.setRawTitle(formQuestion.getQuestionText());
        question.setRawContent((formQuestion.getReferenceAnswer() == null ? "" : formQuestion.getReferenceAnswer())
                + "\n"
                + (formQuestion.getQuestionText() == null ? "" : formQuestion.getQuestionText()));
        contentNormalizationService.normalizeInterviewQuestion(question);
        interviewQuestionRepository.save(question);
        return "redirect:/admin/questions";
    }

    @GetMapping("/admin/questions/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        InterviewQuestion question = interviewQuestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("题目ID无效: " + id));
        model.addAttribute("question", question);
        return "admin/question-form";
    }

    @GetMapping("/admin/questions/delete/{id}")
    public String delete(@PathVariable Long id) {
        interviewQuestionRepository.deleteById(id);
        return "redirect:/admin/questions";
    }
}
