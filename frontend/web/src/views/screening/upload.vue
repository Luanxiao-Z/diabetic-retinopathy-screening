<template>
  <div class="drs-page">
    <PageHeader
      title="筛查上传"
      subtitle="上传眼底照片，系统逐张完成 AI 分级推理并生成可解释热力图"
      :crumbs="['筛查业务', '筛查上传']"
    >
      <template #actions>
        <el-button :disabled="submitting" @click="loadDemoSample">
          <AppIcon name="image" :size="15" class="btn-ico" />演示样例
        </el-button>
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
                  <img v-if="it.previewUrl" :src="it.previewUrl" class="qi-thumb" alt="影像预览" />
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
              <el-empty :image-size="96" description="选择眼底图后会自动开始筛查，结果将在此展示" />
              <ol class="flow">
                <li><b>1</b> 填写患者信息（可留空）</li>
                <li><b>2</b> 选择一张或多张眼底影像（自动开始筛查）</li>
                <li><b>3</b> 左右切换查看每张影像的分级与热力图</li>
              </ol>
            </div>

            <!-- 结果卡片：左右切换，每次只显示一张 -->
            <div v-else class="carousel">
              <div class="carousel-head">
                <button
                  type="button"
                  class="nav-btn"
                  :disabled="resultIndex <= 0"
                  aria-label="上一张"
                  @click="prevResult"
                >
                  <AppIcon name="chevronLeft" :size="16" />
                </button>
                <span class="carousel-idx">
                  第 {{ resultIndex + 1 }} / {{ results.length }} 张
                </span>
                <button
                  type="button"
                  class="nav-btn"
                  :disabled="resultIndex >= results.length - 1"
                  aria-label="下一张"
                  @click="nextResult"
                >
                  <AppIcon name="chevronRight" :size="16" />
                </button>
              </div>

              <ResultCard :record="results[resultIndex]" />

              <div v-if="results.length > 1" class="carousel-dots">
                <button
                  v-for="(r, i) in results"
                  :key="r.id"
                  type="button"
                  class="dot"
                  :class="{ 'is-active': i === resultIndex }"
                  :aria-label="`查看第 ${i + 1} 张结果`"
                  @click="resultIndex = i"
                ></button>
              </div>
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
  /** 本地预览地址（objectURL），用于队列缩略图 */
  previewUrl?: string
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
  if (!file.raw) return
  const item: QueueItem = {
    key: `${file.uid}-${Date.now()}`,
    name: file.name,
    file: file.raw,
    previewUrl: URL.createObjectURL(file.raw),
    status: 'pending'
  }
  items.value.push(item)
  // 选择即自动开始筛查（串行队列，多选时依次处理）
  drainQueue()
  // 展示由队列接管（show-file-list=false），此处不清理 el-upload 内部列表，
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

/* ---------------- 提交（逐张串行） ---------------- */
let draining = false

/** 串行消费队列中所有 pending 项；重复调用会被忽略 */
async function drainQueue() {
  if (draining) return
  draining = true
  submitting.value = true
  let ok = 0
  let fail = 0
  try {
    for (;;) {
      const it = items.value.find((i) => i.status === 'pending')
      if (!it) break
      const success = await runOne(it)
      if (success) ok += 1
      else fail += 1
    }
  } finally {
    draining = false
    submitting.value = false
  }
  if (ok && !fail) {
    ElMessage.success(`筛查完成，共 ${ok} 条记录`)
  } else if (fail && !ok) {
    ElMessage.error(`筛查失败 ${fail} 条，可点击「重试失败」重新提交`)
  } else if (ok && fail) {
    ElMessage.warning(`完成 ${ok} 条，失败 ${fail} 条，可点击「重试失败」重新提交`)
  }
}

/** 提交单张影像，返回是否成功 */
async function runOne(it: QueueItem): Promise<boolean> {
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
    // 新结果插到最前，并自动切到该结果
    results.value = [...data, ...results.value]
    resultIndex.value = 0
    return true
  } catch (e) {
    it.status = 'error'
    it.error = (e as Error).message || '筛查失败'
    return false
  }
}

/** 手动触发（用于首次未自动开始或补充提交） */
function handleSubmit() {
  if (!items.value.some((i) => i.status === 'pending')) {
    ElMessage.warning('请先选择眼底影像')
    return
  }
  drainQueue()
}

/** 失败项重试：状态回退为 pending 后重新排队 */
function retryFailed() {
  const failed = items.value.filter((i) => i.status === 'error')
  if (!failed.length) return
  failed.forEach((i) => {
    i.status = 'pending'
    i.error = undefined
  })
  drainQueue()
}

/* ---------------- 结果左右切换 ---------------- */
const resultIndex = ref(0)

function prevResult() {
  if (resultIndex.value > 0) resultIndex.value -= 1
}

function nextResult() {
  if (resultIndex.value < results.value.length - 1) resultIndex.value += 1
}

function resetAll() {
  form.patientName = ''
  form.patientAge = undefined
  form.patientGender = ''
  form.remark = ''
  uploadRef.value?.clearFiles()
  // 释放本地预览占用的 objectURL
  items.value.forEach((i) => i.previewUrl && URL.revokeObjectURL(i.previewUrl))
  items.value = []
  results.value = []
  resultIndex.value = 0
}

/**
 * 演示样例：加载内置样例眼底图并直接进入筛查流程，便于演示与验收。
 * 样例图随前端静态资源一同发布（public/samples/demo-fundus.png）。
 */
const DEMO_SAMPLE_URL = '/samples/demo-fundus.png'

async function loadDemoSample() {
  try {
    const resp = await fetch(DEMO_SAMPLE_URL)
    if (!resp.ok) throw new Error('样例图片不可用')
    const blob = await resp.blob()
    const file = new File([blob], 'demo-fundus.png', { type: blob.type || 'image/png' })

    if (!form.patientName) form.patientName = '演示样例患者'
    if (form.patientAge == null) form.patientAge = 58
    if (!form.patientGender) form.patientGender = 'MALE'
    if (!form.remark) form.remark = '演示样例数据'

    const item: QueueItem = {
      key: `demo-${Date.now()}`,
      name: '演示样例（demo-fundus.png）',
      file,
      previewUrl: URL.createObjectURL(file),
      status: 'pending'
    }
    items.value.push(item)
    ElMessage.info('已载入演示样例，正在自动筛查…')
    await drainQueue()
  } catch (e) {
    ElMessage.error((e as Error).message || '演示样例加载失败')
  }
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
  /* 两列等高：配合列内最后一张卡片 flex:1，使左右两列底部对齐 */
  align-items: stretch;
}

.col-left,
.col-right {
  display: flex;
  flex-direction: column;
  gap: var(--drs-gap);
}

.col-left > .drs-card:last-child,
.col-right > .drs-card:last-child {
  flex: 1;
}

/* 列内已由 gap 控制间距，避免与 .mt 叠加 */
.col-left .mt {
  margin-top: 0;
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

/* 队列缩略图 */
.qi-thumb {
  width: 30px;
  height: 30px;
  object-fit: cover;
  border-radius: 5px;
  flex-shrink: 0;
  background: var(--drs-ink-100);
}

/* ---------- 结果轮播（每次只显示一张） ---------- */
.carousel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.carousel-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.carousel-idx {
  font-size: 12.5px;
  color: var(--drs-ink-600);
  font-variant-numeric: tabular-nums;
  min-width: 96px;
  text-align: center;
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--drs-border);
  border-radius: 50%;
  background: var(--drs-surface);
  color: var(--drs-ink-600);
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}

.nav-btn:hover:not(:disabled) {
  background: var(--drs-primary-50);
  border-color: var(--drs-primary-200);
  color: var(--drs-primary-800);
}

.nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.carousel-dots {
  display: flex;
  justify-content: center;
  gap: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: var(--drs-ink-300);
  cursor: pointer;
  transition: background-color 0.16s ease, width 0.16s ease;
}

.dot.is-active {
  width: 20px;
  border-radius: 4px;
  background: var(--drs-primary);
}

@media (max-width: 1024px) {
  .upload-cols {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
