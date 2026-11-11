<template>
  <span class="count-up">{{ display }}</span>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

/**
 * 数字滚动：仅用于关键汇总数字（工作台指标卡、统计页顶部汇总）。
 * 三次缓出，默认 900ms；系统开启「减少动态效果」时直接落到终值。
 */
const props = withDefaults(
  defineProps<{
    value: number
    /** 小数位，0 表示整数 */
    decimals?: number
    /** 是否千分位分组 */
    grouped?: boolean
    duration?: number
  }>(),
  { decimals: 0, grouped: true, duration: 900 }
)

const display = ref(format(props.value))
let raf = 0

function format(v: number): string {
  const fixed = props.decimals > 0 ? v.toFixed(props.decimals) : String(Math.round(v))
  if (!props.grouped) return fixed
  const [int, dec] = fixed.split('.')
  const groupedInt = int.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return dec ? `${groupedInt}.${dec}` : groupedInt
}

function prefersReduced(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function run(target: number) {
  cancelAnimationFrame(raf)
  if (prefersReduced() || props.duration <= 0) {
    display.value = format(target)
    return
  }
  const start = performance.now()
  const tick = (now: number) => {
    const p = Math.min(1, (now - start) / props.duration)
    // 三次缓出：起手快、收尾稳
    const eased = 1 - Math.pow(1 - p, 3)
    display.value = format(target * eased)
    if (p < 1) raf = requestAnimationFrame(tick)
  }
  raf = requestAnimationFrame(tick)
}

onMounted(() => run(props.value))
watch(
  () => props.value,
  (v) => run(v)
)
onBeforeUnmount(() => cancelAnimationFrame(raf))
</script>

<style scoped>
.count-up {
  font-variant-numeric: tabular-nums;
}
</style>
