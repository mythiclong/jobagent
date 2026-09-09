package com.zhihang.jobagent.repository;

import com.zhihang.jobagent.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findAllByOrderByIdAsc();

    Optional<InterviewQuestion> findByQuestionHash(String questionHash);
}
