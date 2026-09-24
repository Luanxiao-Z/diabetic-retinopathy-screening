<template>
  <div class="drs-page">
    <PageHeader
      title="随访待办"
      subtitle="汇总需要医生处理的筛查事项：待人工复核、需转诊、逾期未复诊"
      :crumbs="['筛查业务', '随访待办']"
    >
      <template #actions>
        <el-button :loading="loading" @click="loadAll">
          <AppIcon name="refresh" :size="15" class="btn-ico" />刷新
        </el-button>
      </template>
    </PageHeader>

    <!-- ============ 概览 ============ -->
    <div class="drs-grid-4 kpi-row">
      <StatCard
        icon="clock"
        label="待人工复核"
        :value="reviewList.length"
        unit="例"
        tone="warn"
        foot="置信度低于阈值"
      />
      <StatCard
        icon="hospital"
        label="需转诊"
        :value="referralList.length"
        unit="例"
        tone="danger"
        foot="重度及以上，待处理"
      />
      <StatCard
        icon="refreshClock"
        label="逾期未复诊"
        :value="overdueList.length"
        unit="人"
        tone="violet"
        foot="超过 90 天未复查"
      />
      <StatCard
        icon="activity"
        label="随访患者总数"
        :value="patientTotal"
        unit="人"
        tone="brand"
        foot="已登记姓名的患者"
      />
    </div>

    <!-- ============ 待人工复核 ============ -->
    <section class="drs-card mt">
      <div class="drs-card-head">
        <h3>待人工复核</h3>
        <span class="drs-card-meta">
          置信度低于阈值，建议核对影像后确认复核
          <el-button link type="primary" @click="go('/screening/records?needReview=true')">查看全部</el-button>
        </span>
      </div>
      <div class="drs-card-body">
        <div v-if="!reviewList.length" class="empty-row">暂无待复核记录</div>
        <ul v-else class="todo-list">
          <li v-for="r in reviewList" :key="r.id" class="todo-item">
            <span class="ti-main">
              <span class="ti-name">{{ r.patientName || '未登记患者' }}</span>
              <span class="ti-sub">{{ r.levelName || '—' }} · 置信度 {{ pct(r.confidence) }} · {{ r.createTime }}</span>
            </span>
            <span class="ti-ops">
              <el-button link type="primary" @click="openReport(r.id)">报告</el-button>
              <el-button link type="warning" @click="go('/screening/records?needReview=true')">去复核</el-button>
            </span>
          </li>
        </ul>
      </div>
    </section>

    <!-- ============ 需转诊 ============ -->
    <section class="drs-card mt">
      <div class="drs-card-head">
        <h3>需转诊（重度及以上）</h3>
        <span class="drs-card-meta">
          建议尽快转诊上级医院
          <el-button link type="primary" @click="go('/screening/records?level=LEVEL_3')">查看全部</el-button>
        </span>
      </div>
      <div class="drs-card-body">
        <div v-if="!referralList.length" class="empty-row">暂无需转诊记录</div>
        <ul v-else class="todo-list">
          <li v-for="r in referralList" :key="r.id" class="todo-item">
            <span class="ti-main">
              <span class="ti-name">{{ r.patientName || '未登记患者' }}</span>
              <span class="ti-sub">{{ r.levelName || '—' }} · {{ r.suggestionName || '—' }} · {{ r.createTime }}</span>
            </span>
            <span class="ti-ops">
              <el-button link type="primary" @click="openReport(r.id)">诊断报告</el-button>
            </span>
          </li>
        </ul>
      </div>
    </section>

    <!-- ============ 逾期未复诊 ============ -->
    <section class="drs-card mt">
      <div class="drs-card-head">
        <h3>逾期未复诊</h3>
        <span class="drs-card-meta">
          最近一次筛查距今超过 {{ OVERDUE_DAYS }} 天，且分级为中度及以上
          <el-button link type="primary" @click="go('/screening/patients')">患者随访</el-button>
        </span>
      </div>
      <div class="drs-card-body">
        <div v-if="!overdueList.length" class="empty-row">暂无逾期患者</div>
        <ul v-else class="todo-list">
          <li v-for="p in overdueList" :key="p.patientName" class="todo-item">
            <span class="ti-main">
              <span class="ti-name">{{ p.patientName }}</span>
              <span class="ti-sub">
                最近 {{ p.latestLevelName || '—' }} · {{ p.latestTime }} · 已 {{ daysSince(p.latestTime) }} 天未复查
              </span>
            </span>
            <span class="ti-ops">
              <el-button link type="primary" @click="go('/screening/patients')">随访时间线</el-button>
            </span>
          </li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { pagePatients, pageScreening } from '@/api/screening'
import type { PatientFollowUpVO, ScreeningRecordVO } from '@/types/screening'

/** 逾期阈值（天）：超过该天数未复查且分级 >= LEVEL_2 视为逾期 */
const OVERDUE_DAYS = 90
const PAGE_SIZE = 20

const router = useRouter()
const loading = ref(false)
const reviewList = ref<ScreeningRecordVO[]>([])
const referralList = ref<ScreeningRecordVO[]>([])
const overdueList = ref<PatientFollowUpVO[]>([])
const patientTotal = ref(0)

function pct(c?: number) {
  return c == null ? '—' : `${(c * 100).toFixed(1)}%`
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

async function loadAll() {
  loading.value = true
  try {
    const [review, l3, l4, patients] = await Promise.all([
      pageScreening({ needReview: true, current: 1, pageSize: PAGE_SIZE }),
      pageScreening({ level: 'LEVEL_3', current: 1, pageSize: PAGE_SIZE }),
      pageScreening({ level: 'LEVEL_4', current: 1, pageSize: PAGE_SIZE }),
      pagePatients({ current: 1, pageSize: 200 })
    ])

    reviewList.value = review.list
    referralList.value = [...l4.list, ...l3.list]
    patientTotal.value = patients.total
    overdueList.value = patients.list
      .filter((p) => levelIndex(p.latestLevel) >= 2 && daysSince(p.latestTime) > OVERDUE_DAYS)
      .sort((a, b) => daysSince(b.latestTime) - daysSince(a.latestTime))
      .slice(0, PAGE_SIZE)
  } catch (e) {
    ElMessage.error((e as Error).message || '待办加载失败')
  } finally {
    loading.value = false
  }
}

function go(path: string) {
  router.push(path)
}

function openReport(id: string) {
  // 页内跳转，不新开浏览器标签页
  router.push({ path: `/screening/records/${id}/report` })
}

onMounted(loadAll)
</script>

<style scoped>
.btn-ico {
  margin-right: 5px;
}

.kpi-row {
  margin-bottom: 0;
}

.mt {
  margin-top: var(--drs-gap);
}

.empty-row {
  padding: 14px 0;
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
  padding: 10px 0;
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
  font-size: 13.5px;
  font-weight: 500;
  color: var(--drs-ink-800);
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
  .todo-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
