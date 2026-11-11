package com.example.exam.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false, unique = true)
    private String roomName;

    @Column(nullable = false)
    private String status; // SCHEDULED, LIVE, COMPLETED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private LocalDateTime createdAt;
}
