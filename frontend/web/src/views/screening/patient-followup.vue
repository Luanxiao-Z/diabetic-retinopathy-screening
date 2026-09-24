<template>
  <div class="drs-page">
    <PageHeader
      title="患者随访"
      subtitle="按患者归并历次筛查，识别分级进展与好转（DR 为进展性疾病，纵向对比才有临床意义）"
      :crumbs="['筛查业务', '患者随访']"
    >
      <template #actions>
        <el-button :loading="loading" @click="loadData">
          <AppIcon name="refresh" :size="15" class="btn-ico" />刷新
        </el-button>
      </template>
    </PageHeader>

    <section class="drs-card filter-card">
      <div class="drs-card-head">
        <h3>筛选条件</h3>
        <span class="drs-card-meta">仅统计已填写患者姓名的记录</span>
      </div>
      <div class="drs-card-body">
        <el-form :model="query" label-position="top" @submit.prevent>
          <div class="filter-grid">
            <el-form-item label="患者姓名">
              <el-input v-model="query.patientName" placeholder="支持模糊匹配" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :loading="loading" @click="handleQuery">
                <AppIcon name="search" :size="15" class="btn-ico" />查询
              </el-button>
              <el-button @click="handleReset">
                <AppIcon name="refresh" :size="15" class="btn-ico" />重置
              </el-button>
            </div>
          </div>
        </el-form>
      </div>
    </section>

    <section class="drs-card table-card">
      <div class="drs-card-head">
        <h3>随访列表</h3>
        <span class="drs-card-meta">共 {{ total }} 位患者</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" row-key="patientName" border>
          <el-table-column label="患者" min-width="140">
            <template #default="{ row }">
              <span class="patient">
                <span class="patient-avatar" aria-hidden="true">{{ initial(row.patientName) }}</span>
                <span class="patient-meta">
                  <span class="patient-name">{{ row.patientName }}</span>
                  <span class="patient-sub">{{ genderLabel(row.patientGender) }}{{ row.patientAge != null ? ` · ${row.patientAge} 岁` : '' }}</span>
                </span>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="筛查次数" min-width="100">
            <template #default="{ row }">
              <span class="count">{{ row.totalCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="分级变化" min-width="230">
            <template #default="{ row }">
              <span class="change">
                <template v-if="row.previousLevel">
                  <span class="chip" :style="chipStyle(row.previousLevel)">{{ row.previousLevelName }}</span>
                  <AppIcon name="arrowRight" :size="14" class="change-arrow" />
                </template>
                <span class="chip" :style="chipStyle(row.latestLevel)">{{ row.latestLevelName || '—' }}</span>
                <span class="trend" :class="`trend-${(row.trendDirection || '').toLowerCase()}`">
                  {{ trendLabel(row.trendDirection) }}
                </span>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="最近置信度" min-width="120">
            <template #default="{ row }">{{ confidenceText(row.latestConfidence) }}</template>
          </el-table-column>
          <el-table-column label="待复核" min-width="96">
            <template #default="{ row }">
              <span v-if="row.needReviewCount" class="review-badge">{{ row.needReviewCount }}</span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="最近筛查" min-width="164">
            <template #default="{ row }">{{ row.latestTime || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTimeline(row as PatientFollowUpVO)">随访时间线</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :image-size="80" description="暂无随访数据（需上传时填写患者姓名）" />
          </template>
        </el-table>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <!-- ============ 随访时间线 ============ -->
    <el-drawer v-model="drawerVisible" :title="`随访时间线 · ${activePatient}`" size="560px" destroy-on-close>
      <div v-loading="loadingTimeline" class="timeline">
        <div v-if="!timeline.length && !loadingTimeline" class="timeline-empty">
          <el-empty :image-size="72" description="暂无筛查记录" />
        </div>
        <div v-for="(r, i) in timeline" :key="r.id" class="tl-item">
          <div class="tl-rail" aria-hidden="true">
            <span class="tl-dot" :style="{ background: levelColor(r.resultLevel) }"></span>
            <span v-if="i < timeline.length - 1" class="tl-line"></span>
          </div>
          <div class="tl-body">
            <div class="tl-head">
              <span class="chip" :style="chipStyle(r.resultLevel)">{{ r.levelName || '—' }}</span>
              <span class="tl-time">{{ r.createTime }}</span>
            </div>
            <div class="tl-meta">
              <span>置信度 {{ confidenceText(r.confidence) }}</span>
              <span v-if="r.suggestionName">· {{ r.suggestionName }}</span>
              <span v-if="r.needReview" class="tl-review">需人工复核</span>
            </div>
            <div v-if="r.remark" class="tl-remark">备注：{{ r.remark }}</div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import { pagePatients, patientTimeline } from '@/api/screening'
import { GENDER_OPTIONS, LEVEL_COLOR } from '@/types/screening'
import type { PatientFollowUpVO, ScreeningRecordVO } from '@/types/screening'

const loading = ref(false)
const list = ref<PatientFollowUpVO[]>([])
const total = ref(0)

const query = reactive({
  patientName: '',
  current: 1,
  pageSize: 10
})

const drawerVisible = ref(false)
const loadingTimeline = ref(false)
const activePatient = ref('')
const timeline = ref<ScreeningRecordVO[]>([])

const TREND_LABEL: Record<string, string> = {
  UP: '进展',
  DOWN: '好转',
  SAME: '持平',
  FIRST: '首次',
  UNKNOWN: '未知'
}

function trendLabel(dir?: string) {
  return TREND_LABEL[dir || ''] || '—'
}

function initial(name?: string) {
  return name ? name.charAt(0).toUpperCase() : '?'
}

function genderLabel(g?: string) {
  return GENDER_OPTIONS.find((o) => o.value === g)?.label || '—'
}

function confidenceText(c?: number) {
  return c == null ? '—' : `${(c * 100).toFixed(1)}%`
}

function levelColor(level?: string) {
  return LEVEL_COLOR[level || ''] || '#64748b'
}

function chipStyle(level?: string) {
  const hex = levelColor(level)
  return {
    color: hex,
    background: hexToRgba(hex, 0.1),
    borderColor: hexToRgba(hex, 0.28)
  }
}

function hexToRgba(hex: string, alpha: number) {
  const h = hex.replace('#', '')
  const full = h.length === 3 ? h.split('').map((c) => c + c).join('') : h
  return `rgba(${parseInt(full.slice(0, 2), 16)}, ${parseInt(full.slice(2, 4), 16)}, ${parseInt(full.slice(4, 6), 16)}, ${alpha})`
}

async function loadData() {
  loading.value = true
  try {
    const res = await pagePatients({ ...query })
    list.value = res.list
    total.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message || '随访数据加载失败')
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  query.current = 1
  loadData()
}

function handleReset() {
  query.patientName = ''
  query.current = 1
  loadData()
}

function handleSizeChange() {
  query.current = 1
  loadData()
}

async function openTimeline(row: PatientFollowUpVO) {
  activePatient.value = row.patientName
  drawerVisible.value = true
  loadingTimeline.value = true
  try {
    const res = await patientTimeline(row.patientName)
    timeline.value = res.list
  } catch (e) {
    ElMessage.error((e as Error).message || '时间线加载失败')
  } finally {
    loadingTimeline.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.btn-ico {
  margin-right: 5px;
}

.filter-card {
  margin-bottom: var(--drs-gap);
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0 var(--drs-gap);
  align-items: end;
}

.filter-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-grid :deep(.el-input) {
  width: 100%;
}

.filter-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  padding-bottom: 1px;
}

.table-card {
  overflow: hidden;
}

.table-wrap {
  overflow-x: auto;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px var(--drs-gap-lg);
  border-top: 1px solid var(--drs-border);
}

/* ---------- 患者单元格 ---------- */
.patient {
  display: inline-flex;
  align-items: center;
  gap: 9px;
}

.patient-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
  font-size: 12px;
  font-weight: 600;
}

.patient-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.patient-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--drs-ink-800);
}

.patient-sub {
  font-size: 11.5px;
  color: var(--drs-ink-500);
}

.count {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--drs-ink-800);
}

/* ---------- 分级变化 ---------- */
.change {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.change-arrow {
  color: var(--drs-ink-400);
}

.chip {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.trend {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
}

.trend-up {
  color: var(--drs-danger);
  background: var(--drs-danger-bg);
}

.trend-down {
  color: var(--drs-ok);
  background: var(--drs-ok-bg);
}

.trend-same {
  color: var(--drs-ink-600);
  background: var(--drs-ink-100);
}

.trend-first {
  color: var(--drs-info);
  background: var(--drs-info-bg);
}

.trend-unknown {
  color: var(--drs-ink-500);
  background: var(--drs-ink-100);
}

.review-badge {
  display: inline-block;
  min-width: 22px;
  padding: 1px 7px;
  border-radius: 999px;
  text-align: center;
  background: var(--drs-warn-bg);
  color: var(--drs-warn);
  font-size: 12px;
  font-weight: 600;
}

.muted {
  color: var(--drs-ink-400);
}

/* ---------- 时间线 ---------- */
.timeline {
  padding: 4px 4px 20px;
}

.timeline-empty {
  padding: 30px 0;
}

.tl-item {
  display: flex;
  gap: 12px;
}

.tl-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 14px;
  padding-top: 6px;
}

.tl-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tl-line {
  flex: 1;
  width: 2px;
  background: var(--drs-border);
  margin: 4px 0;
}

.tl-body {
  flex: 1;
  min-width: 0;
  padding-bottom: 18px;
}

.tl-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.tl-time {
  font-size: 12px;
  color: var(--drs-ink-500);
}

.tl-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 6px;
  font-size: 12px;
  color: var(--drs-ink-600);
}

.tl-review {
  padding: 1px 7px;
  border-radius: 999px;
  background: var(--drs-warn-bg);
  color: var(--drs-warn);
  font-weight: 500;
}

.tl-remark {
  margin-top: 6px;
  padding: 7px 10px;
  border-radius: var(--drs-radius-xs);
  background: var(--drs-surface-2);
  font-size: 12px;
  color: var(--drs-ink-600);
}
</style>
