<template>
  <div class="drs-page">
    <PageHeader
      title="使用指南"
      subtitle="DR 智能筛查系统的角色说明、操作流程与常见问题"
      :crumbs="['帮助', '使用指南']"
    />

    <div class="guide-layout">
      <!-- 目录 -->
      <nav class="drs-card toc" aria-label="指南目录">
        <div class="drs-card-head"><h3>目录</h3></div>
        <div class="drs-card-body">
          <a v-for="s in sections" :key="s.id" :href="`#${s.id}`" class="toc-item">
            <AppIcon name="chevronRight" :size="13" />{{ s.title }}
          </a>
        </div>
      </nav>

      <div class="guide-body">
        <!-- 1 快速上手 -->
        <section id="quickstart" class="drs-card">
          <div class="drs-card-head"><h3>一、快速上手</h3></div>
          <div class="drs-card-body">
            <ol class="steps">
              <li v-for="(s, i) in quickstart" :key="i">
                <span class="step-no">{{ i + 1 }}</span>
                <span class="step-text"><b>{{ s.title }}</b><em>{{ s.desc }}</em></span>
              </li>
            </ol>
          </div>
        </section>

        <!-- 2 角色与权限 -->
        <section id="roles" class="drs-card">
          <div class="drs-card-head">
            <h3>二、角色与数据权限</h3>
            <span class="drs-card-meta">系统采用轻量化角色映射，不建 RBAC 表</span>
          </div>
          <div class="drs-card-body">
            <table class="guide-table">
              <thead>
                <tr><th>角色</th><th>数据权限</th><th>可用功能</th></tr>
              </thead>
              <tbody>
                <tr v-for="r in roles" :key="r.role">
                  <td><span class="role-chip" :class="r.cls">{{ r.role }}</span></td>
                  <td>{{ r.scope }}</td>
                  <td>{{ r.menus }}</td>
                </tr>
              </tbody>
            </table>
            <p class="tip">
              <AppIcon name="info" :size="13" />
              数据权限决定你<b>能看到谁的记录</b>：医生仅可见本人创建的筛查记录，管理员可见全量。顶栏右侧的「数据范围」徽标会实时显示当前账号的权限范围。
            </p>
          </div>
        </section>

        <!-- 3 功能说明 -->
        <section id="features" class="drs-card">
          <div class="drs-card-head"><h3>三、核心功能说明</h3></div>
          <div class="drs-card-body">
            <div v-for="f in features" :key="f.name" class="feat">
              <span class="feat-ico" aria-hidden="true"><AppIcon :name="f.icon" :size="16" /></span>
              <div>
                <div class="feat-name">{{ f.name }}</div>
                <div class="feat-desc">{{ f.desc }}</div>
              </div>
            </div>
          </div>
        </section>

        <!-- 4 分级与建议口径 -->
        <section id="levels" class="drs-card">
          <div class="drs-card-head">
            <h3>四、分级与转诊口径</h3>
            <span class="drs-card-meta">与后端字典 B_DR_LEVEL / B_DR_SUGGESTION 一致</span>
          </div>
          <div class="drs-card-body">
            <table class="guide-table">
              <thead><tr><th>分级</th><th>说明</th><th>转诊建议</th></tr></thead>
              <tbody>
                <tr v-for="l in levels" :key="l.code">
                  <td><code>{{ l.code }}</code></td>
                  <td>{{ l.name }}</td>
                  <td><span class="sg-chip" :class="l.cls">{{ l.suggestion }}</span></td>
                </tr>
              </tbody>
            </table>
            <p class="tip">
              <AppIcon name="info" :size="13" />
              模型置信度低于 <b>0.70</b> 的结果会被标记为「待复核」，需医师核对影像后确认复核，复核意见会随报告一并留存。
            </p>
          </div>
        </section>

        <!-- 5 常见问题 -->
        <section id="faq" class="drs-card">
          <div class="drs-card-head"><h3>五、常见问题</h3></div>
          <div class="drs-card-body">
            <div v-for="(q, i) in faqs" :key="i" class="faq">
              <div class="faq-q">{{ q.q }}</div>
              <div class="faq-a">{{ q.a }}</div>
            </div>
          </div>
        </section>

        <!-- 6 免责声明 -->
        <section id="disclaimer" class="drs-card">
          <div class="drs-card-head"><h3>六、免责声明</h3></div>
          <div class="drs-card-body">
            <p class="disclaimer">
              本系统为<b>医学人工智能课程项目</b>的演示实现。其筛查结果由 AI 模型辅助生成，
              仅供临床参考，<b>不能替代执业医师的诊断意见</b>；影像与个人信息均存储于本地私有对象存储，
              不用于任何商业用途。
            </p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'
import PageHeader from '@/components/PageHeader.vue'

const sections = [
  { id: 'quickstart', title: '快速上手' },
  { id: 'roles', title: '角色与数据权限' },
  { id: 'features', title: '核心功能说明' },
  { id: 'levels', title: '分级与转诊口径' },
  { id: 'faq', title: '常见问题' },
  { id: 'disclaimer', title: '免责声明' }
]

const quickstart = [
  { title: '登录系统', desc: '使用分配到的账号登录；演示环境可在登录页一键填入演示账号。' },
  { title: '上传眼底影像', desc: '进入「筛查上传」，填写患者信息（选填）并选择一张或多张眼底图，逐张独立推理。' },
  { title: '查看筛查结果', desc: '结果卡片展示分级、置信度、各分级概率与 Grad-CAM 热力图，可切换热力图叠加。' },
  { title: '处理待复核项', desc: '置信度低于阈值的结果进入「随访待办」，核对影像后执行复核确认。' },
  { title: '导出与打印', desc: '在「筛查记录」导出 Excel，或打开单条记录的诊断报告并打印／另存为 PDF。' }
]

const roles = [
  {
    role: '管理员',
    cls: 'role-admin',
    scope: '全部数据（ALL）',
    menus: '全部功能，含系统管理（用户管理 / 字典管理 / 操作日志）'
  },
  {
    role: '医生',
    cls: 'role-doctor',
    scope: '仅本人数据（SELF）',
    menus: '筛查上传、筛查记录、随访待办、患者随访、统计分析、个人中心'
  }
]

const features = [
  { icon: 'upload', name: '筛查上传', desc: '多张眼底图逐张独立推理，单张失败不影响其它影像，可单独重试。' },
  { icon: 'list', name: '筛查记录', desc: '按患者、分级、复核状态与时间检索；支持批量删除、导出 Excel；拖动表头可调整列宽（自动记忆）。' },
  { icon: 'inbox', name: '随访待办', desc: '汇总待人工复核、需转诊与逾期未复诊三类待办，是日常工作的入口。' },
  { icon: 'activity', name: '患者随访', desc: '按患者归并历次筛查，识别分级「进展 / 好转 / 持平」，并提供随访时间线。' },
  { icon: 'chart', name: '统计分析', desc: '分级与转诊建议分布、近 30 天趋势，并给出与前 30 天的环比。' },
  { icon: 'shield', name: '人工复核', desc: '对低置信度结果执行复核确认，记录复核人、时间与意见，形成可追溯闭环。' },
  { icon: 'download', name: '诊断报告', desc: 'A4 版式报告，含影像、热力图、分级结论、概率表与免责声明，可打印或另存为 PDF。' },
  { icon: 'clock', name: '操作日志', desc: '记录登录、上传、删除、导出、复核与账号变更等关键动作，含操作人、耗时与 IP。' }
]

const levels = [
  { code: 'LEVEL_0', name: '正常（Normal）', suggestion: '定期复查', cls: 'sg-review' },
  { code: 'LEVEL_1', name: '轻度 NPDR', suggestion: '定期复查', cls: 'sg-review' },
  { code: 'LEVEL_2', name: '中度 NPDR', suggestion: '建议眼科就诊', cls: 'sg-clinic' },
  { code: 'LEVEL_3', name: '重度 NPDR', suggestion: '建议尽快转诊', cls: 'sg-referral' },
  { code: 'LEVEL_4', name: 'PDR（增殖性）', suggestion: '建议尽快转诊', cls: 'sg-referral' }
]

const faqs = [
  {
    q: '为什么我看不到某条筛查记录？',
    a: '多半是数据权限所致：医生账号仅能看到本人创建的记录。请确认该记录是否为当前账号上传，或改用管理员账号查看。'
  },
  {
    q: '上传后提示「无法连接模型推理服务」怎么办？',
    a: '说明模型服务（8000 端口）未启动或不可达。请联系维护人员启动模型服务后重试；已上传失败的影像可在队列中点击「重试失败」。'
  },
  {
    q: '热力图不显示是什么原因？',
    a: '热力图生成失败不会阻断筛查主流程，记录仍会正常落库。可稍后在记录详情中重新查看，若持续缺失请联系维护人员。'
  },
  {
    q: '导出的 Excel 里图片链接打不开？',
    a: '影像存储于私有对象桶，导出时生成的是 30 分钟有效的预签名链接。超时后请在系统内重新导出。'
  },
  {
    q: '如何调整表格列宽？',
    a: '拖动表头列之间的分隔线即可，调整结果会按页面自动记忆；点击列表右上角的「恢复默认」可重置。'
  }
]
</script>

<style scoped>
.guide-layout {
  display: grid;
  grid-template-columns: minmax(180px, 220px) minmax(0, 1fr);
  gap: var(--drs-gap);
  align-items: start;
}

/* 目录 */
.toc {
  position: sticky;
  top: 16px;
}

.toc-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  border-radius: var(--drs-radius-xs);
  font-size: 13px;
  color: var(--drs-ink-600);
  transition: background-color 0.16s ease, color 0.16s ease;
}

.toc-item:hover {
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
}

.guide-body {
  display: flex;
  flex-direction: column;
  gap: var(--drs-gap);
}

/* 步骤 */
.steps {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.steps li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.step-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--drs-primary-50);
  color: var(--drs-primary-800);
  font-size: 12px;
  font-weight: 600;
}

.step-text {
  display: flex;
  flex-direction: column;
  line-height: 1.6;
}

.step-text b {
  font-size: 13.5px;
  color: var(--drs-ink-800);
}

.step-text em {
  font-style: normal;
  font-size: 12.5px;
  color: var(--drs-ink-500);
}

/* 表格 */
.guide-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.guide-table th,
.guide-table td {
  border: 1px solid var(--drs-border);
  padding: 8px 12px;
  text-align: left;
  vertical-align: top;
}

.guide-table th {
  background: var(--drs-surface-2);
  color: var(--drs-ink-600);
  font-weight: 600;
}

.guide-table code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  color: var(--drs-ink-700);
}

.role-chip,
.sg-chip {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.role-admin {
  color: var(--drs-violet);
  background: var(--drs-violet-bg);
}

.role-doctor {
  color: var(--drs-primary-700);
  background: var(--drs-primary-50);
}

.sg-review {
  color: var(--drs-ok);
  background: var(--drs-ok-bg);
}

.sg-clinic {
  color: var(--drs-warn);
  background: var(--drs-warn-bg);
}

.sg-referral {
  color: var(--drs-danger);
  background: var(--drs-danger-bg);
}

/* 功能条目 */
.feat {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--drs-border);
}

.feat:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.feat-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 9px;
  background: var(--drs-primary-50);
  color: var(--drs-primary-700);
}

.feat-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--drs-ink-800);
}

.feat-desc {
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--drs-ink-500);
}

/* FAQ */
.faq + .faq {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed var(--drs-border);
}

.faq-q {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--drs-ink-800);
  margin-bottom: 4px;
}

.faq-a {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--drs-ink-600);
}

/* 提示与声明 */
.tip {
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

.tip :deep(svg) {
  margin-top: 3px;
  flex-shrink: 0;
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
  .guide-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .toc {
    position: static;
  }
}
</style>
