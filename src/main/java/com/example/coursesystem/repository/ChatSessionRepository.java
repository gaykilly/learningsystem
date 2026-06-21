package com.example.coursesystem.repository;

import com.example.coursesystem.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByStudentIdOrderByUpdateTimeDesc(String studentId);

    Optional<ChatSession> findBySessionId(String sessionId);

    @Modifying
    void deleteBySessionId(String sessionId);
}
