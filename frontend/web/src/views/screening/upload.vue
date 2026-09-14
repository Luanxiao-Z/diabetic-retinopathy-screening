<template>
  <div class="drs-page">
    <h1 class="drs-page-title">筛查上传</h1>
    <p class="drs-page-subtitle">上传眼底照片，系统自动完成 AI 分级推理并生成可解释热力图</p>

    <div class="upload-cols">
      <div class="drs-card upload-left">
        <div class="panel-title">患者信息与影像</div>
        <el-form :model="form" label-width="80px" label-position="right">
          <el-form-item label="患者姓名">
            <el-input v-model="form.patientName" placeholder="选填，留空记为未登记" clearable />
          </el-form-item>
          <el-form-item label="患者年龄">
            <el-input-number v-model="form.patientAge" :min="0" :max="120" :controls="false" placeholder="选填" style="width: 140px" />
          </el-form-item>
          <el-form-item label="患者性别">
            <el-select v-model="form.patientGender" placeholder="选填" clearable style="width: 160px">
              <el-option v-for="g in genderOptions" :key="g.value" :label="g.label" :value="g.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
          </el-form-item>
        </el-form>

        <el-upload
          ref="uploadRef"
          class="fundus-upload"
          drag
          multiple
          accept="image/*"
          :auto-upload="false"
          :show-file-list="true"
          list-type="picture-card"
        >
          <div class="upload-inner">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="currentColor"><path d="M12 3l-4 4h3v6h2V7h3l-4-4zM5 19h14v2H5z" /></svg>
            <div class="upload-text">拖拽眼底图到此处，或<em>点击上传</em></div>
            <div class="upload-hint">支持多张；单张 jpg/png，自动推理分级</div>
          </div>
        </el-upload>

        <div class="upload-actions">
          <el-button type="primary" :loading="submitting" :disabled="submitting" @click="handleSubmit">
            开始筛查
          </el-button>
          <el-button :disabled="submitting" @click="resetAll">清空</el-button>
        </div>
      </div>

      <div class="upload-right">
        <div class="panel-title result-title">
          筛查结果
          <span v-if="results.length" class="result-count">共 {{ results.length }} 条</span>
        </div>
        <div v-if="!results.length" class="result-empty drs-card">
          <el-empty description="上传并筛查后，结果将在此展示" />
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
  return fileList.value
    .map((f) => f.raw as File | undefined)
    .filter((f): f is File => !!f)
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
.upload-cols {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 16px;
  align-items: start;
}

.upload-left {
  padding: 18px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 14px;
}

.fundus-upload {
  margin-top: 6px;
}

.fundus-upload :deep(.el-upload-dragger) {
  padding: 18px;
  width: 100%;
}

.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--drs-primary);
}

.upload-text {
  font-size: 13px;
  color: var(--drs-text);
}

.upload-text em {
  color: var(--drs-primary);
  font-style: normal;
  font-weight: 600;
}

.upload-hint {
  font-size: 11px;
  color: var(--drs-text-soft);
}

.upload-actions {
  margin-top: 14px;
  display: flex;
  gap: 10px;
}

.result-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.result-count {
  font-size: 12px;
  color: var(--drs-text-soft);
  font-weight: 400;
}

.result-empty {
  padding: 40px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
</style>
