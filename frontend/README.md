# drs-web — 糖尿病视网膜病变智能筛查系统（前端）

前端 PC 端 Web 应用。基于 **Vue 3 + TypeScript + Vite** 构建，
为医师与管理员提供眼底图上传筛查、结果查看与人工复核、患者随访、
统计分析、系统管理等界面。

> 项目总体说明、功能清单与后端接口见**仓库根目录 `README.md`**；
> 本文档只覆盖前端目录自身的构建、结构与开发约定。

---

## 一、技术栈

| 类别 | 选型 | 版本 |
| --- | --- | --- |
| 框架 | Vue 3（Composition API + `<script setup>`） | ^3.5.13 |
| 语言 | TypeScript | ~5.6.3 |
| 构建 | Vite | ^6.0.5 |
| UI 组件库 | Element Plus | ^2.9.1 |
| 状态管理 | Pinia | ^2.3.0 |
| 路由 | Vue Router | ^4.5.0 |
| 图表 | ECharts | ^5.5.1 |
| HTTP | axios | ^1.7.9 |
| 代码规范 | ESLint + Prettier + Stylelint | — |

包管理器统一使用 **Yarn**（仓库内含 `yarn.lock`）。

---

## 二、目录结构

```
frontend/web/
├── index.html
├── vite.config.ts          构建配置（含 /api 开发代理 → 后端 8080）
├── package.json
├── tsconfig.json
├── eslint.config.js
└── src/
    ├── main.ts             应用入口（全局注入 Element Plus 中文 locale）
    ├── App.vue
    ├── api/                接口封装，按域拆分（auth / user / admin / screening）
    ├── components/         通用组件（AppIcon、PageHeader、ResultCard、EChart 等）
    ├── composables/        组合式函数
    ├── directives/         自定义指令
    ├── layouts/            布局外壳（BasicLayout：侧栏 + 顶栏 + 内容区）
    ├── router/             路由表与导航守卫
    ├── stores/             Pinia store（user / permission / screening）
    ├── styles/             全局样式与设计令牌（theme.css）
    ├── types/              类型定义（与后端 VO / DTO 对齐）
    ├── utils/              工具函数（request、download、format）
    └── views/              页面（见「五、页面清单」）
```

---

## 三、环境要求

| 依赖 | 版本 |
| --- | --- |
| Node.js | 20+（本项目在 22.x 下开发） |
| Yarn | 1.x / 3.x 均可 |
| 后端服务 | 需先启动，默认 `http://localhost:8080` |

---

## 四、快速开始

```bash
cd frontend/web

# 安装依赖
yarn install

# 启动开发服务器（默认 http://127.0.0.1:5173）
yarn dev

# 生产构建（先做类型检查，再打包）
yarn build

# 预览构建产物
yarn preview
```

### 可用脚本

| 命令 | 作用 |
| --- | --- |
| `yarn dev` | 启动 Vite 开发服务器（端口 5173） |
| `yarn build` | `vue-tsc --noEmit && vite build`，含类型检查 |
| `yarn preview` | 本地预览 `dist/` 产物 |
| `yarn lint` | ESLint 检查 `.js/.ts/.vue` |
| `yarn format` | Prettier 格式化 `src/**` |
| `yarn stylelint` | Stylelint 检查样式 |

### 开发代理

`vite.config.ts` 已配置代理，开发环境下 `/api` 请求转发至 `http://localhost:8080`，
**无需处理跨域**：

```ts
server: {
  port: 5173,
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true }
  }
}
```

生产环境由 Nginx 反向代理（配置见 `../deploy/`）。

---

## 五、页面清单

| 路径 | 页面 | 说明 |
| --- | --- | --- |
| `/login` | 登录 | 含图形验证码 |
| `/register` | 注册 | 自助注册医生账号 |
| `/dashboard` | 综合看板 | 指标卡、趋势图、待办概览 |
| `/screening/upload` | 筛查上传 | 批量上传眼底图并触发筛查 |
| `/screening/records` | 筛查记录 | 列表、筛选、详情、批量删除 |
| `/screening/records/:id/report` | 诊断报告 | 可打印 / 导出 PDF |
| `/screening/patients` | 患者随访 | 患者维度聚合、时间线抽屉 |
| `/screening/todos` | 随访待办 | 三类待办分页表格 |
| `/screening/statistics` | 统计分析 | 分级分布、转诊率、趋势 |
| `/model-info` | 模型信息 | 模型元信息与训练指标 |
| `/admin/users` | 用户管理 | 管理员 |
| `/admin/dicts` | 字典管理 | 管理员 |
| `/admin/logs` | 操作日志 | 管理员 |
| `/profile` | 个人中心 | 资料编辑、修改密码 |
| `/guide` | 使用指南 | |
| `/about` | 关于系统 | |
| `/403` | 无权限 | |

---

## 六、开发约定

以下为项目既定约定，新增代码请遵循：

### 样式与主题

1. **设计令牌集中在 `styles/theme.css`**，组件内**不写死颜色**，一律引用 CSS 变量。
2. **两列等高布局用「给定列高 + 列内各自滚动」**，不要依赖内容互相撑高
   （后者会把结果卡 `.rc-image` 撑开变形）。

### 组件与图标

3. **图标统一使用 `AppIcon` 组件**，不使用 emoji，也不直接内联 SVG。
4. **Element Plus 必须全局注入中文 locale**（`main.ts` 中
   `app.use(ElementPlus, { locale: zhCn })`），否则分页（Total / 跳到）、
   表格空态等会显示英文。

### 接口与数据

5. **接口统一走 `utils/request.ts`**，已处理统一响应体解包与 `X-Access-Token` 注入。
6. **后端 Jackson 为 `non_null`**，null 字段不下发，类型定义需按可选字段处理。
7. **概率值契约为 0~1 的 softmax 输出**，前端**不得**再做
   `v > 1 ? v / 100 : v` 之类的单位猜测。
8. **概率展示保留一位小数**，极小值显示 `<0.1%`，**不要用 `Math.round`**
   （会把 0.41% 显示为 0%、99.58% 显示为 100%，造成「硬分类」假象）。
9. **`confidence` 仅用于展示**，不参与任何判定逻辑（筛选、统计口径均以
   `uncertainty` 为准）。

### 时间格式

10. **界面时间统一为 `yyyy-MM-dd HH:mm:ss`**，使用 `utils/format.ts` 的
    `formatDateTime()`；**仅操作日志保留 ISO8601 原文**（面向排障，便于与后端日志对齐）。
    该函数只做字符串替换（`T` → 空格），**不经过 `Date` 解析**，避免时区偏移。

### 交互

11. **双击行 = 该列表的默认操作**（如筛查记录 → 详情、用户管理 → 编辑）。
12. **图表自适应**：`EChart` 组件用 `ResizeObserver` 观察容器尺寸，
    仅监听 `window.resize` 无法覆盖侧栏折叠、标签切换等场景。

---

## 七、常见问题

| 现象 | 原因与处理 |
| --- | --- |
| 分页控件显示英文（Total / Go to） | Element Plus 未注入中文 locale，检查 `main.ts` |
| 接口 401 | 令牌缺失或已过期，重新登录 |
| 开发环境请求跨域 | 确认 `vite.config.ts` 代理配置存在且后端已启动 |
| 页面时间显示带 `T` | 该处未使用 `formatDateTime()`，见「开发约定」第 10 条 |
| `yarn build` 类型报错但开发时正常 | 开发模式不做类型检查，`build` 会执行 `vue-tsc --noEmit`，按提示修正类型 |
| 新增模块后报「xx is not a function」但导入正常 | Vite HMR 缓存问题，**重启 dev server** 后再判断 |

---

## 八、相关文档

| 文档 | 位置 |
| --- | --- |
| 项目总览、功能与部署 | `../README.md` |
| 系统实现说明（含设计取舍） | `../docs/系统实现说明.md` |
| 界面规范与验收要求 | `../spec/` |
