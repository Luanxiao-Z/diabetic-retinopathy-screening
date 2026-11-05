# 前端 PC 应用目录结构

```text
frontend/
├── README.md                      # 前端目录说明
└── web/                           # Vue 3 + Vite 应用（唯一前端应用）
    ├── src/
    │   ├── main.ts                # 应用入口（全局注册 Element Plus + zhCn）
    │   ├── App.vue                # 根组件
    │   ├── api/                   # 接口封装（auth / user / admin / screening）
    │   ├── components/            # 通用组件
    │   │   ├── AppIcon.vue        # 统一图标组件
    │   │   ├── EChart.vue
    │   │   ├── PageHeader.vue
    │   │   ├── ResultCard.vue
    │   │   └── StatCard.vue
    │   ├── composables/           # 组合式函数
    │   │   └── useFitColumns.ts   # 列表列宽自适应
    │   ├── directives/
    │   │   └── permission.ts      # v-permission 指令
    │   ├── layouts/
    │   │   └── BasicLayout.vue    # 顶栏 + 分组侧栏 + 多标签页
    │   ├── router/
    │   │   └── index.ts           # 静态路由与全局守卫
    │   ├── stores/                # Pinia（user / permission / screening）
    │   ├── styles/
    │   │   └── theme.css          # 设计令牌（CSS 变量）与全局样式
    │   ├── types/                 # 前后端共享类型定义
    │   ├── utils/                 # request / format / download
    │   ├── views/                 # 页面（按业务模块分目录）
    │   └── vite-env.d.ts
    ├── public/
    │   └── samples/
    │       └── demo-fundus.png    # 内置演示样例眼底图
    ├── .env                       # VITE_API_BASE=/api/v1
    ├── .editorconfig
    ├── .prettierrc.json
    ├── .stylelintrc.json
    ├── .yarnrc
    ├── eslint.config.js
    ├── index.html
    ├── package.json
    ├── tsconfig.json
    ├── tsconfig.node.json
    ├── vite.config.ts             # /api 代理至后端 8080
    └── yarn.lock
```

## 目录说明

| 目录 | 说明 |
| --- | --- |
| `src/api/` | 每个接口文件同时定义请求参数与响应类型，与后端契约一致 |
| `src/composables/` | 可复用逻辑；列宽自适应 `useFitColumns` 首次渲染按内容实测列宽，双击表头重置，不持久化 |
| `src/directives/` | `v-permission` 依据当前用户权限集控制元素显隐，见[前端页面权限控制规范](../design/前端页面权限控制规范.md) |
| `src/router/` | 路由集中在单个 `index.ts`，无 `modules/` 拆分；`meta.public` / `meta.permission` 控制准入 |
| `src/stores/` | Pinia Store，统一使用组合式（Setup）写法 |
| `src/styles/` | 原生 CSS + CSS 变量，颜色与间距等设计令牌集中在 `theme.css`，组件内不写死色值 |
| `src/types/` | 跨模块复用的类型定义 |
| `public/samples/` | 演示样例图，随前端静态资源发布 |

## 相关规范

- [前端PC端应用开发规范](前端PC端应用开发规范.md)
- [前端页面权限控制规范](../design/前端页面权限控制规范.md)
