<template>
  <div class="drs-page">
    <h1 class="drs-page-title">统计分析</h1>
    <p class="drs-page-subtitle">分级分布、转诊建议分布与近 30 天筛查趋势</p>

    <div class="metric-grid">
      <div class="metric drs-card" v-for="m in metrics" :key="m.label">
        <div class="metric-value" :style="{ color: m.color }">{{ m.value }}</div>
        <div class="metric-label">{{ m.label }}</div>
      </div>
    </div>

    <div v-loading="loading" class="chart-grid">
      <div class="drs-card chart-card">
        <div class="panel-title">DR 分级分布</div>
        <EChart v-if="stats" :option="levelOption" />
        <el-empty v-else description="暂无数据" />
      </div>
      <div class="drs-card chart-card">
        <div class="panel-title">转诊建议分布</div>
        <EChart v-if="stats" :option="suggestionOption" />
        <el-empty v-else description="暂无数据" />
      </div>
      <div class="drs-card chart-card chart-wide">
        <div class="panel-title">近 30 天筛查趋势</div>
        <EChart v-if="stats" :option="trendOption" />
        <el-empty v-else description="暂无数据" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import EChart from '@/components/EChart.vue'
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

const metrics = computed(() => [
  { label: '累计筛查', value: total.value, color: '#0891B2' },
  { label: '转诊率', value: referralRate.value, color: '#EF4444' },
  { label: '需转诊', value: referralCount.value, color: '#F59E0B' },
  { label: '建议就诊', value: clinicCount.value, color: '#22C55E' }
])

function toPieData(dist: Record<string, number> | undefined, labelMap: Record<string, string>, colorMap: Record<string, string>) {
  if (!dist) return { data: [], colors: [] as string[] }
  const data = Object.entries(dist).map(([k, v]) => ({ name: labelMap[k] || k, value: v }))
  const colors = Object.keys(dist).map((k) => colorMap[k] || '#0891B2')
  return { data, colors }
}

const levelOption = computed<EChartsOption>(() => {
  const { data, colors } = toPieData(stats.value?.levelDistribution, LEVEL_LABEL, LEVEL_COLOR)
  return {
    color: colors,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c}' },
        data
      }
    ]
  }
})

const suggestionOption = computed<EChartsOption>(() => {
  const { data, colors } = toPieData(stats.value?.suggestionDistribution, SUGGESTION_LABEL, SUGGESTION_COLOR)
  return {
    color: colors,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '45%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c}' },
        data
      }
    ]
  }
})

const trendOption = computed<EChartsOption>(() => {
  const trend = stats.value?.trend || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 50 },
    xAxis: {
      type: 'category',
      data: trend.map((t) => t.date.slice(5)),
      axisLabel: { rotate: 45, fontSize: 10 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: trend.map((t) => t.count),
        lineStyle: { color: '#0891B2', width: 3 },
        itemStyle: { color: '#0891B2' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(8,145,178,0.28)' },
              { offset: 1, color: 'rgba(8,145,178,0.02)' }
            ]
          }
        }
      }
    ]
  }
})

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await statisticsScreening({})
  } catch {
    // 静默
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.metric {
  padding: 18px;
  text-align: center;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
}

.metric-label {
  font-size: 13px;
  color: var(--drs-text-soft);
  margin-top: 4px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card {
  padding: 16px 18px;
}

.chart-wide {
  grid-column: 1 / -1;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
