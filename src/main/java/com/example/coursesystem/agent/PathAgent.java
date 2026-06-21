package com.example.coursesystem.agent;

import com.example.coursesystem.entity.LearningPath;
import com.example.coursesystem.repository.LearningPathRepository;
import com.example.coursesystem.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 路径智能体 - 负责学习路径规划
 */
@Component
@Scope("prototype")
public class PathAgent extends BaseAgent {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private LearningPathRepository pathRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getAgentName() {
        return "路径智能体";
    }

    @Override
    public String getAgentDescription() {
        return "负责根据学生画像规划个性化学习路径";
    }

    @Override
    public String process(String message, String context) {
        String prompt = buildFullPrompt(message, context);
        return aiService.callAI(buildSystemPrompt(), prompt);
    }

    @Override
    public Flux<String> processStream(String message, String context) {
        String prompt = buildFullPrompt(message, context);
        return aiService.callAIStream(buildSystemPrompt(), prompt);
    }

    /**
     * 生成学习路径
     */
    public LearningPath generatePath(String studentId, String courseName) {
        String profileContext = profileService.getProfileContext(studentId);

        String prompt = String.format("""
                请为以下学生规划「%s」课程的学习路径。

                【学生画像】
                %s

                请以JSON格式返回学习路径：
                {
                    "title": "路径标题",
                    "description": "路径描述",
                    "steps": [
                        {
                            "step": 1,
                            "title": "步骤标题",
                            "description": "步骤描述",
                            "resources": ["推荐资源类型"],
                            "duration": "预计时长"
                        }
                    ],
                    "recommendedResources": [
                        {
                            "type": "资源类型",
                            "name": "资源名称",
                            "reason": "推荐原因"
                        }
                    ]
                }

                只返回JSON，不要其他内容。
                """, courseName, profileContext);

        String result = aiService.callAI(buildSystemPrompt(), prompt);

        try {
            ObjectNode json = (ObjectNode) objectMapper.readTree(result);

            LearningPath path = new LearningPath();
            path.setStudentId(studentId);
            path.setCourseName(courseName);
            path.setTitle(json.get("title").asText());
            path.setDescription(json.get("description").asText());
            path.setSteps(json.get("steps").toString());
            path.setRecommendedResources(json.get("recommendedResources").toString());

            return pathRepository.save(path);
        } catch (Exception e) {
            e.printStackTrace();
            // 返回一个默认路径
            LearningPath path = new LearningPath();
            path.setStudentId(studentId);
            path.setCourseName(courseName);
            path.setTitle(courseName + "学习路径");
            path.setDescription("正在为您规划学习路径...");
            path.setSteps("[]");
            path.setRecommendedResources("[]");
            return pathRepository.save(path);
        }
    }

    /**
     * 获取学生的学习路径列表
     */
    public List<LearningPath> getStudentPaths(String studentId) {
        return pathRepository.findByStudentIdOrderByCreateTimeDesc(studentId);
    }

    /**
     * 获取学生的活跃学习路径
     */
    public List<LearningPath> getActivePaths(String studentId) {
        return pathRepository.findByStudentIdAndStatusOrderByCreateTimeDesc(studentId, "active");
    }

    @Override
    protected String buildSystemPrompt() {
        return "你是「路径智能体」，负责规划个性化学习路径。由浅入深，理论与实践结合，考虑学生薄弱环节。给出明确的学习顺序和时间建议。";
    }
}
