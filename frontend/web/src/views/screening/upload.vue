<template>
  <div class="drs-page">
    <PageHeader
      title="筛查上传"
      subtitle="上传眼底照片，系统自动完成 AI 分级推理并生成可解释热力图"
      :crumbs="['筛查业务', '筛查上传']"
    >
      <template #actions>
        <el-button :disabled="submitting" @click="resetAll">
          <AppIcon name="refresh" :size="15" class="btn-ico" />清空
        </el-button>
        <el-button type="primary" :loading="submitting" :disabled="submitting" @click="handleSubmit">
          <AppIcon name="scan" :size="15" class="btn-ico" />开始筛查
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
              :show-file-list="true"
            >
              <div class="upload-inner">
                <AppIcon name="image" :size="30" />
                <span class="upload-text">拖拽眼底图到此处，或<em>点击选择</em></span>
                <span class="upload-hint">可一次上传多张，逐张自动分级</span>
              </div>
            </el-upload>

            <p class="upload-note">
              <AppIcon name="info" :size="13" />
              影像仅用于本次筛查推理，存储于私有对象桶，链接 30 分钟内有效。
            </p>
          </div>
        </section>
      </div>

      <!-- ============ 右：结果 ============ -->
      <div class="col-right">
        <div class="result-head">
          <h3>筛查结果</h3>
          <span v-if="results.length" class="drs-card-meta">本次共 {{ results.length }} 条</span>
        </div>

        <div v-if="!results.length" class="drs-card result-empty">
          <el-empty :image-size="96" description="上传并筛查后，结果将在此展示" />
          <ol class="flow">
            <li><b>1</b> 填写患者信息（可留空）</li>
            <li><b>2</b> 选择一张或多张眼底影像</li>
            <li><b>3</b> 点击「开始筛查」获取分级与热力图</li>
          </ol>
        </div>

        <div v-else class="result-grid">
          <ResultCard v-for="r in results" :key="r.id" :record="r" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadInstance, UploadUserFile } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import ResultCard from '@/components/ResultCard.vue'
import { uploadScreening } from '@/api/screening'
import { GENDER_OPTIONS } from '@/types/screening'
import type { ScreeningRecordVO } from '@/types/screening'

const genderOptions = GENDER_OPTIONS
const uploadRef = ref<UploadInstance>()
const submitting = ref(false)
const results = ref<ScreeningRecordVO[]>([])

const form = reactive({
  patientName: '',
  patientAge: undefined as number | undefined,
  patientGender: '',
  remark: ''
})

const fileList = computed<UploadUserFile[]>(() => {
  const inst = uploadRef.value as unknown as { uploadFiles?: UploadUserFile[] } | undefined
  return inst?.uploadFiles ?? []
})

function gatherFiles(): File[] {
  return fileList.value.map((f) => f.raw as File | undefined).filter((f): f is File => !!f)
}

async function handleSubmit() {
  const files = gatherFiles()
  if (!files.length) {
    ElMessage.warning('请至少上传一张眼底图片')
    return
  }
  submitting.value = true
  try {
    const data = await uploadScreening({
      files,
      patientName: form.patientName || undefined,
      patientAge: form.patientAge,
      patientGender: form.patientGender || undefined,
      remark: form.remark || undefined
    })
    results.value = data
    ElMessage.success(`筛查完成，共 ${data.length} 条记录`)
    uploadRef.value?.clearFiles()
  } catch (e) {
    ElMessage.error((e as Error).message || '筛查失败，请重试')
  } finally {
    submitting.value = false
  }
}

function resetAll() {
  form.patientName = ''
  form.patientAge = undefined
  form.patientGender = ''
  form.remark = ''
  uploadRef.value?.clearFiles()
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
.result-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: var(--drs-gap-sm);
}

.result-head h3 {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
  color: var(--drs-ink-800);
}

.result-empty {
  padding: 24px;
}

.flow {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 360px;
  margin-inline: auto;
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
