<template>
  <article ref="root" class="paper" :class="{ 'is-fit': fit }" :style="fit ? { zoom } : undefined">
  <header class="rp-head">
    <div class="rp-brand">
      <span class="rp-mark" aria-hidden="true"><AppIcon name="scan" :size="20" /></span>
      <div>
        <div class="rp-org">糖尿病视网膜病变（DR）智能筛查系统</div>
        <div class="rp-sub">Diabetic Retinopathy Screening Report</div>
      </div>
    </div>
    <div class="rp-no">
      <div class="rp-no-label">报告编号</div>
      <div class="rp-no-value">{{ record.id }}</div>
    </div>
  </header>

  <h1 class="rp-title">眼底筛查诊断报告</h1>

  <section class="rp-section">
    <h2 class="rp-h2">一、受检者信息</h2>
    <dl class="rp-grid">
      <div><dt>姓名</dt><dd>{{ record.patientName || '未登记' }}</dd></div>
      <div><dt>性别</dt><dd>{{ genderLabel }}</dd></div>
      <div><dt>年龄</dt><dd>{{ record.patientAge != null ? `${record.patientAge} 岁` : '—' }}</dd></div>
      <div><dt>筛查时间</dt><dd>{{ formatDateTime(record.createTime) }}</dd></div>
    </dl>
  </section>

  <section class="rp-section">
    <h2 class="rp-h2">二、影像资料</h2>
    <div class="rp-images">
      <figure class="rp-fig">
        <img
          v-if="record.imageUrl && !imageFailed"
          :src="record.imageUrl"
          alt="眼底原始影像"
          @error="imageFailed = true"
        />
        <div v-else class="rp-img-empty">{{ emptyReason('影像') }}</div>
        <figcaption>图 1 眼底原始影像</figcaption>
      </figure>
      <figure class="rp-fig">
        <img
          v-if="record.gradCamUrl && !camFailed"
          :src="record.gradCamUrl"
          alt="Grad-CAM 热力图"
          @error="camFailed = true"
        />
        <div v-else class="rp-img-empty">{{ emptyReason('热力图') }}</div>
        <figcaption>图 2 Grad-CAM 关注区域热力图</figcaption>
      </figure>
    </div>
    <p class="rp-note">
      热力图由 Grad-CAM 生成，颜色越深表示模型判定时关注度越高，用于辅助理解模型依据，不作为诊断依据。
    </p>
  </section>

  <section class="rp-section">
    <h2 class="rp-h2">三、筛查结论</h2>
    <div class="rp-verdict">
      <div class="rp-verdict-main">
        <div class="rp-verdict-label">AI 分级结果</div>
        <div class="rp-verdict-level" :style="{ color: levelColor }">{{ record.levelName || '—' }}</div>
        <div class="rp-verdict-code">{{ record.resultLevel || '—' }}</div>
      </div>
      <div class="rp-verdict-side">
        <div class="rp-kv"><span>模型置信度</span><b>{{ confidenceText }}</b></div>
        <div class="rp-kv">
          <span>不确定性</span>
          <b>{{ record.uncertainty == null ? '—' : record.uncertainty.toFixed(3) }}</b>
        </div>
        <div class="rp-kv"><span>转诊建议</span><b>{{ record.suggestionName || '—' }}</b></div>
        <div class="rp-kv">
          <span>复核状态</span>
          <b>{{ record.reviewStatusName || '无需复核' }}</b>
        </div>
      </div>
    </div>

    <table class="rp-table">
      <thead>
        <tr><th>分级</th><th>说明</th><th>模型概率</th></tr>
      </thead>
      <tbody>
        <tr v-for="lv in levelOrder" :key="lv" :class="{ 'is-hit': lv === record.resultLevel }">
          <td>{{ lv }}</td>
          <td>{{ levelLabel[lv] }}</td>
          <td class="num">{{ probPercent(lv) }}</td>
        </tr>
      </tbody>
    </table>
  </section>

  <section v-if="record.reviewStatus === 'CONFIRMED'" class="rp-section">
    <h2 class="rp-h2">四、医师复核</h2>
    <dl class="rp-grid">
      <div><dt>复核人</dt><dd>{{ record.reviewer || '—' }}</dd></div>
      <div><dt>复核时间</dt><dd>{{ formatDateTime(record.reviewTime) }}</dd></div>
      <div class="rp-span"><dt>复核意见</dt><dd>{{ record.reviewRemark || '（未填写）' }}</dd></div>
    </dl>
  </section>

  <section v-if="record.remark" class="rp-section">
    <h2 class="rp-h2">{{ record.reviewStatus === 'CONFIRMED' ? '五' : '四' }}、备注</h2>
    <p class="rp-text">{{ record.remark }}</p>
  </section>

  <section class="rp-section rp-disclaimer">
    <h2 class="rp-h2">免责声明</h2>
    <p class="rp-text">
      本报告由人工智能模型（{{ record.modelVersion || '—' }}）辅助生成，结果仅供临床参考，
      <b>不能替代眼科医师的诊断意见</b>。最终诊断与治疗方案请以执业医师的判断为准。
      影像数据存储于私有对象存储，报告中的图片链接具有时效性。
    </p>
  </section>

  <footer class="rp-foot">
    <span>生成时间：{{ generatedAt }}</span>
    <span>模型版本：{{ record.modelVersion || '—' }}</span>
  </footer>
  </article>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { formatDateTime, formatNow } from '@/utils/format'
import { GENDER_OPTIONS, LEVEL_COLOR, LEVEL_LABEL, LEVEL_ORDER } from '@/types/screening'
import type { ScreeningRecordVO } from '@/types/screening'

/**
 * 诊断报告纸（A4）。
 *
 * 独立报告页与「筛查记录详情」页右栏共用同一份版式，避免两处各写一套；
 * 组件只负责渲染，数据加载与打印动作由使用方处理。
 */
const props = withDefaults(
  defineProps<{
    record: ScreeningRecordVO
    /** 嵌入窄容器时按父级可用宽高等比缩小，整页完整显示且不产生滚动条 */
    fit?: boolean
  }>(),
  { fit: false }
)

/* ---------- 嵌入模式：按父容器可用空间等比缩放 ---------- */
const A4_W = 794 // 210mm @96dpi
const A4_H = 1123 // 297mm @96dpi
const root = ref<HTMLElement>()
const zoom = ref(1)
let observer: ResizeObserver | undefined

function measure() {
  if (!props.fit) return
  const parent = root.value?.parentElement
  if (!parent) return
  const w = parent.clientWidth
  if (!w) return
  // 只按宽度缩放：高度由 A4 比例自然撑开，交给页面滚动
  zoom.value = Math.min(w / A4_W, 1)
}

onMounted(() => {
  measure()
  const parent = root.value?.parentElement
  if (props.fit && parent && typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(() => measure())
    observer.observe(parent)
  }
})

onBeforeUnmount(() => observer?.disconnect())

/** 影像加载失败标记：预签名 URL 过期或对象不存在时降级为占位提示，避免显示破图 */
const imageFailed = ref(false)
const camFailed = ref(false)

// 复用组件时切换记录，需要重置上一次的图片失败状态
watch(
  () => props.record.id,
  () => {
    imageFailed.value = false
    camFailed.value = false
  }
)

/**
 * 影像缺失时的说明文案。
 * 演示数据脚本生成的记录不含真实影像（对象键为占位路径），需与「链接过期」区分提示。
 */
function emptyReason(kind: string): string {
  const key = props.record.imageKey || ''
  if (key.startsWith('demo/')) {
    return `${kind}不可用（演示数据未包含影像）`
  }
  return `${kind}不可用（链接可能已过期，请重新打开）`
}

const levelOrder = LEVEL_ORDER
const levelLabel = LEVEL_LABEL

const levelColor = computed(() => LEVEL_COLOR[props.record.resultLevel || ''] || '#0891b2')
const confidenceText = computed(() =>
  props.record.confidence == null ? '—' : `${(clamp01(props.record.confidence) * 100).toFixed(1)}%`
)
const genderLabel = computed(() => {
  const g = GENDER_OPTIONS.find((o) => o.value === props.record.patientGender)
  return g ? g.label : props.record.patientGender || '—'
})
const generatedAt = computed(() => formatNow())

/** 后端概率契约为 0~1 的 softmax 输出；仅做防御性钳制，不做单位猜测 */
function clamp01(v: number): number {
  return Math.min(1, Math.max(0, v))
}

/**
 * 概率显示文本。保留一位小数并对极小的非零值显式标注：
 * 若用 Math.round，99.58% 会显示为 100%、0.41% 会显示为 0%，掩盖真实分布。
 */
function probPercent(lv: string): string {
  const v = props.record.probabilities?.[lv]
  if (v == null) return '—'
  const p = clamp01(v) * 100
  if (p === 0) return '0%'
  if (p < 0.1) return '<0.1%'
  return `${p.toFixed(1)}%`
}
</script>

<style scoped>
.paper {
  width: 210mm;
  min-height: 297mm;
  margin: 24px auto;
  padding: 18mm 16mm;
  background: #fff;
  box-shadow: 0 2px 20px rgba(15, 23, 42, 0.1);
  color: #0f172a;
  font-size: 13px;
  line-height: 1.7;
}

/* 嵌入窄容器时不保留 A4 外留白，由父级居中 */
.paper.is-fit {
  margin: 0 auto;
}

.rp-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 2px solid #0891b2;
}

.rp-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rp-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: #e6f7fa;
  color: #0e7490;
  flex-shrink: 0;
}

.rp-org {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.rp-sub {
  font-size: 11px;
  color: #64748b;
  letter-spacing: 0.3px;
}

.rp-no {
  text-align: right;
}

.rp-no-label {
  font-size: 11px;
  color: #64748b;
}

.rp-no-value {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: #334155;
  word-break: break-all;
  max-width: 200px;
}

.rp-title {
  font-size: 22px;
  font-weight: 600;
  text-align: center;
  letter-spacing: 4px;
  margin: 22px 0 24px;
}

.rp-section {
  margin-bottom: 20px;
}

.rp-h2 {
  font-size: 13px;
  font-weight: 600;
  color: #0e7490;
  margin: 0 0 10px;
  padding-left: 9px;
  border-left: 3px solid #0891b2;
}

/* 信息栅格 */
.rp-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 24px;
  margin: 0;
}

.rp-grid > div {
  display: flex;
  gap: 10px;
  border-bottom: 1px dashed #e4e8ee;
  padding-bottom: 6px;
}

.rp-grid .rp-span {
  grid-column: 1 / -1;
}

.rp-grid dt {
  width: 68px;
  flex-shrink: 0;
  color: #64748b;
}

.rp-grid dd {
  margin: 0;
  color: #0f172a;
  font-weight: 500;
}

/* 影像 */
.rp-images {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.rp-fig {
  margin: 0;
}

.rp-fig img {
  width: 100%;
  height: 180px;
  object-fit: contain;
  background: #0f172a;
  border-radius: 6px;
  display: block;
}

.rp-img-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 180px;
  background: #f1f5f9;
  border-radius: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.rp-fig figcaption {
  margin-top: 6px;
  text-align: center;
  font-size: 11px;
  color: #64748b;
}

.rp-note {
  margin: 10px 0 0;
  font-size: 11px;
  color: #64748b;
  line-height: 1.6;
}

/* 结论 */
.rp-verdict {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  border: 1px solid #e4e8ee;
  border-radius: 8px;
  margin-bottom: 14px;
}

.rp-verdict-label {
  font-size: 11px;
  color: #64748b;
}

.rp-verdict-level {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0.5px;
}

.rp-verdict-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: #94a3b8;
}

.rp-verdict-side {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rp-kv {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #e4e8ee;
  font-size: 12px;
}

.rp-kv span {
  color: #64748b;
}

.rp-kv b {
  color: #0f172a;
  font-weight: 600;
}

/* 概率表 */
.rp-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.rp-table th,
.rp-table td {
  border: 1px solid #e4e8ee;
  padding: 6px 10px;
  text-align: left;
}

.rp-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.rp-table td.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.rp-table tr.is-hit td {
  background: #ecfeff;
  font-weight: 600;
  color: #0e7490;
}

.rp-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.8;
  color: #334155;
}

.rp-disclaimer .rp-text {
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  color: #475569;
}

.rp-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 26px;
  padding-top: 12px;
  border-top: 1px solid #e4e8ee;
  font-size: 11px;
  color: #94a3b8;
}

/* 独立成页时铺满纸张；嵌在详情页右栏时由使用方缩放 */
@media print {
  .paper {
    width: auto;
    min-height: 0;
    margin: 0;
    padding: 0;
    box-shadow: none;
  }

  @page {
    size: A4;
    margin: 14mm 12mm;
  }
}
</style>
