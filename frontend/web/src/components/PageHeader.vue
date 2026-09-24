<template>
  <header class="page-header">
    <nav v-if="crumbs.length" class="crumbs" aria-label="面包屑导航">
      <template v-for="(c, i) in crumbs" :key="c + i">
        <span v-if="i > 0" class="crumbs-sep" aria-hidden="true">/</span>
        <span :class="['crumbs-item', { 'is-current': i === crumbs.length - 1 }]">{{ c }}</span>
      </template>
    </nav>

    <div class="head-row">
      <div class="head-main">
        <h1 class="head-title">{{ title }}</h1>
        <p v-if="subtitle" class="head-sub">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.actions" class="head-actions">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="$slots.extra" class="head-extra">
      <slot name="extra" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

/**
 * 统一页头：面包屑 + 标题 + 副标题 + 右侧操作区。
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
  margin-bottom: var(--drs-gap-lg);
}

.crumbs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: var(--drs-ink-500);
  margin-bottom: 10px;
}

.crumbs-sep {
  color: var(--drs-ink-300);
}

.crumbs-item.is-current {
  color: var(--drs-ink-700);
  font-weight: 500;
}

.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--drs-gap);
  flex-wrap: wrap;
}

.head-main {
  min-width: 0;
}

.head-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.2px;
  margin: 0;
  color: var(--drs-ink-900);
}

.head-sub {
  font-size: 13px;
  color: var(--drs-ink-500);
  margin: 4px 0 0;
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
