<template>
  <div class="login-page">
    <!-- ============ 左：品牌区 ============ -->
    <aside class="brand-panel">
      <div class="brand-top">
        <span class="brand-mark" aria-hidden="true"><AppIcon name="scan" :size="26" /></span>
        <div>
          <div class="brand-title">DR 智能筛查系统</div>
          <div class="brand-sub">Diabetic Retinopathy Screening</div>
        </div>
      </div>

      <div class="brand-body">
        <h1>眼底一张图<br />完成 DR 分级与转诊建议</h1>
        <ul class="feature-list">
          <li v-for="f in features" :key="f.title">
            <span class="feature-ico" aria-hidden="true"><AppIcon :name="f.icon" :size="16" /></span>
            <span>
              <b>{{ f.title }}</b>
              <em>{{ f.desc }}</em>
            </span>
          </li>
        </ul>
      </div>

      <div class="brand-foot">
        <AppIcon name="lock" :size="13" />
        会话令牌由服务端 Redis 管理，30 分钟无操作自动失效
      </div>
    </aside>

    <!-- ============ 右：登录表单 ============ -->
    <main class="form-panel">
      <div class="form-card">
        <h2>账号登录</h2>
        <p class="form-sub">请使用系统分配的账号登录</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="请输入用户名"
              autocomplete="username"
              @keyup.enter="onSubmit"
            >
              <template #prefix><AppIcon name="user" :size="16" /></template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="onSubmit"
            >
              <template #prefix><AppIcon name="lock" :size="16" /></template>
            </el-input>
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            native-type="submit"
            @click="onSubmit"
          >
            {{ loading ? '登录中…' : '登录' }}
          </el-button>
        </el-form>

        <div class="demo-box">
          <div class="demo-title">
            <AppIcon name="info" :size="13" />
            演示账号（点击自动填入）
          </div>
          <div class="demo-list">
            <button
              v-for="d in demoAccounts"
              :key="d.username"
              type="button"
              class="demo-item"
              @click="fill(d)"
            >
              <span class="demo-role">{{ d.role }}</span>
              <span class="demo-user">{{ d.username }}</span>
              <span class="demo-scope">{{ d.scope }}</span>
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const features = [
  { icon: 'scan', title: 'AI 自动分级', desc: 'MobileNetV3 五分类，输出各分级概率' },
  { icon: 'eye', title: '可解释热力图', desc: 'Grad-CAM 标注模型关注区域' },
  { icon: 'shield', title: '数据权限隔离', desc: '医生仅见本人记录，管理员可查全量' }
]

const demoAccounts = [
  { role: '管理员', username: 'admin', password: 'admin123', scope: '全部数据' },
  { role: '医生', username: 'doctor', password: 'doctor123', scope: '仅本人数据' }
]

function fill(d: { username: string; password: string }) {
  form.username = d.username
  form.password = d.password
}

async function onSubmit() {
  if (!formRef.value) return
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    ElMessage.error((e as Error).message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  height: 100vh;
  background: var(--drs-bg);
}

/* ===================== 品牌区 ===================== */
.brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px 48px;
  color: #e6f7fb;
  background: linear-gradient(150deg, #0e7490 0%, #0891b2 52%, #155e75 100%);
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  flex-shrink: 0;
}

.brand-title {
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.3px;
}

.brand-sub {
  font-size: 12px;
  opacity: 0.8;
}

.brand-body h1 {
  font-size: 30px;
  line-height: 1.4;
  font-weight: 600;
  margin: 0 0 28px;
  color: #fff;
  letter-spacing: 0.5px;
}

.feature-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.feature-list li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.feature-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.feature-list b {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.feature-list em {
  font-style: normal;
  font-size: 12.5px;
  opacity: 0.82;
}

.brand-foot {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  opacity: 0.8;
}

/* ===================== 表单区 ===================== */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  overflow-y: auto;
}

.form-card {
  width: 100%;
  max-width: 380px;
}

.form-card h2 {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--drs-ink-900);
}

.form-sub {
  font-size: 13px;
  color: var(--drs-ink-500);
  margin: 0 0 24px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

/* ---------- 演示账号 ---------- */
.demo-box {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px dashed var(--drs-border);
}

.demo-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--drs-ink-500);
  margin-bottom: 10px;
}

.demo-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.demo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--drs-border);
  border-radius: var(--drs-radius-sm);
  background: var(--drs-surface);
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.demo-item:hover {
  border-color: var(--drs-primary-200);
  background: var(--drs-primary-50);
}

.demo-role {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
  font-size: 11.5px;
  font-weight: 500;
}

.demo-user {
  font-size: 13px;
  font-weight: 500;
  color: var(--drs-ink-800);
}

.demo-scope {
  margin-left: auto;
  font-size: 11.5px;
  color: var(--drs-ink-500);
}

/* ===================== 响应式 ===================== */
@media (max-width: 900px) {
  .login-page {
    grid-template-columns: minmax(0, 1fr);
  }

  .brand-panel {
    display: none;
  }
}
</style>
