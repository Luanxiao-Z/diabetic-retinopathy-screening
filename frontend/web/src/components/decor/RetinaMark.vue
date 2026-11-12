<template>
  <svg
    class="retina-mark"
    :class="{ 'is-static': !animated }"
    :width="size"
    :height="size"
    viewBox="0 0 128 128"
    aria-hidden="true"
    focusable="false"
  >
    <!-- 四角定位框：模拟影像分析取景 -->
    <path class="retina-mark__bracket" d="M8 24V10h14" />
    <path class="retina-mark__bracket" d="M106 10h14v14" />
    <path class="retina-mark__bracket" d="M120 104v14h-14" />
    <path class="retina-mark__bracket" d="M22 118H8v-14" />

    <!-- 视野圆 + 缓慢旋转的刻度虚线环 -->
    <circle class="retina-mark__ring" cx="64" cy="64" r="46" />
    <circle class="retina-mark__orbit" cx="64" cy="64" r="56" />
    <g class="retina-mark__ticks">
      <line x1="114" y1="64" x2="118" y2="64" />
      <line x1="107.3" y1="89" x2="110.8" y2="91" />
      <line x1="89" y1="107.3" x2="91" y2="110.8" />
      <line x1="64" y1="114" x2="64" y2="118" />
      <line x1="39" y1="107.3" x2="37" y2="110.8" />
      <line x1="20.7" y1="89" x2="17.2" y2="91" />
      <line x1="14" y1="64" x2="10" y2="64" />
      <line x1="20.7" y1="39" x2="17.2" y2="37" />
      <line x1="39" y1="20.7" x2="37" y2="17.2" />
      <line x1="64" y1="14" x2="64" y2="10" />
      <line x1="89" y1="20.7" x2="91" y2="17.2" />
      <line x1="107.3" y1="39" x2="110.8" y2="37" />
    </g>

    <!-- 血管与视盘 -->
    <path class="retina-mark__vessel" d="M50 56c-9 5-14 12-15 20" />
    <path class="retina-mark__vessel" d="M56 49c6-4 13-5 19-3" />
    <ellipse class="retina-mark__disc" cx="46" cy="58" rx="8" ry="6.4" />

    <!-- 黄斑区：明暗呼吸 -->
    <circle class="retina-mark__macula" cx="80" cy="76" r="8" :fill="maculaFill" />
    <circle cx="80" cy="76" r="2.6" fill="#0e7490" opacity="0.5" />

    <!-- 扫描线：裁剪在视野圆内，7s 往返 -->
    <g :clip-path="`url(#${clipId})`">
      <rect class="retina-mark__scan" x="12" y="18" width="104" height="1.4" :fill="scanFill" />
    </g>

    <defs>
      <radialGradient :id="gradId">
        <stop offset="0%" stop-color="#67e8f9" stop-opacity="0.95" />
        <stop offset="100%" stop-color="#0891b2" stop-opacity="0.15" />
      </radialGradient>
      <linearGradient :id="scanId" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0%" stop-color="#0891b2" stop-opacity="0" />
        <stop offset="50%" stop-color="#0891b2" stop-opacity="0.6" />
        <stop offset="100%" stop-color="#0891b2" stop-opacity="0" />
      </linearGradient>
      <clipPath :id="clipId">
        <circle cx="64" cy="64" r="56" />
      </clipPath>
    </defs>
  </svg>
</template>

<script setup lang="ts">
import { useId } from 'vue'

/**
 * 视网膜定位装饰（影像分析取景语言）。
 * 四角框 + 视野圆 + 12 刻度缓慢旋转虚线环 + 视盘/黄斑双焦点 + 扫描线。
 * 纯装饰：aria-hidden，不承载信息；动画可在 prefers-reduced-motion 下停止。
 */
withDefaults(
  defineProps<{
    size?: number
    animated?: boolean
  }>(),
  { size: 150, animated: true }
)

// 同一页面可能出现多个实例，渐变与裁剪 id 必须唯一
const uid = useId()
const gradId = `retina-macula-${uid}`
const scanId = `retina-scan-${uid}`
const clipId = `retina-clip-${uid}`
const maculaFill = `url(#${gradId})`
const scanFill = `url(#${scanId})`
</script>

<style scoped>
.retina-mark {
  display: block;
  flex-shrink: 0;
}

.retina-mark__bracket {
  fill: none;
  stroke: var(--drs-primary-300);
  stroke-width: 1.8;
  stroke-linecap: square;
  opacity: 0.9;
}

.retina-mark__ring {
  fill: none;
  stroke: var(--drs-primary-200);
  stroke-width: 1.2;
}

.retina-mark__orbit {
  fill: none;
  stroke: var(--drs-primary);
  stroke-width: 1.2;
  stroke-dasharray: 1.5 6;
  opacity: 0.5;
  transform-box: fill-box;
  transform-origin: center;
  animation: drs-spin 90s linear infinite;
}

.retina-mark__ticks line {
  stroke: var(--drs-primary-300);
  stroke-width: 1.1;
  stroke-linecap: round;
}

.retina-mark__vessel {
  fill: none;
  stroke: var(--drs-primary-300);
  stroke-width: 1.2;
  stroke-linecap: round;
  opacity: 0.6;
}

.retina-mark__disc {
  fill: var(--drs-primary-100);
  stroke: var(--drs-primary-300);
  stroke-width: 1;
}

.retina-mark__macula {
  transform-box: fill-box;
  transform-origin: center;
  animation: drs-breathe-glow 5s ease-in-out infinite;
}

.retina-mark__scan {
  --scan-distance: 80px;

  animation: drs-scan-y 7s cubic-bezier(0.45, 0, 0.55, 1) infinite;
}

/* 静态模式：保留图案，停掉全部动画（用于打印或低性能场景） */
.retina-mark.is-static .retina-mark__orbit,
.retina-mark.is-static .retina-mark__macula,
.retina-mark.is-static .retina-mark__scan {
  animation: none;
}
</style>
