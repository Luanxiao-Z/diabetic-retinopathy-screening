<template>
  <svg
    class="ring-field"
    :width="size"
    :height="size"
    viewBox="0 0 340 340"
    :style="{ opacity }"
    aria-hidden="true"
    focusable="false"
  >
    <!-- 3 实 1 虚：虚线层 90s 极慢旋转，仅作呼吸感的结构装饰 -->
    <circle class="ring-field__ring" cx="170" cy="170" r="150" opacity="0.28" />
    <circle class="ring-field__ring" cx="170" cy="170" r="118" opacity="0.22" />
    <circle class="ring-field__dash" cx="170" cy="170" r="86" />
    <circle class="ring-field__ring" cx="170" cy="170" r="54" opacity="0.16" />
  </svg>
</template>

<script setup lang="ts">
/**
 * 同心弧水印：页面右上角的 L1 图形层装饰。
 * 纯装饰，不承载信息，不参与布局（使用方自行定位为 absolute）。
 */
withDefaults(
  defineProps<{
    size?: number
    opacity?: number
  }>(),
  { size: 340, opacity: 0.55 }
)
</script>

<style scoped>
.ring-field {
  display: block;
  pointer-events: none;
}

.ring-field__ring {
  fill: none;
  stroke: var(--drs-primary);
  stroke-width: 1;
}

.ring-field__dash {
  fill: none;
  stroke: var(--drs-primary);
  stroke-width: 1;
  stroke-dasharray: 3 6;
  opacity: 0.35;
  transform-box: fill-box;
  transform-origin: center;
  animation: drs-spin 90s linear infinite;
}
</style>
