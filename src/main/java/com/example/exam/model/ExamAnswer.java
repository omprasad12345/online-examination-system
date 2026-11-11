package com.example.exam.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "exam_answers")
public class ExamAnswer {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_result_id", nullable = false)
    private ExamResult examResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // The option selected by the student (0 if unanswered)
    @Column(nullable = false)
    private int selectedOption;

    // Constructors
    public ExamAnswer() {
    }

    public ExamAnswer(ExamResult examResult, Question question, int selectedOption) {
        this.examResult = examResult;
        this.question = question;
        this.selectedOption = selectedOption;
    }

}
