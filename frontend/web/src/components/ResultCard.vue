<template>
  <div class="result-card drs-card">
    <div class="rc-image">
      <div class="rc-image-box">
        <el-image
          v-if="record.imageUrl"
          :src="record.imageUrl"
          fit="contain"
          :preview-src-list="previewList"
          :initial-index="0"
          class="rc-img"
        >
          <template #error>
            <div class="rc-img-fallback">图像不可用</div>
          </template>
        </el-image>
        <img
          v-if="showCam && record.gradCamUrl"
          :src="record.gradCamUrl"
          class="rc-cam"
          alt="热力图"
        />
      </div>
      <div v-if="record.gradCamUrl" class="rc-cam-switch">
        <el-switch v-model="showCam" size="small" />
        <span>热力图叠加</span>
      </div>
      <el-tag v-if="record.levelName" :color="levelColor" class="rc-level" effect="dark">
        {{ record.levelName }}
      </el-tag>
    </div>

    <div class="rc-body">
      <div class="rc-head">
        <span class="rc-patient">{{ record.patientName || '未登记患者' }}</span>
        <el-tag v-if="record.suggestionName" :color="suggestionColor" effect="dark" class="rc-suggest">
          {{ record.suggestionName }}
        </el-tag>
      </div>

      <div class="rc-meta">
        <span v-if="record.patientGender">{{ genderLabel }}</span>
        <span v-if="record.patientAge != null">{{ record.patientAge }} 岁</span>
      </div>

      <div class="rc-confidence">
        <div class="rc-confidence-label">
          <span>置信度</span>
          <strong>{{ confidenceText }}</strong>
        </div>
        <el-progress
          :percentage="confidencePercent"
          :stroke-width="8"
          :color="levelColor"
          :show-text="false"
        />
      </div>

      <div class="rc-probs">
        <div v-for="lv in levelOrder" :key="lv" class="rc-prob-row">
          <span class="rc-prob-name">{{ levelLabel[lv] }}</span>
          <div class="rc-prob-bar">
            <div
              class="rc-prob-fill"
              :style="{ width: probPercent(lv) + '%', background: levelColorMap[lv] }"
            ></div>
          </div>
          <span class="rc-prob-val">{{ probPercent(lv) }}%</span>
        </div>
      </div>

      <div class="rc-foot">
        <span v-if="record.modelVersion">模型 {{ record.modelVersion }}</span>
        <span v-if="record.createTime">{{ record.createTime }}</span>
      </div>
      <div v-if="record.remark" class="rc-remark">备注：{{ record.remark }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ScreeningRecordVO } from '@/types/screening'
import {
  GENDER_OPTIONS,
  LEVEL_COLOR,
  LEVEL_LABEL,
  LEVEL_ORDER,
  SUGGESTION_COLOR,
  SUGGESTION_LABEL
} from '@/types/screening'

const props = defineProps<{ record: ScreeningRecordVO }>()

const showCam = ref(false)

const previewList = computed(() => {
  const list: string[] = []
  if (props.record.imageUrl) list.push(props.record.imageUrl)
  if (props.record.gradCamUrl) list.push(props.record.gradCamUrl)
  return list
})

const levelColor = computed(() => LEVEL_COLOR[props.record.resultLevel || ''] || '#0891B2')
const suggestionColor = computed(
  () => SUGGESTION_COLOR[props.record.suggestion || ''] || '#64748B'
)
const levelColorMap = LEVEL_COLOR
const levelOrder = LEVEL_ORDER
const levelLabel = LEVEL_LABEL

const genderLabel = computed(() => {
  const g = GENDER_OPTIONS.find((o) => o.value === props.record.patientGender)
  return g ? g.label : props.record.patientGender || ''
})

const confidencePercent = computed(() => Math.round((props.record.confidence ?? 0) * 100))
const confidenceText = computed(() => `${(props.record.confidence ?? 0) * 100}%`)

function probPercent(lv: string): number {
  const v = props.record.probabilities?.[lv]
  if (v == null) return 0
  // 后端概率可能为 0~1 或已为百分比，统一归一
  const p = v > 1 ? v / 100 : v
  return Math.round(p * 100)
}
</script>

<style scoped>
.result-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.rc-image {
  position: relative;
  background: #0f172a;
  height: 200px;
}

.rc-image-box {
  position: relative;
  width: 100%;
  height: 100%;
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
  color: #94a3b8;
  font-size: 12px;
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
}

.rc-cam-switch {
  position: absolute;
  left: 8px;
  bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(15, 23, 42, 0.55);
  color: #fff;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 6px;
}

.rc-level {
  position: absolute;
  right: 8px;
  top: 8px;
  border: none;
  font-weight: 600;
}

.rc-body {
  padding: 14px 16px;
}

.rc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.rc-patient {
  font-size: 15px;
  font-weight: 600;
}

.rc-suggest {
  border: none;
  font-weight: 600;
}

.rc-meta {
  display: flex;
  gap: 12px;
  color: var(--drs-text-soft);
  font-size: 12px;
  margin-top: 4px;
}

.rc-confidence {
  margin-top: 12px;
}

.rc-confidence-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--drs-text-soft);
  margin-bottom: 4px;
}

.rc-confidence-label strong {
  color: var(--drs-text);
}

.rc-probs {
  margin-top: 12px;
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
  width: 48px;
  color: var(--drs-text-soft);
  flex-shrink: 0;
}

.rc-prob-bar {
  flex: 1;
  height: 8px;
  background: #eef2f7;
  border-radius: 4px;
  overflow: hidden;
}

.rc-prob-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s ease;
}

.rc-prob-val {
  width: 38px;
  text-align: right;
  color: var(--drs-text);
}

.rc-foot {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--drs-text-soft);
}

.rc-remark {
  margin-top: 6px;
  font-size: 12px;
  color: var(--drs-text-soft);
}
</style>
