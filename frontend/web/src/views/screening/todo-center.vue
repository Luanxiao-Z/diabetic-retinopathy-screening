<template>
  <div class="drs-page">
    <PageHeader title="随访待办" :crumbs="['筛查业务', '随访待办']">
      <template #actions>
        <el-button :loading="loading" @click="loadData">
          <AppIcon name="refresh" :size="15" class="btn-ico" />刷新
        </el-button>
      </template>
    </PageHeader>

    <section class="drs-card">
      <div class="drs-card-head">
        <el-radio-group v-model="activeTab" :disabled="loading">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="review">待人工复核</el-radio-button>
          <el-radio-button value="referral">需转诊</el-radio-button>
          <el-radio-button value="overdue">逾期未复诊</el-radio-button>
        </el-radio-group>

        <div class="head-right">
          <el-input
            v-model="keyword"
            placeholder="按患者姓名筛选"
            clearable
            class="kw-input"
            @keyup.enter="handleQuery"
          >
            <template #prefix><AppIcon name="search" :size="14" /></template>
          </el-input>
          <el-button type="primary" :loading="loading" @click="handleQuery">
            <AppIcon name="search" :size="15" class="btn-ico" />查询
          </el-button>
          <el-button @click="handleReset">
            <AppIcon name="refresh" :size="15" class="btn-ico" />重置
          </el-button>
        </div>
      </div>

      <div class="table-wrap">
        <el-table
          v-loading="loading"
          :data="list"
          row-key="id"
          border
          @row-dblclick="onRowDblClick"
        >
          <el-table-column v-if="activeTab === 'all'" label="类型" min-width="120">
            <template #default="{ row }">
              <span class="ti-type" :class="`tt-${row.type}`">{{ row.typeName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="patientName" label="患者" min-width="150" />
          <el-table-column prop="levelName" label="分级" min-width="130">
            <template #default="{ row }">{{ row.levelName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="detail" label="说明" min-width="180">
            <template #default="{ row }">{{ row.detail || '—' }}</template>
          </el-table-column>
          <el-table-column prop="time" label="时间" min-width="170">
            <template #default="{ row }">{{ row.time || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <template v-if="row.type === 'overdue'">
                <el-button link type="primary" @click="go('/screening/patients')">
                  随访时间线
                </el-button>
              </template>
              <template v-else>
                <el-button link type="primary" @click="openReport(row.recordId)">报告</el-button>
                <el-button v-if="row.type === 'review'" link type="warning" @click="goReview">
                  去复核
                </el-button>
              </template>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :image-size="80" :description="emptyText" />
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
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import { pageTodos } from '@/api/screening'
import { useScreeningStore } from '@/stores/screening'
import type { TodoItemVO, TodoType } from '@/types/screening'

type TabKey = 'all' | TodoType

const TYPE_LABEL: Record<TodoType, string> = {
  review: '待人工复核',
  referral: '需转诊',
  overdue: '逾期未复诊'
}

const router = useRouter()
const screeningStore = useScreeningStore()
const loading = ref(false)
const list = ref<TodoItemVO[]>([])
const total = ref(0)
const activeTab = ref<TabKey>('all')
const keyword = ref('')
/** 上次加载时的数据版本，用于 KeepAlive 恢复后判断是否需要刷新 */
const loadedVersion = ref(-1)

const query = reactive({ current: 1, pageSize: 10 })

const emptyText = computed(() => {
  const kw = keyword.value.trim()
  if (kw) return `没有匹配「${kw}」的待办`
  return activeTab.value === 'all' ? '暂无待办事项' : `暂无${TYPE_LABEL[activeTab.value as TodoType]}记录`
})

async function loadData() {
  loading.value = true
  try {
    const res = await pageTodos({
      todoType: activeTab.value,
      patientName: keyword.value.trim() || undefined,
      current: query.current,
      pageSize: query.pageSize
    })
    list.value = res.list
    total.value = res.total
    loadedVersion.value = screeningStore.dataVersion
  } catch (e) {
    ElMessage.error((e as Error).message || '待办加载失败')
  } finally {
    loading.value = false
  }
}

// 切换待办类型时回到第 1 页重新查询
watch(activeTab, () => {
  query.current = 1
  loadData()
})

function handleQuery() {
  query.current = 1
  loadData()
}

function handleReset() {
  keyword.value = ''
  query.current = 1
  loadData()
}

function handleSizeChange() {
  query.current = 1
  loadData()
}

/** 双击行 → 该待办的主要动作（逾期看时间线，其余看诊断报告） */
function onRowDblClick(row: TodoItemVO) {
  if (row.type === 'overdue') {
    go('/screening/patients')
  } else if (row.recordId) {
    openReport(row.recordId)
  }
}

function go(path: string) {
  router.push(path)
}

function goReview() {
  router.push('/screening/records?needReview=true')
}

function openReport(id?: string) {
  if (!id) return
  // 页内跳转，不新开浏览器标签页
  router.push({ path: `/screening/records/${id}/report` })
}

onMounted(loadData)

// 完成新的筛查后切回本页时刷新
onActivated(() => {
  if (loadedVersion.value !== screeningStore.dataVersion) loadData()
})
</script>

<style scoped>
.btn-ico {
  margin-right: 5px;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.kw-input {
  width: 200px;
}

.table-wrap {
  overflow-x: auto;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: var(--drs-gap) var(--drs-gap-lg);
  border-top: 1px solid var(--drs-border);
}

/* 「全部」模式下用于区分待办类型 */
.ti-type {
  display: inline-block;
  padding: 1px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.tt-review {
  background: var(--drs-warn-bg);
  color: var(--drs-warn);
}

.tt-referral {
  background: var(--drs-danger-bg);
  color: var(--drs-danger);
}

.tt-overdue {
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
}

@media (max-width: 900px) {
  .drs-card-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .head-right {
    width: 100%;
  }

  .kw-input {
    flex: 1;
    width: auto;
  }
}
</style>
