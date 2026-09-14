<template>
  <div class="forbidden">
    <div class="fb-code">403</div>
    <div class="fb-title">无访问权限</div>
    <div class="fb-desc">当前账号没有访问该页面的权限，请联系管理员或返回首页。</div>
    <el-button type="primary" @click="goHome">返回看板</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
function goHome() {
  router.push('/dashboard')
}
</script>

<style scoped>
.forbidden {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: var(--drs-bg);
}

.fb-code {
  font-size: 80px;
  font-weight: 800;
  color: var(--drs-primary);
  line-height: 1;
}

.fb-title {
  font-size: 22px;
  font-weight: 600;
}

.fb-desc {
  color: var(--drs-text-soft);
  margin-bottom: 10px;
}
</style>
