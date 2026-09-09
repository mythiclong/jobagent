package com.zhihang.jobagent.config;

import com.zhihang.jobagent.entity.InterviewQuestion;
import com.zhihang.jobagent.repository.InterviewQuestionRepository;
import com.zhihang.jobagent.service.ContentNormalizationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class InterviewQuestionInitializer {

    @Bean
    public CommandLineRunner initInterviewQuestions(InterviewQuestionRepository interviewQuestionRepository,
                                                    ContentNormalizationService contentNormalizationService) {
        return args -> {
            if (interviewQuestionRepository.count() > 0) {
                return;
            }

            interviewQuestionRepository.saveAll(Arrays.asList(
                    buildQuestion(contentNormalizationService, "Java后端", "自我介绍", "请做一个 1 分钟左右的自我介绍。",
                            "介绍年级、专业、目标方向、技术基础和项目背景。", "专业,项目,目标方向,技能"),
                    buildQuestion(contentNormalizationService, "Java后端", "项目题", "请介绍一个你做过的后端项目，你在其中负责什么？",
                            "说明项目背景、职责拆分、关键技术、难点和结果。", "项目背景,职责,技术栈,难点,结果"),
                    buildQuestion(contentNormalizationService, "Java后端", "原理题", "进程和线程有什么区别？",
                            "可从资源隔离、调度、并发和使用场景几方面回答。", "并发基础,资源隔离,调度"),
                    buildQuestion(contentNormalizationService, "前端开发", "项目题", "请介绍一个你做过的前端项目，重点说说页面与交互实现。",
                            "可以从页面结构、组件拆分、接口联调和性能优化展开。", "组件化,联调,性能优化"),
                    buildQuestion(contentNormalizationService, "前端开发", "原理题", "事件循环是什么？",
                            "重点说明调用栈、任务队列和宏任务微任务的关系。", "JavaScript,事件循环,异步"),
                    buildQuestion(contentNormalizationService, "产品经理", "项目题", "请介绍一次你参与需求分析或产品设计的经历。",
                            "可以从问题背景、用户需求、方案设计和上线结果展开。", "需求拆解,方案落地,复盘"),
                    buildQuestion(contentNormalizationService, "产品经理", "场景题", "如果用户反馈某个功能不好用，你会怎么处理？",
                            "可从收集反馈、判断优先级、提出方案和验证效果回答。", "用户反馈,优先级,验证")
            ));
        };
    }

    private InterviewQuestion buildQuestion(ContentNormalizationService contentNormalizationService,
                                            String jobDirection,
                                            String questionType,
                                            String questionText,
                                            String referenceAnswer,
                                            String keyPoints) {
        InterviewQuestion question = new InterviewQuestion();
        question.setJobDirection(jobDirection);
        question.setQuestionType(questionType);
        question.setQuestionText(questionText);
        question.setReferenceAnswer(referenceAnswer);
        question.setKeyPoints(keyPoints);
        contentNormalizationService.normalizeInterviewQuestion(question);
        return question;
    }
}
