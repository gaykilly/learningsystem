<template>
  <div class="chat-layout">
    <!-- 左侧会话列表侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="logo-area">
          <span class="logo-icon">🎓</span>
          <span class="logo-text">学习智能体</span>
        </div>
        <el-button class="new-chat-btn" @click="newChat">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>
      <div class="history-list">
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          :class="['history-item', currentSessionId === session.sessionId ? 'active' : '']"
          @click="loadSession(session)"
        >
          <el-icon class="history-icon"><ChatDotRound /></el-icon>
          <div class="history-info">
            <div class="history-title">{{ session.title }}</div>
            <div class="history-time">{{ formatTime(session.updateTime) }}</div>
          </div>
          <el-icon class="delete-icon" @click.stop="handleDeleteSession(session.sessionId)">
            <Delete />
          </el-icon>
        </div>
        <div v-if="sessions.length === 0" class="empty-history">
          <el-icon :size="32" color="#555"><ChatDotRound /></el-icon>
          <p>暂无对话记录</p>
        </div>
      </div>
    </div>

    <!-- 右侧对话区域 -->
    <div class="chat-main">
      <!-- 无消息时的欢迎页 -->
      <div v-if="messages.length === 0 && !loading" class="welcome-page">
        <div class="welcome-bg"></div>
        <div class="welcome-content">
          <div class="welcome-avatar animate__animated animate__fadeInDown">🎓</div>
          <h1 class="animate__animated animate__fadeInUp animate__delay-1s">你好，我是学习智能体</h1>
          <p class="welcome-sub animate__animated animate__fadeInUp animate__delay-1s">有什么我可以帮助你的？</p>

          <div class="agent-cards">
            <div v-for="(desc, key, index) in agentDescriptions" :key="key"
              class="agent-card animate__animated animate__fadeInUp"
              :style="{ animationDelay: (0.2 + index * 0.1) + 's' }"
              @click="selectedAgent = key; $refs.inputRef?.focus()">
              <div class="agent-card-icon">{{ agentIcons[key] }}</div>
              <div class="agent-card-name">{{ agentNames[key] }}</div>
              <div class="agent-card-desc">{{ desc }}</div>
            </div>
          </div>
        </div>

        <!-- 底部输入框（欢迎页） -->
        <div class="welcome-input">
          <div class="input-wrapper">
            <el-select v-model="selectedAgent" class="agent-select" size="small">
              <el-option v-for="(name, key) in agentNames" :key="key" :label="name" :value="key" />
            </el-select>
            <el-input
              ref="inputRef"
              v-model="inputMessage"
              type="textarea"
              :rows="1"
              :autosize="{ minRows: 1, maxRows: 4 }"
              placeholder="输入你的问题..."
              @keydown.enter.exact.prevent="handleSend"
              @keydown.enter.shift.exact="void 0"
            />
            <el-button class="send-btn" :disabled="!inputMessage.trim() || loading" @click="handleSend">
              <el-icon v-if="!loading"><Promotion /></el-icon>
              <el-icon v-else class="is-loading"><Loading /></el-icon>
            </el-button>
          </div>
          <div class="input-hint">Enter 发送，Shift+Enter 换行</div>
        </div>
      </div>

      <!-- 有消息时的对话页 -->
      <div v-else class="chat-page">
        <!-- 顶部标题栏 -->
        <div class="chat-header">
          <div class="chat-title">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ currentTitle }}</span>
          </div>
          <el-select v-model="selectedAgent" class="header-agent-select" size="small">
            <el-option v-for="(name, key) in agentNames" :key="key" :label="name" :value="key" />
          </el-select>
        </div>

        <!-- 消息列表 -->
        <div class="message-list" ref="messageList">
          <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
            <!-- 用户消息 -->
            <div v-if="msg.role === 'user'" class="message-row user-row">
              <div class="user-bubble">
                <div class="bubble-text">{{ msg.content }}</div>
              </div>
              <div class="avatar user-avatar">我</div>
            </div>
            <!-- AI消息 -->
            <div v-else class="message-row ai-row">
              <div class="avatar ai-avatar">🎓</div>
              <div class="ai-bubble">
                <div class="bubble-text markdown-body" v-html="renderMarkdown(msg.content)"></div>
                <div v-if="msg.time" class="msg-time">{{ msg.time }}</div>
              </div>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="message-row ai-row">
            <div class="avatar ai-avatar">🎓</div>
            <div class="ai-bubble">
              <div class="typing-indicator">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部输入框 -->
        <div class="chat-input">
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="1"
              :autosize="{ minRows: 1, maxRows: 4 }"
              placeholder="输入你的问题..."
              @keydown.enter.exact.prevent="handleSend"
              @keydown.enter.shift.exact="void 0"
            />
            <el-button class="send-btn" :disabled="!inputMessage.trim() || loading" @click="handleSend">
              <el-icon v-if="!loading"><Promotion /></el-icon>
              <el-icon v-else class="is-loading"><Loading /></el-icon>
            </el-button>
          </div>
          <div class="input-hint">Enter 发送，Shift+Enter 换行</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { marked } from 'marked'
import { ElMessageBox, ElMessage } from 'element-plus'
import { sendMessage, getSessions, getSessionMessages, deleteSession } from '../api'
import { Plus, ChatDotRound, Promotion, Loading, Delete } from '@element-plus/icons-vue'

const selectedAgent = ref('tutor')
const inputMessage = ref('')
const messages = ref([])
const loading = ref(false)
const messageList = ref(null)
const inputRef = ref(null)
const studentId = ref('student_001')
const sessions = ref([])
const currentSessionId = ref(null)
const currentTitle = ref('新对话')

const agentNames = {
  tutor: '辅导智能体',
  profile: '画像智能体',
  resource: '资源智能体',
  path: '路径智能体',
  eval: '评估智能体'
}

const agentIcons = {
  tutor: '📖',
  profile: '👤',
  resource: '📚',
  path: '🗺️',
  eval: '📊'
}

const agentDescriptions = {
  tutor: '解答学习疑问，提供个性化辅导',
  profile: '了解你并构建学习画像',
  resource: '生成教学大纲、PPT、练习题等',
  path: '规划个性化学习路径',
  eval: '评估学习效果，提供改进建议'
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function renderMarkdown(text) {
  if (!text) return ''
  return marked(text)
}

function scrollToBottom() {
  nextTick(() => {
    if (messageList.value) {
      messageList.value.scrollTop = messageList.value.scrollHeight
    }
  })
}

// 新建对话
function newChat() {
  messages.value = []
  currentSessionId.value = null
  currentTitle.value = '新对话'
}

// 加载会话的所有消息
async function loadSession(session) {
  currentSessionId.value = session.sessionId
  currentTitle.value = session.title
  try {
    const res = await getSessionMessages(session.sessionId)
    messages.value = res.data.map(msg => ([
      { role: 'user', content: msg.content, time: new Date(msg.createTime).toLocaleTimeString() },
      msg.response ? { role: 'ai', content: msg.response, time: new Date(msg.createTime).toLocaleTimeString() } : null
    ])).flat().filter(Boolean)
    scrollToBottom()
  } catch (e) {
    console.error('加载会话消息失败:', e)
    ElMessage.error('加载对话失败')
  }
}

// 删除会话
async function handleDeleteSession(sessionId) {
  try {
    await ElMessageBox.confirm('确定删除这条对话记录吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteSession(sessionId)
    ElMessage.success('已删除')
    // 如果删除的是当前会话，清空消息
    if (currentSessionId.value === sessionId) {
      newChat()
    }
    loadSessions()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 加载会话列表
async function loadSessions() {
  try {
    const res = await getSessions(studentId.value)
    sessions.value = res.data
  } catch (e) {
    console.error('加载会话列表失败:', e)
  }
}

// 发送消息
async function handleSend() {
  const message = inputMessage.value.trim()
  if (!message || loading.value) return

  messages.value.push({
    role: 'user',
    content: message,
    time: new Date().toLocaleTimeString()
  })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  const aiMessage = { role: 'ai', content: '', time: '' }
  messages.value.push(aiMessage)

  try {
    const res = await sendMessage(message, studentId.value, selectedAgent.value, currentSessionId.value)
    aiMessage.content = res.data.response
    aiMessage.time = new Date().toLocaleTimeString()
    // 保存返回的sessionId
    if (!currentSessionId.value) {
      currentSessionId.value = res.data.sessionId
      currentTitle.value = message.length > 30 ? message.substring(0, 30) + '...' : message
    }
    loading.value = false
    scrollToBottom()
    loadSessions()
  } catch (error) {
    loading.value = false
    aiMessage.content = '抱歉，发生了错误：' + error.message
    aiMessage.time = new Date().toLocaleTimeString()
  }
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: calc(100vh - 48px);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--border-color, #e5e5e5);
}

/* ========== 侧边栏 ========== */
.sidebar {
  width: 260px;
  background: #16161e;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  border-right: 1px solid rgba(255,255,255,0.06);
}

.sidebar-header {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.new-chat-btn {
  width: 100%;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.12);
  color: #e0e0e0;
  height: 40px;
  border-radius: 10px;
  font-size: 14px;
}

.new-chat-btn:hover {
  background: rgba(255,255,255,0.14);
  border-color: rgba(255,255,255,0.2);
  color: #fff;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: all 0.2s;
}

.history-item:hover {
  background: rgba(255,255,255,0.08);
}

.history-item:hover .delete-icon {
  opacity: 1;
}

.history-item.active {
  background: rgba(114, 46, 209, 0.15);
  border: 1px solid rgba(114, 46, 209, 0.2);
}

.history-item:not(.active) {
  border: 1px solid transparent;
}

.delete-icon {
  opacity: 0;
  color: #666;
  font-size: 14px;
  flex-shrink: 0;
  transition: all 0.15s;
  padding: 4px;
  border-radius: 4px;
}

.delete-icon:hover {
  color: #F56C6C;
  background: rgba(245, 108, 108, 0.1);
}

.history-icon {
  color: #888;
  font-size: 16px;
  flex-shrink: 0;
}

.history-info {
  flex: 1;
  min-width: 0;
}

.history-title {
  color: #e0e0e0;
  font-size: 13px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 450;
}

.history-time {
  color: #666;
  font-size: 11px;
  margin-top: 2px;
}

.empty-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #555;
}

.empty-history p {
  margin-top: 8px;
  font-size: 13px;
}

/* ========== 欢迎页 ========== */
.welcome-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary, #fafafa);
  position: relative;
  overflow: hidden;
}

.welcome-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 25% 40%, rgba(114, 46, 209, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 75% 60%, rgba(59, 130, 246, 0.06) 0%, transparent 50%);
  pointer-events: none;
  animation: bgPulse 6s ease-in-out infinite alternate;
}

@keyframes bgPulse {
  0% { opacity: 0.8; }
  100% { opacity: 1; }
}

.dark-theme .welcome-bg {
  background:
    radial-gradient(ellipse at 20% 50%, rgba(146, 84, 222, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(48, 128, 255, 0.08) 0%, transparent 50%);
}

.welcome-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.welcome-avatar {
  font-size: 48px;
  margin-bottom: 16px;
}

.welcome-content h1 {
  font-size: 28px;
  color: var(--text-primary, #1a1a2e);
  margin: 0 0 8px 0;
  font-weight: 600;
}

.welcome-sub {
  color: var(--text-secondary, #888);
  font-size: 16px;
  margin: 0 0 40px 0;
}

.agent-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  max-width: 700px;
  width: 100%;
}

.agent-card {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #e8e8e8);
  border-radius: 14px;
  padding: 20px 16px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
  backdrop-filter: blur(8px);
}

.agent-card:hover {
  border-color: rgba(114, 46, 209, 0.4);
  box-shadow: 0 4px 16px rgba(114, 46, 209, 0.1);
  transform: translateY(-2px);
}

.agent-card-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.agent-card-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #1a1a2e);
  margin-bottom: 4px;
}

.agent-card-desc {
  font-size: 11px;
  color: var(--text-muted, #999);
  line-height: 1.4;
}

/* ========== 欢迎页输入框 ========== */
.welcome-input {
  padding: 0 20px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.welcome-input .input-wrapper {
  max-width: 700px;
  width: 100%;
}

/* ========== 对话页 ========== */
.chat-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary, #fafafa);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: var(--bg-card, #fff);
  border-bottom: 1px solid var(--border-color, #e8e8e8);
  backdrop-filter: blur(12px);
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary, #1a1a2e);
}

.header-agent-select {
  width: 130px;
}

/* ========== 消息列表 ========== */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.message-wrapper {
  padding: 0 20px;
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.user-row {
  flex-direction: row-reverse;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
  font-weight: 600;
  border: 1.5px solid transparent;
}

.user-avatar {
  background: linear-gradient(135deg, #722ed1 0%, #9254de 100%);
  color: #fff;
  box-shadow: 0 1px 3px rgba(114, 46, 209, 0.2);
}

.ai-avatar {
  background: linear-gradient(135deg, #722ed1 0%, #9254de 100%);
  color: #fff;
  font-size: 16px;
  box-shadow: 0 1px 3px rgba(114, 46, 209, 0.2);
}

.user-bubble {
  background: linear-gradient(135deg, #722ed1 0%, #8b47ea 100%);
  color: #fff;
  padding: 10px 16px;
  border-radius: 16px 16px 4px 16px;
  max-width: 70%;
  line-height: 1.7;
  font-size: 13px;
  box-shadow: 0 1px 4px rgba(114, 46, 209, 0.2);
  border: 1px solid rgba(114, 46, 209, 0.15);
}

.ai-bubble {
  background: var(--bg-card, #fff);
  padding: 12px 16px;
  border-radius: 16px 16px 16px 4px;
  max-width: 80%;
  font-size: 13px;
  line-height: 1.7;
  border: 1px solid var(--border-color, #e5e5e5);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.bubble-text {
  word-break: break-word;
  line-height: 1.7;
}

.msg-time {
  font-size: 11px;
  color: #bbb;
  margin-top: 6px;
}

/* ========== 输入框通用 ========== */
.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #e5e5e5);
  border-radius: 16px;
  padding: 10px 10px 10px 18px;
  transition: all 0.2s;
  backdrop-filter: blur(12px);
}

.input-wrapper:focus-within {
  border-color: #722ed1;
  box-shadow: 0 0 0 3px rgba(114, 46, 209, 0.08), 0 2px 8px rgba(0, 0, 0, 0.06);
}

.input-wrapper .el-select {
  flex-shrink: 0;
}

.agent-select {
  width: 100px;
}

.input-wrapper :deep(.el-textarea__inner) {
  border: none !important;
  box-shadow: none !important;
  padding: 4px 0;
  resize: none;
  line-height: 1.5;
}

.send-btn {
  width: 34px;
  height: 34px;
  border-radius: 12px !important;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #722ed1 !important;
  border: none !important;
  color: #fff !important;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(114, 46, 209, 0.2);
}

.send-btn:hover:not(:disabled) {
  background: #8b47ea !important;
  box-shadow: 0 2px 6px rgba(114, 46, 209, 0.3);
}

.send-btn:disabled {
  background: var(--border-color, #c0c4cc) !important;
  cursor: not-allowed;
  box-shadow: none;
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: #bbb;
  margin-top: 6px;
}

/* ========== 对话页输入框 ========== */
.chat-input {
  padding: 12px 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chat-input .input-wrapper {
  max-width: 800px;
  width: 100%;
}

/* ========== 打字动画 ========== */
.typing-indicator {
  display: flex;
  gap: 5px;
  padding: 4px 0;
}

.typing-dot {
  width: 6px;
  height: 6px;
  background: rgba(114, 46, 209, 0.5);
  border-radius: 50%;
  animation: typingPulse 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(2) { animation-delay: 200ms; }
.typing-dot:nth-child(3) { animation-delay: 400ms; }

@keyframes typingPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* ========== Markdown 样式 ========== */
:deep(.markdown-body) p { margin: 6px 0; }
:deep(.markdown-body) h1,
:deep(.markdown-body) h2,
:deep(.markdown-body) h3 { margin: 12px 0 6px; color: var(--text-primary, #1a1a2e); }
:deep(.markdown-body) code {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 13px;
}
:deep(.markdown-body) pre {
  background: var(--bg-hover, #1e1e2e);
  color: #abb2bf;
  padding: 14px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}
:deep(.markdown-body) pre code {
  background: none;
  color: inherit;
  padding: 0;
}
:deep(.markdown-body) table {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}
:deep(.markdown-body) th,
:deep(.markdown-body) td {
  border: 1px solid #e0e0e0;
  padding: 6px 10px;
  text-align: left;
}
:deep(.markdown-body) th {
  background: var(--bg-hover, #f5f5f5);
  font-weight: 600;
}
:deep(.markdown-body) ul,
:deep(.markdown-body) ol {
  padding-left: 20px;
  margin: 6px 0;
}
:deep(.markdown-body) blockquote {
  border-left: 3px solid #722ed1;
  padding-left: 12px;
  color: #666;
  margin: 8px 0;
}

/* 滚动条 */
.history-list::-webkit-scrollbar,
.message-list::-webkit-scrollbar {
  width: 4px;
}

.history-list::-webkit-scrollbar-thumb,
.message-list::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.15);
  border-radius: 2px;
}
</style>
