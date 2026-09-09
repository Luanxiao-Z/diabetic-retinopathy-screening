<template>
  <div class="dashboard">
    <h1>筛查看板</h1>
    <el-alert
      type="info"
      :closable="false"
      title="阶段 0 占位页面：登录与权限链路已打通，业务页面将在阶段 4 落地。"
    />
    <p>当前用户：{{ userStore.username }}（{{ userStore.role }}）</p>
    <el-button type="primary" @click="logout">退出登录</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.dashboard {
  padding: 24px;
}
</style>
