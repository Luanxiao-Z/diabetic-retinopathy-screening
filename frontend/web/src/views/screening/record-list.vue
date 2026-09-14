<template>
  <div class="drs-page">
    <h1 class="drs-page-title">筛查记录</h1>
    <p class="drs-page-subtitle">查询、查看与导出历史筛查记录（数据权限：医生仅本人，管理员全量）</p>

    <div class="drs-card filter-bar">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="患者姓名">
          <el-input v-model="query.patientName" placeholder="模糊匹配" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="DR 分级">
          <el-select v-model="query.level" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="o in levelOptions" :key="o.value" :label="o.label" :value="o.value" />
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
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
        <el-form-item class="export-item">
          <el-button v-permission="'biz:screening:export'" type="success" @click="handleExport">
            导出 Excel
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="drs-card table-card">
      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column prop="patientName" label="患者姓名" min-width="110">
          <template #default="{ row }">{{ row.patientName || '未登记' }}</template>
        </el-table-column>
        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ genderLabel(row.patientGender) }}</template>
        </el-table-column>
        <el-table-column prop="patientAge" label="年龄" width="70">
          <template #default="{ row }">{{ row.patientAge ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="DR 分级" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.levelName" :color="levelColor(row.resultLevel)" effect="dark" :style="{ border: 'none' }">
              {{ row.levelName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置信度" width="100">
          <template #default="{ row }">{{ confidenceText(row.confidence) }}</template>
        </el-table-column>
        <el-table-column label="转诊建议" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.suggestionName" :color="suggestionColor(row.suggestion)" effect="dark" :style="{ border: 'none' }">
              {{ row.suggestionName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="筛查时间" min-width="150" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row as ScreeningRecordVO)">详情</el-button>
            <el-button v-permission="'biz:screening:delete'" link type="danger" @click="handleDelete(row as ScreeningRecordVO)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
    </div>

    <el-dialog v-model="detailVisible" title="筛查记录详情" width="560px" destroy-on-close>
      <ResultCard v-if="currentRecord" :record="currentRecord" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ResultCard from '@/components/ResultCard.vue'
import { detailScreening, exportScreening, pageScreening, removeScreening } from '@/api/screening'
import { GENDER_OPTIONS, LEVEL_COLOR, LEVEL_OPTIONS, SUGGESTION_COLOR } from '@/types/screening'
import type { ScreeningRecordVO } from '@/types/screening'

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
  startDate: '',
  endDate: '',
  current: 1,
  pageSize: 10
})

const detailVisible = ref(false)
const currentRecord = ref<ScreeningRecordVO | null>(null)

function levelColor(level?: string) {
  return LEVEL_COLOR[level || ''] || '#0891B2'
}
function suggestionColor(s?: string) {
  return SUGGESTION_COLOR[s || ''] || '#64748B'
}
function genderLabel(g?: string) {
  return genderOptions.find((o) => o.value === g)?.label || g || '-'
}
function confidenceText(c?: number) {
  return c == null ? '-' : `${(c * 100).toFixed(1)}%`
}

function buildQuery() {
  const q = { ...query }
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

onMounted(loadData)
</script>

<style scoped>
.filter-bar {
  padding: 16px 18px 0;
  margin-bottom: 16px;
}

.filter-bar :deep(.el-form-item) {
  margin-bottom: 16px;
}

.export-item {
  float: right;
}

.table-card {
  padding: 14px 18px 18px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
