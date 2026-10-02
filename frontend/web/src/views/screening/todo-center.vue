<template>
  <div class="drs-page">
    <PageHeader title="随访待办" :crumbs="['筛查业务', '随访待办']">
      <template #actions>
        <el-button :loading="loading" @click="loadAll">
          <AppIcon name="refresh" :size="15" class="btn-ico" />刷新
        </el-button>
      </template>
    </PageHeader>

    <section class="drs-card">
      <div class="drs-card-head">
        <el-radio-group v-model="activeTab" :disabled="loading">
          <el-radio-button value="all">全部（{{ totalCount }}）</el-radio-button>
          <el-radio-button value="review">待人工复核（{{ reviewRows.length }}）</el-radio-button>
          <el-radio-button value="referral">需转诊（{{ referralRows.length }}）</el-radio-button>
          <el-radio-button value="overdue">逾期未复诊（{{ overdueRows.length }}）</el-radio-button>
        </el-radio-group>
        <span class="drs-card-meta">{{ tabHint }}</span>
      </div>

      <div class="drs-card-body">
        <div v-if="!currentRows.length" class="empty-row">{{ emptyText }}</div>

        <ul v-else class="todo-list">
          <li v-for="row in currentRows" :key="row.key" class="todo-item">
            <span class="ti-main">
              <span class="ti-name">
                {{ row.name }}
                <span v-if="activeTab === 'all'" class="ti-type" :class="`tt-${row.type}`">
                  {{ TYPE_LABEL[row.type] }}
                </span>
              </span>
              <span class="ti-sub">{{ row.desc }}</span>
            </span>
            <span class="ti-ops">
              <template v-if="row.type === 'overdue'">
                <el-button link type="primary" @click="go('/screening/patients')">随访时间线</el-button>
              </template>
              <template v-else>
                <el-button link type="primary" @click="openReport(row.recordId!)">报告</el-button>
                <el-button v-if="row.type === 'review'" link type="warning" @click="goReview()">
                  去复核
                </el-button>
              </template>
            </span>
          </li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import { pagePatients, pageScreening } from '@/api/screening'
import { useScreeningStore } from '@/stores/screening'
import type { PatientFollowUpVO, ScreeningRecordVO } from '@/types/screening'

/** 逾期阈值（天）：超过该天数未复查且分级 >= LEVEL_2 视为逾期 */
const OVERDUE_DAYS = 90
const PAGE_SIZE = 20

type TodoType = 'review' | 'referral' | 'overdue'
type TabKey = 'all' | TodoType

interface TodoRow {
  key: string
  type: TodoType
  name: string
  desc: string
  /** review / referral 指向筛查记录 */
  recordId?: string
}

const TYPE_LABEL: Record<TodoType, string> = {
  review: '待人工复核',
  referral: '需转诊',
  overdue: '逾期未复诊'
}

const TAB_HINT: Record<TabKey, string> = {
  all: '按类型汇总，可切换到单一类型查看',
  review: '模型不确定性达阈值，建议核对影像后确认复核',
  referral: '分级为重度及以上，建议尽快转诊上级医院',
  overdue: `最近一次筛查距今超过 ${OVERDUE_DAYS} 天，且分级为中度及以上`
}

const router = useRouter()
const screeningStore = useScreeningStore()
const loading = ref(false)
const activeTab = ref<TabKey>('all')

const reviewRows = ref<TodoRow[]>([])
const referralRows = ref<TodoRow[]>([])
const overdueRows = ref<TodoRow[]>([])

/** 上次加载时的数据版本，用于 KeepAlive 恢复后判断是否需要刷新 */
const loadedVersion = ref(-1)

const totalCount = computed(
  () => reviewRows.value.length + referralRows.value.length + overdueRows.value.length
)

const tabHint = computed(() => TAB_HINT[activeTab.value])

const emptyText = computed(() => {
  if (activeTab.value === 'all') return '暂无待办事项'
  return `暂无${TYPE_LABEL[activeTab.value as TodoType]}记录`
})

/** 「全部」模式按类型聚合；单类型模式直接取对应列表 */
const currentRows = computed<TodoRow[]>(() => {
  if (activeTab.value === 'all') {
    return [...reviewRows.value, ...referralRows.value, ...overdueRows.value]
  }
  if (activeTab.value === 'review') return reviewRows.value
  if (activeTab.value === 'referral') return referralRows.value
  return overdueRows.value
})

/** 不确定性（归一化预测熵）格式化 */
function unc(u?: number) {
  return u == null ? '—' : u.toFixed(3)
}

function daysSince(time?: string): number {
  if (!time) return 0
  const t = new Date(time.replace(' ', 'T')).getTime()
  if (Number.isNaN(t)) return 0
  return Math.max(0, Math.floor((Date.now() - t) / 86400000))
}

/** 中度及以上（LEVEL_2..LEVEL_4）视为需要持续随访 */
function levelIndex(level?: string): number {
  if (!level || !level.startsWith('LEVEL_')) return -1
  const n = Number(level.slice(6))
  return Number.isFinite(n) ? n : -1
}

function toReviewRow(r: ScreeningRecordVO): TodoRow {
  return {
    key: `review-${r.id}`,
    type: 'review',
    name: r.patientName || '未登记患者',
    desc: `${r.levelName || '—'} · 不确定性 ${unc(r.uncertainty)} · ${r.createTime || '—'}`,
    recordId: r.id
  }
}

function toReferralRow(r: ScreeningRecordVO): TodoRow {
  return {
    key: `referral-${r.id}`,
    type: 'referral',
    name: r.patientName || '未登记患者',
    desc: `${r.levelName || '—'} · ${r.suggestionName || '—'} · ${r.createTime || '—'}`,
    recordId: r.id
  }
}

function toOverdueRow(p: PatientFollowUpVO): TodoRow {
  return {
    key: `overdue-${p.patientName}`,
    type: 'overdue',
    name: p.patientName,
    desc: `最近 ${p.latestLevelName || '—'} · ${p.latestTime || '—'} · 已 ${daysSince(p.latestTime)} 天未复查`
  }
}

async function loadAll() {
  loading.value = true
  try {
    const [review, l3, l4, patients] = await Promise.all([
      pageScreening({ needReview: true, current: 1, pageSize: PAGE_SIZE }),
      pageScreening({ level: 'LEVEL_3', current: 1, pageSize: PAGE_SIZE }),
      pageScreening({ level: 'LEVEL_4', current: 1, pageSize: PAGE_SIZE }),
      pagePatients({ current: 1, pageSize: 200 })
    ])

    reviewRows.value = review.list.map(toReviewRow)
    referralRows.value = [...l4.list, ...l3.list].map(toReferralRow)
    overdueRows.value = patients.list
      .filter((p) => levelIndex(p.latestLevel) >= 2 && daysSince(p.latestTime) > OVERDUE_DAYS)
      .sort((a, b) => daysSince(b.latestTime) - daysSince(a.latestTime))
      .slice(0, PAGE_SIZE)
      .map(toOverdueRow)

    loadedVersion.value = screeningStore.dataVersion
  } catch (e) {
    ElMessage.error((e as Error).message || '待办加载失败')
  } finally {
    loading.value = false
  }
}

function go(path: string) {
  router.push(path)
}

function goReview() {
  router.push('/screening/records?needReview=true')
}

function openReport(id: string) {
  // 页内跳转，不新开浏览器标签页
  router.push({ path: `/screening/records/${id}/report` })
}

onMounted(loadAll)

// 数据变更后切回本页时刷新，避免展示过期的待办
onActivated(() => {
  if (loadedVersion.value !== screeningStore.dataVersion) loadAll()
})
</script>

<style scoped>
.btn-ico {
  margin-right: 5px;
}

.empty-row {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--drs-ink-500);
}

.todo-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px dashed var(--drs-border);
}

.todo-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.ti-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.5;
}

.ti-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--drs-ink-800);
}

/* 「全部」模式下用于区分待办类型 */
.ti-type {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 500;
}

.tt-review {
  background: var(--drs-warn-bg);
  color: var(--drs-warn);
}

.tt-referral {
  background: var(--drs-danger-bg);
  color: var(--drs-danger);
}

.tt-overdue {
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
}

.ti-sub {
  font-size: 12px;
  color: var(--drs-ink-500);
}

.ti-ops {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .drs-card-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .todo-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
