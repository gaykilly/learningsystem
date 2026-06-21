package com.example.coursesystem.agent;

import com.example.coursesystem.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 辅导智能体 - 负责答疑解惑（加分项）
 */
@Component
@Scope("prototype")
public class TutorAgent extends BaseAgent {

    @Autowired
    private ProfileService profileService;

    @Override
    public String getAgentName() {
        return "辅导智能体";
    }

    @Override
    public String getAgentDescription() {
        return "负责为学生提供即时答疑和学习辅导";
    }

    @Override
    public String process(String message, String context) {
        String studentId = extractStudentId(context);
        String profileContext = profileService.getProfileContext(studentId);
        String prompt = buildFullPrompt(message, profileContext);
        return aiService.callAI(buildSystemPrompt(), prompt);
    }

    @Override
    public Flux<String> processStream(String message, String context) {
        String studentId = extractStudentId(context);
        String profileContext = profileService.getProfileContext(studentId);
        String prompt = buildFullPrompt(message, profileContext);
        return aiService.callAIStream(buildSystemPrompt(), prompt);
    }

    @Override
    protected String buildSystemPrompt() {
        return "你是「辅导智能体」，为学生提供个性化学习辅导。用通俗易懂的语言解答疑问，提供示例帮助理解，引导思考而非直接给答案。根据学生水平调整解释深度。使用Markdown格式输出。";
    }

    private String extractStudentId(String context) {
        if (context != null && context.contains("studentId:")) {
            return context.split("studentId:")[1].split(";")[0].trim();
        }
        return "default";
    }
}
