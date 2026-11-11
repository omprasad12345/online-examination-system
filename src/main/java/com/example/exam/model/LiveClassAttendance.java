
package com.example.exam.model;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "live_class_attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_live_class_student",
                        columnNames = {"live_class_id", "student_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveClassAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Live class reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_class_id", nullable = false)
    private LiveClass liveClass;

    // Student reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // First time the student joined
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    // Most recent joining time
    private LocalDateTime lastJoinedAt;

    // Most recent leaving time
    private LocalDateTime leftAt;

    // Total attendance duration in seconds
    @Builder.Default
    @Column(nullable = false)
    private Long totalDurationSeconds = 0L;

    // Whether the student is currently in the meeting
    @Builder.Default
    @Column(nullable = false)
    private Boolean currentlyJoined = false;

    // Tracks whether the student is temporarily disconnected
    @Builder.Default
    @Column(nullable = false)
    private Boolean connectionLost = false;

    // Time when the temporary disconnection occurred
    private LocalDateTime disconnectedAt;

    // Last successful heartbeat received from the browser
    private LocalDateTime lastHeartbeatAt;
}
