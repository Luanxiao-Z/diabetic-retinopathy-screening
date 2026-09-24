<template>
  <div class="drs-page">
    <PageHeader
      title="关于系统"
      subtitle="系统定位、技术架构、版本信息与第三方组件说明"
      :crumbs="['帮助', '关于系统']"
    />

    <!-- 系统简介 -->
    <section class="drs-card">
      <div class="drs-card-head"><h3>系统简介</h3></div>
      <div class="drs-card-body">
        <p class="para">
          本系统面向基层医疗与眼科筛查场景，提供<b>糖尿病视网膜病变（DR）智能筛查</b>能力：
          上传眼底照片后由 AI 模型自动完成五级分级（LEVEL_0 正常 ~ LEVEL_4 增殖期）并生成
          Grad-CAM 可解释热力图，同时给出分级转诊建议；系统支持筛查记录管理、低置信度人工复核、
          患者纵向随访、统计分析与诊断报告导出。
        </p>
      </div>
    </section>

    <!-- 技术架构 -->
    <section class="drs-card mt">
      <div class="drs-card-head">
        <h3>技术架构</h3>
        <span class="drs-card-meta">三端分离：前端 / 后端 / 模型服务</span>
      </div>
      <div class="drs-card-body">
        <table class="about-table">
          <thead><tr><th>层次</th><th>技术选型</th><th>职责</th></tr></thead>
          <tbody>
            <tr v-for="t in stacks" :key="t.layer">
              <td class="layer">{{ t.layer }}</td>
              <td>{{ t.tech }}</td>
              <td>{{ t.duty }}</td>
            </tr>
          </tbody>
        </table>
        <p class="hint">
          <AppIcon name="info" :size="13" />
          前后端通过统一响应体 <code>{ code, msg, data }</code> 交互；认证采用 UUID 令牌 + Redis 会话，
          数据权限分为「全部数据（ALL）」与「仅本人数据（SELF）」两级。
        </p>
      </div>
    </section>

    <!-- 版本与运行信息 -->
    <div class="grid-2 mt">
      <section class="drs-card">
        <div class="drs-card-head">
          <h3>版本信息</h3>
          <span class="drs-card-meta">构建与运行环境</span>
        </div>
        <div class="drs-card-body">
          <dl class="kv">
            <div><dt>系统版本</dt><dd>{{ APP_VERSION }}</dd></div>
            <div><dt>前端框架</dt><dd>Vue 3 + TypeScript + Vite</dd></div>
            <div><dt>运行环境</dt><dd>{{ envText }}</dd></div>
            <div><dt>当前时间</dt><dd>{{ now }}</dd></div>
          </dl>
        </div>
      </section>

      <section class="drs-card">
        <div class="drs-card-head">
          <h3>模型信息</h3>
          <span class="drs-card-meta">
            <el-button link type="primary" @click="router.push('/model-info')">查看详情</el-button>
          </span>
        </div>
        <div class="drs-card-body">
          <dl class="kv">
            <div><dt>模型版本</dt><dd>{{ info?.version || '—' }}</dd></div>
            <div><dt>骨干网络</dt><dd>{{ info?.backbone || '—' }}</dd></div>
            <div><dt>推理设备</dt><dd>{{ info?.device || '—' }}</dd></div>
            <div><dt>训练状态</dt><dd>{{ info?.trained ? '已加载训练权重' : '未训练（随机初始化）' }}</dd></div>
          </dl>
        </div>
      </section>
    </div>

    <!-- 第三方组件 -->
    <section class="drs-card mt">
      <div class="drs-card-head">
        <h3>第三方组件与许可</h3>
        <span class="drs-card-meta">均为开源组件，遵循各自许可协议</span>
      </div>
      <div class="drs-card-body">
        <ul class="oss-list">
          <li v-for="o in oss" :key="o.name">
            <span class="oss-name">{{ o.name }}</span>
            <span class="oss-license">{{ o.license }}</span>
            <span class="oss-use">{{ o.use }}</span>
          </li>
        </ul>
      </div>
    </section>

    <!-- 免责声明 -->
    <section class="drs-card mt">
      <div class="drs-card-head"><h3>免责声明</h3></div>
      <div class="drs-card-body">
        <p class="disclaimer">
          本系统为<b>医学人工智能课程项目</b>的演示实现，AI 筛查结果<b>仅供临床参考，不能替代执业医师的诊断意见</b>。
          系统使用的眼底影像数据来源于公开数据集（APTOS 2019），不含真实患者隐私信息；
          运行过程中产生的影像与数据均存储于本地私有对象存储，不用于任何商业用途。
        </p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'
import { modelInfo } from '@/api/screening'
import type { ModelInfoVO } from '@/types/screening'

const APP_VERSION = 'v1.0.0'
const router = useRouter()
const info = ref<ModelInfoVO | null>(null)

const now = new Date().toLocaleString('zh-CN')
const envText = computed(() => {
  const ua = navigator.userAgent
  if (ua.includes('Edg')) return 'Microsoft Edge'
  if (ua.includes('Chrome')) return 'Google Chrome'
  if (ua.includes('Firefox')) return 'Mozilla Firefox'
  if (ua.includes('Safari')) return 'Safari'
  return '浏览器'
})

const stacks = [
  { layer: '前端', tech: 'Vue 3 + TypeScript + Vite + Element Plus + ECharts', duty: 'PC 端业务界面与数据可视化' },
  { layer: '后端', tech: 'Spring Boot 3 + Java 21 + MyBatis-Plus + Redis', duty: '业务接口、权限与数据权限、Excel 导出' },
  { layer: '模型服务', tech: 'Python + FastAPI + PyTorch（CUDA）+ timm', duty: 'DR 五级推理与 Grad-CAM 热力图生成' },
  { layer: '数据存储', tech: 'MySQL 8 + Redis 7 + MinIO', duty: '业务数据、会话缓存、影像对象存储' }
]

const oss = [
  { name: 'Vue.js / Vite', license: 'MIT', use: '前端框架与构建工具' },
  { name: 'Element Plus', license: 'MIT', use: 'UI 组件库' },
  { name: 'Apache ECharts', license: 'Apache-2.0', use: '统计图表' },
  { name: 'Spring Boot', license: 'Apache-2.0', use: '后端框架' },
  { name: 'MyBatis-Plus', license: 'Apache-2.0', use: '持久层框架' },
  { name: 'Apache POI', license: 'Apache-2.0', use: 'Excel 导出' },
  { name: 'FastAPI / PyTorch', license: 'MIT / BSD-3-Clause', use: '模型服务与深度学习推理' },
  { name: 'MinIO Java SDK', license: 'Apache-2.0', use: '对象存储访问' }
]

onMounted(async () => {
  try {
    info.value = await modelInfo()
  } catch {
    // 模型服务不可达时不阻塞「关于系统」页展示
  }
})
</script>

<style scoped>
.para {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.9;
  color: var(--drs-ink-700);
}

.mt {
  margin-top: var(--drs-gap);
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--drs-gap);
  align-items: stretch;
}

.about-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.about-table th,
.about-table td {
  border: 1px solid var(--drs-border);
  padding: 8px 12px;
  text-align: left;
  vertical-align: top;
}

.about-table th {
  background: var(--drs-surface-2);
  color: var(--drs-ink-600);
  font-weight: 600;
}

.about-table .layer {
  width: 90px;
  font-weight: 500;
  color: var(--drs-ink-800);
}

.hint {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 12px 0 0;
  padding: 10px 12px;
  border-radius: var(--drs-radius-sm);
  background: var(--drs-primary-50);
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--drs-primary-800);
}

.hint :deep(svg) {
  margin-top: 3px;
  flex-shrink: 0;
}

.hint code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}

.kv {
  margin: 0;
}

.kv > div {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--drs-border);
}

.kv > div:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.kv dt {
  width: 82px;
  flex-shrink: 0;
  font-size: 12.5px;
  color: var(--drs-ink-500);
}

.kv dd {
  margin: 0;
  font-size: 13px;
  color: var(--drs-ink-800);
  word-break: break-all;
}

.oss-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.oss-list li {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 7px 0;
  border-bottom: 1px dashed var(--drs-border);
  font-size: 13px;
}

.oss-list li:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.oss-name {
  width: 150px;
  flex-shrink: 0;
  font-weight: 500;
  color: var(--drs-ink-800);
}

.oss-license {
  width: 110px;
  flex-shrink: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--drs-primary-700);
}

.oss-use {
  color: var(--drs-ink-500);
}

.disclaimer {
  margin: 0;
  padding: 14px 16px;
  border: 1px dashed var(--drs-border-strong);
  border-radius: var(--drs-radius-sm);
  background: var(--drs-surface-2);
  font-size: 13px;
  line-height: 1.8;
  color: var(--drs-ink-600);
}

@media (max-width: 1024px) {
  .grid-2 {
    grid-template-columns: minmax(0, 1fr);
  }

  .oss-list li {
    flex-wrap: wrap;
  }
}
</style>
