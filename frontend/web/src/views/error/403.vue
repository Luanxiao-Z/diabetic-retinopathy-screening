<template>
  <div class="forbidden">
    <div class="fb-card drs-card">
      <span class="fb-ico" aria-hidden="true"><AppIcon name="lock" :size="28" /></span>
      <div class="fb-code">403</div>
      <h1 class="fb-title">无访问权限</h1>
      <p class="fb-desc">
        当前账号（{{ roleText }}）没有访问该页面的功能权限。<br />
        如需开通，请联系系统管理员为账号分配对应角色。
      </p>

      <div class="fb-actions">
        <el-button @click="goBack">
          <AppIcon name="chevronLeft" :size="15" class="btn-ico" />返回上一页
        </el-button>
        <el-button type="primary" @click="goHome">
          <AppIcon name="dashboard" :size="15" class="btn-ico" />返回工作台
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const roleText = computed(() => {
  if (userStore.role === 'ADMIN') return '管理员'
  if (userStore.role === 'DOCTOR') return '医生'
  return '未分配角色'
})

function goHome() {
  router.push('/dashboard')
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/dashboard')
  }
}
</script>

<style scoped>
.forbidden {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--drs-bg);
}

.fb-card {
  width: 100%;
  max-width: 460px;
  padding: 40px 32px;
  text-align: center;
}

.fb-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: var(--drs-warn-bg);
  color: var(--drs-warn);
  margin-bottom: 14px;
}

.fb-code {
  font-size: 44px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -1px;
  color: var(--drs-ink-300);
}

.fb-title {
  font-size: 20px;
  font-weight: 600;
  margin: 10px 0 8px;
  color: var(--drs-ink-900);
}

.fb-desc {
  font-size: 13px;
  line-height: 1.8;
  color: var(--drs-ink-500);
  margin: 0 0 24px;
}

.fb-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.btn-ico {
  margin-right: 5px;
}
</style>
