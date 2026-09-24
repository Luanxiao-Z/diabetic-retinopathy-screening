<template>
  <svg
    class="drs-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <template v-for="(d, i) in shapes" :key="i">
      <circle v-if="d.t === 'c'" :cx="d.cx" :cy="d.cy" :r="d.r" />
      <rect v-else-if="d.t === 'r'" :x="d.x" :y="d.y" :width="d.w" :height="d.h" :rx="d.rx" />
      <line v-else-if="d.t === 'l'" :x1="d.x1" :y1="d.y1" :x2="d.x2" :y2="d.y2" />
      <polyline v-else-if="d.t === 'pl'" :points="d.points" />
      <polygon v-else-if="d.t === 'pg'" :points="d.points" />
      <path v-else :d="d.d" />
    </template>
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * 统一图标组件：24×24 线性图标集（stroke 风格，Lucide 形制）。
 * 全站图标一律经此组件取用，保证线宽、圆角、尺寸一致；不使用 emoji 作图标。
 */
type Shape =
  | { t: 'c'; cx: string; cy: string; r: string }
  | { t: 'r'; x: string; y: string; w: string; h: string; rx?: string }
  | { t: 'l'; x1: string; y1: string; x2: string; y2: string }
  | { t: 'pl'; points: string }
  | { t: 'pg'; points: string }
  | { t: 'p'; d: string }

const props = withDefaults(
  defineProps<{ name: string; size?: number; strokeWidth?: number }>(),
  { size: 18, strokeWidth: 1.8 }
)

const ICONS: Record<string, Shape[]> = {
  dashboard: [
    { t: 'r', x: '3', y: '3', w: '7', h: '9', rx: '1' },
    { t: 'r', x: '14', y: '3', w: '7', h: '5', rx: '1' },
    { t: 'r', x: '14', y: '12', w: '7', h: '9', rx: '1' },
    { t: 'r', x: '3', y: '16', w: '7', h: '5', rx: '1' }
  ],
  upload: [
    { t: 'p', d: 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4' },
    { t: 'pl', points: '17 8 12 3 7 8' },
    { t: 'l', x1: '12', y1: '3', x2: '12', y2: '15' }
  ],
  list: [
    { t: 'l', x1: '8', y1: '6', x2: '21', y2: '6' },
    { t: 'l', x1: '8', y1: '12', x2: '21', y2: '12' },
    { t: 'l', x1: '8', y1: '18', x2: '21', y2: '18' },
    { t: 'l', x1: '3', y1: '6', x2: '3.01', y2: '6' },
    { t: 'l', x1: '3', y1: '12', x2: '3.01', y2: '12' },
    { t: 'l', x1: '3', y1: '18', x2: '3.01', y2: '18' }
  ],
  chart: [
    { t: 'p', d: 'M3 3v18h18' },
    { t: 'p', d: 'M18 17V9' },
    { t: 'p', d: 'M13 17V5' },
    { t: 'p', d: 'M8 17v-3' }
  ],
  trend: [
    { t: 'pl', points: '22 7 13.5 15.5 8.5 10.5 2 17' },
    { t: 'pl', points: '16 7 22 7 22 13' }
  ],
  scan: [
    { t: 'p', d: 'M3 7V5a2 2 0 0 1 2-2h2' },
    { t: 'p', d: 'M17 3h2a2 2 0 0 1 2 2v2' },
    { t: 'p', d: 'M21 17v2a2 2 0 0 1-2 2h-2' },
    { t: 'p', d: 'M7 21H5a2 2 0 0 1-2-2v-2' },
    { t: 'p', d: 'M7 12s2-3 5-3 5 3 5 3-2 3-5 3-5-3-5-3Z' },
    { t: 'c', cx: '12', cy: '12', r: '1.4' }
  ],
  activity: [{ t: 'p', d: 'M22 12h-4l-3 9L9 3l-3 9H2' }],
  user: [
    { t: 'p', d: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' },
    { t: 'c', cx: '12', cy: '7', r: '4' }
  ],
  users: [
    { t: 'p', d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' },
    { t: 'c', cx: '9', cy: '7', r: '4' },
    { t: 'p', d: 'M22 21v-2a4 4 0 0 0-3-3.87' },
    { t: 'p', d: 'M16 3.13a4 4 0 0 1 0 7.75' }
  ],
  logout: [
    { t: 'p', d: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4' },
    { t: 'pl', points: '16 17 21 12 16 7' },
    { t: 'l', x1: '21', y1: '12', x2: '9', y2: '12' }
  ],
  chevronDown: [{ t: 'pl', points: '6 9 12 15 18 9' }],
  chevronLeft: [{ t: 'pl', points: '15 18 9 12 15 6' }],
  chevronRight: [{ t: 'pl', points: '9 18 15 12 9 6' }],
  arrowRight: [
    { t: 'l', x1: '5', y1: '12', x2: '19', y2: '12' },
    { t: 'pl', points: '12 5 19 12 12 19' }
  ],
  search: [
    { t: 'c', cx: '11', cy: '11', r: '7' },
    { t: 'l', x1: '20.5', y1: '20.5', x2: '16.2', y2: '16.2' }
  ],
  refresh: [
    { t: 'p', d: 'M3 12a9 9 0 0 1 9-9 9.7 9.7 0 0 1 6.7 2.7L21 8' },
    { t: 'pl', points: '21 3 21 8 16 8' },
    { t: 'p', d: 'M21 12a9 9 0 0 1-9 9 9.7 9.7 0 0 1-6.7-2.7L3 16' },
    { t: 'pl', points: '8 16 3 16 3 21' }
  ],
  download: [
    { t: 'p', d: 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4' },
    { t: 'pl', points: '7 10 12 15 17 10' },
    { t: 'l', x1: '12', y1: '15', x2: '12', y2: '3' }
  ],
  trash: [
    { t: 'pl', points: '3 6 5 6 21 6' },
    { t: 'p', d: 'M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2' }
  ],
  eye: [
    { t: 'p', d: 'M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z' },
    { t: 'c', cx: '12', cy: '12', r: '3' }
  ],
  shield: [
    { t: 'p', d: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z' },
    { t: 'p', d: 'm9 12 2 2 4-4' }
  ],
  database: [
    { t: 'p', d: 'M3 5v14a9 3 0 0 0 18 0V5' },
    { t: 'p', d: 'M3 12a9 3 0 0 0 18 0' },
    { t: 'c', cx: '12', cy: '5', r: '9' }
  ],
  alert: [
    { t: 'p', d: 'm21.7 18-8-14a2 2 0 0 0-3.5 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3Z' },
    { t: 'l', x1: '12', y1: '9', x2: '12', y2: '13' },
    { t: 'l', x1: '12', y1: '17', x2: '12.01', y2: '17' }
  ],
  check: [
    { t: 'p', d: 'M22 11.1V12a10 10 0 1 1-5.9-9.1' },
    { t: 'pl', points: '22 4 12 14 9 11' }
  ],
  clock: [
    { t: 'c', cx: '12', cy: '12', r: '9' },
    { t: 'pl', points: '12 7 12 12 15 14' }
  ],
  calendar: [
    { t: 'r', x: '3', y: '5', w: '18', h: '17', rx: '2' },
    { t: 'l', x1: '16', y1: '3', x2: '16', y2: '7' },
    { t: 'l', x1: '8', y1: '3', x2: '8', y2: '7' },
    { t: 'l', x1: '3', y1: '10', x2: '21', y2: '10' }
  ],
  image: [
    { t: 'r', x: '3', y: '3', w: '18', h: '18', rx: '2' },
    { t: 'c', cx: '9', cy: '9', r: '2' },
    { t: 'p', d: 'm21 15-3.1-3.1a2 2 0 0 0-2.8 0L6 21' }
  ],
  filter: [{ t: 'pg', points: '22 3 2 3 10 12.5 10 19 14 21 14 12.5 22 3' }],
  info: [
    { t: 'c', cx: '12', cy: '12', r: '9' },
    { t: 'l', x1: '12', y1: '16', x2: '12', y2: '12' },
    { t: 'l', x1: '12', y1: '8', x2: '12.01', y2: '8' }
  ],
  lock: [
    { t: 'r', x: '3', y: '11', w: '18', h: '10', rx: '2' },
    { t: 'p', d: 'M7 11V7a5 5 0 0 1 10 0v4' }
  ],
  menu: [
    { t: 'l', x1: '3', y1: '6', x2: '21', y2: '6' },
    { t: 'l', x1: '3', y1: '12', x2: '21', y2: '12' },
    { t: 'l', x1: '3', y1: '18', x2: '21', y2: '18' }
  ],
  stethoscope: [
    { t: 'p', d: 'M4 3v6a5 5 0 0 0 10 0V3' },
    { t: 'p', d: 'M9 14v2a5 5 0 0 0 10 0v-2' },
    { t: 'c', cx: '19', cy: '11', r: '2' }
  ],
  refreshClock: [
    { t: 'c', cx: '12', cy: '12', r: '9' },
    { t: 'pl', points: '12 8 12 12 15 14' }
  ],
  layers: [
    { t: 'pg', points: '12 2 2 7 12 12 22 7 12 2' },
    { t: 'pl', points: '2 17 12 22 22 17' },
    { t: 'pl', points: '2 12 12 17 22 12' }
  ],
  hospital: [
    { t: 'p', d: 'M3 21h18' },
    { t: 'p', d: 'M5 21V7l7-4 7 4v14' },
    { t: 'l', x1: '12', y1: '9', x2: '12', y2: '15' },
    { t: 'l', x1: '9', y1: '12', x2: '15', y2: '12' }
  ],
  book: [
    { t: 'p', d: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20' },
    { t: 'p', d: 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z' },
    { t: 'l', x1: '9', y1: '7', x2: '16', y2: '7' }
  ],
  inbox: [
    { t: 'pl', points: '22 12 16 12 14 15 10 15 8 12 2 12' },
    { t: 'p', d: 'M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11Z' }
  ]
}

const shapes = computed<Shape[]>(() => ICONS[props.name] || ICONS.info)
</script>

<style scoped>
.drs-icon {
  display: block;
  flex-shrink: 0;
}
</style>
