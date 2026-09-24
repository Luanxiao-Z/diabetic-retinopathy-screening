<template>
  <component
    :is="clickable ? 'button' : 'div'"
    class="stat"
    :class="[`tone-${tone}`, { 'is-clickable': clickable }]"
    :type="clickable ? 'button' : undefined"
    :aria-label="clickable ? ariaLabel || `${label}，点击查看详情` : undefined"
    @click="onActivate"
  >
    <span class="stat-ico" aria-hidden="true">
      <AppIcon :name="icon" :size="20" />
    </span>

    <span class="stat-label">{{ label }}</span>

    <span class="stat-num">
      {{ displayValue }}<span v-if="unit" class="stat-unit">{{ unit }}</span>
    </span>

    <span v-if="foot || clickable" class="stat-foot">
      <span class="stat-foot-text">{{ foot }}</span>
      <span v-if="clickable" class="stat-go">
        {{ goText }}
        <AppIcon name="arrowRight" :size="13" />
      </span>
    </span>
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'

/**
 * 指标卡：图标 + 标签 + 数值（可带单位）+ 脚注。
 * 可点击时使用原生 button 语义，键盘 Enter/Space 均可触发（无障碍）。
 */
const props = withDefaults(
  defineProps<{
    icon: string
    label: string
    value: string | number
    unit?: string
    foot?: string
    goText?: string
    tone?: 'brand' | 'ok' | 'warn' | 'danger' | 'info' | 'violet' | 'neutral'
    clickable?: boolean
    ariaLabel?: string
  }>(),
  { unit: '', foot: '', goText: '去查看', tone: 'brand', clickable: false, ariaLabel: '' }
)

const emit = defineEmits<{ (e: 'click'): void }>()

const displayValue = computed(() => (props.value === '' || props.value == null ? '—' : props.value))

function onActivate() {
  if (props.clickable) emit('click')
}
</script>

<style scoped>
.stat {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 18px;
  background: var(--drs-surface);
  border: 1px solid var(--drs-border);
  border-radius: var(--drs-radius);
  box-shadow: var(--drs-shadow-sm);
  text-align: left;
  font: inherit;
  color: inherit;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.stat.is-clickable {
  cursor: pointer;
}

.stat.is-clickable:hover {
  border-color: var(--drs-primary-200);
  box-shadow: var(--drs-shadow);
}

.stat.is-clickable:focus-visible {
  outline: 2px solid var(--drs-primary);
  outline-offset: 2px;
}

.stat-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  margin-bottom: 6px;
  background: var(--tone-bg);
  color: var(--tone-fg);
}

.stat-label {
  font-size: 13px;
  color: var(--drs-ink-500);
}

.stat-num {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.25;
  letter-spacing: -0.5px;
  color: var(--drs-ink-900);
  font-variant-numeric: tabular-nums;
}

.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--drs-ink-500);
  margin-left: 4px;
}

.stat-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px dashed var(--drs-border);
  font-size: 12px;
  color: var(--drs-ink-500);
}

.stat-go {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--tone-fg);
  font-weight: 500;
}

/* ---- 色调 ---- */
.tone-brand {
  --tone-bg: var(--drs-primary-50);
  --tone-fg: var(--drs-primary-700);
}

.tone-ok {
  --tone-bg: var(--drs-ok-bg);
  --tone-fg: var(--drs-ok);
}

.tone-warn {
  --tone-bg: var(--drs-warn-bg);
  --tone-fg: var(--drs-warn);
}

.tone-danger {
  --tone-bg: var(--drs-danger-bg);
  --tone-fg: var(--drs-danger);
}

.tone-info {
  --tone-bg: var(--drs-info-bg);
  --tone-fg: var(--drs-info);
}

.tone-violet {
  --tone-bg: var(--drs-violet-bg);
  --tone-fg: var(--drs-violet);
}

.tone-neutral {
  --tone-bg: var(--drs-ink-100);
  --tone-fg: var(--drs-ink-600);
}
</style>
