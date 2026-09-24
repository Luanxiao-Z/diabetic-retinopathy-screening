<template>
  <div class="drs-page">
    <PageHeader title="个人中心" subtitle="当前登录账号、数据权限与功能权限" :crumbs="['账户', '个人中心']" />

    <div class="profile-grid">
      <!-- ============ 账号信息 ============ -->
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>账号信息</h3>
          <span class="drs-card-meta">登录态由服务端会话管理</span>
        </div>
        <div class="drs-card-body">
          <div class="id-card">
            <span class="id-avatar" aria-hidden="true">{{ avatarText }}</span>
            <div class="id-meta">
              <div class="id-name">{{ userStore.username || '—' }}</div>
              <span class="id-role">{{ roleText }}</span>
            </div>
          </div>

          <dl class="info-list">
            <div class="info-row">
              <dt>角色</dt>
              <dd>{{ userStore.role || '—' }}</dd>
            </div>
            <div class="info-row">
              <dt>数据权限</dt>
              <dd>
                <span class="scope-pill">{{ scopeText }}</span>
                <span class="info-hint">{{ scopeHint }}</span>
              </dd>
            </div>
            <div class="info-row">
              <dt>功能权限</dt>
              <dd>{{ permissions.length }} 项</dd>
            </div>
          </dl>
        </div>
      </section>

      <!-- ============ 功能权限 ============ -->
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>功能权限</h3>
          <span class="drs-card-meta">由角色映射，非 RBAC 表</span>
        </div>
        <div class="drs-card-body">
          <div v-for="grp in permissionGroups" :key="grp.title" class="perm-group">
            <div class="perm-group-title">{{ grp.title }}</div>
            <div class="perm-tags">
              <span v-for="p in grp.items" :key="p.code" class="perm-tag" :title="p.code">
                <AppIcon name="check" :size="13" />
                {{ p.label }}
              </span>
            </div>
          </div>
          <p v-if="!permissionGroups.length" class="perm-empty">当前账号未分配任何功能权限</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const permissions = computed(() => userStore.permissions)
const avatarText = computed(() =>
  userStore.username ? userStore.username.charAt(0).toUpperCase() : 'U'
)
const roleText = computed(() => {
  if (userStore.role === 'ADMIN') return '管理员'
  if (userStore.role === 'DOCTOR') return '医生'
  return '未分配角色'
})
const scopeText = computed(() => {
  if (userStore.dataScope === 'ALL') return '全部数据（ALL）'
  if (userStore.dataScope === 'SELF') return '仅本人数据（SELF）'
  return userStore.role === 'ADMIN' ? '全部数据（ALL）' : '仅本人数据（SELF）'
})
const scopeHint = computed(() =>
  userStore.dataScope === 'SELF' || userStore.role === 'DOCTOR'
    ? '仅可查询、导出与删除本人创建的筛查记录'
    : '可查询、导出与删除全部筛查记录'
)

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

const GROUP_OF: Record<string, string> = {
  'biz:screening': '筛查业务',
  'common:dict': '公共数据',
  'admin:user': '系统管理 · 用户',
  'admin:dict': '系统管理 · 字典'
}

const permissionGroups = computed(() => {
  const buckets = new Map<string, { code: string; label: string }[]>()
  for (const code of permissions.value) {
    const prefix = code.split(':').slice(0, 2).join(':')
    const title = GROUP_OF[prefix] || '其他'
    if (!buckets.has(title)) buckets.set(title, [])
    buckets.get(title)!.push({ code, label: LABELS[code] || code })
  }
  return [...buckets.entries()].map(([title, items]) => ({ title, items }))
})
</script>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
  gap: var(--drs-gap);
  align-items: start;
}

/* ---------- 账号信息 ---------- */
.id-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: var(--drs-gap);
  border-bottom: 1px dashed var(--drs-border);
}

.id-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
  font-size: 22px;
  font-weight: 600;
}

.id-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--drs-ink-900);
}

.id-role {
  display: inline-block;
  margin-top: 4px;
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--drs-ink-100);
  color: var(--drs-ink-600);
  font-size: 12px;
}

.info-list {
  margin: var(--drs-gap) 0 0;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: var(--drs-gap);
  padding: 10px 0;
  border-bottom: 1px solid var(--drs-border);
}

.info-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.info-row dt {
  width: 84px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--drs-ink-500);
}

.info-row dd {
  margin: 0;
  font-size: 13.5px;
  color: var(--drs-ink-800);
}

.scope-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--drs-primary-50);
  border: 1px solid var(--drs-primary-100);
  color: var(--drs-primary-800);
  font-size: 12px;
  font-weight: 500;
}

.info-hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--drs-ink-500);
}

/* ---------- 功能权限 ---------- */
.perm-group + .perm-group {
  margin-top: var(--drs-gap);
}

.perm-group-title {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: var(--drs-ink-400);
  margin-bottom: 8px;
}

.perm-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.perm-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 11px;
  border-radius: 999px;
  background: var(--drs-surface-2);
  border: 1px solid var(--drs-border);
  color: var(--drs-ink-700);
  font-size: 12.5px;
}

.perm-tag :deep(svg) {
  color: var(--drs-ok);
}

.perm-empty {
  margin: 0;
  font-size: 13px;
  color: var(--drs-ink-500);
}

@media (max-width: 1024px) {
  .profile-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
