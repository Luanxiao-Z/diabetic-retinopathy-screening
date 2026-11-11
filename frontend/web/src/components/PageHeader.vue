<template>
  <header class="page-header">
    <!-- 面包屑与操作区同行：不再单占一行的 h1，标题信息由面包屑末项承担 -->
    <div class="head-bar">
      <nav v-if="crumbs.length" class="crumbs" aria-label="面包屑导航">
        <template v-for="(c, i) in crumbs" :key="c + i">
          <span v-if="i > 0" class="crumbs-sep" aria-hidden="true">/</span>
          <span :class="['crumbs-item', { 'is-current': i === crumbs.length - 1 }]">{{ c }}</span>
        </template>
      </nav>
      <div v-if="$slots.actions" class="head-actions">
        <slot name="actions" />
      </div>
    </div>

    <p v-if="subtitle" class="head-sub">{{ subtitle }}</p>

    <div v-if="$slots.extra" class="head-extra">
      <slot name="extra" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

/**
 * 统一页头：面包屑（含当前页）+ 右侧操作区同一行。
 * 不再渲染独立 h1，当前页名由面包屑末项承担，纵向空间留给内容。
 * 未显式传入 crumbs 时，按路由 meta.crumb 自动推导。
 */
const props = defineProps<{
  title: string
  subtitle?: string
  crumbs?: string[]
}>()

const route = useRoute()

const crumbs = computed<string[]>(() => {
  if (props.crumbs) return props.crumbs
  const meta = route.meta as { crumb?: string; group?: string }
  const group = meta.group
  const leaf = meta.crumb || props.title
  return group ? [group, leaf] : [leaf]
})
</script>

<style scoped>
.page-header {
  margin-bottom: var(--drs-gap);
}

/* 面包屑与右侧操作按钮同一行，按钮靠右对齐 */
.head-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--drs-gap);
  min-height: 32px;
}

.crumbs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: var(--drs-ink-500);
}

.crumbs-sep {
  color: var(--drs-ink-300);
}

.crumbs-item.is-current {
  color: var(--drs-ink-700);
  font-weight: 500;
}

.head-sub {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--drs-ink-500);
}

.head-actions {
  display: flex;
  align-items: center;
  gap: var(--drs-gap-xs);
  flex-shrink: 0;
}

.head-extra {
  margin-top: var(--drs-gap-sm);
}
</style>
