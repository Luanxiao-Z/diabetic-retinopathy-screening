<template>
  <div class="drs-page">
    <PageHeader
      title="筛查记录"
      :subtitle="scopeSubtitle"
      :crumbs="['筛查业务', '筛查记录']"
    >
      <template #actions>
        <el-button
          v-permission="'biz:screening:delete'"
          type="danger"
          plain
          :disabled="!selectedIds.length || loading"
          @click="handleBatchDelete"
        >
          <AppIcon name="trash" :size="15" class="btn-ico" />批量删除{{ selectedIds.length ? `（${selectedIds.length}）` : '' }}
        </el-button>
        <el-button
          v-permission="'biz:screening:export'"
          type="primary"
          :disabled="loading"
          @click="handleExport"
        >
          <AppIcon name="download" :size="15" class="btn-ico" />
          {{ selectedIds.length ? `导出所选（${selectedIds.length}）` : '导出 Excel' }}
        </el-button>
      </template>
    </PageHeader>

    <!-- ============ 筛选条件 ============ -->
    <section class="drs-card filter-card">
      <div class="drs-card-head">
        <h3>筛选条件</h3>
        <span class="drs-card-meta">按患者、分级与时间范围检索</span>
      </div>
      <div class="drs-card-body">
        <el-form :model="query" label-position="top" @submit.prevent>
          <div class="filter-grid">
            <el-form-item label="患者姓名">
              <el-input v-model="query.patientName" placeholder="支持模糊匹配" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="DR 分级">
              <el-select v-model="query.level" placeholder="全部分级" clearable>
                <el-option v-for="o in levelOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="复核状态">
              <el-select v-model="query.needReview" placeholder="全部" clearable>
                <el-option label="待人工复核" value="true" />
                <el-option label="置信度达标" value="false" />
              </el-select>
            </el-form-item>
            <el-form-item label="筛查时间">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                class="date-range"
              />
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

    <!-- ============ 记录列表 ============ -->
    <section class="drs-card table-card">
      <div class="drs-card-head">
        <h3>记录列表</h3>
        <span class="drs-card-meta">
          共 {{ total }} 条{{ selectedIds.length ? ` · 已选 ${selectedIds.length} 条` : '' }} ·
          拖动表头分隔线可调整列宽
          <el-button v-if="hasCustom" link type="primary" @click="resetWidths">恢复默认</el-button>
        </span>
      </div>

      <div class="table-wrap">
        <el-table
          v-loading="loading"
          :data="list"
          row-key="id"
          border
          @selection-change="onSelectionChange"
          @header-dragend="onHeaderDragend"
        >
          <el-table-column type="selection" width="46" />
          <el-table-column prop="patientName" label="患者姓名" :min-width="widthOf('patientName', 130)">
            <template #default="{ row }">{{ row.patientName || '未登记' }}</template>
          </el-table-column>
          <el-table-column prop="patientGender" label="性别" :min-width="widthOf('patientGender', 90)">
            <template #default="{ row }">{{ genderLabel(row.patientGender) }}</template>
          </el-table-column>
          <el-table-column prop="patientAge" label="年龄" :min-width="widthOf('patientAge', 90)">
            <template #default="{ row }">{{ row.patientAge ?? '—' }}</template>
          </el-table-column>
          <el-table-column prop="resultLevel" label="DR 分级" :min-width="widthOf('resultLevel', 130)">
            <template #default="{ row }">
              <span v-if="row.levelName" class="chip" :style="chipStyle(row.resultLevel, LEVEL_COLOR)">
                {{ row.levelName }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="confidence" label="置信度" :min-width="widthOf('confidence', 150)">
            <template #default="{ row }">
              <span class="conf">
                <span class="conf-bar" aria-hidden="true">
                  <span
                    class="conf-fill"
                    :class="{ 'is-low': row.needReview }"
                    :style="{ width: confPercent(row.confidence) }"
                  ></span>
                </span>
                <span class="conf-txt">{{ confidenceText(row.confidence) }}</span>
              </span>
              <span v-if="row.needReview" class="review-tag" :title="`置信度低于阈值 ${row.reviewThreshold ?? ''}，建议人工复核`">
                待复核
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="suggestion" label="转诊建议" :min-width="widthOf('suggestion', 150)">
            <template #default="{ row }">
              <span v-if="row.suggestionName" class="chip" :style="chipStyle(row.suggestion, SUGGESTION_COLOR)">
                {{ row.suggestionName }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="reviewStatus" label="复核状态" :min-width="widthOf('reviewStatus', 130)">
            <template #default="{ row }">
              <span v-if="row.reviewStatus === 'CONFIRMED'" class="rv rv-done" :title="`${row.reviewer || ''} ${row.reviewTime || ''} ${row.reviewRemark || ''}`">
                <AppIcon name="check" :size="12" />已复核
              </span>
              <span v-else-if="row.needReview" class="rv rv-pending">待复核</span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="筛查时间" :min-width="widthOf('createTime', 170)" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row as ScreeningRecordVO)">详情</el-button>
              <el-button
                v-permission="'biz:screening:review'"
                link
                type="warning"
                :disabled="!row.needReview || row.reviewStatus === 'CONFIRMED'"
                @click="handleReview(row as ScreeningRecordVO)"
              >
                复核
              </el-button>
              <el-button link type="primary" @click="openReport(row as ScreeningRecordVO)">报告</el-button>
              <el-button
                v-permission="'biz:screening:delete'"
                link
                type="danger"
                @click="handleDelete(row as ScreeningRecordVO)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :image-size="80" description="没有符合条件的筛查记录" />
          </template>
        </el-table>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="detailVisible" title="筛查记录详情" width="620px" destroy-on-close>
      <ResultCard v-if="currentRecord" :record="currentRecord" />
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentRecord"
          type="primary"
          @click="openReport(currentRecord)"
        >
          <AppIcon name="download" :size="15" class="btn-ico" />打印诊断报告
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import ResultCard from '@/components/ResultCard.vue'
import { useColumnWidths } from '@/composables/useColumnWidths'
import {
  detailScreening,
  exportScreening,
  pageScreening,
  removeScreening,
  removeScreeningBatch,
  reviewScreening
} from '@/api/screening'
import { useUserStore } from '@/stores/user'
import { GENDER_OPTIONS, LEVEL_COLOR, LEVEL_OPTIONS, SUGGESTION_COLOR } from '@/types/screening'
import type { ScreeningRecordVO } from '@/types/screening'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
/** 列宽可拖拽调整并按表记忆；初始宽度按内容与容器自适应 */
const { widthOf, onHeaderDragend, resetWidths, hasCustom } = useColumnWidths('screening-records')
const levelOptions = LEVEL_OPTIONS
const genderOptions = GENDER_OPTIONS

const loading = ref(false)
const list = ref<ScreeningRecordVO[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const selectedIds = ref<string[]>([])

const query = reactive({
  patientName: '',
  level: '',
  needReview: '',
  startDate: '',
  endDate: '',
  current: 1,
  pageSize: 10
})

const detailVisible = ref(false)
const currentRecord = ref<ScreeningRecordVO | null>(null)

const scopeSubtitle = computed(() =>
  userStore.dataScope === 'ALL' || userStore.role === 'ADMIN'
    ? '查询、查看与导出全部筛查记录（数据权限：全部数据）'
    : '查询、查看与导出本人创建的筛查记录（数据权限：仅本人数据）'
)

/** 由等级色生成浅底深字的标签样式，避免大面积实色块 */
function chipStyle(key: string | undefined, colorMap: Record<string, string>) {
  const hex = colorMap[key || ''] || '#64748b'
  return {
    color: hex,
    background: hexToRgba(hex, 0.1),
    borderColor: hexToRgba(hex, 0.28)
  }
}

function hexToRgba(hex: string, alpha: number) {
  const h = hex.replace('#', '')
  const full = h.length === 3 ? h.split('').map((c) => c + c).join('') : h
  const r = parseInt(full.slice(0, 2), 16)
  const g = parseInt(full.slice(2, 4), 16)
  const b = parseInt(full.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function genderLabel(g?: string) {
  return genderOptions.find((o) => o.value === g)?.label || g || '—'
}

function confidenceText(c?: number) {
  return c == null ? '—' : `${(c * 100).toFixed(1)}%`
}

function confPercent(c?: number) {
  return c == null ? '0%' : `${Math.max(0, Math.min(1, c)) * 100}%`
}

function buildQuery() {
  const q = { ...query, needReview: undefined as boolean | undefined }
  if (query.needReview === 'true') q.needReview = true
  else if (query.needReview === 'false') q.needReview = false
  if (dateRange.value) {
    q.startDate = `${dateRange.value[0]} 00:00:00`
    q.endDate = `${dateRange.value[1]} 23:59:59`
  } else {
    q.startDate = ''
    q.endDate = ''
  }
  return q
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageScreening(buildQuery())
    list.value = res.list
    total.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message || '查询失败')
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
  query.level = ''
  query.needReview = ''
  dateRange.value = null
  query.current = 1
  loadData()
}

function handleSizeChange() {
  query.current = 1
  loadData()
}

function onSelectionChange(rows: ScreeningRecordVO[]) {
  selectedIds.value = rows.map((r) => r.id)
}

async function openDetail(row: ScreeningRecordVO) {
  try {
    currentRecord.value = await detailScreening(row.id)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error((e as Error).message || '获取详情失败')
  }
}

async function handleDelete(row: ScreeningRecordVO) {
  try {
    await ElMessageBox.confirm(`确认删除该筛查记录（患者：${row.patientName || '未登记'}）？`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await removeScreening(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    ElMessage.error((e as Error).message || '删除失败')
  }
}

function handleExport() {
  const ids = selectedIds.value.length ? selectedIds.value : undefined
  exportScreening(ids, buildQuery())
}

/** 人工复核确认（可填写复核意见） */
async function handleReview(row: ScreeningRecordVO) {
  let remark = ''
  try {
    const res = await ElMessageBox.prompt(
      `记录「${row.patientName || '未登记患者'}」置信度 ${confidenceText(row.confidence)}，低于阈值，请核对影像后确认。`,
      '人工复核确认',
      {
        confirmButtonText: '确认复核',
        cancelButtonText: '取消',
        inputPlaceholder: '复核意见（选填）',
        inputValue: '',
        type: 'warning'
      }
    )
    remark = res.value || ''
  } catch {
    return
  }
  try {
    await reviewScreening(row.id, remark || undefined)
    ElMessage.success('已完成复核')
    loadData()
  } catch (e) {
    ElMessage.error((e as Error).message || '复核失败')
  }
}

/** 打开诊断报告（当前页内跳转，不新开浏览器标签页） */
function openReport(row: ScreeningRecordVO) {
  detailVisible.value = false
  router.push({ path: `/screening/records/${row.id}/report` })
}

/** 批量删除：逐条执行并汇总结果 */
async function handleBatchDelete() {
  const ids = [...selectedIds.value]
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(
      `确认删除所选 ${ids.length} 条筛查记录？将同时清理对象存储中的影像与热力图，且不可恢复。`,
      '高风险操作',
      { type: 'warning', confirmButtonText: '确认删除' }
    )
  } catch {
    return
  }
  loading.value = true
  try {
    const { success, failed } = await removeScreeningBatch(ids)
    if (failed.length) {
      ElMessage.warning(`删除完成：成功 ${success} 条，失败 ${failed.length} 条（${failed[0].reason}）`)
    } else {
      ElMessage.success(`已删除 ${success} 条记录`)
    }
    selectedIds.value = []
    loadData()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 支持从看板「待人工复核」卡片带入筛选
  if (route.query.needReview === 'true') query.needReview = 'true'
  if (route.query.level) query.level = String(route.query.level)
  loadData()
})
</script>

<style scoped>
.btn-ico {
  margin-right: 5px;
}

/* ---------- 筛选栏 ---------- */
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

.filter-grid :deep(.el-select),
.filter-grid :deep(.el-input) {
  width: 100%;
}

.date-range {
  width: 100%;
}

.filter-actions {
  display: flex;
  gap: 10px;
  padding-bottom: 1px;
  align-items: center;
}

/* ---------- 表格 ---------- */
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

/* 等级 / 建议标签：浅底深字，兼顾可读与克制 */
.chip {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

/* 置信度：细进度条 + 数值，避免纯数字难以横向比较 */
.conf {
  display: flex;
  align-items: center;
  gap: 7px;
}

.conf-bar {
  flex: 1;
  min-width: 34px;
  height: 5px;
  border-radius: 3px;
  background: var(--drs-ink-100);
  overflow: hidden;
}

.conf-fill {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: var(--drs-primary);
}

.conf-fill.is-low {
  background: var(--drs-warn);
}

.review-tag {
  display: inline-block;
  margin-top: 4px;
  padding: 1px 7px;
  border-radius: 999px;
  background: var(--drs-warn-bg);
  color: var(--drs-warn);
  font-size: 11px;
  font-weight: 600;
}

/* 复核状态 */
.rv {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.rv-pending {
  color: var(--drs-warn);
  background: var(--drs-warn-bg);
}

.rv-done {
  color: var(--drs-ok);
  background: var(--drs-ok-bg);
}

.muted {
  color: var(--drs-ink-400);
}

.conf-txt {
  font-size: 12px;
  color: var(--drs-ink-600);
  font-variant-numeric: tabular-nums;
  min-width: 44px;
  text-align: right;
}

@media (max-width: 900px) {
  .filter-actions {
    padding-bottom: 0;
    padding-top: 4px;
  }
}
</style>
