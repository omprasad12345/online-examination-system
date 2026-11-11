package com.example.exam.repository;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamResult;
import com.example.exam.model.Question;
import com.example.exam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

}
