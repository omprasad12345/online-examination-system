package com.example.exam.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects; // Import for equals/hashCode

@Setter
@Getter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;



    // We can use the 'username' field for email, or add a separate one.


    @Column(nullable = true) // Mobile number can be optional
    private String mobileNumber;

    @Column(nullable = true)
    private String fullName;

    @Column(nullable = true) // Profile pic URL is optional
    private String profilePicUrl;


    @Column(nullable = false)
    private String role;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExamResult> results = new ArrayList<>();

    // --- Manual Getters and Setters ---


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

