<template>
  <div class="report-page drs-page">
    <!-- 工具条：仅屏幕显示 -->
    <div class="toolbar no-print">
      <button type="button" class="tb-btn" @click="goBack">
        <AppIcon name="chevronLeft" :size="15" />返回
      </button>
      <span class="tb-title">诊断报告预览</span>
      <span class="tb-spacer"></span>
      <button type="button" class="tb-btn tb-primary" :disabled="!record" @click="print">
        <AppIcon name="download" :size="15" />打印 / 另存为 PDF
      </button>
    </div>

    <div v-if="loading" class="state no-print">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="!record" class="state no-print">
      <el-empty description="未找到该筛查记录，或当前账号无权访问" />
    </div>

    <!-- A4 报告纸 -->
    <ReportPaper v-else-if="record" :record="record" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import ReportPaper from '@/components/report/ReportPaper.vue'
import { detailScreening } from '@/api/screening'
import type { ScreeningRecordVO } from '@/types/screening'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const record = ref<ScreeningRecordVO | null>(null)
/** 影像加载失败标记：预签名 URL 过期或对象不存在时降级为占位提示，避免显示破图 */
function print() {
  window.print()
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/screening/records')
}

async function load(id: string) {
  loading.value = true
  try {
    record.value = await detailScreening(id)
    document.title = `诊断报告 · ${record.value.patientName || '未登记患者'}`
  } catch {
    record.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => load(String(route.params.id)))

/**
 * 多标签页使用 KeepAlive 缓存组件：同一路由仅 :id 变化时组件会被**复用**，
 * onMounted 不会再次触发。若不监听参数变化，会出现「打开 A 的报告后再打开 B，
 * 仍显示 A 的数据与图片状态」的问题。
 */
watch(
  () => route.params.id,
  (id) => {
    if (id) load(String(id))
  }
)
</script>

<style scoped>
/* 报告作为壳层内的普通页面展示（不占满视口），打印时再全幅输出 */
.report-page {
  padding-bottom: 32px;
}

/* ---------- 工具条 ---------- */
.toolbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0 12px;
  margin-bottom: 4px;
  background: var(--drs-bg);
  border-bottom: 1px solid var(--drs-border);
}

.tb-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--drs-ink-800);
}

.tb-spacer {
  flex: 1;
}

.tb-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid var(--drs-border);
  border-radius: var(--drs-radius-sm);
  background: var(--drs-surface);
  color: var(--drs-ink-700);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease, color 0.16s ease;
}

.tb-btn:hover {
  border-color: var(--drs-primary-200);
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
}

.tb-primary {
  background: var(--drs-primary);
  border-color: var(--drs-primary);
  color: #fff;
}

.tb-primary:hover {
  background: var(--drs-primary-hover);
  border-color: var(--drs-primary-hover);
  color: #fff;
}

.tb-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.state {
  max-width: 820px;
  margin: 24px auto;
  padding: 24px;
  background: var(--drs-surface);
  border-radius: var(--drs-radius);
}

/* ---------- A4 报告纸 ---------- */

/* ===================== 打印 ===================== */
@media print {
  .no-print {
    display: none !important;
  }

  .report-page {
    background: #fff;
    padding: 0;
  }
}
</style>
