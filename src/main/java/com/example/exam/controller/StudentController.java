package com.example.exam.controller;

import com.example.exam.model.Exam;
import com.example.exam.model.ExamResult;
import com.example.exam.model.User;
import com.example.exam.repository.ExamRepository;
import com.example.exam.repository.ExamResultRepository;
import com.example.exam.repository.UserRepository;
import com.example.exam.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamResultRepository examResultRepository;

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Helper method to get the currently authenticated user
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName(); // This is the email
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/dashboard")
    public String studentDashboard(Model model) {

        User student = getAuthenticatedUser();
        model.addAttribute("student", student); // For the welcome header

        // 1. Get all results for this student (newest first)
        List<ExamResult> pastResults = examResultRepository.findByStudentOrderBySubmissionTimeDesc(student);
        model.addAttribute("pastResults", pastResults);

        // 2. Calculate Stats for KPI cards
        int totalTaken = pastResults.size();
        double totalScore = 0;
        int totalPossible = 0;
        int highestScorePercent = 0;

        for (ExamResult result : pastResults) {
            totalScore += result.getScoreAchieved();
            totalPossible += result.getTotalMarks();
            if (result.getTotalMarks() > 0) {
                int percent = (int) ((result.getScoreAchieved() * 100.0) / result.getTotalMarks());
                if (percent > highestScorePercent) {
                    highestScorePercent = percent;
                }
            }
        }

        double averageScore = (totalPossible > 0) ? (totalScore * 100.0) / totalPossible : 0;

        model.addAttribute("totalTaken", totalTaken);
        model.addAttribute("averageScore", averageScore);
        model.addAttribute("highestScore", highestScorePercent);

        // 3. Prepare data for the chart (needs to be in chronological order)
        List<ExamResult> chartResults = new ArrayList<>(pastResults);
        Collections.reverse(chartResults); // Reverse to be oldest first

        List<String> chartLabels = chartResults.stream()
                .map(r -> r.getExam().getTitle())
                .collect(Collectors.toList());

        List<Integer> chartData = chartResults.stream()
                .map(r -> r.getTotalMarks() > 0 ? (int) ((r.getScoreAchieved() * 100.0) / r.getTotalMarks()) : 0)
                .collect(Collectors.toList());

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        // 4. Get available exams (logic is unchanged)
        List<Exam> allExams = examRepository.findAll();
        Set<Exam> takenExams = pastResults.stream()
                .map(ExamResult::getExam)
                .collect(Collectors.toSet());
        List<Exam> availableExams = allExams.stream()
                .filter(exam -> !takenExams.contains(exam))
                .collect(Collectors.toList());
        model.addAttribute("availableExams", availableExams);

        return "student/dashboard";
    }
    @GetMapping("/my-results")
    public String getMyResultsPage(Model model) {
        // 1. Get the currently logged-in student
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Get all results for this student
        List<ExamResult> pastResults = examResultRepository.findByStudent(student);
        model.addAttribute("pastResults", pastResults);

        // 3. Return the new template
        return "student/my_results";
    }
    @GetMapping("/profile")
    public String getProfilePage(Model model) {
        User student = getAuthenticatedUser();
        model.addAttribute("student", student);
        return "student/profile"; // <-- New HTML file we need to create
    }


    @PostMapping("/profile/update-details")
    public String updateProfileDetails(@ModelAttribute User formData, RedirectAttributes redirectAttributes) {
        User student = getAuthenticatedUser();

        student.setFullName(formData.getFullName());
        student.setMobileNumber(formData.getMobileNumber());

        userRepository.save(student);

        redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully!");
        return "redirect:/student/profile";
    }


    @PostMapping("/profile/update-picture")
    public String updateProfilePicture(@RequestParam("profilePicFile") MultipartFile profilePicFile,
                                       RedirectAttributes redirectAttributes) {

        if (profilePicFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload.");
            return "redirect:/student/profile";
        }

        try {
            User student = getAuthenticatedUser();
            String filePath = fileStorageService.saveFile(profilePicFile);
            student.setProfilePicUrl(filePath);
            userRepository.save(student);
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error uploading file: " + e.getMessage());
        }

        return "redirect:/student/profile";
    }


    @PostMapping("/profile/update-password")
    public String updatePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes redirectAttributes) {

        User student = getAuthenticatedUser();

        // 1. Check if newPassword and confirmPassword match
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match.");
            return "redirect:/student/profile";
        }

        // 2. Check if the oldPassword is correct
        if (!passwordEncoder.matches(oldPassword, student.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Incorrect old password.");
            return "redirect:/student/profile";
        }

        // 3. All checks passed, update the password
        student.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(student);

        redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
        return "redirect:/student/profile";
    }

    @GetMapping("/result")
    public String showResult() {
        // This page shows the result immediately after submission
        // It gets its data from FlashAttributes ( ExamController)
        return "student/result";
    }
}

