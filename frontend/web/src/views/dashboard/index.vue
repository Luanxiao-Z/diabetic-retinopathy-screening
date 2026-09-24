<template>
  <div class="drs-page">
    <PageHeader
      title="筛查看板"
      subtitle="糖尿病视网膜病变（DR）智能筛查总览 · 数据实时同步"
      :crumbs="['综合看板', '筛查看板']"
    >
      <template #actions>
        <el-button :loading="loading" @click="load">
          <AppIcon name="refresh" :size="15" class="btn-ico" />刷新
        </el-button>
        <el-button v-permission="'biz:screening:create'" type="primary" @click="go('/screening/upload')">
          <AppIcon name="upload" :size="15" class="btn-ico" />开始筛查
        </el-button>
      </template>
    </PageHeader>

    <!-- ============ 指标卡 ============ -->
    <div class="drs-grid-4 kpi-row">
      <StatCard
        icon="activity"
        label="累计筛查"
        :value="total"
        unit="例"
        tone="brand"
        clickable
        foot="全部筛查记录"
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
        foot="置信度低于阈值"
        aria-label="待人工复核数量，点击进入筛查记录并筛选待复核"
        @click="goReviewList()"
      />
    </div>

    <!-- ============ 趋势（整行） ============ -->
    <section class="drs-card mt">
      <div class="drs-card-head">
        <h3>近 30 天筛查趋势</h3>
        <span class="drs-card-meta">
          近 30 天 {{ recentTotal }} 例 · 前 30 天 {{ prevTotal }} 例 ·
          <b :class="growthClass">环比 {{ growthText }}</b>
        </span>
      </div>
      <div class="drs-card-body">
        <EChart v-if="hasTrend" :option="trendOption" height="240px" />
        <el-empty v-else :image-size="72" description="暂无趋势数据，完成筛查后自动生成" />
      </div>
    </section>

    <!-- ============ 分级分布 / 快捷操作 ============ -->
    <div class="drs-grid-2 mt">
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>DR 分级分布</h3>
          <span class="drs-card-meta">共 {{ total }} 例</span>
        </div>
        <div class="drs-card-body">
          <div v-for="lv in levelOrder" :key="lv" class="dist-row">
            <span class="dist-name">
              <i class="dist-dot" :style="{ background: levelColor[lv] }" aria-hidden="true"></i>
              {{ levelLabel[lv] }}
            </span>
            <span class="dist-bar">
              <span
                class="dist-fill"
                :style="{ width: distPercent(lv) + '%', background: levelColor[lv] }"
              ></span>
            </span>
            <span class="dist-val">{{ levelDist[lv] || 0 }}</span>
            <span class="dist-pct">{{ distPercent(lv) }}%</span>
          </div>
          <p v-if="!total" class="panel-empty">暂无筛查数据</p>
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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import AppIcon from '@/components/AppIcon.vue'
import EChart from '@/components/EChart.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { statisticsScreening } from '@/api/screening'
import { useUserStore } from '@/stores/user'
import { LEVEL_COLOR, LEVEL_LABEL, LEVEL_ORDER } from '@/types/screening'
import type { ScreeningStatisticsVO } from '@/types/screening'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const stats = ref<ScreeningStatisticsVO | null>(null)

const levelOrder = LEVEL_ORDER
const levelLabel = LEVEL_LABEL
const levelColor = LEVEL_COLOR

const hasCreate = computed(() => userStore.permissions.includes('biz:screening:create'))
const roleText = computed(() => (userStore.role === 'ADMIN' ? '管理员视角 · 全量数据' : '医生视角 · 本人数据'))

const levelDist = computed(() => stats.value?.levelDistribution || {})
const referralCount = computed(() => stats.value?.suggestionDistribution?.['REFERRAL'] || 0)
const needReviewCount = computed(() => stats.value?.needReviewCount || 0)
const total = computed(() => stats.value?.total || 0)

const referralRate = computed(() => {
  const r = stats.value?.referralRate
  return r == null ? '0%' : `${(Number(r) * 100).toFixed(1)}%`
})

const trend = computed(() => stats.value?.trend || [])
const hasTrend = computed(() => trend.value.some((t) => t.count > 0))

/* 环比：近 30 天 vs 前 30 天 */
const recentTotal = computed(() => stats.value?.recentTotal ?? 0)
const prevTotal = computed(() => stats.value?.prevTotal ?? 0)
const growthRate = computed(() => stats.value?.growthRate)
const growthText = computed(() => {
  const r = growthRate.value
  if (r == null) return '—'
  const pct = (Number(r) * 100).toFixed(1)
  return Number(r) > 0 ? `+${pct}%` : `${pct}%`
})
const growthClass = computed(() => {
  const r = growthRate.value
  if (r == null || Number(r) === 0) return 'growth-flat'
  return Number(r) > 0 ? 'growth-up' : 'growth-down'
})

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

const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((t) => t.date.slice(5)),
    axisLine: { lineStyle: { color: '#e4e8ee' } },
    axisLabel: { color: '#64748b', fontSize: 11, interval: 4 },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#f1f5f9' } },
    axisLabel: { color: '#64748b', fontSize: 11 }
  },
  series: [
    {
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: trend.value.map((t) => t.count),
      lineStyle: { color: '#0891b2', width: 2.5 },
      itemStyle: { color: '#0891b2' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(8,145,178,0.24)' },
            { offset: 1, color: 'rgba(8,145,178,0.02)' }
          ]
        }
      }
    }
  ]
}))

function distPercent(lv: string): number {
  const t = total.value
  if (!t) return 0
  return Math.round(((levelDist.value[lv] || 0) / t) * 100)
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
    stats.value = await statisticsScreening({})
  } catch {
    // 静默：看板数据不可用时不影响导航
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.kpi-row {
  margin-bottom: var(--drs-gap);
}

.mt {
  margin-top: var(--drs-gap);
}

.btn-ico {
  margin-right: 5px;
}

/* 环比涨跌配色：上升用青（业务量增长为正向），下降用中性灰，避免与医疗告警色混淆 */
.growth-up {
  color: var(--drs-primary-700);
  font-weight: 600;
}

.growth-down {
  color: var(--drs-ink-600);
  font-weight: 600;
}

.growth-flat {
  color: var(--drs-ink-500);
}

/* ---------- 分级分布 ---------- */
.dist-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 0;
  font-size: 13px;
}

.dist-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 104px;
  flex-shrink: 0;
  color: var(--drs-ink-600);
}

.dist-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dist-bar {
  flex: 1;
  height: 8px;
  background: var(--drs-ink-100);
  border-radius: 4px;
  overflow: hidden;
  min-width: 60px;
}

.dist-fill {
  display: block;
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s ease;
}

.dist-val {
  width: 34px;
  text-align: right;
  font-variant-numeric: tabular-nums;
  color: var(--drs-ink-800);
}

.dist-pct {
  width: 42px;
  text-align: right;
  font-size: 12px;
  color: var(--drs-ink-500);
  font-variant-numeric: tabular-nums;
}

.panel-empty {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--drs-ink-500);
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
