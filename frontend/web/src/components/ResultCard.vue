<template>
  <article class="result-card drs-card">
    <!-- ============ 影像区 ============ -->
    <div class="rc-image">
      <el-image
        v-if="record.imageUrl"
        :src="record.imageUrl"
        fit="contain"
        :preview-src-list="previewList"
        :initial-index="0"
        class="rc-img"
        alt="眼底影像"
      >
        <template #error>
          <div class="rc-img-fallback">
            <AppIcon name="image" :size="22" />
            <span>影像不可用</span>
          </div>
        </template>
      </el-image>
      <div v-else class="rc-img-fallback">
        <AppIcon name="image" :size="22" />
        <span>无影像</span>
      </div>

      <img
        v-if="showCam && record.gradCamUrl"
        :src="record.gradCamUrl"
        class="rc-cam"
        alt="Grad-CAM 热力图叠加"
      />

      <span class="rc-level-chip" :style="levelChipStyle">{{ record.levelName || '未分级' }}</span>

      <label v-if="record.gradCamUrl" class="rc-cam-toggle">
        <el-switch v-model="showCam" size="small" />
        <span>热力图叠加</span>
      </label>
    </div>

    <!-- ============ 信息区 ============ -->
    <div class="rc-body">
      <div class="rc-head">
        <span class="rc-patient">{{ record.patientName || '未登记患者' }}</span>
        <span class="rc-meta">
          <span v-if="record.patientGender">{{ genderLabel }}</span>
          <span v-if="record.patientAge != null">{{ record.patientAge }} 岁</span>
        </span>
      </div>

      <!-- 转诊建议 -->
      <div class="rc-suggest" :style="suggestStyle">
        <AppIcon name="hospital" :size="15" />
        <span class="rc-suggest-text">
          <b>{{ record.suggestionName || '暂无建议' }}</b>
          <em>{{ suggestionHint }}</em>
        </span>
      </div>

      <!-- 置信度 -->
      <div class="rc-block">
        <div class="rc-block-head">
          <span>模型置信度</span>
          <b>{{ confidenceText }}</b>
        </div>
        <el-progress
          :percentage="confidencePercent"
          :stroke-width="8"
          :color="levelColor"
          :show-text="false"
        />
      </div>

      <!-- 各级概率 -->
      <div class="rc-block">
        <div class="rc-block-head">
          <span>各分级概率</span>
        </div>
        <div class="rc-probs">
          <div v-for="lv in levelOrder" :key="lv" class="rc-prob-row">
            <span class="rc-prob-name" :class="{ 'is-hit': lv === record.resultLevel }">
              {{ levelLabel[lv] }}
            </span>
            <span class="rc-prob-bar">
              <span
                class="rc-prob-fill"
                :style="{ width: probPercent(lv) + '%', background: levelColorMap[lv] }"
              ></span>
            </span>
            <span class="rc-prob-val">{{ probPercent(lv) }}%</span>
          </div>
        </div>
      </div>

      <p v-if="record.remark" class="rc-remark">备注：{{ record.remark }}</p>

      <div class="rc-foot">
        <span v-if="record.modelVersion" class="rc-version" :title="record.modelVersion">
          {{ record.modelVersion }}
        </span>
        <span v-if="record.createTime">{{ record.createTime }}</span>
      </div>

      <p v-if="record.reviewStatus === 'CONFIRMED'" class="rc-review rc-review-done">
        <AppIcon name="check" :size="12" />
        已由 {{ record.reviewer || '—' }} 于 {{ record.reviewTime || '—' }} 完成人工复核
        <template v-if="record.reviewRemark">：{{ record.reviewRemark }}</template>
      </p>
      <p v-else-if="record.needReview" class="rc-review rc-review-pending">
        <AppIcon name="clock" :size="12" />
        置信度低于 {{ record.reviewThreshold ?? 0.7 }}，建议人工复核后再出具结论
      </p>

      <p class="rc-disclaimer">
        <AppIcon name="info" :size="12" />
        AI 辅助筛查结果，仅供临床参考，最终诊断请以眼科医师意见为准。
      </p>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import type { ScreeningRecordVO } from '@/types/screening'
import {
  GENDER_OPTIONS,
  LEVEL_COLOR,
  LEVEL_LABEL,
  LEVEL_ORDER,
  SUGGESTION_COLOR
} from '@/types/screening'

const props = defineProps<{ record: ScreeningRecordVO }>()

const showCam = ref(false)

const previewList = computed(() => {
  const list: string[] = []
  if (props.record.imageUrl) list.push(props.record.imageUrl)
  if (props.record.gradCamUrl) list.push(props.record.gradCamUrl)
  return list
})

const levelColor = computed(() => LEVEL_COLOR[props.record.resultLevel || ''] || '#0891b2')
const levelColorMap = LEVEL_COLOR
const levelOrder = LEVEL_ORDER
const levelLabel = LEVEL_LABEL

const levelChipStyle = computed(() => {
  const hex = levelColor.value
  return {
    color: hex,
    background: hexToRgba(hex, 0.92)
  }
})

const suggestStyle = computed(() => {
  const hex = SUGGESTION_COLOR[props.record.suggestion || ''] || '#64748b'
  return {
    color: hex,
    background: hexToRgba(hex, 0.08),
    borderColor: hexToRgba(hex, 0.22)
  }
})

const suggestionHint = computed(() => {
  switch (props.record.suggestion) {
    case 'REVIEW':
      return '定期复查，暂无需临床干预'
    case 'CLINIC':
      return '建议眼科门诊就诊评估'
    case 'REFERRAL':
      return '建议尽快转诊至上级医院'
    default:
      return '请结合临床判断'
  }
})

const genderLabel = computed(() => {
  const g = GENDER_OPTIONS.find((o) => o.value === props.record.patientGender)
  return g ? g.label : props.record.patientGender || ''
})

const confidencePercent = computed(() => Math.round((props.record.confidence ?? 0) * 100))
const confidenceText = computed(() => `${((props.record.confidence ?? 0) * 100).toFixed(1)}%`)

function probPercent(lv: string): number {
  const v = props.record.probabilities?.[lv]
  if (v == null) return 0
  // 后端概率可能为 0~1 或已为百分比，统一归一
  const p = v > 1 ? v / 100 : v
  return Math.round(p * 100)
}

function hexToRgba(hex: string, alpha: number) {
  const h = hex.replace('#', '')
  const full = h.length === 3 ? h.split('').map((c) => c + c).join('') : h
  const r = parseInt(full.slice(0, 2), 16)
  const g = parseInt(full.slice(2, 4), 16)
  const b = parseInt(full.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
</script>

<style scoped>
.result-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===================== 影像区 ===================== */
.rc-image {
  position: relative;
  height: 200px;
  background: #0f172a;
}

.rc-img,
.rc-cam {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.rc-cam {
  position: absolute;
  inset: 0;
  opacity: 0.6;
  mix-blend-mode: screen;
  pointer-events: none;
}

.rc-img-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 100%;
  color: #94a3b8;
  font-size: 12px;
}

.rc-level-chip {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 11px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.6;
}

.rc-cam-toggle {
  position: absolute;
  left: 10px;
  bottom: 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.62);
  color: #e2e8f0;
  font-size: 11.5px;
  cursor: pointer;
}

/* ===================== 信息区 ===================== */
.rc-body {
  padding: 14px 16px 16px;
}

.rc-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.rc-patient {
  font-size: 15px;
  font-weight: 600;
  color: var(--drs-ink-900);
}

.rc-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: var(--drs-ink-500);
}

.rc-suggest {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 10px;
  padding: 9px 12px;
  border: 1px solid transparent;
  border-radius: var(--drs-radius-sm);
}

.rc-suggest :deep(svg) {
  margin-top: 2px;
  flex-shrink: 0;
}

.rc-suggest-text {
  display: flex;
  flex-direction: column;
  line-height: 1.5;
}

.rc-suggest-text b {
  font-size: 13px;
  font-weight: 600;
}

.rc-suggest-text em {
  font-style: normal;
  font-size: 11.5px;
  opacity: 0.85;
}

.rc-block {
  margin-top: 14px;
}

.rc-block-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  font-size: 12px;
  color: var(--drs-ink-500);
  margin-bottom: 6px;
}

.rc-block-head b {
  color: var(--drs-ink-900);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.rc-probs {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rc-prob-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.rc-prob-name {
  width: 62px;
  flex-shrink: 0;
  color: var(--drs-ink-500);
}

.rc-prob-name.is-hit {
  color: var(--drs-ink-900);
  font-weight: 600;
}

.rc-prob-bar {
  flex: 1;
  height: 7px;
  border-radius: 4px;
  background: var(--drs-ink-100);
  overflow: hidden;
}

.rc-prob-fill {
  display: block;
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s ease;
}

.rc-prob-val {
  width: 40px;
  text-align: right;
  color: var(--drs-ink-600);
  font-variant-numeric: tabular-nums;
}

.rc-remark {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--drs-ink-600);
  padding: 8px 10px;
  border-radius: var(--drs-radius-xs);
  background: var(--drs-surface-2);
}

.rc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--drs-border);
  font-size: 11px;
  color: var(--drs-ink-400);
}

.rc-version {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60%;
}

.rc-disclaimer {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin: 10px 0 0;
  font-size: 11px;
  line-height: 1.6;
  color: var(--drs-ink-400);
}

.rc-review {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin: 10px 0 0;
  padding: 7px 10px;
  border-radius: var(--drs-radius-xs);
  font-size: 11.5px;
  line-height: 1.6;
}

.rc-review :deep(svg) {
  margin-top: 2px;
  flex-shrink: 0;
}

.rc-review-pending {
  background: var(--drs-warn-bg);
  color: var(--drs-warn);
}

.rc-review-done {
  background: var(--drs-ok-bg);
  color: var(--drs-ok);
}

.rc-disclaimer :deep(svg) {
  margin-top: 3px;
  flex-shrink: 0;
}
</style>
