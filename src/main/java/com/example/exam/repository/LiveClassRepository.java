package com.example.exam.repository;

import com.example.exam.model.LiveClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveClassRepository
        extends JpaRepository<LiveClass, Long> {

    List<LiveClass> findAllByOrderByScheduledAtDesc();

    List<LiveClass> findByStatusOrderByScheduledAtAsc(String status);
}
