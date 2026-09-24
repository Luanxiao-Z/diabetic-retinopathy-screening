<template>
  <div class="drs-page">
    <PageHeader
      title="筛查上传"
      subtitle="上传眼底照片，系统逐张完成 AI 分级推理并生成可解释热力图"
      :crumbs="['筛查业务', '筛查上传']"
    >
      <template #actions>
        <el-button :disabled="submitting" @click="resetAll">
          <AppIcon name="refresh" :size="15" class="btn-ico" />清空
        </el-button>
        <el-button
          v-if="failedCount"
          type="warning"
          :disabled="submitting"
          @click="retryFailed"
        >
          <AppIcon name="refresh" :size="15" class="btn-ico" />重试失败（{{ failedCount }}）
        </el-button>
        <el-button type="primary" :loading="submitting" :disabled="submitting || !pendingCount" @click="handleSubmit">
          <AppIcon name="scan" :size="15" class="btn-ico" />
          开始筛查{{ pendingCount ? `（${pendingCount}）` : '' }}
        </el-button>
      </template>
    </PageHeader>

    <div class="upload-cols">
      <!-- ============ 左：信息与影像 ============ -->
      <div class="col-left">
        <section class="drs-card">
          <div class="drs-card-head">
            <h3>患者信息</h3>
            <span class="drs-card-meta">全部选填</span>
          </div>
          <div class="drs-card-body">
            <el-form :model="form" label-position="top">
              <el-form-item label="患者姓名">
                <el-input v-model="form.patientName" placeholder="留空记为未登记" clearable />
              </el-form-item>
              <div class="form-row">
                <el-form-item label="患者年龄">
                  <el-input-number
                    v-model="form.patientAge"
                    :min="0"
                    :max="120"
                    :controls="false"
                    placeholder="岁"
                    class="full"
                  />
                </el-form-item>
                <el-form-item label="患者性别">
                  <el-select v-model="form.patientGender" placeholder="请选择" clearable class="full">
                    <el-option v-for="g in genderOptions" :key="g.value" :label="g.label" :value="g.value" />
                  </el-select>
                </el-form-item>
              </div>
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可填写就诊号、病史等" />
              </el-form-item>
            </el-form>
          </div>
        </section>

        <section class="drs-card mt">
          <div class="drs-card-head">
            <h3>眼底影像</h3>
            <span class="drs-card-meta">支持多张 · jpg / png</span>
          </div>
          <div class="drs-card-body">
            <el-upload
              ref="uploadRef"
              class="fundus-upload"
              drag
              multiple
              accept="image/*"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="onFileChange"
            >
              <div class="upload-inner">
                <AppIcon name="image" :size="30" />
                <span class="upload-text">拖拽眼底图到此处，或<em>点击选择</em></span>
                <span class="upload-hint">可一次上传多张，逐张独立推理</span>
              </div>
            </el-upload>

            <!-- 逐张状态列表 -->
            <div v-if="items.length" class="queue">
              <div class="queue-head">
                <span>待处理队列（{{ items.length }} 张）</span>
                <span class="queue-progress">已完成 {{ doneCount }} / {{ items.length }}</span>
              </div>
              <el-progress
                :percentage="progressPercent"
                :stroke-width="6"
                :status="failedCount && !submitting ? 'warning' : undefined"
                :show-text="false"
              />
              <ul class="queue-list">
                <li v-for="it in items" :key="it.key" class="queue-item">
                  <span class="qi-ico" :class="`qi-${it.status}`">
                    <AppIcon :name="statusIcon(it.status)" :size="13" />
                  </span>
                  <span class="qi-name" :title="it.name">{{ it.name }}</span>
                  <span class="qi-status" :class="`qi-txt-${it.status}`">{{ statusText(it) }}</span>
                </li>
              </ul>
            </div>

            <p class="upload-note">
              <AppIcon name="info" :size="13" />
              逐张独立提交：单张失败不影响其它影像，可在完成后单独重试。
            </p>
          </div>
        </section>
      </div>

      <!-- ============ 右：结果 ============ -->
      <div class="col-right">
        <section class="drs-card">
          <div class="drs-card-head">
            <h3>筛查结果</h3>
            <span class="drs-card-meta">{{ results.length ? `本次共 ${results.length} 条` : '等待筛查' }}</span>
          </div>
          <div class="drs-card-body">
            <div v-if="!results.length" class="result-empty">
              <el-empty :image-size="96" description="上传并筛查后，结果将在此展示" />
              <ol class="flow">
                <li><b>1</b> 填写患者信息（可留空）</li>
                <li><b>2</b> 选择一张或多张眼底影像</li>
                <li><b>3</b> 点击「开始筛查」逐张获取分级与热力图</li>
              </ol>
            </div>

            <div v-else class="result-grid">
              <ResultCard v-for="r in results" :key="r.id" :record="r" />
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import ResultCard from '@/components/ResultCard.vue'
import { uploadScreening } from '@/api/screening'
import { GENDER_OPTIONS } from '@/types/screening'
import type { ScreeningRecordVO } from '@/types/screening'

type ItemStatus = 'pending' | 'uploading' | 'done' | 'error'

interface QueueItem {
  key: string
  name: string
  file: File
  status: ItemStatus
  error?: string
  costMs?: number
}

const genderOptions = GENDER_OPTIONS
const uploadRef = ref<UploadInstance>()
const submitting = ref(false)
const results = ref<ScreeningRecordVO[]>([])
const items = ref<QueueItem[]>([])

const form = reactive({
  patientName: '',
  patientAge: undefined as number | undefined,
  patientGender: '',
  remark: ''
})

const pendingCount = computed(() => items.value.filter((i) => i.status === 'pending').length)
const failedCount = computed(() => items.value.filter((i) => i.status === 'error').length)
const doneCount = computed(() => items.value.filter((i) => i.status === 'done').length)
const progressPercent = computed(() =>
  items.value.length ? Math.round((doneCount.value / items.value.length) * 100) : 0
)

/* ---------------- 文件入队 ---------------- */
function onFileChange(file: UploadFile) {
  if (file.raw) {
    items.value.push({
      key: `${file.uid}-${Date.now()}`,
      name: file.name,
      file: file.raw,
      status: 'pending'
    })
  }
  // 展示由下方队列接管（show-file-list=false），此处不清理 el-upload 内部列表，
  // 避免在 on-change 中触发 clearFiles 造成递归。
}

function statusIcon(s: ItemStatus) {
  if (s === 'done') return 'check'
  if (s === 'error') return 'alert'
  if (s === 'uploading') return 'refresh'
  return 'clock'
}

function statusText(it: QueueItem) {
  switch (it.status) {
    case 'uploading':
      return '推理中…'
    case 'done':
      return it.costMs != null ? `完成 ${(it.costMs / 1000).toFixed(1)}s` : '完成'
    case 'error':
      return it.error || '失败'
    default:
      return '等待'
  }
}

/* ---------------- 提交（逐张） ---------------- */
async function runQueue(targets: QueueItem[]) {
  if (!targets.length) return
  submitting.value = true
  let okCount = 0
  for (const it of targets) {
    it.status = 'uploading'
    it.error = undefined
    const startedAt = Date.now()
    try {
      const data = await uploadScreening({
        files: [it.file],
        patientName: form.patientName || undefined,
        patientAge: form.patientAge,
        patientGender: form.patientGender || undefined,
        remark: form.remark || undefined
      })
      it.status = 'done'
      it.costMs = Date.now() - startedAt
      results.value = [...data, ...results.value]
      okCount += 1
    } catch (e) {
      it.status = 'error'
      it.error = (e as Error).message || '筛查失败'
    }
  }
  submitting.value = false

  if (okCount === targets.length) {
    ElMessage.success(`筛查完成，共 ${okCount} 条记录`)
  } else {
    ElMessage.warning(`完成 ${okCount} 条，失败 ${targets.length - okCount} 条，可点击「重试失败」重新提交`)
  }
}

function handleSubmit() {
  const targets = items.value.filter((i) => i.status === 'pending')
  if (!targets.length) {
    ElMessage.warning('请先选择眼底影像')
    return
  }
  runQueue(targets)
}

function retryFailed() {
  const targets = items.value.filter((i) => i.status === 'error')
  if (!targets.length) return
  runQueue(targets)
}

function resetAll() {
  form.patientName = ''
  form.patientAge = undefined
  form.patientGender = ''
  form.remark = ''
  uploadRef.value?.clearFiles()
  items.value = []
  results.value = []
}
</script>

<style scoped>
.btn-ico {
  margin-right: 5px;
}

.upload-cols {
  display: grid;
  grid-template-columns: minmax(340px, 420px) minmax(0, 1fr);
  gap: var(--drs-gap);
  align-items: start;
}

.mt {
  margin-top: var(--drs-gap);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 var(--drs-gap);
}

.full {
  width: 100%;
}

/* ---------- 上传区 ---------- */
.fundus-upload :deep(.el-upload-dragger) {
  padding: 20px 16px;
  width: 100%;
  border-radius: var(--drs-radius-sm);
  border-color: var(--drs-border-strong);
  background: var(--drs-bg-soft);
  transition: border-color 0.18s ease, background-color 0.18s ease;
}

.fundus-upload :deep(.el-upload-dragger:hover) {
  border-color: var(--drs-primary);
  background: var(--drs-primary-50);
}

.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--drs-primary);
}

.upload-text {
  font-size: 13px;
  color: var(--drs-ink-700);
}

.upload-text em {
  color: var(--drs-primary);
  font-style: normal;
  font-weight: 600;
}

.upload-hint {
  font-size: 12px;
  color: var(--drs-ink-500);
}

/* ---------- 队列 ---------- */
.queue {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--drs-border);
}

.queue-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  color: var(--drs-ink-600);
  margin-bottom: 8px;
}

.queue-progress {
  color: var(--drs-ink-500);
  font-variant-numeric: tabular-nums;
}

.queue-list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  max-height: 220px;
  overflow-y: auto;
}

.queue-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  font-size: 12px;
}

.qi-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--drs-ink-100);
  color: var(--drs-ink-500);
}

.qi-done {
  background: var(--drs-ok-bg);
  color: var(--drs-ok);
}

.qi-error {
  background: var(--drs-danger-bg);
  color: var(--drs-danger);
}

.qi-uploading {
  background: var(--drs-primary-50);
  color: var(--drs-primary-700);
}

.qi-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--drs-ink-700);
}

.qi-status {
  flex-shrink: 0;
  max-width: 45%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--drs-ink-500);
}

.qi-txt-done {
  color: var(--drs-ok);
}

.qi-txt-error {
  color: var(--drs-danger);
}

.qi-txt-uploading {
  color: var(--drs-primary-700);
}

.upload-note {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--drs-ink-500);
}

.upload-note :deep(svg) {
  margin-top: 3px;
}

/* ---------- 结果区 ---------- */
.result-empty {
  padding: 20px 0 8px;
}

.flow {
  list-style: none;
  margin: 8px auto 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 360px;
}

.flow li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--drs-ink-600);
}

.flow b {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--drs-gap);
}

@media (max-width: 1024px) {
  .upload-cols {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
