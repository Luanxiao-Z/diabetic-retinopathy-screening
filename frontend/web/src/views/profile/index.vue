<template>
  <div class="drs-page">
    <h1 class="drs-page-title">个人中心</h1>
    <p class="drs-page-subtitle">当前登录账号信息与权限</p>

    <div class="drs-card profile-card">
      <div class="profile-head">
        <el-avatar :size="64" class="profile-avatar">{{ avatarText }}</el-avatar>
        <div>
          <div class="profile-name">{{ userStore.username }}</div>
          <el-tag :type="userStore.role === 'ADMIN' ? 'warning' : 'primary'" effect="light" round>
            {{ userStore.role === 'ADMIN' ? '管理员' : '医生' }}
          </el-tag>
        </div>
      </div>

      <el-divider />

      <div class="profile-block">
        <div class="profile-label">数据权限</div>
        <div class="profile-value">
          {{ userStore.role === 'ADMIN' ? '全部数据（ALL）' : '仅本人数据（SELF）' }}
        </div>
      </div>

      <div class="profile-block">
        <div class="profile-label">权限编码（{{ permissions.length }}）</div>
        <div class="profile-tags">
          <el-tag v-for="p in permissions" :key="p" class="perm-tag" effect="plain" type="info">
            {{ permissionLabel(p) }}
          </el-tag>
          <span v-if="!permissions.length" class="profile-empty">无</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const permissions = computed(() => userStore.permissions)
const avatarText = computed(() => (userStore.username ? userStore.username.charAt(0).toUpperCase() : 'U'))

const LABELS: Record<string, string> = {
  'biz:screening:create': '上传筛查',
  'biz:screening:view': '查看记录',
  'biz:screening:export': '导出报告',
  'biz:screening:delete': '删除记录',
  'common:dict:view': '字典查看',
  'admin:user:view': '用户管理',
  'admin:user:edit': '用户编辑',
  'admin:dict:view': '字典管理',
  'admin:dict:edit': '字典编辑'
}

function permissionLabel(code: string) {
  return LABELS[code] || code
}
</script>

<style scoped>
.profile-card {
  padding: 24px;
  max-width: 640px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  background: var(--drs-primary);
  color: #fff;
  font-size: 24px;
  font-weight: 600;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.profile-block {
  margin-top: 16px;
}

.profile-label {
  font-size: 13px;
  color: var(--drs-text-soft);
  margin-bottom: 8px;
}

.profile-value {
  font-size: 14px;
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.perm-tag {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.profile-empty {
  color: var(--drs-text-soft);
}
</style>
