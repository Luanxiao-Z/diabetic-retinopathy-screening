<template>
  <div class="drs-page">
    <PageHeader
      title="字典管理"
      subtitle="维护系统字典域与字典项（仅管理员可访问）"
      :crumbs="['系统管理', '字典管理']"
    >
      <template #actions>
        <el-button v-permission="'admin:dict:edit'" type="primary" @click="openDomainCreate">
          <AppIcon name="layers" :size="15" class="btn-ico" />新增字典域
        </el-button>
      </template>
    </PageHeader>

    <div class="dict-grid">
      <!-- ============ 左：字典域 ============ -->
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>字典域</h3>
          <span class="drs-card-meta">共 {{ domains.length }} 个</span>
        </div>
        <div v-loading="loadingDomains" class="domain-list">
          <button
            v-for="d in domains"
            :key="d.domainCode"
            type="button"
            class="domain-item"
            :class="{ 'is-active': d.domainCode === activeDomain }"
            @click="selectDomain(d.domainCode)"
          >
            <span class="domain-main">
              <span class="domain-name">{{ d.domainName }}</span>
              <code class="domain-code">{{ d.domainCode }}</code>
            </span>
            <span v-if="canEdit" class="domain-ops" @click.stop>
              <button type="button" class="mini-btn" title="编辑字典域" @click="openDomainEdit(d)">
                <AppIcon name="filter" :size="13" />
              </button>
              <button type="button" class="mini-btn mini-danger" title="删除字典域" @click="handleDomainDelete(d)">
                <AppIcon name="trash" :size="13" />
              </button>
            </span>
          </button>
          <el-empty v-if="!domains.length && !loadingDomains" :image-size="64" description="暂无字典域" />
        </div>
      </section>

      <!-- ============ 右：字典项 ============ -->
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>{{ activeDomainName || '字典项' }}</h3>
          <span class="drs-card-meta">
            <template v-if="activeDomain">共 {{ items.length }} 项</template>
            <template v-else>请先选择左侧字典域</template>
          </span>
        </div>

        <div v-if="!activeDomain" class="drs-card-body">
          <el-empty :image-size="80" description="请从左侧选择一个字典域" />
        </div>

        <template v-else>
          <div class="items-toolbar">
            <el-button
              v-permission="'admin:dict:edit'"
              type="primary"
              size="small"
              @click="openItemCreate"
            >
              <AppIcon name="layers" :size="14" class="btn-ico" />新增字典项
            </el-button>
            <el-button size="small" @click="loadItems">
              <AppIcon name="refresh" :size="14" class="btn-ico" />刷新
            </el-button>
          </div>

          <div class="table-wrap">
            <el-table v-loading="loadingItems" :data="items" row-key="itemCode" border>
              <el-table-column prop="itemCode" label="编码" min-width="140" />
              <el-table-column prop="itemName" label="名称" min-width="150" />
              <el-table-column label="扩展值" min-width="110">
                <template #default="{ row }">{{ row.itemValue || '—' }}</template>
              </el-table-column>
              <el-table-column label="排序" min-width="80">
                <template #default="{ row }">{{ row.sort ?? '—' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="130" fixed="right">
                <template #default="{ row }">
                  <el-button v-permission="'admin:dict:edit'" link type="primary" @click="openItemEdit(row as DictItemVO)">
                    编辑
                  </el-button>
                  <el-button
                    v-permission="'admin:dict:edit'"
                    link
                    type="danger"
                    @click="handleItemDelete(row as DictItemVO)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty :image-size="70" description="该字典域下暂无字典项" />
              </template>
            </el-table>
          </div>
        </template>
      </section>
    </div>

    <!-- ============ 字典域表单 ============ -->
    <el-dialog
      v-model="domainDialog"
      :title="domainEditing ? '编辑字典域' : '新增字典域'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="domainFormRef" :model="domainForm" :rules="domainRules" label-width="88px">
        <el-form-item label="编码" prop="domainCode">
          <el-input
            v-model="domainForm.domainCode"
            :disabled="domainEditing"
            placeholder="仅字母、数字、下划线，如 B_DR_LEVEL"
          />
        </el-form-item>
        <el-form-item label="名称" prop="domainName">
          <el-input v-model="domainForm.domainName" placeholder="如 DR 分级" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="domainForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="domainDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDomain">保存</el-button>
      </template>
    </el-dialog>

    <!-- ============ 字典项表单 ============ -->
    <el-dialog
      v-model="itemDialog"
      :title="itemEditing ? '编辑字典项' : '新增字典项'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="88px">
        <el-form-item label="所属字典域">
          <el-input :model-value="activeDomain" disabled />
        </el-form-item>
        <el-form-item label="编码" prop="itemCode">
          <el-input
            v-model="itemForm.itemCode"
            :disabled="itemEditing"
            placeholder="仅字母、数字、下划线，如 LEVEL_2"
          />
        </el-form-item>
        <el-form-item label="名称" prop="itemName">
          <el-input v-model="itemForm.itemName" placeholder="如 中度 NPDR" />
        </el-form-item>
        <el-form-item label="扩展值">
          <el-input v-model="itemForm.itemValue" placeholder="选填，如 2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort" :min="0" :max="9999" :controls="false" class="full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveItem">保存</el-button>
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
  createDictDomain,
  createDictItem,
  listDictDomains,
  listDictItems,
  removeDictDomain,
  removeDictItem,
  updateDictDomain,
  updateDictItem
} from '@/api/admin'
import { useUserStore } from '@/stores/user'
import type { DictDomainVO, DictItemVO } from '@/types/admin'

const userStore = useUserStore()
const canEdit = computed(() => userStore.permissions.includes('admin:dict:edit'))

const CODE_PATTERN = /^[A-Za-z0-9_]+$/

const loadingDomains = ref(false)
const loadingItems = ref(false)
const saving = ref(false)
const domains = ref<DictDomainVO[]>([])
const items = ref<DictItemVO[]>([])
const activeDomain = ref('')

const activeDomainName = computed(
  () => domains.value.find((d) => d.domainCode === activeDomain.value)?.domainName || ''
)

/* ---------------- 字典域 ---------------- */
const domainDialog = ref(false)
const domainEditing = ref(false)
const domainFormRef = ref<FormInstance>()
const domainForm = reactive({ domainCode: '', domainName: '', remark: '' })

const domainRules = computed<FormRules>(() => ({
  domainCode: [
    { required: true, message: '请输入字典域编码', trigger: 'blur' },
    {
      validator: (_r, v: string, cb) =>
        !v || CODE_PATTERN.test(v) ? cb() : cb(new Error('编码仅限字母、数字、下划线')),
      trigger: 'blur'
    }
  ],
  domainName: [{ required: true, message: '请输入字典域名称', trigger: 'blur' }]
}))

async function loadDomains() {
  loadingDomains.value = true
  try {
    domains.value = await listDictDomains()
    if (!activeDomain.value && domains.value.length) {
      activeDomain.value = domains.value[0].domainCode
      await loadItems()
    } else if (activeDomain.value && !domains.value.some((d) => d.domainCode === activeDomain.value)) {
      activeDomain.value = ''
      items.value = []
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '字典域加载失败')
  } finally {
    loadingDomains.value = false
  }
}

async function selectDomain(code: string) {
  activeDomain.value = code
  await loadItems()
}

function openDomainCreate() {
  domainEditing.value = false
  domainForm.domainCode = ''
  domainForm.domainName = ''
  domainForm.remark = ''
  domainDialog.value = true
}

function openDomainEdit(d: DictDomainVO) {
  domainEditing.value = true
  domainForm.domainCode = d.domainCode
  domainForm.domainName = d.domainName
  domainForm.remark = d.remark || ''
  domainDialog.value = true
}

async function saveDomain() {
  if (!domainFormRef.value) return
  const ok = await domainFormRef.value.validate().catch(() => false)
  if (!ok) return
  saving.value = true
  try {
    if (domainEditing.value) {
      await updateDictDomain(domainForm.domainCode, {
        domainName: domainForm.domainName,
        remark: domainForm.remark || undefined
      })
      ElMessage.success('已保存')
    } else {
      await createDictDomain({
        domainCode: domainForm.domainCode,
        domainName: domainForm.domainName,
        remark: domainForm.remark || undefined
      })
      ElMessage.success('已新增字典域')
      activeDomain.value = domainForm.domainCode
    }
    domainDialog.value = false
    await loadDomains()
    if (activeDomain.value) await loadItems()
  } catch (e) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDomainDelete(d: DictDomainVO) {
  try {
    await ElMessageBox.confirm(
      `确认删除字典域「${d.domainName}」？其下所有字典项将被级联删除，且不可恢复。`,
      '高风险操作',
      { type: 'warning', confirmButtonText: '确认删除' }
    )
  } catch {
    return
  }
  try {
    await removeDictDomain(d.domainCode)
    ElMessage.success('已删除')
    if (activeDomain.value === d.domainCode) {
      activeDomain.value = ''
      items.value = []
    }
    await loadDomains()
  } catch (e) {
    ElMessage.error((e as Error).message || '删除失败')
  }
}

/* ---------------- 字典项 ---------------- */
const itemDialog = ref(false)
const itemEditing = ref(false)
const itemFormRef = ref<FormInstance>()
const itemForm = reactive({ itemCode: '', itemName: '', itemValue: '', sort: 1 })

const itemRules = computed<FormRules>(() => ({
  itemCode: [
    { required: true, message: '请输入字典项编码', trigger: 'blur' },
    {
      validator: (_r, v: string, cb) =>
        !v || CODE_PATTERN.test(v) ? cb() : cb(new Error('编码仅限字母、数字、下划线')),
      trigger: 'blur'
    }
  ],
  itemName: [{ required: true, message: '请输入字典项名称', trigger: 'blur' }]
}))

async function loadItems() {
  if (!activeDomain.value) return
  loadingItems.value = true
  try {
    items.value = await listDictItems(activeDomain.value)
  } catch (e) {
    ElMessage.error((e as Error).message || '字典项加载失败')
  } finally {
    loadingItems.value = false
  }
}

function openItemCreate() {
  itemEditing.value = false
  itemForm.itemCode = ''
  itemForm.itemName = ''
  itemForm.itemValue = ''
  itemForm.sort = items.value.length + 1
  itemDialog.value = true
}

function openItemEdit(row: DictItemVO) {
  itemEditing.value = true
  itemForm.itemCode = row.itemCode
  itemForm.itemName = row.itemName
  itemForm.itemValue = row.itemValue || ''
  itemForm.sort = row.sort ?? 1
  itemDialog.value = true
}

async function saveItem() {
  if (!itemFormRef.value || !activeDomain.value) return
  const ok = await itemFormRef.value.validate().catch(() => false)
  if (!ok) return
  saving.value = true
  try {
    if (itemEditing.value) {
      await updateDictItem(activeDomain.value, itemForm.itemCode, {
        itemName: itemForm.itemName,
        itemValue: itemForm.itemValue || undefined,
        sort: itemForm.sort
      })
      ElMessage.success('已保存')
    } else {
      await createDictItem(activeDomain.value, {
        itemCode: itemForm.itemCode,
        itemName: itemForm.itemName,
        itemValue: itemForm.itemValue || undefined,
        sort: itemForm.sort
      })
      ElMessage.success('已新增字典项')
    }
    itemDialog.value = false
    await loadItems()
  } catch (e) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleItemDelete(row: DictItemVO) {
  try {
    await ElMessageBox.confirm(`确认删除字典项「${row.itemName}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await removeDictItem(activeDomain.value, row.itemCode)
    ElMessage.success('已删除')
    await loadItems()
  } catch (e) {
    ElMessage.error((e as Error).message || '删除失败')
  }
}

onMounted(loadDomains)
</script>

<style scoped>
.btn-ico {
  margin-right: 5px;
}

.dict-grid {
  display: grid;
  grid-template-columns: minmax(240px, 300px) minmax(0, 1fr);
  gap: var(--drs-gap);
  /* 左右两张卡片等高，底部对齐 */
  align-items: stretch;
}

.dict-grid > .drs-card {
  display: flex;
  flex-direction: column;
}

.dict-grid > .drs-card > .domain-list,
.dict-grid > .drs-card > .table-wrap {
  flex: 1;
}

/* ---------- 字典域列表 ---------- */
.domain-list {
  padding: 10px;
  max-height: 620px;
  overflow-y: auto;
}

.domain-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  margin-bottom: 4px;
  border: 1px solid transparent;
  border-radius: var(--drs-radius-sm);
  background: none;
  cursor: pointer;
  text-align: left;
  font: inherit;
  transition: background-color 0.16s ease, border-color 0.16s ease;
}

.domain-item:hover {
  background: var(--drs-ink-50);
}

.domain-item.is-active {
  background: var(--drs-primary-50);
  border-color: var(--drs-primary-100);
}

.domain-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.domain-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--drs-ink-800);
}

.domain-item.is-active .domain-name {
  color: var(--drs-primary-800);
}

.domain-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: var(--drs-ink-400);
}

.domain-ops {
  display: inline-flex;
  gap: 2px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.domain-item:hover .domain-ops,
.domain-item.is-active .domain-ops {
  opacity: 1;
}

.mini-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: var(--drs-radius-xs);
  background: none;
  color: var(--drs-ink-500);
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease;
}

.mini-btn:hover {
  background: var(--drs-surface);
  color: var(--drs-primary);
}

.mini-danger:hover {
  color: var(--drs-danger);
}

/* ---------- 字典项 ---------- */
.items-toolbar {
  display: flex;
  gap: 8px;
  padding: 12px var(--drs-gap-lg) 0;
}

.table-wrap {
  overflow-x: auto;
  padding: 12px var(--drs-gap-lg) var(--drs-gap-lg);
}

.full {
  width: 100%;
}

@media (max-width: 1024px) {
  .dict-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
