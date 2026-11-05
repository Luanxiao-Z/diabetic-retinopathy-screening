# 前端PC端应用开发规范

## 1. 项目概述

### 1.1 技术栈

- **框架**: Vue 3.x + TypeScript
- **构建工具**: Vite 6.x
- **UI 库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4.x
- **HTTP 客户端**: Axios
- **样式**: 原生 CSS + CSS 变量（设计令牌集中在 `src/styles/theme.css`）
- **图标**: 自封装 `AppIcon` 组件，不使用 emoji
- **图表**: ECharts
- **代码规范**: ESLint + Prettier + Stylelint
- **包管理器**: Yarn

### 1.2 项目结构

目录结构定义参见[前端PC端应用目录规划](前端PC端应用目录规划.md)。

## 2. 开发环境配置

### 2.1 版本要求

- Node.js: 推荐 22（Vite 6 要求 `^20.19.0 || >=22.12.0`）
- 包管理器: Yarn 1.22，不混用 npm，避免 lock 文件冲突
- 推荐使用 `nvm` 或 `fnm` 管理 Node.js 版本

### 2.2 开发命令

```bash
# 安装依赖
yarn install

# 启动开发服务器（http://localhost:5173，/api 代理至后端 8080）
yarn dev

# 构建生产包（内含 vue-tsc --noEmit 类型检查）
yarn build

# 预览构建产物
yarn preview

# 代码检查
yarn lint

# 格式化
yarn format
```

## 3. 编码规范

### 3.1 TypeScript 规范

- 所有新功能必须使用 TypeScript 编写，禁止新增 `.js` 文件。
- 类型与接口定义使用帕斯卡命名法：`UserProfile`、`PageResult`
- 变量和函数使用小驼峰命名法：`userName`、`getUserInfo`
- 常量使用大写下划线分隔：`MAX_COUNT`、`DEFAULT_TIMEOUT`
- 禁止使用 `any`，确需动态类型时使用 `unknown` 并做类型收窄。
- 类型定义优先使用 `type`，需声明合并或继承时使用 `interface`。

### 3.2 Vue 组件规范

#### 3.2.1 组件结构

顺序固定为 `<template>` → `<script setup lang="ts">` → `<style scoped>`：

```vue
<template>
  <!-- 模板内容 -->
</template>

<script setup lang="ts">
// 1. 导入 2. 类型定义 3. props/emits 4. 响应式状态 5. 计算属性 6. 方法 7. 生命周期
</script>

<style scoped lang="scss">
/* 样式定义 */
</style>
```

#### 3.2.2 组件命名

- 文件名使用帕斯卡命名法：`UserCard.vue`、`DataTable.vue`
- 单文件组件必须使用 `.vue` 扩展名
- 组件名与文件名保持一致
- 基础组件以 `Base` 开头，业务组件按业务域命名

#### 3.2.3 Props 定义

使用类型声明 + `withDefaults`：

```typescript
interface Props {
  title: string
  visible: boolean
  count?: number // 可选属性
}

withDefaults(defineProps<Props>(), {
  count: 0,
  visible: false
})
```

#### 3.2.4 Emits 定义

使用 Vue 3.3+ 类型字面量语法（不使用已废弃的调用签名写法）：

```typescript
const emit = defineEmits<{
  'update:modelValue': [value: string]
  change: [value: string]
}>()

// 触发
emit('update:modelValue', 'newValue')
```

## 4. 接口契约规范

前端类型定义必须与[后端应用开发规范](../backend/后端应用开发规范.md)保持一致，不得自行变更字段名。

### 4.1 统一响应类型

```typescript
/** 统一响应体 */
interface ApiResponse<T = unknown> {
  /** 业务结果码，200 表示成功 */
  code: number
  /** 结果描述 */
  msg: string
  /** 业务数据 */
  data?: T
  /** 明细错误列表，批量/校验场景返回 */
  errList?: string[]
  /** 链路追踪 ID，5xx 时返回 */
  traceId?: string
}

/** 分页响应数据 */
interface PageResult<T> {
  /** 总记录数 */
  total: number
  /** 当前页码，从 1 开始 */
  current: number
  /** 每页条数 */
  pageSize: number
  /** 当前页数据 */
  list: T[]
}

/** 分页请求参数 */
interface PageQuery {
  current: number
  pageSize: number
}
```

### 4.2 结果判定

- `code === 200` 判定为成功，其余一律视为失败。
- 失败提示优先展示 `msg`；存在 `errList` 时逐条展示明细。
- `code` 为 `401` 时清除本地登录态并跳转登录页；`403` 时提示无权限。
- 分页接口无数据时不应提示错误，`list` 为空数组属正常结果。

### 4.3 HTTP 请求封装

- 使用 Axios 统一封装，配置请求/响应拦截器。
- 请求头统一携带 `X-Access-Token`，令牌从 Pinia Store 读取。
- 接口基础路径为 `/api`，开发环境通过 Vite 代理转发至后端。
- 统一超时时间 30 秒，文件上传接口单独设置（建议 120 秒）。
- 统一错误处理在响应拦截器中完成，业务代码只处理成功分支。

**HTTP 方法与参数传递**（遵循 RESTful 约定）：

| 方法 | 用途 | Axios 传参方式 |
| --- | --- | --- |
| `GET` | 查询（列表、详情、统计） | `params`（自动拼接为 query string），**不传请求体** |
| `POST` | 新增资源 | `data`（JSON 请求体） |
| `PUT` / `PATCH` | 更新资源 | `data`（JSON 请求体） |
| `DELETE` | 删除资源 | `params` 传 ID；批量删除用 `data` 传 ID 数组 |

```typescript
// utils/http.ts
import axios from 'axios'
import { useUserStore } from '@/stores/user'

const http = axios.create({ baseURL: '/api/v1', timeout: 30000 })

http.interceptors.request.use((config) => {
  config.headers['X-Access-Token'] = useUserStore().token
  return config
})

http.interceptors.response.use(
  (res) => res.data,
  (err) => { /* 统一错误处理：401 跳登录、403 提示无权限 */ return Promise.reject(err) }
)

export default http
```

### 4.4 接口文件组织

- API 定义放在 `src/api/` 目录（`auth.ts` / `user.ts` / `admin.ts` / `screening.ts`），路径使用**资源名词**，不含动词。
- 每个接口文件包含完整的 TypeScript 请求/响应类型定义，类型集中在 `src/types/`。
- 接口函数名采用 `{动作}{资源}` 形式，与 HTTP 方法语义对应。

```typescript
// api/screening.ts
import request from '@/utils/request'
import type { PageResult, ScreeningRecordVO, ScreeningPageQuery } from '@/types/screening'

const BASE = '/biz/screening-records'

/** 分页查询 */
export const pageScreeningRecords = (params: ScreeningQuery & PageQuery) =>
  http.get<never, ApiResponse<PageResult<ScreeningRecord>>>(BASE, { params })

/** 查询详情 */
export const getScreeningRecord = (id: string) =>
  http.get<never, ApiResponse<ScreeningRecord>>(`${BASE}/${id}`)

/** 新增 */
export const createScreeningRecord = (data: ScreeningCreateDTO) =>
  http.post<never, ApiResponse<{ id: string }>>(BASE, data)

/** 删除 */
export const deleteScreeningRecord = (id: string) =>
  http.delete<never, ApiResponse>(`${BASE}/${id}`)
```

> **禁止**出现 `/screening-record/list`、`/screening-record/add` 这类 RPC 风格路径。

## 5. 状态管理规范

### 5.1 Pinia Store 规范

- Store 定义放在 `src/stores/` 目录。
- **统一使用组合式 API（Setup Store）定义 Store**，与组件 `<script setup>` 风格保持一致。
- Store 文件名与 ID 使用小驼峰：`user`、`appConfig`。
- State、Getter、Action 职责清晰分离。

### 5.2 Store 定义示例

```typescript
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

interface UserInfo {
  id: string
  name: string
}

export const useUserStore = defineStore('user', () => {
  // state
  const userInfo = ref<UserInfo | null>(null)
  const token = ref<string>('')

  // getters
  const isLoggedIn = computed(() => !!token.value)

  // actions
  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  function setToken(value: string) {
    token.value = value
  }

  function reset() {
    userInfo.value = null
    token.value = ''
  }

  return { userInfo, token, isLoggedIn, setUserInfo, setToken, reset }
})
```

## 6. 路由规范

### 6.1 路由定义

- 路由配置集中在 `src/router/index.ts`（本项目无 `modules/` 拆分），按业务模块分组书写。
- 路由组件使用异步加载（`() => import('...')`）。
- 页面级权限通过路由 `meta.permission` 标记，公开页面通过 `meta.public` 标记，由全局守卫校验，详见[前端页面权限控制规范](../design/前端页面权限控制规范.md)。

### 6.2 路由命名

- 路由 `name` 使用帕斯卡命名法：`UserManagement`、`DataAnalysis`
- 路由 `path` 使用小写短横线分隔：`/user-management`、`/data-analysis`
- 动态参数使用 `:id` 形式。

## 7. 样式规范

### 7.1 样式组织

- 使用原生 CSS，不引入 SCSS/SASS 预处理器。
- 设计令牌与全局样式集中在 `src/styles/theme.css`。
- 组件样式必须使用 `scoped`，跨组件覆盖 Element Plus 样式时使用 `:deep()`。
- 类名使用小写短横线分隔，保持语义化。

### 7.2 变量和主题

- 颜色、间距、圆角等常量统一定义为 CSS 变量（如 `--drs-primary`、`--drs-border`），禁止在组件内硬编码色值。
- 图标统一使用 `AppIcon` 组件，不使用 emoji。
- 列表表格只保留横向框线；列宽由 `useFitColumns` 按内容自动适配，固定列与显式宽度列跳过，双击表头可重置，不做持久化。

## 8. 组件开发规范

### 8.1 组件分类

- **通用组件**: `AppIcon`、`PageHeader`、`StatCard`、`EChart`、`ResultCard` 等，直接放在 `src/components/`
- **布局组件**: 页面外壳放在 `src/layouts/`
- **页面组件**: 按业务模块分目录放在 `src/views/`

### 8.2 组件复用

- 优先使用 Element Plus 组件；Element Plus 在 `main.ts` 中全局注册并注入中文语言包 `zhCn`，否则分页与表格空态会显示英文。
- 公共组件无需额外注册；单个组件文件超过 400 行时应拆分。

### 8.3 组件通信

- 父子组件通信使用 `props` 和 `emit`。
- 跨层级通信使用 `provide/inject` 或 Pinia。
- 兄弟组件通信通过父组件或 Pinia，**禁止使用事件总线**。

## 9. 性能优化

### 9.1 组件优化

- 合理使用 `v-show` 和 `v-if`（频繁切换用 `v-show`，条件渲染用 `v-if`）。
- 长列表使用虚拟滚动。
- 必要时使用 `v-memo` 优化列表渲染。

### 9.2 资源优化

- 图片资源压缩和懒加载；演示样例图放在 `public/samples/`。
- 第三方库按需引入，ECharts 图表通过 `EChart` 组件按需初始化。
- 路由组件异步加载。

### 9.3 代码分割

- 路由级别代码分割。
- 大型第三方库（如 ECharts、PDF 生成库）单独分包。

## 10. 安全规范

### 10.1 XSS 防护

- 不直接使用 `v-html`，除非内容完全可信并经服务端消毒。
- 对用户输入进行适当转义。
- 验证所有外部数据。

### 10.2 数据安全

- 登录令牌当前保存在 `localStorage` 的 `token` 键（与 `stores/user.ts`、`utils/request.ts`、`utils/download.ts` 的实现一致）；退出登录时清除。若后续改用 HttpOnly Cookie，需同步修改这三处。
- 密码等凭据不得写入任何持久化存储，也不得写入日志。
- API 调用统一由 `utils/request.ts` 拦截器注入 `X-Access-Token`；文件下载走 `utils/download.ts`，同样需要该请求头。
- 禁止在代码中硬编码密钥、地址等敏感信息；连接凭据通过环境变量注入。
- 涉及患者隐私数据的页面禁止截图分享、禁止打印无关字段。

### 10.3 内容安全策略

- 配置合适的内容安全策略（CSP）。
- 验证外部资源的完整性（SRI）。

## 11. 测试规范

### 11.1 单元测试

- 使用 Vitest 进行单元测试。
- 组件测试使用 Vue Test Utils。
- 重要业务逻辑必须有对应测试。

### 11.2 测试覆盖率

- 关键业务逻辑测试覆盖率不低于 80%。
- 新功能必须包含相应测试。
- 修复 Bug 必须提供回归测试。

## 12. 提交与分支规范

### 12.1 提交规范

- 提交消息遵循 Conventional Commits 规范。
- 提交类型包括：`feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`chore`。

### 12.2 Git 工作流

- 主分支：`main`
- 开发分支：`develop`
- 功能分支：`feature/xxx`
- 修复分支：`hotfix/xxx`
- 发布分支：`release/xxx`

### 12.3 提交前检查

- 提交前自动运行 ESLint、Prettier、Stylelint 与类型检查。
- 检查不通过不得提交。

## 13. 部署规范

### 13.1 构建配置

- 生产环境移除调试信息与 `console`。
- 启用代码压缩和混淆。
- 生产环境不生成 Source Map（或上传至内部平台后删除本地产物）。

### 13.2 环境配置

- 使用 `.env` 系列文件区分环境：`.env.development`、`.env.production`。
- 仅 `VITE_` 前缀的变量会暴露到客户端，**禁止在前端环境变量中存放密钥**。
- 敏感配置信息不提交到代码仓库。
