<template>
  <div class="drs-page">
    <PageHeader
      title="用户管理"
      subtitle="维护系统账号、角色与启用状态（仅管理员可访问）"
      :crumbs="['系统管理', '用户管理']"
    >
      <template #actions>
        <el-button v-permission="'admin:user:edit'" type="primary" @click="openCreate">
          <AppIcon name="users" :size="15" class="btn-ico" />新增用户
        </el-button>
      </template>
    </PageHeader>

    <section class="drs-card filter-card">
      <div class="drs-card-head">
        <h3>筛选条件</h3>
        <span class="drs-card-meta">按用户名、角色与状态检索</span>
      </div>
      <div class="drs-card-body">
        <el-form :model="query" label-position="top" @submit.prevent>
          <div class="filter-grid">
            <el-form-item label="用户名">
              <el-input v-model="query.username" placeholder="支持模糊匹配" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="角色">
              <el-select v-model="query.role" placeholder="全部角色" clearable>
                <el-option v-for="o in ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" placeholder="全部状态" clearable>
                <el-option v-for="o in USER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :loading="loading" @click="handleQuery">
                <AppIcon name="search" :size="15" class="btn-ico" />查询
              </el-button>
              <el-button @click="handleReset">
                <AppIcon name="refresh" :size="15" class="btn-ico" />重置
              </el-button>
            </div>
          </div>
        </el-form>
      </div>
    </section>

    <section class="drs-card table-card">
      <div class="drs-card-head">
        <h3>账号列表</h3>
        <span class="drs-card-meta">共 {{ total }} 个账号</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" row-key="id">
          <el-table-column prop="username" label="用户名" min-width="130">
            <template #default="{ row }">
              <span class="uname">
                <span class="uname-avatar" aria-hidden="true">{{ initial(row.username) }}</span>
                {{ row.username }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="姓名" min-width="110">
            <template #default="{ row }">{{ row.realName || '—' }}</template>
          </el-table-column>
          <el-table-column label="角色" width="110">
            <template #default="{ row }">
              <span class="chip" :class="row.role === 'ADMIN' ? 'chip-admin' : 'chip-doctor'">
                {{ roleLabel(row.role) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="手机号" min-width="130">
            <template #default="{ row }">{{ row.phone || '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="106">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 'ENABLED'"
                :disabled="!canEdit || row.username === userStore.username"
                inline-prompt
                active-text="启用"
                inactive-text="停用"
                @change="(v: string | number | boolean) => toggleState(row as UserVO, Boolean(v))"
              />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="160">
            <template #default="{ row }">{{ row.createTime || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button v-permission="'admin:user:edit'" link type="primary" @click="openEdit(row as UserVO)">
                编辑
              </el-button>
              <el-button
                v-permission="'admin:user:edit'"
                link
                type="danger"
                :disabled="row.username === userStore.username"
                @click="handleDelete(row as UserVO)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :image-size="80" description="没有符合条件的账号" />
          </template>
        </el-table>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <!-- ============ 新增 / 编辑 ============ -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑用户' : '新增用户'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="editing" placeholder="登录账号，创建后不可修改" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名（选填）" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" class="full">
            <el-option v-for="o in ROLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号（选填）" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="o in USER_STATUS_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="editing ? '重置密码' : '初始密码'" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="editing ? '留空则不修改密码' : '6-64 位'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  changeUserState,
  createUser,
  pageUsers,
  removeUser,
  updateUser
} from '@/api/admin'
import { useUserStore } from '@/stores/user'
import { ROLE_OPTIONS, USER_STATUS_OPTIONS, roleLabel } from '@/types/admin'
import type { UserVO } from '@/types/admin'

const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('admin:user:edit'))

const loading = ref(false)
const saving = ref(false)
const list = ref<UserVO[]>([])
const total = ref(0)

const query = reactive({
  username: '',
  role: '',
  status: '',
  current: 1,
  pageSize: 10
})

const dialogVisible = ref(false)
const editing = ref(false)
const editingId = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  realName: '',
  role: 'DOCTOR',
  phone: '',
  status: 'ENABLED',
  password: ''
})

const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [
    {
      validator: (_r, v: string, cb) => {
        if (!v || /^1[3-9]\d{9}$/.test(v)) cb()
        else cb(new Error('手机号格式不正确'))
      },
      trigger: 'blur'
    }
  ],
  password: [
    {
      validator: (_r, v: string, cb) => {
        if (editing.value && !v) return cb()
        if (!v) return cb(new Error('请输入初始密码'))
        if (v.length < 6 || v.length > 64) return cb(new Error('密码长度需为 6-64'))
        cb()
      },
      trigger: 'blur'
    }
  ]
}))

function initial(name?: string) {
  return name ? name.charAt(0).toUpperCase() : '?'
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageUsers({ ...query })
    list.value = res.list
    total.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  query.current = 1
  loadData()
}

function handleReset() {
  query.username = ''
  query.role = ''
  query.status = ''
  query.current = 1
  loadData()
}

function handleSizeChange() {
  query.current = 1
  loadData()
}

function resetForm() {
  form.username = ''
  form.realName = ''
  form.role = 'DOCTOR'
  form.phone = ''
  form.status = 'ENABLED'
  form.password = ''
}

function openCreate() {
  editing.value = false
  editingId.value = ''
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: UserVO) {
  editing.value = true
  editingId.value = row.id
  form.username = row.username
  form.realName = row.realName || ''
  form.role = row.role
  form.phone = row.phone || ''
  form.status = row.status || 'ENABLED'
  form.password = ''
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  saving.value = true
  try {
    if (editing.value) {
      await updateUser(editingId.value, {
        realName: form.realName || undefined,
        role: form.role,
        phone: form.phone || undefined,
        status: form.status,
        password: form.password || undefined
      })
      ElMessage.success('已保存')
    } else {
      await createUser({
        username: form.username,
        realName: form.realName || undefined,
        role: form.role,
        phone: form.phone || undefined,
        status: form.status,
        password: form.password
      })
      ElMessage.success('已新增用户')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleState(row: UserVO, enabled: boolean) {
  const next = enabled ? 'ENABLED' : 'DISABLED'
  try {
    await changeUserState(row.id, next)
    ElMessage.success(enabled ? '已启用' : '已停用')
    loadData()
  } catch (e) {
    ElMessage.error((e as Error).message || '操作失败')
    loadData()
  }
}

async function handleDelete(row: UserVO) {
  try {
    await ElMessageBox.confirm(`确认删除账号「${row.username}」？该操作为逻辑删除。`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await removeUser(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    ElMessage.error((e as Error).message || '删除失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.btn-ico {
  margin-right: 5px;
}

.filter-card {
  margin-bottom: var(--drs-gap);
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0 var(--drs-gap);
  align-items: end;
}

.filter-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-grid :deep(.el-select),
.filter-grid :deep(.el-input) {
  width: 100%;
}

.filter-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  padding-bottom: 1px;
}

.table-card {
  overflow: hidden;
}

.table-wrap {
  overflow-x: auto;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px var(--drs-gap-lg);
  border-top: 1px solid var(--drs-border);
}

.uname {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.uname-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.chip {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 500;
}

.chip-admin {
  color: var(--drs-violet);
  background: var(--drs-violet-bg);
  border-color: rgba(124, 58, 237, 0.22);
}

.chip-doctor {
  color: var(--drs-primary-700);
  background: var(--drs-primary-50);
  border-color: var(--drs-primary-100);
}

.full {
  width: 100%;
}
</style>
