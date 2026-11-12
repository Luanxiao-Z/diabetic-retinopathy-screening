<template>
  <div ref="el" class="echart" role="img" aria-label="统计图表" :style="{ height }"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
// 按需引入：全项目只用折线/饼/柱三类图，全量 echarts 会显著增大构建产物
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsOption } from 'echarts'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  CanvasRenderer
])

/**
 * ECharts 容器。height 支持尺寸档位（如 240px / 300px / 360px），
 * 未传入时使用默认 320px，避免各页图表高度不一致。
 */
const props = withDefaults(defineProps<{ option: EChartsOption; height?: string }>(), {
  height: '320px'
})

const el = ref<HTMLElement>()
const chart = shallowRef<echarts.ECharts>()
let observer: ResizeObserver | undefined

function render() {
  if (!el.value) return
  if (!chart.value) {
    chart.value = echarts.init(el.value)
  }
  /*
   * 统一动画时长与缓动，避免各页图表节奏不一：
   * 900ms + cubicOut 与条形/数字滚动的数据动效同源；页面显式传入 animation 时以页面为准。
   */
  chart.value.setOption(
    { animationDuration: 900, animationEasing: 'cubicOut', ...props.option },
    true
  )
}

function resize() {
  chart.value?.resize()
}

onMounted(() => {
  render()
  /*
   * 仅监听 window.resize 不足以覆盖「容器尺寸变化但窗口未变」的场景——
   * 例如侧栏折叠/展开、标签页切换、父容器从 display:none 恢复，
   * 这些情况下图表仍按旧宽度渲染，出现留白或溢出。
   * ResizeObserver 直接观察容器本身，可覆盖上述全部情况。
   */
  if (el.value && typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(() => resize())
    observer.observe(el.value)
  }
  window.addEventListener('resize', resize)
})

watch(
  () => props.option,
  () => render(),
  { deep: true }
)

watch(
  () => props.height,
  () => resize()
)

onBeforeUnmount(() => {
  observer?.disconnect()
  window.removeEventListener('resize', resize)
  chart.value?.dispose()
})
</script>

<style scoped>
.echart {
  width: 100%;
}
</style>
