package com.example.coursesystem.controller;

import com.example.coursesystem.agent.*;
import com.example.coursesystem.entity.ChatSession;
import com.example.coursesystem.entity.LearningHistory;
import com.example.coursesystem.repository.ChatSessionRepository;
import com.example.coursesystem.repository.LearningHistoryRepository;
import com.example.coursesystem.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 对话控制器 - 处理与学生的对话交互
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ProfileAgent profileAgent;

    @Autowired
    private ResourceAgent resourceAgent;

    @Autowired
    private PathAgent pathAgent;

    @Autowired
    private TutorAgent tutorAgent;

    @Autowired
    private EvalAgent evalAgent;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private LearningHistoryRepository historyRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;

    /**
     * 流式对话接口
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String studentId,
            @RequestParam(defaultValue = "tutor") String agentType) {

        String context = "studentId:" + studentId + ";";

        BaseAgent agent = getAgent(agentType);
        return agent.processStream(message, context);
    }

    /**
     * 非流式对话接口
     */
    @PostMapping("/send")
    public Map<String, Object> sendMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String studentId = request.getOrDefault("studentId", "default");
        String agentType = request.getOrDefault("agentType", "tutor");
        String sessionId = request.get("sessionId");

        // 如果没有sessionId，创建新会话
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
            ChatSession session = new ChatSession();
            session.setSessionId(sessionId);
            session.setStudentId(studentId);
            session.setTitle(message.length() > 30 ? message.substring(0, 30) + "..." : message);
            session.setAgentType(agentType);
            sessionRepository.save(session);
        }

        String context = "studentId:" + studentId + ";";

        BaseAgent agent = getAgent(agentType);
        String response = agent.process(message, context);

        // 保存对话记录
        saveHistory(studentId, "chat", message, response, agentType, sessionId);

        // 更新会话时间和标题
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setUpdateTime(java.time.LocalDateTime.now());
            sessionRepository.save(session);
        });

        // 异步更新画像
        new Thread(() -> profileService.updateProfileFromChat(studentId, message)).start();

        return Map.of("response", response, "sessionId", sessionId);
    }

    /**
     * 获取可用的智能体列表
     */
    @GetMapping("/agents")
    public Map<String, String> getAgents() {
        return Map.of(
                "profile", profileAgent.getAgentName() + " - " + profileAgent.getAgentDescription(),
                "resource", resourceAgent.getAgentName() + " - " + resourceAgent.getAgentDescription(),
                "path", pathAgent.getAgentName() + " - " + pathAgent.getAgentDescription(),
                "tutor", tutorAgent.getAgentName() + " - " + tutorAgent.getAgentDescription(),
                "eval", evalAgent.getAgentName() + " - " + evalAgent.getAgentDescription()
        );
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public List<ChatSession> getSessions(
            @RequestParam(defaultValue = "student_001") String studentId) {
        return sessionRepository.findByStudentIdOrderByUpdateTimeDesc(studentId);
    }

    /**
     * 获取会话内的所有消息
     */
    @GetMapping("/session/{sessionId}/messages")
    public List<LearningHistory> getSessionMessages(@PathVariable String sessionId) {
        return historyRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }

    /**
     * 删除会话
     */
    @Transactional
    @DeleteMapping("/session/{sessionId}")
    public Map<String, String> deleteSession(@PathVariable String sessionId) {
        historyRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteBySessionId(sessionId);
        return Map.of("status", "ok");
    }

    /**
     * 获取对话历史（兼容旧接口）
     */
    @GetMapping("/history")
    public List<LearningHistory> getChatHistory(
            @RequestParam(defaultValue = "student_001") String studentId) {
        return historyRepository.findByStudentIdAndTypeOrderByCreateTimeDesc(studentId, "chat");
    }

    private BaseAgent getAgent(String agentType) {
        return switch (agentType) {
            case "profile" -> profileAgent;
            case "resource" -> resourceAgent;
            case "path" -> pathAgent;
            case "eval" -> evalAgent;
            default -> tutorAgent;
        };
    }

    private void saveHistory(String studentId, String type, String content, String response, String agentType, String sessionId) {
        try {
            LearningHistory history = new LearningHistory();
            history.setStudentId(studentId);
            history.setType(type);
            history.setContent(content);
            history.setResponse(response);
            history.setKnowledgeTags(agentType);
            history.setSessionId(sessionId);
            historyRepository.save(history);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
