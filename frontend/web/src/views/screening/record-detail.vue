<template>
  <div class="drs-page drs-stagger">
    <PageHeader
      title="筛查记录详情"
      :subtitle="record ? `${record.patientName || '未登记'} · ${formatDateTime(record.createTime)}` : '正在加载…'"
      style="--i: 0"
    >
      <template #actions>
        <el-button @click="goBack">
          <AppIcon name="chevronLeft" :size="15" class="btn-ico" />返回列表
        </el-button>
        <el-button v-if="record" type="primary" @click="openReport">
          <AppIcon name="download" :size="15" class="btn-ico" />打印诊断报告
        </el-button>
      </template>
    </PageHeader>

    <div class="detail-split" style="--i: 1">
      <!-- 左：ResultCard 自带 drs-card，不再套一层卡片容器 -->
      <div v-loading="loading" class="detail-pane">
        <ResultCard v-if="record" :record="record" @reload="load" />
        <el-empty v-else-if="!loading" :image-size="80" description="记录不存在或已被删除" />
      </div>

      <!-- 右：直接铺 A4 报告纸，不再套卡片容器，避免双层白底与多余留白 -->
      <div class="report-pane">
        <ReportPaper v-if="record" :record="record" fit />
        <el-empty v-else-if="!loading" :image-size="80" description="报告不可用" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import ResultCard from '@/components/ResultCard.vue'
import ReportPaper from '@/components/report/ReportPaper.vue'
import { detailScreening } from '@/api/screening'
import { usePageRefresh } from '@/composables/usePageRefresh'
import { formatDateTime } from '@/utils/format'
import type { ScreeningRecordVO } from '@/types/screening'

/**
 * 筛查记录详情独立页。
 *
 * 原先在列表页用弹窗展示，多标签场景下无法并排比对不同患者，
 * 因此改为独立路由：标签标题由布局按 ?name= 追加患者姓名后缀。
 */
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const record = ref<ScreeningRecordVO | null>(null)
const recordId = String(route.params.id || '')

async function load() {
  if (!recordId) return
  loading.value = true
  try {
    record.value = await detailScreening(recordId)
  } catch (e) {
    record.value = null
    ElMessage.error((e as Error).message || '获取详情失败')
  } finally {
    loading.value = false
  }
}

// 刷新入口在多标签栏
usePageRefresh(load)


function goBack() {
  router.push('/screening/records')
}

function openReport() {
  if (!record.value) return
  router.push({
    path: `/screening/records/${record.value.id}/report`,
    query: { name: record.value.patientName || '' }
  })
}

onMounted(load)
</script>

<style scoped>
/* 左详情、右报告：宽屏并排，窄屏上下堆叠 */
.detail-pane {
  min-height: 320px;
}

.detail-split {
  display: grid;
  /* 报告按 A4 比例占 65%，详情 35% */
  grid-template-columns: minmax(0, 35fr) minmax(0, 65fr);
  gap: var(--drs-gap);
  align-items: start;
}

/*
 * 右栏只是定位容器：无背景、无边框，A4 纸直接铺在页面底色上。
 * 高度随报告自然撑开，由页面滚动；报告内部不产生滚动条。
 */
.report-pane {
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

@media (max-width: 1280px) {
  .detail-split {
    grid-template-columns: minmax(0, 1fr);
  }


}

.btn-ico {
  margin-right: 5px;
}
</style>
