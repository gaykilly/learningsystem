# 个性化学习智能体系统

> 基于多智能体协作的个性化学习辅助平台

## 项目简介

本系统是一个基于 **Spring Boot + Vue.js + AI** 的个性化学习智能体系统，通过多个专业智能体协作，为学生提供智能化的学习辅导服务。系统采用 MiMo AI（小米大模型）作为核心推理引擎，实现了智能对话、学习画像构建、资源生成、路径规划和考试冲刺等核心功能。

---

## 系统架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue.js + Element Plus)             │
│  ┌─────────┬──────────┬──────────┬──────────┬──────────┬──────┐ │
│  │智能对话 │ 学习画像 │ 资源生成 │ 学习路径 │ 知识库  │考试冲刺│ │
│  └────┬────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴──┬───┘ │
│       │         │          │          │          │        │     │
│       └─────────┴──────────┴──────────┴──────────┴────────┘     │
│                              │ HTTP/REST API                    │
└──────────────────────────────┼──────────────────────────────────┘
                               │
┌──────────────────────────────┼──────────────────────────────────┐
│                     后端 (Spring Boot)                           │
│  ┌───────────────────────────┴──────────────────────────────┐   │
│  │                    Controller 层                          │   │
│  │  ChatController | ResourceController | ExamController    │   │
│  │  ProfileController | LearningPathController | Knowledge  │   │
│  └───────────────────────────┬──────────────────────────────┘   │
│                              │                                  │
│  ┌───────────────────────────┴──────────────────────────────┐   │
│  │                    Agent 智能体层                         │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │   │
│  │  │辅导智能体│ │画像智能体│ │资源智能体│ │路径智能体│    │   │
│  │  │ TutorAgent│ │ProfileAgent│ResourceAgent│ PathAgent│    │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │   │
│  │  ┌──────────┐                                           │   │
│  │  │评估智能体│                                           │   │
│  │  │ EvalAgent│                                           │   │
│  │  └──────────┘                                           │   │
│  └───────────────────────────┬──────────────────────────────┘   │
│                              │                                  │
│  ┌───────────────────────────┴──────────────────────────────┐   │
│  │                    Service 业务层                         │   │
│  │  AIService | ProfileService | ExamService | Knowledge    │   │
│  └───────────────────────────┬──────────────────────────────┘   │
│                              │                                  │
│  ┌───────────────────────────┴──────────────────────────────┐   │
│  │                    Repository 数据访问层                  │   │
│  │  StudentProfile | LearningHistory | LearningPath | ...   │   │
│  └───────────────────────────┬──────────────────────────────┘   │
│                              │                                  │
└──────────────────────────────┼──────────────────────────────────┘
                               │
┌──────────────────────────────┼──────────────────────────────────┐
│                         数据存储层                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │  MySQL 8.0   │    │  MiMo AI API │    │  OkHttp SSE  │      │
│  │  数据持久化  │    │  大模型推理   │    │  流式响应    │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
└─────────────────────────────────────────────────────────────────┘
```

### 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **前端** | Vue 3.4 | 渐进式 JavaScript 框架 |
| **UI 组件** | Element Plus 2.4 | Vue 3 组件库 |
| **路由** | Vue Router 4.2 | 前端路由管理 |
| **HTTP 客户端** | Axios 1.6 | API 请求 |
| **Markdown 渲染** | Marked 11.1 | AI 回复渲染 |
| **构建工具** | Vite 5.0 | 前端构建 |
| **后端框架** | Spring Boot 3.2 | Java Web 框架 |
| **ORM** | Spring Data JPA | 数据库操作 |
| **数据库** | MySQL 8.0 | 数据持久化 |
| **AI 服务** | MiMo AI (小米) | 大模型推理 |
| **流式响应** | WebFlux + OkHttp SSE | 流式 AI 对话 |
| **文件生成** | Apache POI | Word/PPT 生成 |

---

## 核心功能模块

### 1. 智能对话模块 (ChatView)

**功能描述：** 基于多智能体的 AI 对话系统，支持选择不同专业智能体进行针对性对话。

**核心特性：**
- 多智能体切换（辅导、画像、资源、路径、评估）
- 流式响应输出（SSE）
- 会话管理（新建、切换、删除）
- 对话历史持久化
- Markdown 格式渲染
- 欢迎页面与智能体卡片展示

**API 接口：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat/send` | POST | 发送消息（非流式） |
| `/api/chat/stream` | GET | 流式对话（SSE） |
| `/api/chat/agents` | GET | 获取智能体列表 |
| `/api/chat/sessions` | GET | 获取会话列表 |
| `/api/chat/session/{id}/messages` | GET | 获取会话消息 |
| `/api/chat/session/{id}` | DELETE | 删除会话 |

---

### 2. 学习画像模块 (ProfileView)

**功能描述：** 通过对话自动分析学生特征，构建多维度学习画像。

**画像维度（8个）：**

| 维度 | 字段 | 说明 |
|------|------|------|
| 专业 | major | 学生所学专业 |
| 知识基础 | knowledgeLevel | 初级/中级/高级 |
| 认知风格 | cognitiveStyle | 视觉型/听觉型/实践型 |
| 学习偏好 | learningPreference | 喜欢的学习方式 |
| 易错点 | weakPoints | 常犯错误类型 |
| 学习目标 | learningGoal | 短期/长期目标 |
| 学习进度 | learningProgress | 当前学习阶段 |
| 年级 | grade | 学生年级 |

**工作原理：**
```
学生对话 → ProfileAgent → AIService → 分析对话内容 → 提取画像信息 → 更新数据库
```

**API 接口：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/profile/get` | GET | 获取学生画像 |
| `/api/profile/update` | POST | 通过对话更新画像 |
| `/api/profile/updateField` | POST | 手动更新画像字段 |
| `/api/profile/context` | GET | 获取画像上下文 |

---

### 3. 资源生成模块 (ResourceView)

**功能描述：** 基于学生画像和知识库，AI 自动生成个性化学习资源。

**支持的资源类型：**

| 类型 | 说明 | 输出格式 |
|------|------|----------|
| 教学大纲 | 完整的高校教学大纲 | Markdown |
| PPT 课件大纲 | 10-20 页 PPT 结构 | Markdown |
| 练习题 | 选择题+填空题+简答题 | Markdown |
| 思维导图 | 层级知识点结构 | Markdown 缩进 |
| 复习提纲 | 期末复习重点提炼 | Markdown |

**个性化机制：**
1. 获取学生画像（知识基础、薄弱点等）
2. 查询知识库相关内容
3. 构建个性化 Prompt
4. 调用 AI 生成内容
5. 保存生成历史

**API 接口：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/resource/syllabus` | POST | 生成教学大纲 |
| `/api/resource/ppt` | POST | 生成 PPT 大纲 |
| `/api/resource/exercise` | POST | 生成练习题 |
| `/api/resource/mindmap` | POST | 生成思维导图 |
| `/api/resource/review` | POST | 生成复习提纲 |
| `/api/resource/{type}/stream` | GET | 流式生成资源 |
| `/api/resource/history` | GET | 获取生成历史 |
| `/api/resource/ppt/download` | POST | 下载 PPT 文件 |
| `/api/resource/download/word` | POST | 下载 Word 文件 |

---

### 4. 学习路径模块 (PathView)

**功能描述：** 根据学生画像和课程特点，AI 规划个性化学习路径。

**路径结构：**
```json
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
```

**API 接口：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/path/generate` | POST | 生成学习路径 |
| `/api/path/generate/stream` | GET | 流式生成路径 |
| `/api/path/list` | GET | 获取路径列表 |
| `/api/path/active` | GET | 获取活跃路径 |

---

### 5. 知识库模块 (KnowledgeView)

**功能描述：** 管理课程知识点，为 AI 对话和资源生成提供知识上下文。

**数据模型：**
- 课程 → 章节 → 知识点
- 每个知识点包含：编号、名称、内容、类型、难度、关键词

**API 接口：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/knowledge/courses` | GET | 获取所有课程 |
| `/api/knowledge/list` | GET | 获取课程知识点 |
| `/api/knowledge/search` | GET | 搜索知识点 |
| `/api/knowledge/add` | POST | 添加知识点 |
| `/api/knowledge/batch-add` | POST | 批量添加知识点 |

---

### 6. 考试冲刺模块 (ExamView)

**功能描述：** 基于答题记录和知识点掌握度，提供智能化的考前冲刺服务。

**核心功能：**

#### 6.1 知识点掌握度追踪
```
答题记录 → 更新掌握度算法：
掌握度 = 正确率 × 70% + 连续正确加成 × 30%
```

#### 6.2 薄弱知识点分析
- 自动识别掌握度最低的知识点
- TOP5 薄弱知识点展示

#### 6.3 模拟试卷生成
- 基于薄弱知识点智能出题
- 题型分布：选择题 40%、填空题 20%、简答题 40%
- 可配置题目数量（10-50 题）

#### 6.4 考前冲刺计划
- 根据距离考试天数生成计划
- 优先安排薄弱知识点复习
- 每日具体学习任务

#### 6.5 错题本与间隔重复
- 自动收集错题
- 基于艾宾浩斯遗忘曲线的间隔重复算法
- 智能推荐待复习题目

**API 接口：**

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/exam/answer` | POST | 保存答题记录 |
| `/api/exam/weak-points` | GET | 获取薄弱知识点 |
| `/api/exam/mastery` | GET | 获取知识点掌握度 |
| `/api/exam/wrong-questions` | GET | 获取错题列表 |
| `/api/exam/review` | GET | 获取待复习题目 |
| `/api/exam/mock-exam` | POST | 生成模拟试卷 |
| `/api/exam/study-plan` | POST | 生成冲刺计划 |
| `/api/exam/stats` | GET | 获取学习统计 |

---

## 智能体设计

### 智能体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      BaseAgent (基类)                        │
│  - aiService: AIService                                     │
│  - process(message, context): String                        │
│  - processStream(message, context): Flux<String>            │
│  - buildSystemPrompt(): String                              │
│  - buildFullPrompt(message, context): String                │
└─────────────────────────┬───────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│  TutorAgent   │ │ ProfileAgent  │ │ ResourceAgent │
│  辅导智能体   │ │ 画像智能体    │ │ 资源智能体    │
│ - 答疑解惑    │ │ - 画像构建    │ │ - 资源生成    │
│ - 个性化辅导  │ │ - 画像更新    │ │ - 大纲/PPT   │
└───────────────┘ └───────────────┘ └───────────────┘
        │                 │                 │
        ▼                 ▼                 ▼
┌───────────────┐ ┌───────────────┐
│   PathAgent   │ │   EvalAgent   │
│  路径智能体   │ │ 评估智能体    │
│ - 路径规划    │ │ - 效果评估    │
│ - 资源推荐    │ │ - 学习报告    │
└───────────────┘ └───────────────┘
```

### 智能体职责

| 智能体 | 职责 | System Prompt |
|--------|------|---------------|
| **TutorAgent** | 答疑解惑 | 用通俗易懂的语言解答疑问，提供示例帮助理解，引导思考而非直接给答案 |
| **ProfileAgent** | 画像构建 | 通过对话了解学生特征，每次聚焦 1-2 个维度提问 |
| **ResourceAgent** | 资源生成 | 生成高质量学习资源，根据学生画像调整难度 |
| **PathAgent** | 路径规划 | 由浅入深，理论与实践结合，考虑学生薄弱环节 |
| **EvalAgent** | 效果评估 | 客观公正，数据驱动，提出具体可行的改进建议 |

---

## 数据库设计

### ER 图

```
┌──────────────────┐     ┌──────────────────┐
│  student_profile │     │  learning_history│
├──────────────────┤     ├──────────────────┤
│ id (PK)          │     │ id (PK)          │
│ student_id (UK)  │◄────│ student_id (FK)  │
│ student_name     │     │ session_id       │
│ major            │     │ type             │
│ knowledge_level  │     │ content          │
│ cognitive_style  │     │ response         │
│ learning_pref    │     │ knowledge_tags   │
│ weak_points      │     │ duration         │
│ learning_goal    │     │ create_time      │
│ learning_progress│     └──────────────────┘
│ grade            │
│ update_time      │     ┌──────────────────┐
└──────────────────┘     │  chat_session    │
                         ├──────────────────┤
┌──────────────────┐     │ session_id (PK)  │
│  learning_path   │     │ student_id (FK)  │
├──────────────────┤     │ title            │
│ id (PK)          │     │ agent_type       │
│ student_id (FK)  │     │ create_time      │
│ course_name      │     │ update_time      │
│ title            │     └──────────────────┘
│ description      │
│ steps (JSON)     │     ┌──────────────────┐
│ recommended_res  │     │ knowledge_base   │
│ progress         │     ├──────────────────┤
│ status           │     │ id (PK)          │
│ create_time      │     │ course_name      │
│ update_time      │     │ chapter_no       │
└──────────────────┘     │ chapter_name     │
                         │ knowledge_no     │
┌──────────────────┐     │ knowledge_name   │
│ question_record  │     │ content          │
├──────────────────┤     │ type             │
│ id (PK)          │     │ difficulty       │
│ student_id (FK)  │     │ keywords         │
│ question         │     │ create_time      │
│ question_type    │     └──────────────────┘
│ student_answer   │
│ correct_answer   │     ┌──────────────────┐
│ is_correct       │     │knowledge_mastery │
│ knowledge_tags   │     ├──────────────────┤
│ difficulty       │     │ id (PK)          │
│ course_name      │     │ student_id (FK)  │
│ answer_time      │     │ course_name      │
│ review_count     │     │ knowledge_tag    │
│ next_review_time │     │ knowledge_name   │
└──────────────────┘     │ mastery_level    │
                         │ total_attempts   │
┌──────────────────┐     │ correct_count    │
│resource_history  │     │ streak           │
├──────────────────┤     │ last_attempt_time│
│ id (PK)          │     └──────────────────┘
│ type             │
│ input_text       │
│ output_text      │
│ create_time      │
└──────────────────┘
```

### 表说明

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `student_profile` | 学生画像表 | 8 个维度字段 |
| `learning_history` | 学习历史表 | 对话记录、资源生成记录 |
| `chat_session` | 聊天会话表 | 会话管理 |
| `learning_path` | 学习路径表 | 路径步骤（JSON） |
| `knowledge_base` | 知识库表 | 课程-章节-知识点 |
| `question_record` | 答题记录表 | 答案、正误、复习信息 |
| `knowledge_mastery` | 知识掌握度表 | 掌握度、连续正确数 |
| `resource_history` | 资源生成历史表 | 输入输出记录 |

---

## AI 服务集成

### MiMo AI 接口

系统使用小米 MiMo AI 大模型，兼容 Anthropic Messages API 格式。

**接口配置：**
```yaml
mimo:
  api-key: ${MIMO_API_KEY}
  base-url: "https://api.xiaomimimo.com"
  model: "mimo-v2.5-pro"
```

**请求格式：**
```json
{
  "model": "mimo-v2.5-pro",
  "max_tokens": 2000,
  "thinking": { "type": "disabled" },
  "system": "系统提示词",
  "messages": [
    { "role": "user", "content": "用户消息" }
  ]
}
```

**流式响应处理：**
```
SSE Event → content_block_delta → text_delta → 输出文本
         → message_stop → 完成
```

---

## 前端页面设计

### 页面路由

| 路径 | 组件 | 说明 |
|------|------|------|
| `/chat` | ChatView | 智能对话（默认首页） |
| `/profile` | ProfileView | 学习画像 |
| `/resource` | ResourceView | 资源生成 |
| `/path` | PathView | 学习路径 |
| `/knowledge` | KnowledgeView | 知识库管理 |
| `/exam` | ExamView | 考试冲刺 |

### UI 设计特点

- **深色侧边栏** + **浅色/深色主内容区**
- **响应式布局**，支持侧边栏折叠
- **暗色模式**切换
- **Markdown** 富文本渲染
- **ECharts** 图表可视化（雷达图）
- **Element Plus** 组件库

---

## 项目结构

```
coursesystem/
├── pom.xml                          # Maven 配置
├── src/main/
│   ├── java/com/example/coursesystem/
│   │   ├── CourseSystemApplication.java    # 启动类
│   │   ├── agent/                          # 智能体层
│   │   │   ├── BaseAgent.java              # 智能体基类
│   │   │   ├── TutorAgent.java             # 辅导智能体
│   │   │   ├── ProfileAgent.java           # 画像智能体
│   │   │   ├── ResourceAgent.java          # 资源智能体
│   │   │   ├── PathAgent.java              # 路径智能体
│   │   │   └── EvalAgent.java              # 评估智能体
│   │   ├── controller/                     # 控制层
│   │   │   ├── ChatController.java
│   │   │   ├── ProfileController.java
│   │   │   ├── ResourceController.java
│   │   │   ├── LearningPathController.java
│   │   │   ├── ExamController.java
│   │   │   ├── KnowledgeController.java
│   │   │   └── AIRequest.java
│   │   ├── entity/                         # 实体层
│   │   │   ├── StudentProfile.java
│   │   │   ├── LearningHistory.java
│   │   │   ├── LearningPath.java
│   │   │   ├── KnowledgeBase.java
│   │   │   ├── KnowledgeMastery.java
│   │   │   ├── QuestionRecord.java
│   │   │   ├── ResourceHistory.java
│   │   │   └── ChatSession.java
│   │   ├── repository/                     # 数据访问层
│   │   │   ├── StudentProfileRepository.java
│   │   │   ├── LearningHistoryRepository.java
│   │   │   ├── LearningPathRepository.java
│   │   │   ├── KnowledgeBaseRepository.java
│   │   │   ├── KnowledgeMasteryRepository.java
│   │   │   ├── QuestionRecordRepository.java
│   │   │   ├── ResourceHistoryRepository.java
│   │   │   └── ChatSessionRepository.java
│   │   ├── service/                        # 业务层
│   │   │   ├── AIService.java
│   │   │   ├── ProfileService.java
│   │   │   ├── ExamService.java
│   │   │   └── KnowledgeService.java
│   │   └── config/                         # 配置层
│   │       ├── CorsConfig.java
│   │       ├── SecurityConfig.java
│   │       └── DataInitializer.java
│   └── resources/
│       └── application.yml                 # 应用配置
└── frontend/                             # 前端项目
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.js
        ├── App.vue
        ├── api/index.js
        └── views/
            ├── ChatView.vue
            ├── ProfileView.vue
            ├── ResourceView.vue
            ├── PathView.vue
            ├── KnowledgeView.vue
            └── ExamView.vue
```

---

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0

### 数据库初始化

```sql
CREATE DATABASE course_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 后端启动

```bash
# 克隆项目
git clone https://github.com/gaykilly/learningsystem.git
cd learningsystem

# 配置 API Key（可选，已有默认值）
export MIMO_API_KEY=your_api_key

# 启动后端
mvn spring-boot:run
```

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

### 访问地址

- 前端：http://localhost:3000
- 后端 API：http://localhost:8080

---

## API 调用示例

### 发送对话消息

```bash
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "message": "什么是机器学习？",
    "studentId": "student_001",
    "agentType": "tutor"
  }'
```

### 生成教学大纲

```bash
curl -X POST http://localhost:8080/api/resource/syllabus \
  -H "Content-Type: application/json" \
  -d '{
    "input": "机器学习基础",
    "studentId": "student_001"
  }'
```

### 获取学习统计

```bash
curl "http://localhost:8080/api/exam/stats?studentId=student_001&courseName=人工智能导论"
```

---

## 系统特色

1. **多智能体协作**：5 个专业智能体各司其职，协同工作
2. **个性化推荐**：基于学生画像的智能资源生成
3. **流式响应**：SSE 实时输出，提升用户体验
4. **知识图谱**：结构化知识库，支撑 AI 生成质量
5. **间隔重复**：基于遗忘曲线的智能复习算法
6. **数据驱动**：答题记录追踪，掌握度量化分析

---

## License

MIT License
