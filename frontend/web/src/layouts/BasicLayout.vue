<template>
  <el-container class="layout">
    <el-aside width="220px" class="layout-aside">
      <div class="brand">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 21s-7-4.5-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 11c0 5.5-7 10-7 10z" />
            <circle cx="12" cy="11" r="2.2" />
          </svg>
        </div>
        <div class="brand-text">
          <div class="brand-title">DR 智能筛查</div>
          <div class="brand-sub">Diabetic Retinopathy</div>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="layout-menu" :collapse="false">
        <el-menu-item
          v-for="item in visibleMenus"
          :key="item.path"
          :index="item.path"
        >
          <span class="menu-icon" v-html="iconSvg(item.icon)"></span>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-right">
          <el-tag v-if="userStore.role" :type="userStore.role === 'ADMIN' ? 'warning' : 'primary'" effect="light" round>
            {{ userStore.role === 'ADMIN' ? '管理员' : '医生' }}
          </el-tag>
          <el-dropdown trigger="click" @command="onCommand">
            <span class="header-user">
              <el-avatar :size="30" class="header-avatar">{{ avatarText }}</el-avatar>
              <span class="header-name">{{ userStore.username }}</span>
              <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor" class="header-caret">
                <path d="M7 10l5 5 5-5z" />
              </svg>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

interface MenuItem {
  path: string
  title: string
  icon: string
  permission?: string
}

const ICONS: Record<string, string> = {
  dashboard:
    'M3 3h8v8H3V3zm10 0h8v8h-8V3zM3 13h8v8H3v-8zm10 0h8v8h-8v-8z',
  upload: 'M12 3l-4 4h3v6h2V7h3l-4-4zM5 19h14v2H5z',
  records: 'M4 6h16v2H4V6zm0 5h16v2H4v-2zm0 5h16v2H4v-2z',
  statistics: 'M4 20h3V10H4v10zm6.5 0h3V4h-3v16zM17 20h3v-7h-3v7z',
  profile:
    'M12 12a5 5 0 1 0 0-10 5 5 0 0 0 0 10zm0 2c-5 0-9 2.5-9 6v2h18v-2c0-3.5-4-6-9-6z'
}

const MENUS: MenuItem[] = [
  { path: '/dashboard', title: '筛查看板', icon: 'dashboard', permission: 'biz:screening:view' },
  { path: '/screening/upload', title: '筛查上传', icon: 'upload', permission: 'biz:screening:create' },
  { path: '/screening/records', title: '筛查记录', icon: 'records', permission: 'biz:screening:view' },
  { path: '/screening/statistics', title: '统计分析', icon: 'statistics', permission: 'biz:screening:view' },
  { path: '/profile', title: '个人中心', icon: 'profile' }
]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const visibleMenus = computed(() =>
  MENUS.filter((m) => !m.permission || userStore.permissions.includes(m.permission))
)

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || 'DR 智能筛查系统')

const avatarText = computed(() => (userStore.username ? userStore.username.charAt(0).toUpperCase() : 'U'))

function iconSvg(path: string) {
  return `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="${path}"/></svg>`
}

function onCommand(cmd: string) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.layout-aside {
  background: linear-gradient(180deg, #0e7490 0%, #0891b2 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}

.brand-logo {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 11px;
  opacity: 0.75;
}

.layout-menu {
  border-right: none;
  background: transparent;
  flex: 1;
  padding-top: 8px;
}

.layout-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.82);
  height: 48px;
  margin: 4px 10px;
  border-radius: 8px;
}

.layout-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.layout-menu :deep(.el-menu-item.is-active) {
  background: #fff;
  color: var(--drs-primary);
  font-weight: 600;
}

.menu-icon {
  display: inline-flex;
  align-items: center;
  margin-right: 10px;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid var(--drs-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.header-avatar {
  background: var(--drs-primary);
  color: #fff;
  font-weight: 600;
}

.header-name {
  font-size: 14px;
  color: var(--drs-text);
}

.layout-main {
  background: var(--drs-bg);
  padding: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
