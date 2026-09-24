<template>
  <div class="report-page">
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
    <article v-else class="paper">
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
          <div><dt>筛查时间</dt><dd>{{ record.createTime || '—' }}</dd></div>
        </dl>
      </section>

      <section class="rp-section">
        <h2 class="rp-h2">二、影像资料</h2>
        <div class="rp-images">
          <figure class="rp-fig">
            <img v-if="record.imageUrl" :src="record.imageUrl" alt="眼底影像" />
            <div v-else class="rp-img-empty">影像不可用</div>
            <figcaption>图 1 眼底原始影像</figcaption>
          </figure>
          <figure class="rp-fig">
            <img v-if="record.gradCamUrl" :src="record.gradCamUrl" alt="Grad-CAM 热力图" />
            <div v-else class="rp-img-empty">热力图不可用</div>
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
              <td class="num">{{ probPercent(lv) }}%</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-if="record.reviewStatus === 'CONFIRMED'" class="rp-section">
        <h2 class="rp-h2">四、医师复核</h2>
        <dl class="rp-grid">
          <div><dt>复核人</dt><dd>{{ record.reviewer || '—' }}</dd></div>
          <div><dt>复核时间</dt><dd>{{ record.reviewTime || '—' }}</dd></div>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { detailScreening } from '@/api/screening'
import { GENDER_OPTIONS, LEVEL_COLOR, LEVEL_LABEL, LEVEL_ORDER } from '@/types/screening'
import type { ScreeningRecordVO } from '@/types/screening'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const record = ref<ScreeningRecordVO | null>(null)

const levelOrder = LEVEL_ORDER
const levelLabel = LEVEL_LABEL

const levelColor = computed(() => LEVEL_COLOR[record.value?.resultLevel || ''] || '#0891b2')
const confidenceText = computed(() =>
  record.value?.confidence == null ? '—' : `${(record.value.confidence * 100).toFixed(1)}%`
)
const genderLabel = computed(() => {
  const g = GENDER_OPTIONS.find((o) => o.value === record.value?.patientGender)
  return g ? g.label : record.value?.patientGender || '—'
})
const generatedAt = computed(() => new Date().toLocaleString('zh-CN'))

function probPercent(lv: string): number {
  const v = record.value?.probabilities?.[lv]
  if (v == null) return 0
  const p = v > 1 ? v / 100 : v
  return Math.round(p * 100)
}

function print() {
  window.print()
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/screening/records')
}

onMounted(async () => {
  try {
    record.value = await detailScreening(String(route.params.id))
    document.title = `诊断报告 · ${record.value.patientName || '未登记患者'}`
  } catch {
    record.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.report-page {
  min-height: 100vh;
  background: var(--drs-bg);
  padding: 0 0 40px;
}

/* ---------- 工具条 ---------- */
.toolbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: var(--drs-surface);
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

/* ===================== 打印 ===================== */
@media print {
  .no-print {
    display: none !important;
  }

  .report-page {
    background: #fff;
    padding: 0;
  }

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
