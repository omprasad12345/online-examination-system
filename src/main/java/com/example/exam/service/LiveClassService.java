
package com.example.exam.service;

import com.example.exam.model.LiveClass;
import com.example.exam.model.User;
import com.example.exam.repository.LiveClassRepository;

import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LiveClassService {

    private final LiveClassRepository liveClassRepository;

    // Create a new live class
    public LiveClass createLiveClass(
            String title,
            String description,
            LocalDateTime scheduledAt,
            User admin) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Class title is required");
        }

        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Please select a valid future date and time");
        }

        LiveClass liveClass = LiveClass.builder()
                .title(title)
                .description(description)
                .scheduledAt(scheduledAt)
                .roomName("online-exam-" + UUID.randomUUID())
                .status("SCHEDULED")
                .createdBy(admin)
                .createdAt(LocalDateTime.now())
                .build();

        return liveClassRepository.save(liveClass);
    }

    // Get all live classes
    @Transactional(readOnly = true)
    public List<LiveClass> getAllLiveClasses() {
        return liveClassRepository.findAllByOrderByScheduledAtDesc();
    }

    // Get upcoming scheduled classes
    @Transactional(readOnly = true)
    public List<LiveClass> getUpcomingClasses() {
        return liveClassRepository
                .findByStatusOrderByScheduledAtAsc("SCHEDULED");
    }

    // Get all currently running classes
    @Transactional(readOnly = true)
    public List<LiveClass> getLiveClasses() {
        return liveClassRepository
                .findByStatusOrderByScheduledAtAsc("LIVE");
    }

    // Find a class by ID
    @Transactional(readOnly = true)
    public LiveClass getLiveClassById(Long id) {
        return liveClassRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Live class not found with ID: " + id));
    }

    // Start a live class
    public LiveClass startLiveClass(Long id) {

        LiveClass liveClass = getLiveClassById(id);

        if (!"SCHEDULED".equals(liveClass.getStatus())) {
            throw new IllegalStateException(
                    "Only scheduled classes can be started");
        }

        liveClass.setStatus("LIVE");

        return liveClassRepository.save(liveClass);
    }

    // Complete a live class
    public LiveClass completeLiveClass(Long id) {

        LiveClass liveClass = getLiveClassById(id);

        if (!"LIVE".equals(liveClass.getStatus())) {
            throw new IllegalStateException(
                    "Only live classes can be completed");
        }

        liveClass.setStatus("COMPLETED");

        return liveClassRepository.save(liveClass);
    }

    // Delete a live class
    public void deleteLiveClass(Long id) {

        LiveClass liveClass = getLiveClassById(id);

        if ("LIVE".equals(liveClass.getStatus())) {
            throw new IllegalStateException(
                    "A live class cannot be deleted while running");
        }

        liveClassRepository.delete(liveClass);
    }
}

