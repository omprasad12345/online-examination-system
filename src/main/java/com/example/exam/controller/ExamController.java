package com.example.exam.controller;

import com.example.exam.model.*;
import com.example.exam.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/exam")
public class ExamController {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamAnswerRepository examAnswerRepository;

    @GetMapping("/{examId}")
    public String getExamPage(@PathVariable Long examId, Model model) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        model.addAttribute("student", student);


        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + examId));
        model.addAttribute("exam", exam);


        List<Question> questions = exam.getQuestions();
        questions.size();
        model.addAttribute("questions", questions);

        return "student/exam_page";
    }

    @PostMapping("/submit")
    public String submitExam(@RequestParam Long examId,
                             @RequestParam Map<String, String> submittedAnswers,
                             RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + examId));

        int totalMarks = 0;
        int scoreAchieved = 0;

        // Create the result object *before* the loop
        ExamResult result = new ExamResult();
        result.setStudent(student);
        result.setExam(exam);
        result.setSubmissionTime(LocalDateTime.now());


        // First, save the result to get an ID
        examResultRepository.save(result);

        // Loop through all questions in the exam
        for (Question question : exam.getQuestions()) {
            totalMarks += question.getMarks();
            String answerKey = "q_" + question.getId();

            int selectedOption = 0; // Default to 0 (unanswered)

            if (submittedAnswers.containsKey(answerKey)) {
                try {
                    selectedOption = Integer.parseInt(submittedAnswers.get(answerKey));
                } catch (NumberFormatException e) {
                    selectedOption = 0; // Handle invalid data just in case
                }

                if (selectedOption == question.getCorrectAnswer()) {
                    scoreAchieved += question.getMarks();
                }
            }


            ExamAnswer examAnswer = new ExamAnswer(result, question, selectedOption);
            examAnswerRepository.save(examAnswer);
            result.addAnswer(examAnswer);
        }

        // Now, update the result with the final score and save again
        result.setScoreAchieved(scoreAchieved);
        result.setTotalMarks(totalMarks);
        examResultRepository.save(result);

        // Pass data to the result page
        redirectAttributes.addFlashAttribute("score", scoreAchieved);
        redirectAttributes.addFlashAttribute("total", totalMarks);

        // Pass the result ID for the review button
        redirectAttributes.addFlashAttribute("resultId", result.getId());

        return "redirect:/student/result";
    }
}

