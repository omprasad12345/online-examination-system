package com.example.exam.controller;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamResult;
import com.example.exam.model.Question;
import com.example.exam.model.User;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.ExamResultRepository;
import com.example.exam.repository.QuestionRepository;
import com.example.exam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;


    @Autowired
    private ExamResultRepository examResultRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {


        long totalStudents = userRepository.countByRole("ROLE_STUDENT");
        long totalExams = examRepository.count();
        long totalQuestions = questionRepository.count();
        long totalSubmissions = examResultRepository.count();

        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalExams", totalExams);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("totalSubmissions", totalSubmissions);


        List<Exam> allExams = examRepository.findAll();
        List<String> chartLabels = allExams.stream()
                .map(Exam::getTitle)
                .collect(Collectors.toList());
        List<Integer> chartData = allExams.stream()
                .map(exam -> exam.getResults().size())
                .collect(Collectors.toList());

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);


        List<ExamResult> recentResults = examResultRepository.findTop5ByOrderBySubmissionTimeDesc();
        model.addAttribute("recentResults", recentResults);

        return "admin/dashboard";
    }


    @GetMapping("/manage-exams")
    public String getManageExamsPage(Model model) {
        model.addAttribute("exams", examRepository.findAll());
        return "admin/manage_exams";
    }

    @GetMapping("/exam/add")
    public String showAddExamForm(Model model) {
        model.addAttribute("exam", new Exam());
        return "admin/add_exam";
    }

    @PostMapping("/exam/add")
    public String addExam(@ModelAttribute Exam exam) {
        examRepository.save(exam);
        return "redirect:/admin/manage-exams";
    }

    @GetMapping("/exam/{examId}/question/add")
    public String showAddQuestionForm(@PathVariable Long examId, Model model) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + examId));
        Question question = new Question();
        question.setExam(exam);
        model.addAttribute("question", question);
        model.addAttribute("examId", examId);
        return "admin/add_question";
    }


    @PostMapping("/exam/question/add")
    public String addQuestion(@ModelAttribute Question question,
                              @RequestParam Long examId,
                              RedirectAttributes redirectAttributes) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + examId));

        question.setExam(exam);
        questionRepository.save(question);

        redirectAttributes.addFlashAttribute("successMessage", "Question added successfully!");

        return "redirect:/admin/exam/" + examId + "/question/add";
    }

    @GetMapping("/results/{examId}")
    public String viewResults(@PathVariable Long examId, Model model) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + examId));

        List<ExamResult> results = examResultRepository.findByExam(exam);

        model.addAttribute("exam", exam);
        model.addAttribute("results", results);

        return "admin/view_results";
    }

    @GetMapping("/exam/delete/{examId}")
    public String deleteExam(@PathVariable Long examId) {

        examRepository.deleteById(examId);
        return "redirect:/admin/manage-exams";
    }

    @GetMapping("/exam/edit/{examId}")
    public String showEditExamForm(@PathVariable Long examId, Model model) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + examId));
        model.addAttribute("exam", exam);
        return "admin/edit_exam";
    }

    @PostMapping("/exam/update/{examId}")
    public String updateExam(@PathVariable Long examId, @ModelAttribute Exam updatedExam) {
        Exam existingExam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + examId));

        existingExam.setTitle(updatedExam.getTitle());
        existingExam.setDurationInMinutes(updatedExam.getDurationInMinutes());

        examRepository.save(existingExam);
        return "redirect:/admin/manage-exams";
    }

    @GetMapping("/exam/manage-questions/{examId}")
    public String showManageQuestionsPage(@PathVariable Long examId, Model model) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + examId));
        model.addAttribute("exam", exam);
        model.addAttribute("questions", exam.getQuestions());
        return "admin/manage_questions";
    }

    @GetMapping("/question/edit/{questionId}")
    public String showEditQuestionForm(@PathVariable Long questionId, Model model) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid question Id:" + questionId));
        model.addAttribute("question", question);
        return "admin/edit_question.html";
    }

    @PostMapping("/question/update/{questionId}")
    public String updateQuestion(@PathVariable Long questionId, @ModelAttribute Question updatedQuestion) {
        Question existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid question Id:" + questionId));

        existingQuestion.setText(updatedQuestion.getText());
        existingQuestion.setOption1(updatedQuestion.getOption1());
        existingQuestion.setOption2(updatedQuestion.getOption2());
        existingQuestion.setOption3(updatedQuestion.getOption3());
        existingQuestion.setOption4(updatedQuestion.getOption4());
        existingQuestion.setCorrectAnswer(updatedQuestion.getCorrectAnswer());
        existingQuestion.setMarks(updatedQuestion.getMarks());

        questionRepository.save(existingQuestion);

        return "redirect:/admin/exam/manage-questions/" + existingQuestion.getExam().getId();
    }

    @GetMapping("/exam/{examId}/question/delete/{questionId}")
    public String deleteQuestion(@PathVariable Long examId,
                                 @PathVariable Long questionId,
                                 RedirectAttributes redirectAttributes) { // <-- Added RedirectAttributes
        try {
            questionRepository.deleteById(questionId);
            redirectAttributes.addFlashAttribute("successMessage", "Question deleted successfully.");

        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete this question. It has already been answered by one or more students and is part of their exam results.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while trying to delete the question.");
        }

        return "redirect:/admin/exam/manage-questions/" + examId;
    }


    @GetMapping("/students")
    public String getManageStudentsPage(Model model) {

        List<User> students = userRepository.findByRole("ROLE_STUDENT");
        model.addAttribute("students", students);
        return "admin/manage_students";
    }

    @GetMapping("/student/delete/{id}")
    @Transactional
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // 1. Find the student
            Optional<User> studentOpt = userRepository.findById(id);
            if (studentOpt.isEmpty()) {
                throw new IllegalArgumentException("Invalid student Id:" + id);
            }
            User student = studentOpt.get();

            // 2. MANUALLY delete the "children" (ExamResults)
            examResultRepository.deleteByStudent(student);

            // 3. Force Hibernate to execute the pending DELETE statements NOW.
            examResultRepository.flush();

            // 4. NOW it is safe to delete the "parent" (User)
            userRepository.delete(student);

            redirectAttributes.addFlashAttribute("successMessage", "Student account and all associated results have been deleted.");

        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete student. A database integrity error occurred.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while trying to delete the student account: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }




    @PostMapping("/student/reset-password")
    public String resetStudentPassword(@RequestParam("userId") Long userId,
                                       @RequestParam("newPassword") String newPassword,
                                       RedirectAttributes redirectAttributes) {

        if (newPassword.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password cannot be empty.");
            return "redirect:/admin/students";
        }

        try {
            User student = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + userId));

            // Encode the new password before saving
            student.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(student);

            redirectAttributes.addFlashAttribute("successMessage", "Password for " + student.getUsername() + " has been reset.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while trying to reset the password.");
        }
        return "redirect:/admin/students";
    }
}

