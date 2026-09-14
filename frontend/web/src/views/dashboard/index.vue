<template>
  <div class="drs-page">
    <h1 class="drs-page-title">筛查看板</h1>
    <p class="drs-page-subtitle">糖尿病视网膜病变（DR）智能筛查总览</p>

    <div class="metric-grid">
      <div class="metric drs-card" v-for="m in metrics" :key="m.label">
        <div class="metric-icon" :style="{ background: m.bg, color: m.color }">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path :d="m.icon" /></svg>
        </div>
        <div class="metric-body">
          <div class="metric-value">{{ m.value }}</div>
          <div class="metric-label">{{ m.label }}</div>
        </div>
      </div>
    </div>

    <div class="dash-cols">
      <div class="drs-card dash-panel">
        <div class="panel-title">分级分布</div>
        <div v-for="lv in levelOrder" :key="lv" class="dist-row">
          <span class="dist-name">{{ levelLabel[lv] }}</span>
          <div class="dist-bar">
            <div
              class="dist-fill"
              :style="{ width: distPercent(lv) + '%', background: levelColor[lv] }"
            ></div>
          </div>
          <span class="dist-val">{{ levelDist[lv] || 0 }}</span>
        </div>
      </div>

      <div class="drs-card dash-panel">
        <div class="panel-title">快捷操作</div>
        <div class="quick-list">
          <div class="quick-item" v-permission="'biz:screening:create'" @click="go('/screening/upload')">
            <span>上传眼底图筛查</span>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M8 5v14l11-7z" /></svg>
          </div>
          <div class="quick-item" v-permission="'biz:screening:view'" @click="go('/screening/records')">
            <span>查看筛查记录</span>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M8 5v14l11-7z" /></svg>
          </div>
          <div class="quick-item" v-permission="'biz:screening:view'" @click="go('/screening/statistics')">
            <span>统计分析报表</span>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M8 5v14l11-7z" /></svg>
          </div>
        </div>
        <el-alert
          v-if="!hasCreate"
          class="dash-tip"
          type="info"
          :closable="false"
          title="当前账号无上传权限，可查看统计与历史记录。"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { statisticsScreening } from '@/api/screening'
import { useUserStore } from '@/stores/user'
import { LEVEL_COLOR, LEVEL_LABEL, LEVEL_ORDER } from '@/types/screening'
import type { ScreeningStatisticsVO } from '@/types/screening'

const router = useRouter()
const userStore = useUserStore()
const hasCreate = computed(() => userStore.permissions.includes('biz:screening:create'))

const stats = ref<ScreeningStatisticsVO | null>(null)
const levelOrder = LEVEL_ORDER
const levelLabel = LEVEL_LABEL
const levelColor = LEVEL_COLOR

const levelDist = computed(() => stats.value?.levelDistribution || {})
const referralCount = computed(() => stats.value?.suggestionDistribution?.['REFERRAL'] || 0)
const reviewCount = computed(() => stats.value?.suggestionDistribution?.['REVIEW'] || 0)

const total = computed(() => stats.value?.total || 0)
const referralRate = computed(() => {
  const r = stats.value?.referralRate
  return r == null ? '0%' : `${(Number(r) * 100).toFixed(1)}%`
})

const metrics = computed(() => [
  {
    label: '累计筛查',
    value: total.value,
    color: '#0891B2',
    bg: '#E6F4F7',
    icon: 'M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z'
  },
  {
    label: '转诊率',
    value: referralRate.value,
    color: '#EF4444',
    bg: '#FEE2E2',
    icon: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z'
  },
  {
    label: '需转诊人数',
    value: referralCount.value,
    color: '#F59E0B',
    bg: '#FEF3C7',
    icon: 'M12 2L1 21h22L12 2zm0 6l6.5 11h-13L12 8z'
  },
  {
    label: '定期复查',
    value: reviewCount.value,
    color: '#22C55E',
    bg: '#DCFCE7',
    icon: 'M9 16.2l-3.5-3.5L4 14.2 9 19l11-11-1.5-1.5z'
  }
])

function distPercent(lv: string): number {
  const t = total.value
  if (!t) return 0
  return Math.round(((levelDist.value[lv] || 0) / t) * 100)
}

function go(path: string) {
  router.push(path)
}

onMounted(async () => {
  try {
    stats.value = await statisticsScreening({})
  } catch {
    // 静默：看板为空不影响导航
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
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
}

.metric-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.metric-label {
  font-size: 13px;
  color: var(--drs-text-soft);
}

.dash-cols {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
}

.dash-panel {
  padding: 18px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 14px;
}

.dist-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 13px;
}

.dist-name {
  width: 56px;
  color: var(--drs-text-soft);
  flex-shrink: 0;
}

.dist-bar {
  flex: 1;
  height: 10px;
  background: #eef2f7;
  border-radius: 5px;
  overflow: hidden;
}

.dist-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.5s ease;
}

.dist-val {
  width: 32px;
  text-align: right;
  color: var(--drs-text);
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border: 1px solid var(--drs-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
  color: var(--drs-text);
}

.quick-item:hover {
  border-color: var(--drs-primary);
  background: var(--drs-bg);
  color: var(--drs-primary);
}

.dash-tip {
  margin-top: 14px;
}
</style>
