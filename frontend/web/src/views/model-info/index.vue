<template>
  <div class="drs-page">
    <PageHeader
      title="模型信息"
      subtitle="筛查所用 AI 模型的来源、推理环境与训练性能指标"
      :crumbs="['综合看板', '模型信息']"
    >
      <template #actions>
        <el-button :loading="loading" @click="load">
          <AppIcon name="refresh" :size="15" class="btn-ico" />刷新
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="error"
      class="mb"
      type="warning"
      :closable="false"
      :title="error"
      description="模型服务（8000 端口）可能未启动；下方展示的是最近一次成功获取的信息或为空。"
    />

    <div class="drs-grid-4 kpi-row">
      <StatCard
        icon="layers"
        label="模型版本"
        :value="info?.version || '—'"
        tone="brand"
        :foot="info?.trained ? '已加载训练权重' : '随机初始化（未训练）'"
      />
      <StatCard
        icon="activity"
        label="推理设备"
        :value="deviceText"
        tone="info"
        :foot="info?.cuda_available ? 'CUDA 可用' : '未检测到 CUDA'"
      />
      <StatCard
        icon="check"
        label="测试集准确率"
        :value="pct(metrics?.test_acc)"
        tone="ok"
        foot="APTOS 2019 测试集"
      />
      <StatCard
        icon="chart"
        label="宏平均 F1"
        :value="num(metrics?.test_macro_f1)"
        tone="violet"
        :foot="`最佳验证宏 F1 ${num(metrics?.best_val_macro_f1)}`"
      />
    </div>

    <div class="grid-2 mt">
      <!-- 模型配置 -->
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>模型配置</h3>
          <span class="drs-card-meta">来自模型服务 /model/info</span>
        </div>
        <div class="drs-card-body">
          <dl class="info-list">
            <div><dt>骨干网络</dt><dd>{{ info?.backbone || '—' }}</dd></div>
            <div><dt>分类数</dt><dd>{{ info?.num_classes ?? '—' }} 类（LEVEL_0..LEVEL_4）</dd></div>
            <div><dt>输入尺寸</dt><dd>{{ info?.img_size ? `${info.img_size} × ${info.img_size}` : '—' }}</dd></div>
            <div><dt>训练轮次</dt><dd>{{ trainEpochs }}</dd></div>
            <div><dt>训练超参</dt><dd>{{ trainArgs }}</dd></div>
            <div class="full"><dt>权重路径</dt><dd class="mono">{{ info?.weights_path || '—' }}</dd></div>
          </dl>
        </div>
      </section>

      <!-- 逐类 F1 -->
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>各分级 F1</h3>
          <span class="drs-card-meta">测试集逐类表现</span>
        </div>
        <div class="drs-card-body">
          <EChart v-if="hasPerClass" :option="f1Option" height="260px" />
          <el-empty v-else :image-size="72" description="暂无逐类指标（需先完成训练）" />
        </div>
      </section>
    </div>

    <!-- 训练曲线 -->
    <section class="drs-card mt">
      <div class="drs-card-head">
        <h3>训练过程</h3>
        <span class="drs-card-meta">逐轮验证集表现（用于判断是否过拟合）</span>
      </div>
      <div class="drs-card-body">
        <EChart v-if="hasHistory" :option="historyOption" height="300px" />
        <el-empty v-else :image-size="80" description="暂无训练历史（需先完成训练）" />
      </div>
    </section>

    <section class="drs-card mt">
      <div class="drs-card-head"><h3>说明</h3></div>
      <div class="drs-card-body">
        <p class="note">
          本页指标由训练脚本导出（<code>model-service/models/train_metrics.json</code>），
          推理服务启动时自动读取。<b>逐类 F1 反映各分级可靠性差异</b>：样本量少的分级
          （如重度 NPDR）指标波动较大，临床使用时应对低置信度结果执行人工复核。
          模型服务不可达时本页会给出告警提示，但不影响已落库的筛查记录。
        </p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import AppIcon from '@/components/AppIcon.vue'
import EChart from '@/components/EChart.vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { modelInfo } from '@/api/screening'
import { LEVEL_LABEL, LEVEL_ORDER } from '@/types/screening'
import type { ModelInfoVO } from '@/types/screening'

const loading = ref(false)
const error = ref('')
const info = ref<ModelInfoVO | null>(null)

const metrics = computed(() => info.value?.metrics || null)
const deviceText = computed(() => {
  const d = info.value?.device || ''
  if (d.startsWith('cuda')) return 'CUDA (GPU)'
  return d || '—'
})

function pct(v?: number) {
  return v == null ? '—' : `${(Number(v) * 100).toFixed(1)}%`
}
function num(v?: number) {
  return v == null ? '—' : Number(v).toFixed(4)
}

const trainEpochs = computed(() => {
  const h = metrics.value?.history
  return h?.length ? `${h.length} 轮` : '—'
})

const trainArgs = computed(() => {
  const a = metrics.value?.args
  if (!a) return '—'
  const parts: string[] = []
  if (a.batch_size != null) parts.push(`batch ${a.batch_size}`)
  if (a.lr != null) parts.push(`lr ${a.lr}`)
  if (a.weight_scheme) parts.push(`加权 ${a.weight_scheme}`)
  if (a.pretrained != null) parts.push(a.pretrained ? 'ImageNet 预训练' : '随机初始化')
  return parts.length ? parts.join(' · ') : '—'
})

const hasPerClass = computed(() => (metrics.value?.test_f1_per_class?.length || 0) > 0)
const hasHistory = computed(() => (metrics.value?.history?.length || 0) > 0)

const f1Option = computed<EChartsOption>(() => {
  const f1 = metrics.value?.test_f1_per_class || []
  return {
    tooltip: { trigger: 'axis', formatter: '{b}<br/>F1: {c}' },
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: LEVEL_ORDER.map((l) => LEVEL_LABEL[l] || l),
      axisLabel: { color: '#64748b', fontSize: 11, interval: 0 },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e4e8ee' } }
    },
    yAxis: {
      type: 'value',
      max: 1,
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#64748b', fontSize: 11 }
    },
    series: [
      {
        type: 'bar',
        barWidth: '46%',
        data: f1.map((v) => ({
          value: v,
          itemStyle: { color: v >= 0.7 ? '#0891b2' : v >= 0.5 ? '#d97706' : '#dc2626' }
        })),
        label: { show: true, position: 'top', formatter: '{c}', fontSize: 11 }
      }
    ]
  }
})

const historyOption = computed<EChartsOption>(() => {
  const h = metrics.value?.history || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, textStyle: { color: '#475569', fontSize: 12 } },
    grid: { left: 8, right: 20, top: 20, bottom: 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: h.map((r) => r.epoch),
      name: '轮次',
      axisLabel: { color: '#64748b', fontSize: 11 },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e4e8ee' } }
    },
    yAxis: {
      type: 'value',
      max: 1,
      splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { color: '#64748b', fontSize: 11 }
    },
    series: [
      { name: '训练准确率', type: 'line', smooth: true, showSymbol: false, data: h.map((r) => r.train_acc), lineStyle: { color: '#94a3b8', width: 2 } },
      { name: '验证准确率', type: 'line', smooth: true, showSymbol: false, data: h.map((r) => r.val_acc), lineStyle: { color: '#0891b2', width: 2 } },
      { name: '验证宏 F1', type: 'line', smooth: true, showSymbol: false, data: h.map((r) => r.val_macro_f1), lineStyle: { color: '#7c3aed', width: 2 } }
    ]
  }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    info.value = await modelInfo()
  } catch (e) {
    error.value = (e as Error).message || '模型信息获取失败'
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
  margin-bottom: 0;
}

.mt {
  margin-top: var(--drs-gap);
}

.mb {
  margin-bottom: var(--drs-gap);
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--drs-gap);
  align-items: stretch;
}

.info-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 20px;
  margin: 0;
}

.info-list > div {
  display: flex;
  gap: 10px;
  padding-bottom: 6px;
  border-bottom: 1px dashed var(--drs-border);
}

.info-list .full {
  grid-column: 1 / -1;
}

.info-list dt {
  width: 74px;
  flex-shrink: 0;
  font-size: 12.5px;
  color: var(--drs-ink-500);
}

.info-list dd {
  margin: 0;
  font-size: 13px;
  color: var(--drs-ink-800);
  word-break: break-all;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px !important;
}

.note {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--drs-ink-600);
}

.note code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--drs-primary-800);
}

@media (max-width: 1024px) {
  .grid-2 {
    grid-template-columns: minmax(0, 1fr);
  }

  .info-list {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
