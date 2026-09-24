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
        <h1>注册筛查账号<br />即刻开始眼底筛查</h1>
        <ul class="feature-list">
          <li v-for="f in notices" :key="f.title">
            <span class="feature-ico" aria-hidden="true"><AppIcon :name="f.icon" :size="16" /></span>
            <span>
              <b>{{ f.title }}</b>
              <em>{{ f.desc }}</em>
            </span>
          </li>
        </ul>
      </div>

      <div class="brand-foot">
        <AppIcon name="shield" :size="13" />
        注册即表示同意账号仅用于本单位眼底筛查业务
      </div>
    </aside>

    <!-- ============ 右：注册表单 ============ -->
    <main class="form-panel">
      <div class="form-card">
        <h2>注册账号</h2>
        <p class="form-sub">注册后获得医生角色，仅可查看本人上传的筛查记录</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="onSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="4-20 位，字母开头"
              autocomplete="username"
            >
              <template #prefix><AppIcon name="user" :size="16" /></template>
            </el-input>
          </el-form-item>

          <el-form-item label="真实姓名" prop="realName">
            <el-input
              v-model="form.realName"
              size="large"
              maxlength="20"
              placeholder="用于筛查记录与诊断报告署名"
              autocomplete="name"
            />
          </el-form-item>

          <el-form-item label="手机号（选填）" prop="phone">
            <el-input
              v-model="form.phone"
              size="large"
              maxlength="11"
              placeholder="用于联系与账号找回"
              autocomplete="tel"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              show-password
              placeholder="8-64 位，需含字母与数字"
              autocomplete="new-password"
            >
              <template #prefix><AppIcon name="lock" :size="16" /></template>
            </el-input>
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              size="large"
              show-password
              placeholder="请再次输入密码"
              autocomplete="new-password"
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
            {{ loading ? '注册中…' : '注册' }}
          </el-button>
        </el-form>

        <div class="alt-entry">
          已有账号？
          <router-link to="/login">返回登录</router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import { register } from '@/api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

/** 与后端 RegisterDTO 的校验规则保持一致，避免提交后才报错 */
const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_]{3,19}$/,
      message: '需为 4-20 位，以字母开头，仅可含字母、数字与下划线',
      trigger: 'blur'
    }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { max: 20, message: '最长 20 个字符', trigger: 'blur' }
  ],
  phone: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value) return callback()
        if (/^1[3-9]\d{9}$/.test(value)) return callback()
        callback(new Error('请输入正确的手机号'))
      },
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{8,64}$/,
      message: '需为 8-64 位，且同时包含字母与数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value === form.password) return callback()
        callback(new Error('两次输入的密码不一致'))
      },
      trigger: 'blur'
    }
  ]
}

const notices = [
  { icon: 'user', title: '医生角色', desc: '注册即获得医生角色，数据范围仅限本人记录' },
  { icon: 'lock', title: '密码要求', desc: '8-64 位，需同时包含字母与数字' },
  { icon: 'check', title: '即时可用', desc: '无需审核，注册完成后即可登录使用' }
]

async function onSubmit() {
  if (!formRef.value) return
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  loading.value = true
  try {
    const res = await register({
      username: form.username.trim(),
      realName: form.realName.trim(),
      phone: form.phone.trim() || undefined,
      password: form.password
    })
    await ElMessageBox.alert(
      `账号「${res.username}」注册成功，请使用该账号登录。`,
      '注册成功',
      { confirmButtonText: '前往登录', type: 'success' }
    ).catch(() => undefined)
    router.push({ path: '/login', query: { username: res.username } })
  } catch (e) {
    ElMessage.error((e as Error).message || '注册失败，请稍后重试')
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

/* ---------- 返回登录 ---------- */
.alt-entry {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px dashed var(--drs-border);
  font-size: 13px;
  color: var(--drs-ink-500);
  text-align: center;
}

.alt-entry a {
  color: var(--drs-primary);
  font-weight: 600;
  text-decoration: none;
}

.alt-entry a:hover {
  text-decoration: underline;
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
