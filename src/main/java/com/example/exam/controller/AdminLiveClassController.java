
package com.example.exam.controller;

import com.example.exam.dto.LiveClassAttendanceReportDto;
import com.example.exam.model.LiveClass;
import com.example.exam.model.LiveClassAttendance;
import com.example.exam.model.User;
import com.example.exam.repository.UserRepository;
import com.example.exam.service.LiveClassAttendanceExcelService;
import com.example.exam.service.LiveClassAttendanceService;
import com.example.exam.service.LiveClassNotificationService;
import com.example.exam.service.LiveClassService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/live-classes")
@RequiredArgsConstructor
public class AdminLiveClassController {

    private final LiveClassService liveClassService;

    private final UserRepository userRepository;

    private final LiveClassNotificationService notificationService;

    private final LiveClassAttendanceService attendanceService;

    private final LiveClassAttendanceExcelService excelService;


    // ==========================================
    // DISPLAY ALL LIVE CLASSES
    // ==========================================

    @GetMapping
    public String viewLiveClasses(Model model) {

        model.addAttribute(
                "liveClasses",
                liveClassService.getAllLiveClasses()
        );

        return "admin/live-classes";
    }


    // ==========================================
    // DISPLAY CREATE CLASS PAGE
    // ==========================================

    @GetMapping("/create")
    public String showCreateForm() {

        return "admin/create-live-class";
    }


    // ==========================================
    // CREATE A NEW LIVE CLASS
    // ==========================================

    @PostMapping("/create")
    public String createLiveClass(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String scheduledAt,
            Authentication authentication,
            Model model) {

        try {

            User admin = userRepository
                    .findByUsername(authentication.getName())
                    .orElseThrow();

            liveClassService.createLiveClass(
                    title,
                    description,
                    LocalDateTime.parse(scheduledAt),
                    admin
            );

            return "redirect:/admin/live-classes";

        } catch (Exception e) {

            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", title);
            model.addAttribute("description", description);
            model.addAttribute("scheduledAt", scheduledAt);

            return "admin/create-live-class";
        }
    }


    // ==========================================
    // START LIVE CLASS
    // ==========================================

    @PostMapping("/{id}/start")
    public String startLiveClass(@PathVariable Long id) {

        LiveClass liveClass =
                liveClassService.startLiveClass(id);

        // Notify connected students
        notificationService.notifyClassStatus(liveClass);

        // Return to dashboard instead of opening a new page
        return "redirect:/admin/live-classes";
    }


    // ==========================================
    // OPEN JITSI MEETING FOR ADMIN
    // ==========================================

    @GetMapping("/{id}/meeting")
    public String openMeeting(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        LiveClass liveClass =
                liveClassService.getLiveClassById(id);

        // Only running classes can be joined
        if (!"LIVE".equals(liveClass.getStatus())) {

            return "redirect:/admin/live-classes";
        }

        // Get logged-in admin
        User admin = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("Admin not found"));

        // Verify class ownership
        if (!liveClass.getCreatedBy().getId().equals(admin.getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to manage this live class"
            );
        }

        model.addAttribute("classId", liveClass.getId());

        model.addAttribute("classTitle", liveClass.getTitle());

        model.addAttribute("roomName", liveClass.getRoomName());

        model.addAttribute("displayName", admin.getFullName());

        model.addAttribute("isAdmin", true);

        return "live-class-room";
    }


    // ==========================================
    // COMPLETE LIVE CLASS
    // ==========================================

    @PostMapping("/{id}/complete")
    public String completeLiveClass(@PathVariable Long id) {

        LiveClass liveClass =
                liveClassService.completeLiveClass(id);

        // Notify connected students
        notificationService.notifyClassStatus(liveClass);

        return "redirect:/admin/live-classes";
    }


    // ==========================================
    // DELETE LIVE CLASS
    // ==========================================

    @PostMapping("/{id}/delete")
    public String deleteLiveClass(@PathVariable Long id) {

        liveClassService.deleteLiveClass(id);

        return "redirect:/admin/live-classes";
    }


    // ==========================================
    // VIEW ATTENDANCE REPORT
    // ==========================================

    @GetMapping("/{id}/attendance")
    public String viewAttendance(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        LiveClass liveClass =
                liveClassService.getLiveClassById(id);

        User admin = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("Admin not found"));

        // Verify class ownership
        if (!liveClass.getCreatedBy().getId().equals(admin.getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to view this attendance report"
            );
        }

        List<LiveClassAttendanceReportDto> attendanceList =
                attendanceService.getClassAttendance(id)
                        .stream()
                        .map(LiveClassAttendanceReportDto::from)
                        .toList();

        model.addAttribute("liveClass", liveClass);

        model.addAttribute("attendanceList", attendanceList);

        model.addAttribute("totalStudents", attendanceList.size());

        long joinedStudents = attendanceList.stream()
                .filter(LiveClassAttendanceReportDto::currentlyJoined)
                .count();

        model.addAttribute("joinedStudents", joinedStudents);

        return "admin/live-class-attendance";
    }


    // ==========================================
    // EXPORT ATTENDANCE TO EXCEL
    // ==========================================

    @GetMapping("/{id}/attendance/export")
    public ResponseEntity<byte[]> exportAttendance(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        LiveClass liveClass =
                liveClassService.getLiveClassById(id);

        User admin = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("Admin not found"));

        // Verify class ownership
        if (!liveClass.getCreatedBy().getId().equals(admin.getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to export this attendance report"
            );
        }

        byte[] excelFile =
                excelService.exportAttendance(id);

        String fileName =
                "Live_Class_Attendance_" + id + ".xlsx";

        return ResponseEntity.ok()

                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )

                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))

                .contentLength(excelFile.length)

                .body(excelFile);
    }

}
