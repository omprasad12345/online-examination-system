package com.example.exam.repository;

import com.example.exam.model.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {
}
