<template>
  <div class="drs-page">
    <PageHeader title="工作台" :crumbs="['综合看板', '工作台']">
      <template #actions>
        <el-button :loading="loading" @click="load">
          <AppIcon name="refresh" :size="15" class="btn-ico" />刷新
        </el-button>
        <el-button v-permission="'biz:screening:create'" type="primary" @click="go('/screening/upload')">
          <AppIcon name="upload" :size="15" class="btn-ico" />开始筛查
        </el-button>
      </template>
    </PageHeader>

    <!-- ============ 关键指标（概览） ============ -->
    <div class="drs-grid-4 kpi-row">
      <StatCard
        icon="activity"
        label="累计筛查"
        :value="total"
        unit="例"
        tone="brand"
        clickable
        :foot="totalFoot"
        aria-label="累计筛查记录数，点击进入筛查记录列表"
        @click="go('/screening/records')"
      />
      <StatCard
        icon="alert"
        label="转诊率"
        :value="referralRate"
        tone="danger"
        clickable
        foot="建议尽快转诊占比"
        aria-label="转诊率，点击查看转诊建议分布"
        @click="go('/screening/statistics')"
      />
      <StatCard
        icon="hospital"
        label="需转诊"
        :value="referralCount"
        unit="例"
        tone="warn"
        clickable
        foot="建议眼科就诊或转诊"
        aria-label="需转诊人数，点击查看转诊建议分布"
        @click="go('/screening/statistics')"
      />
      <StatCard
        icon="clock"
        label="待人工复核"
        :value="needReviewCount"
        unit="例"
        tone="warn"
        clickable
        foot="模型不确定性达阈值"
        aria-label="待人工复核数量，点击进入筛查记录并筛选待复核"
        @click="goReviewList()"
      />
    </div>

    <!-- ============ 待办摘要 / 快捷入口 ============ -->
    <div class="drs-grid-2 mt">
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>待办事项</h3>
          <el-button link type="primary" @click="go('/screening/todos')">全部待办</el-button>
        </div>
        <div class="drs-card-body">
          <button
            v-for="t in todoItems"
            :key="t.key"
            type="button"
            class="todo-row"
            @click="go(t.path)"
          >
            <span class="todo-ico" :class="`ti-${t.tone}`" aria-hidden="true">
              <AppIcon :name="t.icon" :size="16" />
            </span>
            <span class="todo-text">
              <span class="todo-title">{{ t.title }}</span>
              <span class="todo-desc">{{ t.desc }}</span>
            </span>
            <span class="todo-count" :class="`tc-${t.tone}`">{{ t.count }}</span>
            <AppIcon name="chevronRight" :size="16" class="quick-arrow" />
          </button>
        </div>
      </section>

      <section class="drs-card">
        <div class="drs-card-head">
          <h3>快捷操作</h3>
          <span class="drs-card-meta">{{ roleText }}</span>
        </div>
        <div class="drs-card-body">
          <button
            v-for="q in quickActions"
            :key="q.path"
            type="button"
            class="quick-item"
            @click="go(q.path)"
          >
            <span class="quick-ico" aria-hidden="true"><AppIcon :name="q.icon" :size="18" /></span>
            <span class="quick-text">
              <span class="quick-title">{{ q.title }}</span>
              <span class="quick-desc">{{ q.desc }}</span>
            </span>
            <AppIcon name="chevronRight" :size="16" class="quick-arrow" />
          </button>

          <el-alert
            v-if="!hasCreate"
            class="dash-tip"
            type="info"
            :closable="false"
            title="当前账号无上传权限，可查看统计与历史记录。"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { pagePatients, pageScreening, statisticsScreening } from '@/api/screening'
import { useScreeningStore } from '@/stores/screening'
import { useUserStore } from '@/stores/user'
import type { PatientFollowUpVO, ScreeningStatisticsVO } from '@/types/screening'

/** 与「随访待办」页保持一致的逾期阈值（天） */
const OVERDUE_DAYS = 90

const router = useRouter()
const userStore = useUserStore()
const screeningStore = useScreeningStore()

const loading = ref(false)
const stats = ref<ScreeningStatisticsVO | null>(null)
const pendingReview = ref(0)
const pendingReferral = ref(0)
const overduePatients = ref<PatientFollowUpVO[]>([])

/** 上次加载时的数据版本，用于 KeepAlive 恢复后判断是否需要刷新 */
const loadedVersion = ref(-1)

const hasCreate = computed(() => userStore.permissions.includes('biz:screening:create'))
const roleText = computed(() =>
  userStore.role === 'ADMIN' ? '管理员视角 · 全量数据' : '医生视角 · 本人数据'
)

const total = computed(() => stats.value?.total || 0)
const referralCount = computed(() => stats.value?.suggestionDistribution?.['REFERRAL'] || 0)
const needReviewCount = computed(() => stats.value?.needReviewCount || 0)

const referralRate = computed(() => {
  const r = stats.value?.referralRate
  return r == null ? '0%' : `${(Number(r) * 100).toFixed(1)}%`
})

/** 环比：近 30 天 vs 前 30 天（放在累计筛查卡的脚注，避免单独占一张图） */
const totalFoot = computed(() => {
  const r = stats.value?.growthRate
  if (r == null) return '全部筛查记录'
  const pct = (Number(r) * 100).toFixed(1)
  const arrow = Number(r) > 0 ? '↑' : Number(r) < 0 ? '↓' : '→'
  return `近 30 天 ${stats.value?.recentTotal ?? 0} 例 ${arrow} 环比 ${Math.abs(Number(pct))}%`
})

/** 待办摘要：三类数量 + 直达入口（详细列表在「随访待办」页） */
const todoItems = computed(() => [
  {
    key: 'review',
    title: '待人工复核',
    desc: '模型不确定性达阈值，需核对影像',
    count: pendingReview.value,
    icon: 'clock',
    tone: 'warn',
    path: '/screening/records?needReview=true'
  },
  {
    key: 'referral',
    title: '需转诊',
    desc: '分级为重度及以上，建议尽快转诊',
    count: pendingReferral.value,
    icon: 'hospital',
    tone: 'danger',
    path: '/screening/todos'
  },
  {
    key: 'overdue',
    title: '逾期未复诊',
    desc: `超过 ${OVERDUE_DAYS} 天未复查且中度以上`,
    count: overduePatients.value.length,
    icon: 'refreshClock',
    tone: 'brand',
    path: '/screening/todos'
  }
])

const quickActions = computed(() =>
  [
    {
      path: '/screening/upload',
      title: '上传眼底图筛查',
      desc: '批量上传并自动分级',
      icon: 'upload',
      permission: 'biz:screening:create'
    },
    {
      path: '/screening/records',
      title: '查看筛查记录',
      desc: '检索、查看详情与导出',
      icon: 'list',
      permission: 'biz:screening:view'
    },
    {
      path: '/screening/patients',
      title: '患者随访',
      desc: '纵向对比分级变化',
      icon: 'activity',
      permission: 'biz:screening:view'
    },
    {
      path: '/screening/statistics',
      title: '统计分析报表',
      desc: '分级构成与转诊趋势',
      icon: 'chart',
      permission: 'biz:screening:view'
    }
  ].filter((a) => !a.permission || userStore.permissions.includes(a.permission))
)

function daysSince(time?: string): number {
  if (!time) return 0
  const t = new Date(time.replace(' ', 'T')).getTime()
  if (Number.isNaN(t)) return 0
  return Math.max(0, Math.floor((Date.now() - t) / 86400000))
}

function levelIndex(level?: string): number {
  if (!level || !level.startsWith('LEVEL_')) return -1
  const n = Number(level.slice(6))
  return Number.isFinite(n) ? n : -1
}

function go(path: string) {
  router.push(path)
}

/** 跳转筛查记录并预置「待复核」筛选 */
function goReviewList() {
  router.push({ path: '/screening/records', query: { needReview: 'true' } })
}

async function load() {
  loading.value = true
  try {
    // 待办数量只需 total，pageSize 取 1 以减少传输
    const [s, review, l3, l4, patients] = await Promise.all([
      statisticsScreening({}),
      pageScreening({ needReview: true, current: 1, pageSize: 1 }),
      pageScreening({ level: 'LEVEL_3', current: 1, pageSize: 1 }),
      pageScreening({ level: 'LEVEL_4', current: 1, pageSize: 1 }),
      pagePatients({ current: 1, pageSize: 200 })
    ])
    stats.value = s
    pendingReview.value = review.total
    pendingReferral.value = l3.total + l4.total
    overduePatients.value = patients.list.filter(
      (p) => levelIndex(p.latestLevel) >= 2 && daysSince(p.latestTime) > OVERDUE_DAYS
    )
    loadedVersion.value = screeningStore.dataVersion
  } catch {
    // 静默：看板数据不可用时不影响导航
  } finally {
    loading.value = false
  }
}

onMounted(load)

// 完成新的筛查后切回工作台时刷新指标
onActivated(() => {
  if (loadedVersion.value !== screeningStore.dataVersion) load()
})
</script>

<style scoped>
.kpi-row {
  margin-bottom: 0;
}

.mt {
  margin-top: var(--drs-gap);
}

.btn-ico {
  margin-right: 5px;
}

/* ---------- 待办摘要 ---------- */
.todo-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 11px 12px;
  margin-bottom: 8px;
  border: 1px solid var(--drs-border);
  border-radius: var(--drs-radius-sm);
  background: var(--drs-surface);
  cursor: pointer;
  text-align: left;
  font: inherit;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.todo-row:last-of-type {
  margin-bottom: 0;
}

.todo-row:hover {
  border-color: var(--drs-primary-200);
  background: var(--drs-primary-50);
}

.todo-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 9px;
}

.ti-warn {
  background: var(--drs-warn-bg);
  color: var(--drs-warn);
}

.ti-danger {
  background: var(--drs-danger-bg);
  color: var(--drs-danger);
}

.ti-brand {
  background: var(--drs-primary-50);
  color: var(--drs-primary-700);
}

.todo-text {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.todo-title {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--drs-ink-800);
}

.todo-desc {
  font-size: 12px;
  color: var(--drs-ink-500);
}

.todo-count {
  flex-shrink: 0;
  min-width: 30px;
  text-align: right;
  font-size: 17px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.tc-warn {
  color: var(--drs-warn);
}

.tc-danger {
  color: var(--drs-danger);
}

.tc-brand {
  color: var(--drs-primary-700);
}

/* ---------- 快捷操作 ---------- */
.quick-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 11px 12px;
  margin-bottom: 8px;
  border: 1px solid var(--drs-border);
  border-radius: var(--drs-radius-sm);
  background: var(--drs-surface);
  cursor: pointer;
  text-align: left;
  font: inherit;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.quick-item:last-of-type {
  margin-bottom: 0;
}

.quick-item:hover {
  border-color: var(--drs-primary-200);
  background: var(--drs-primary-50);
}

.quick-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 9px;
  background: var(--drs-primary-50);
  color: var(--drs-primary-700);
}

.quick-text {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.quick-title {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--drs-ink-800);
}

.quick-desc {
  font-size: 12px;
  color: var(--drs-ink-500);
}

.quick-arrow {
  color: var(--drs-ink-400);
  flex-shrink: 0;
}

.dash-tip {
  margin-top: 12px;
}
</style>
