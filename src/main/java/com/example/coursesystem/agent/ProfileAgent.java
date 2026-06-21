package com.example.coursesystem.agent;

import com.example.coursesystem.entity.StudentProfile;
import com.example.coursesystem.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 画像智能体 - 负责学生画像构建和更新
 */
@Component
@Scope("prototype")
public class ProfileAgent extends BaseAgent {

    @Autowired
    private ProfileService profileService;

    @Override
    public String getAgentName() {
        return "画像智能体";
    }

    @Override
    public String getAgentDescription() {
        return "负责通过对话分析学生特征，构建和更新学习画像";
    }

    @Override
    public String process(String message, String context) {
        String studentId = extractStudentId(context);
        String profileContext = profileService.getProfileContext(studentId);

        String prompt = buildFullPrompt(message, profileContext);
        String response = aiService.callAI(buildSystemPrompt(), prompt);

        // 更新画像
        profileService.updateProfileFromChat(studentId, message);

        return response;
    }

    @Override
    public Flux<String> processStream(String message, String context) {
        String studentId = extractStudentId(context);
        String profileContext = profileService.getProfileContext(studentId);

        String prompt = buildFullPrompt(message, profileContext);

        // 异步更新画像
        new Thread(() -> profileService.updateProfileFromChat(studentId, message)).start();

        return aiService.callAIStream(buildSystemPrompt(), prompt);
    }

    @Override
    protected String buildSystemPrompt() {
        return "你是「画像智能体」，通过对话了解学生特征。语气亲切自然，每次聚焦1-2个维度提问，根据回答灵活调整。";
    }

    private String extractStudentId(String context) {
        if (context != null && context.contains("studentId:")) {
            return context.split("studentId:")[1].split(";")[0].trim();
        }
        return "default";
    }
}
