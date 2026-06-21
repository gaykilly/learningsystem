<template>
  <el-container class="app-container" :class="{ 'dark-theme': isDark }">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '200px'" class="app-aside">
      <div class="logo">
        <span class="logo-icon">🎓</span>
        <h2 v-show="!isCollapsed">学习智能体</h2>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="isCollapsed"
        router
        class="aside-menu"
        background-color="#1a1a2e"
        text-color="#a0a0b0"
        active-text-color="#fff"
      >
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>智能对话</template>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <template #title>学习画像</template>
        </el-menu-item>
        <el-menu-item index="/resource">
          <el-icon><Document /></el-icon>
          <template #title>资源生成</template>
        </el-menu-item>
        <el-menu-item index="/path">
          <el-icon><MapLocation /></el-icon>
          <template #title>学习路径</template>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Collection /></el-icon>
          <template #title>知识库</template>
        </el-menu-item>
        <el-menu-item index="/exam">
          <el-icon><Trophy /></el-icon>
          <template #title>考试冲刺</template>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-bottom">
        <div class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <el-icon>
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>
        <div class="theme-btn" @click="toggleTheme">
          <el-icon>
            <Moon v-if="!isDark" />
            <Sunny v-else />
          </el-icon>
        </div>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, User, Document, MapLocation, Collection, Trophy, Fold, Expand, Moon, Sunny } from '@element-plus/icons-vue'

const route = useRoute()
const currentRoute = computed(() => route.path)
const isCollapsed = ref(false)
const isDark = ref(false)

function toggleTheme() {
  isDark.value = !isDark.value
}

watch(isDark, (val) => {
  document.documentElement.classList.toggle('dark', val)
}, { immediate: true })
</script>

<style>
:root {
  --bg-primary: #f5f5f5;
  --bg-card: #ffffff;
  --bg-hover: #f5f7fa;
  --text-primary: #0a0a0a;
  --text-secondary: #737373;
  --text-muted: #a0a0b0;
  --border-color: #e5e5e5;
  --accent: #722ed1;
  --accent-light: rgba(114, 46, 209, 0.1);
  --shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.dark-theme,
:root.dark {
  --bg-primary: #0a0a0a;
  --bg-card: #1a1a1a;
  --bg-hover: #2a2a2a;
  --text-primary: #fafafa;
  --text-secondary: #a0a0a0;
  --text-muted: #666666;
  --border-color: #333333;
  --accent: #9254de;
  --accent-light: rgba(146, 84, 222, 0.15);
  --shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  background: var(--bg-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.app-container {
  height: 100vh;
}

.app-aside {
  background-color: #16161e;
  overflow: visible;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  position: relative;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.logo-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.logo h2 {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.aside-menu {
  border-right: none !important;
  flex: 1;
  overflow-x: hidden;
}

.aside-menu .el-menu-item {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 10px;
}

.aside-menu .el-menu-item:hover {
  background-color: rgba(255,255,255,0.08) !important;
}

.aside-menu .el-menu-item.is-active {
  background-color: rgba(114, 46, 209, 0.18) !important;
  color: #fff !important;
  border: 1px solid rgba(114, 46, 209, 0.25);
}

.aside-menu .el-menu-item {
  border: 1px solid transparent;
}

.sidebar-bottom {
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}

.collapse-btn,
.theme-btn {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a0a0b0;
  cursor: pointer;
  transition: all 0.2s;
}

.collapse-btn:hover,
.theme-btn:hover {
  background-color: rgba(255,255,255,0.08);
  color: #fff;
}

.app-main {
  background-color: var(--bg-primary);
  padding: 24px;
  overflow-y: auto;
  transition: background-color 0.3s;
}
</style>
