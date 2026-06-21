<template>
  <div class="resource-container">
    <!-- 资源类型选择 -->
    <el-card class="type-card">
      <template #header>
        <h3>选择资源类型</h3>
      </template>

      <div class="type-grid">
        <div
          v-for="type in resourceTypes"
          :key="type.value"
          :class="['type-item', selectedType === type.value ? 'active' : '']"
          @click="selectedType = type.value"
        >
          <div class="type-icon">{{ type.icon }}</div>
          <div class="type-name">{{ type.label }}</div>
          <div class="type-desc">{{ type.desc }}</div>
        </div>
      </div>
    </el-card>

    <!-- 输入区域 -->
    <el-card class="input-card">
      <template #header>
        <h3>输入知识点</h3>
      </template>

      <el-input
        v-model="inputText"
        type="textarea"
        :rows="4"
        :placeholder="getPlaceholder()"
      />

      <div class="input-actions">
        <el-button type="primary" size="large" @click="generate" :loading="loading">
          生成资源
        </el-button>
        <el-button size="large" @click="clear">清空</el-button>
      </div>
    </el-card>

    <!-- 生成结果 -->
    <el-card v-if="result" class="result-card">
      <template #header>
        <div class="result-header">
          <h3>生成结果</h3>
          <div class="result-actions">
            <el-button type="primary" size="small" @click="copyResult">复制内容</el-button>
          </div>
        </div>
      </template>

      <div class="result-content" v-html="renderMarkdown(result)"></div>
    </el-card>

    <!-- 历史记录 -->
    <el-card class="history-card">
      <template #header>
        <div class="result-header">
          <h3>生成历史</h3>
          <el-button size="small" @click="loadHistory">刷新</el-button>
        </div>
      </template>

      <el-table :data="historyList" style="width: 100%" max-height="400">
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <el-tag :type="getTypeTag(scope.row.type)">{{ getTypeName(scope.row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inputText" label="输入内容" show-overflow-tooltip />
        <el-table-column label="生成时间" width="160">
          <template #default="scope">
            {{ new Date(scope.row.createTime).toLocaleString('zh-CN') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button type="primary" link @click="viewHistory(scope.row)">查看</el-button>
            <el-button type="danger" link @click="deleteHistory(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { marked } from 'marked'
import { ElMessage } from 'element-plus'
import api, { generateResource, getResourceHistory } from '../api'

const selectedType = ref('syllabus')
const inputText = ref('')
const result = ref('')
const loading = ref(false)
const studentId = ref('student_001')
const historyList = ref([])

const resourceTypes = [
  { value: 'syllabus', label: '教学大纲', icon: '📋', desc: '生成完整的课程教学大纲' },
  { value: 'ppt', label: 'PPT大纲', icon: '📊', desc: '生成PPT课件大纲' },
  { value: 'exercise', label: '练习题', icon: '✏️', desc: '生成配套练习题' },
  { value: 'mindmap', label: '思维导图', icon: '🗺️', desc: '生成知识点思维导图' },
  { value: 'review', label: '复习提纲', icon: '📝', desc: '生成期末复习提纲' }
]

function getPlaceholder() {
  const placeholders = {
    syllabus: '请输入课程名称或知识点，例如：机器学习基础',
    ppt: '请输入知识点，例如：神经网络原理',
    exercise: '请输入知识点，例如：Python函数定义',
    mindmap: '请输入知识点，例如：数据结构与算法',
    review: '请输入课程或知识点，例如：线性代数'
  }
  return placeholders[selectedType.value]
}

function renderMarkdown(text) {
  if (!text) return ''
  return marked(text)
}

async function generate() {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入知识点')
    return
  }

  loading.value = true
  result.value = ''

  try {
    const res = await generateResource(selectedType.value, inputText.value, studentId.value)
    result.value = res.data.content || res.data
    loading.value = false
    loadHistory()
  } catch (error) {
    loading.value = false
    ElMessage.error('生成失败：' + error.message)
  }
}

onMounted(() => {
  loadHistory()
})

function clear() {
  inputText.value = ''
  result.value = ''
}

function copyResult() {
  navigator.clipboard.writeText(result.value)
    .then(() => ElMessage.success('已复制到剪贴板'))
    .catch(() => ElMessage.error('复制失败'))
}

const typeNames = { syllabus: '教学大纲', ppt: 'PPT大纲', exercise: '练习题', mindmap: '思维导图', review: '复习提纲' }
const typeTags = { syllabus: '', ppt: 'success', exercise: 'warning', mindmap: 'info', review: 'danger' }

function getTypeName(type) { return typeNames[type] || type }
function getTypeTag(type) { return typeTags[type] || '' }

async function loadHistory() {
  try {
    const res = await getResourceHistory()
    historyList.value = res.data
  } catch (e) {
    console.error('加载历史失败:', e)
  }
}

function viewHistory(row) {
  selectedType.value = row.type
  inputText.value = row.inputText
  result.value = row.outputText
}

async function deleteHistory(id) {
  try {
    await api.delete(`/resource/history/${id}`)
    loadHistory()
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.resource-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.type-card h3,
.input-card h3,
.result-card h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.type-item {
  padding: 20px;
  background: var(--bg-hover, #f5f7fa);
  border-radius: 10px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.type-item:hover {
  background: #f9f0ff;
  border-color: #722ed1;
}

.type-item.active {
  background: #f9f0ff;
  border-color: #722ed1;
}

.type-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.type-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary, #303133);
  margin-bottom: 4px;
}

.type-desc {
  font-size: 12px;
  color: var(--text-muted, #909399);
}

.input-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-actions {
  display: flex;
  gap: 8px;
}

.result-content {
  line-height: 1.8;
  color: #303133;
}

:deep(.result-content) h1,
:deep(.result-content) h2,
:deep(.result-content) h3 {
  margin: 16px 0 8px 0;
  color: #722ed1;
}

:deep(.result-content) p {
  margin: 8px 0;
}

:deep(.result-content) ul,
:deep(.result-content) ol {
  padding-left: 24px;
}

:deep(.result-content) code {
  background: #f4f4f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

:deep(.result-content) pre {
  background: #282c34;
  color: #abb2bf;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
}

:deep(.result-content) pre code {
  background: none;
  color: inherit;
}
</style>
