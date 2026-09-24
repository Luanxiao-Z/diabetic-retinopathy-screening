<template>
  <div class="drs-page">
    <PageHeader
      title="统计分析"
      subtitle="分级构成、转诊建议分布与近 30 天筛查趋势"
      :crumbs="['筛查业务', '统计分析']"
    >
      <template #actions>
        <el-button :loading="loading" @click="load">
          <AppIcon name="refresh" :size="15" class="btn-ico" />刷新
        </el-button>
      </template>
    </PageHeader>

    <div class="drs-grid-4 kpi-row">
      <StatCard icon="activity" label="累计筛查" :value="total" unit="例" tone="brand" />
      <StatCard icon="alert" label="转诊率" :value="referralRate" tone="danger" />
      <StatCard icon="hospital" label="需转诊" :value="referralCount" unit="例" tone="warn" />
      <StatCard icon="stethoscope" label="建议就诊" :value="clinicCount" unit="例" tone="info" />
    </div>

    <div v-loading="loading" class="chart-grid">
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>DR 分级分布</h3>
          <span class="drs-card-meta">共 {{ total }} 例</span>
        </div>
        <div class="drs-card-body">
          <EChart v-if="hasLevel" :option="levelOption" height="280px" />
          <el-empty v-else :image-size="80" description="暂无分级数据" />
        </div>
      </section>

      <section class="drs-card">
        <div class="drs-card-head">
          <h3>转诊建议分布</h3>
          <span class="drs-card-meta">按建议类型统计</span>
        </div>
        <div class="drs-card-body">
          <EChart v-if="hasSuggestion" :option="suggestionOption" height="280px" />
          <el-empty v-else :image-size="80" description="暂无转诊建议数据" />
        </div>
      </section>

      <section class="drs-card chart-wide">
        <div class="drs-card-head">
          <h3>近 30 天筛查趋势</h3>
          <span class="drs-card-meta">单位：例</span>
        </div>
        <div class="drs-card-body">
          <EChart v-if="hasTrend" :option="trendOption" height="300px" />
          <el-empty v-else :image-size="80" description="暂无趋势数据" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import AppIcon from '@/components/AppIcon.vue'
import EChart from '@/components/EChart.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { statisticsScreening } from '@/api/screening'
import { LEVEL_COLOR, LEVEL_LABEL, SUGGESTION_COLOR, SUGGESTION_LABEL } from '@/types/screening'
import type { ScreeningStatisticsVO } from '@/types/screening'

const loading = ref(false)
const stats = ref<ScreeningStatisticsVO | null>(null)

const total = computed(() => stats.value?.total || 0)
const referralRate = computed(() => {
  const r = stats.value?.referralRate
  return r == null ? '0%' : `${(Number(r) * 100).toFixed(1)}%`
})
const referralCount = computed(() => stats.value?.suggestionDistribution?.['REFERRAL'] || 0)
const clinicCount = computed(() => stats.value?.suggestionDistribution?.['CLINIC'] || 0)

const hasLevel = computed(() => Object.keys(stats.value?.levelDistribution || {}).length > 0)
const hasSuggestion = computed(() => Object.keys(stats.value?.suggestionDistribution || {}).length > 0)
const hasTrend = computed(() => (stats.value?.trend || []).some((t) => t.count > 0))

const AXIS_LABEL = { color: '#64748b', fontSize: 11 }

function toPieData(
  dist: Record<string, number> | undefined,
  labelMap: Record<string, string>,
  colorMap: Record<string, string>
) {
  if (!dist) return { data: [], colors: [] as string[] }
  const entries = Object.entries(dist)
  return {
    data: entries.map(([k, v]) => ({ name: labelMap[k] || k, value: v })),
    colors: entries.map(([k]) => colorMap[k] || '#0891b2')
  }
}

const levelOption = computed<EChartsOption>(() => {
  const { data, colors } = toPieData(stats.value?.levelDistribution, LEVEL_LABEL, LEVEL_COLOR)
  return {
    color: colors,
    tooltip: { trigger: 'item', formatter: '{b}: {c} 例（{d}%）' },
    legend: { bottom: 0, type: 'scroll', textStyle: { color: '#475569', fontSize: 12 } },
    series: [
      {
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { color: '#334155', fontSize: 12, formatter: '{b}\n{c}' },
        labelLine: { length: 8, length2: 8 },
        data
      }
    ]
  }
})

const suggestionOption = computed<EChartsOption>(() => {
  const { data, colors } = toPieData(
    stats.value?.suggestionDistribution,
    SUGGESTION_LABEL,
    SUGGESTION_COLOR
  )
  return {
    color: colors,
    tooltip: { trigger: 'item', formatter: '{b}: {c} 例（{d}%）' },
    legend: { bottom: 0, type: 'scroll', textStyle: { color: '#475569', fontSize: 12 } },
    series: [
      {
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { color: '#334155', fontSize: 12, formatter: '{b}\n{c}' },
        labelLine: { length: 8, length2: 8 },
        data
      }
    ]
  }
})

const trendOption = computed<EChartsOption>(() => {
  const trend = stats.value?.trend || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 20, top: 20, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.map((t) => t.date.slice(5)),
      axisLine: { lineStyle: { color: '#e4e8ee' } },
      axisTick: { show: false },
      axisLabel: { ...AXIS_LABEL, interval: 2 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: AXIS_LABEL
    },
    series: [
      {
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: trend.map((t) => t.count),
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
  }
})

async function load() {
  loading.value = true
  try {
    stats.value = await statisticsScreening({})
  } catch {
    // 静默：图表区已有空态兜底
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.btn-ico {
  margin-right: 5px;
}

.kpi-row {
  margin-bottom: var(--drs-gap);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--drs-gap);
}

.chart-wide {
  grid-column: 1 / -1;
}

@media (max-width: 1024px) {
  .chart-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
