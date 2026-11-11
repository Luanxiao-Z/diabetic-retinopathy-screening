<template>
  <div class="drs-page drs-stagger">
    <PageHeader title="工作台" style="--i: 0">
      <template #actions>
        <el-button v-permission="'biz:screening:create'" type="primary" @click="go('/screening/upload')">
          <AppIcon name="upload" :size="15" class="btn-ico" />开始筛查
        </el-button>
      </template>
    </PageHeader>

    <!-- ============ 关键指标（概览） ============ -->
    <div class="drs-grid-4 kpi-row" style="--i: 1">
      <StatCard
        icon="activity"
        label="累计筛查"
        :value="total"
        unit="例"
        tone="brand"
        animated
        clickable
        :foot="totalFoot"
        aria-label="累计筛查记录数，点击进入筛查记录列表"
        @click="go('/screening/records')"
      />
      <StatCard
        icon="alert"
        label="转诊率"
        :value="referralRateNum"
        unit="%"
        :decimals="1"
        tone="danger"
        animated
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
        animated
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
        animated
        clickable
        foot="模型不确定性达阈值"
        aria-label="待人工复核数量，点击进入筛查记录并筛选待复核"
        @click="goReviewList()"
      />
    </div>

    <!-- ============ 待办摘要 / 近 14 天趋势 ============ -->
    <div class="dash-row mt" style="--i: 2">
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>待办事项</h3>
          <span class="drs-card-meta">点击查看对应列表</span>
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
          <h3>近 14 天筛查数量</h3>
          <span class="drs-card-meta">单位：例</span>
        </div>
        <div class="drs-card-body trend-body">
          <template v-if="trendBars.length > 1">
            <svg class="trend" viewBox="0 0 460 150" preserveAspectRatio="none" aria-hidden="true">
              <!-- 纵轴：0 / 中值 / 最大值三档刻度 -->
              <line
                class="trend-axis-line"
                :x1="TREND_AXIS_W"
                y1="10"
                :x2="TREND_AXIS_W"
                :y2="TREND_H - 10"
              />
              <text
                v-for="t in trendTicks"
                :key="`t${t.v}`"
                class="trend-tick"
                :x="TREND_AXIS_W - 6"
                :y="t.y + 3"
                text-anchor="end"
              >
                {{ t.v }}
              </text>
              <line
                v-for="t in trendTicks.filter((x) => x.v > 0)"
                :key="`g${t.v}`"
                class="trend-grid"
                :x1="TREND_AXIS_W"
                :y1="t.y"
                :x2="TREND_W"
                :y2="t.y"
              />
              <rect
                v-for="(b, i) in trendBars"
                :key="b.key"
                class="trend-bar"
                :class="{ 'is-zero': b.count === 0 }"
                :x="b.x"
                :y="b.y"
                :width="b.w"
                :height="b.h"
                :style="{ '--d': `${(i * 40) / 1000}s` }"
              />
            </svg>
            <div class="trend-axis">
              <span>{{ trendLabels[0] }}</span>
              <span>{{ trendLabels[1] }}</span>
              <span>{{ trendLabels[2] }}</span>
            </div>
          </template>
          <el-empty v-else :image-size="64" description="暂无趋势数据" />
        </div>
      </section>
    </div>

    <!-- ============ 快捷入口 / 模型状态 ============ -->
    <div class="dash-row mt" style="--i: 3">
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

      <section class="drs-card model-card">
        <div class="drs-card-head">
          <h3>模型状态</h3>
          <span class="drs-card-meta">{{ model?.backbone || 'MobileNetV3-S' }}</span>
        </div>
        <div class="drs-card-body model-body">
          <RetinaMark :size="132" />
          <div class="model-meta">
            <span class="model-status" :class="modelOnline ? 'is-online' : 'is-offline'">
              <i class="model-dot" aria-hidden="true"></i>
              {{ modelOnline ? '服务在线' : '暂不可用' }}
            </span>
            <b class="model-version">{{ model?.version || '—' }}</b>
            <p class="model-line">
              {{ model?.device || '未上报' }} · {{ model?.num_classes || 5 }} 分类 ·
              {{ model?.img_size || 224 }}px
            </p>
            <div class="model-metrics">
              <div>
                <b>{{ accText }}</b>
                <span>准确率</span>
              </div>
              <div>
                <b>{{ f1Text }}</b>
                <span>宏 F1</span>
              </div>
            </div>
          </div>
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
import { usePageRefresh } from '@/composables/usePageRefresh'
import StatCard from '@/components/StatCard.vue'
import RetinaMark from '@/components/decor/RetinaMark.vue'
import { modelInfo, pagePatients, pageScreening, statisticsScreening } from '@/api/screening'
import { useScreeningStore } from '@/stores/screening'
import { useUserStore } from '@/stores/user'
import type { ModelInfoVO, PatientFollowUpVO, ScreeningStatisticsVO } from '@/types/screening'

/** 与「随访待办」页保持一致的逾期阈值（天） */
const OVERDUE_DAYS = 90

const router = useRouter()
const userStore = useUserStore()
const screeningStore = useScreeningStore()

const loading = ref(false)
const stats = ref<ScreeningStatisticsVO | null>(null)
/** 模型信息单独维护：接口失败只影响本卡，不影响整个看板 */
const model = ref<ModelInfoVO | null>(null)
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

/** 转诊率：0~1 转百分比数值，供数字滚动使用；无数据时为空串（显示「—」） */
const referralRateNum = computed(() => {
  const r = stats.value?.referralRate
  return r == null ? '' : Number(r) * 100
})

/* ---------- 近 14 天趋势（静态 SVG 折线，不做描边动画） ---------- */
const TREND_W = 460
const TREND_H = 150
/** 左侧纵轴刻度区宽度 */
const TREND_AXIS_W = 34
const TREND_PAD_Y = 10

const trendSeries = computed(() => (stats.value?.trend || []).slice(-14))

/**
 * 柱状而非折线：筛查量在多数日期为 0 或 1，折线会退化成一条平线，
 * 柱状能如实表达「空档日 + 峰值日」，也便于复用条形增长动效。
 */
/** 纵轴上限：只有 0/1 两档时按 2 计，柱高留余量避免整排顶格 */
const trendScaleMax = computed(() => {
  const peak = Math.max(1, ...trendSeries.value.map((d) => d.count))
  return peak <= 1 ? 2 : peak
})

const trendBars = computed(() => {
  const arr = trendSeries.value
  if (arr.length < 2) return []
  const max = trendScaleMax.value
  const plotW = TREND_W - TREND_AXIS_W - 6
  const step = plotW / arr.length
  const w = Math.max(6, Math.min(18, step * 0.56))
  const plotH = TREND_H - TREND_PAD_Y * 2

  return arr.map((d, i) => {
    const h = d.count === 0 ? 2 : Math.max(4, (d.count / max) * plotH)
    return {
      key: d.date || String(i),
      count: d.count,
      x: Number((TREND_AXIS_W + step * i + (step - w) / 2).toFixed(1)),
      y: Number((TREND_H - TREND_PAD_Y - h).toFixed(1)),
      w: Number(w.toFixed(1)),
      h: Number(h.toFixed(1))
    }
  })
})

/** 纵轴刻度：0、中值、上限 */
const trendTicks = computed(() => {
  const max = trendScaleMax.value
  const plotH = TREND_H - TREND_PAD_Y * 2
  return [
    { v: max, y: TREND_PAD_Y },
    { v: Math.round(max / 2), y: TREND_PAD_Y + plotH / 2 },
    { v: 0, y: TREND_H - TREND_PAD_Y }
  ]
})

const trendLabels = computed(() => {
  const arr = trendSeries.value
  if (!arr.length) return ['', '', '']
  const fmt = (s: string) => (s || '').slice(5)
  return [fmt(arr[0].date), fmt(arr[Math.floor(arr.length / 2)].date), fmt(arr[arr.length - 1].date)]
})

/* ---------- 模型状态（仅真实字段，不显示推理耗时） ---------- */
const modelOnline = computed(() => Boolean(model.value?.version))
const accText = computed(() => {
  const v = model.value?.metrics?.test_acc
  return v == null ? '—' : `${(v * 100).toFixed(1)}%`
})
const f1Text = computed(() => {
  const v = model.value?.metrics?.test_macro_f1
  return v == null ? '—' : v.toFixed(3)
})

/** 环比：近 30 天 vs 前 30 天（放在累计筛查卡的脚注，避免单独占一张图） */
const totalFoot = computed(() => {
  const r = stats.value?.growthRate
  if (r == null) return '全部筛查记录'
  const pct = (Number(r) * 100).toFixed(1)
  const arrow = Number(r) > 0 ? '↑' : Number(r) < 0 ? '↓' : '→'
  return `近 30 天 ${stats.value?.recentTotal ?? 0} 例 ${arrow} 环比 ${Math.abs(Number(pct))}%`
})

/** 待办摘要：全部 + 三类数量 + 直达入口（详细列表在「随访待办」页） */
const todoItems = computed(() => [
  {
    key: 'all',
    title: '全部待办',
    desc: '三类待办事项的汇总视图',
    count:
      pendingReview.value + pendingReferral.value + overduePatients.value.length,
    icon: 'inbox',
    tone: 'brand',
    path: '/screening/todos'
  },
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

    // 模型信息失败不阻塞看板（模型服务未启动时后端转发会报错）
    modelInfo()
      .then((m) => (model.value = m))
      .catch(() => (model.value = null))
  } catch {
    // 静默：看板数据不可用时不影响导航
  } finally {
    loading.value = false
  }
}

// 刷新入口统一在多标签栏，页面只负责把 load 注册出去
usePageRefresh(load)

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

/* 两栏：左侧主内容 1.35，右侧概览 1；窄屏自动落为单列 */
.dash-row {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: var(--drs-gap);
}

.btn-ico {
  margin-right: 5px;
}

/* ---------- 近 14 天趋势（静态折线，不做描边动画） ---------- */
.trend-body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.trend {
  width: 100%;
  height: 150px;
  display: block;
}

.trend-grid {
  stroke: var(--drs-ink-100);
  stroke-width: 1;
}

.trend-axis-line {
  stroke: var(--drs-ink-200);
  stroke-width: 1;
}

.trend-tick {
  fill: var(--drs-ink-400);
  font-size: 10px;
}

/* 柱状：从底部生长，逐条 40ms 错峰，与条形分布同一动效语言 */
.trend-bar {
  fill: var(--drs-primary);
  opacity: 0.85;
  transform-box: fill-box;
  transform-origin: bottom;
  transform: scaleY(0);
  animation: drs-grow-y var(--mo-dur-4) var(--mo-ease-out) var(--d, 0s) forwards;
}

.trend-bar.is-zero {
  fill: var(--drs-ink-200);
  opacity: 1;
}

.trend-axis {
  display: flex;
  justify-content: space-between;
  font-size: 10.5px;
  color: var(--drs-ink-400);
}

/* ---------- 模型状态 ---------- */
.model-card {
  display: flex;
  flex-direction: column;
}

.model-body {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  flex: 1;
}

.model-meta {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
  flex: 1;
}

.model-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--drs-ok);
}

.model-status.is-offline {
  color: var(--drs-ink-500);
}

/* 状态点呼吸：仅「在线」时脉动，离线保持静止 */
.model-dot {
  position: relative;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.model-dot::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 1.5px solid currentColor;
  opacity: 0.45;
  animation: drs-breathe-ring 3.4s var(--mo-ease-std) infinite;
}

.is-offline .model-dot::after {
  animation: none;
  opacity: 0;
}

.model-version {
  font-size: 14px;
  font-weight: 600;
  color: var(--drs-ink-900);
}

.model-line {
  margin: 0;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--drs-ink-500);
}

.model-metrics {
  display: flex;
  gap: 20px;
  margin-top: 2px;
  padding-top: 9px;
  border-top: 1px dashed var(--drs-border);
}

.model-metrics div {
  display: flex;
  flex-direction: column;
}

.model-metrics b {
  font-size: 14px;
  font-weight: 600;
  color: var(--drs-ink-800);
  font-variant-numeric: tabular-nums;
}

.model-metrics span {
  font-size: 10.5px;
  color: var(--drs-ink-500);
}

@media (max-width: 1180px) {
  .dash-row {
    grid-template-columns: 1fr;
  }
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
