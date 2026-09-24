<template>
  <div class="drs-page">
    <PageHeader
      title="操作日志"
      subtitle="关键业务动作留痕：登录、筛查上传/删除/导出、用户与字典变更（仅管理员可访问）"
      :crumbs="['系统管理', '操作日志']"
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
        <span class="drs-card-meta">按操作人、模块、结果与时间检索</span>
      </div>
      <div class="drs-card-body">
        <el-form :model="query" label-position="top" @submit.prevent>
          <div class="filter-grid">
            <el-form-item label="操作人">
              <el-input v-model="query.username" placeholder="支持模糊匹配" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="模块">
              <el-select v-model="query.module" placeholder="全部模块" clearable>
                <el-option v-for="o in LOG_MODULE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="结果">
              <el-select v-model="query.result" placeholder="全部结果" clearable>
                <el-option v-for="o in LOG_RESULT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间范围">
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

    <section class="drs-card table-card">
      <div class="drs-card-head">
        <h3>日志列表</h3>
        <span class="drs-card-meta">共 {{ total }} 条</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" row-key="id">
          <el-table-column label="时间" min-width="164">
            <template #default="{ row }">{{ row.createTime || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作人" width="120">
            <template #default="{ row }">{{ row.username || '—' }}</template>
          </el-table-column>
          <el-table-column label="模块" width="110">
            <template #default="{ row }">
              <span class="chip chip-module">{{ row.moduleName || row.module }}</span>
            </template>
          </el-table-column>
          <el-table-column label="动作" width="110">
            <template #default="{ row }">{{ row.actionName || row.action }}</template>
          </el-table-column>
          <el-table-column label="操作对象" min-width="180">
            <template #default="{ row }">
              <span class="target">{{ row.target || '—' }}</span>
              <span v-if="row.errorMsg" class="err">{{ row.errorMsg }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="96">
            <template #default="{ row }">
              <span class="chip" :class="row.result === 'SUCCESS' ? 'chip-ok' : 'chip-fail'">
                {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="耗时" width="90">
            <template #default="{ row }">
              {{ row.costMs == null ? '—' : `${row.costMs} ms` }}
            </template>
          </el-table-column>
          <el-table-column label="IP" width="130">
            <template #default="{ row }">{{ row.ip || '—' }}</template>
          </el-table-column>
          <template #empty>
            <el-empty :image-size="80" description="暂无操作日志" />
          </template>
        </el-table>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import { pageOperationLogs } from '@/api/admin'
import { LOG_MODULE_OPTIONS, LOG_RESULT_OPTIONS } from '@/types/admin'
import type { OperationLogVO } from '@/types/admin'

const loading = ref(false)
const list = ref<OperationLogVO[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)

const query = reactive({
  username: '',
  module: '',
  result: '',
  startDate: '',
  endDate: '',
  current: 1,
  pageSize: 20
})

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
    const res = await pageOperationLogs(buildQuery())
    list.value = res.list
    total.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message || '日志加载失败')
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  query.current = 1
  loadData()
}

function handleReset() {
  query.username = ''
  query.module = ''
  query.result = ''
  dateRange.value = null
  query.current = 1
  loadData()
}

function handleSizeChange() {
  query.current = 1
  loadData()
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
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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

.chip {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.chip-module {
  color: var(--drs-primary-700);
  background: var(--drs-primary-50);
  border-color: var(--drs-primary-100);
}

.chip-ok {
  color: var(--drs-ok);
  background: var(--drs-ok-bg);
  border-color: rgba(22, 163, 74, 0.22);
}

.chip-fail {
  color: var(--drs-danger);
  background: var(--drs-danger-bg);
  border-color: rgba(220, 38, 38, 0.22);
}

.target {
  display: block;
  font-size: 13px;
  color: var(--drs-ink-700);
  word-break: break-all;
}

.err {
  display: block;
  margin-top: 2px;
  font-size: 11.5px;
  color: var(--drs-danger);
}
</style>
