# 前端PC应用目录结构

```text
frontend/                          # 前端代码根目录
└─ web/                            # WEB（PC）端应用
    ├─ src/                        # 源代码根目录
    │   ├─ main.ts                 # 应用入口
    │   ├─ App.vue                 # 根组件
    │   ├─ router/                 # 路由配置
    │   │   ├─ index.ts            # 路由实例与全局守卫
    │   │   └─ modules/            # 按功能模块划分的路由定义
    │   ├─ views/                  # 页面组件（按业务模块分目录）
    │   ├─ apis/                   # API 接口定义（按业务模块划分）
    │   ├─ layout/                 # 布局组件（侧边栏、顶栏、内容区）
    │   ├─ components/             # 公共组件
    │   │   ├─ base/               # 基础通用组件
    │   │   └─ business/           # 业务通用组件
    │   ├─ directives/             # 自定义指令（如 v-permission）
    │   ├─ stores/                 # Pinia 状态管理
    │   ├─ utils/                  # 工具函数（http 封装、存储、格式化）
    │   ├─ types/                  # 全局 TypeScript 类型定义
    │   ├─ config/                 # 应用配置（常量、字典、枚举）
    │   ├─ hooks/                  # 组合式函数
    │   ├─ plugins/                # 插件注册（Element Plus、图标等）
    │   └─ assets/                 # 静态资源
    │       ├─ css/                # 全局样式与变量
    │       └─ images/             # 图片资源
    ├─ public/                     # 公共静态资源（直接拷贝，不参与构建）
    ├─ .env                        # 通用环境变量
    ├─ .env.development            # 开发环境变量
    ├─ .env.production             # 生产环境变量
    ├─ package.json                # 依赖配置
    ├─ vite.config.ts              # Vite 配置
    ├─ eslint.config.mjs           # ESLint 配置
    ├─ .prettierrc.cjs             # Prettier 配置
    ├─ stylelint.config.cjs        # Stylelint 配置
    ├─ tsconfig.json               # TypeScript 根配置
    ├─ tsconfig.app.json           # 应用 TypeScript 配置
    ├─ tsconfig.node.json          # Node 环境 TypeScript 配置
    ├─ index.html                  # HTML 入口文件
    └─ README.md                   # 项目说明
```

## 目录说明

| 目录 | 说明 |
| --- | --- |
| `src/router/modules/` | 路由按模块拆分，由 `index.ts` 汇总；页面权限标记写在路由 `meta.permission` 上 |
| `src/apis/` | 每个接口文件同时导出请求参数与响应类型，与后端契约一致 |
| `src/directives/` | 自定义指令，权限指令 `v-permission` 见[前端页面权限控制规范](../design/前端页面权限控制规范.md) |
| `src/stores/` | Pinia Store，统一使用组合式（Setup）写法 |
| `src/types/` | 跨模块复用的类型定义；模块内私有类型就近定义 |
| `src/config/` | 应用级常量，如存储键名、枚举、静态字典 |

## 相关规范

- [前端PC端应用开发规范](前端PC端应用开发规范.md)
