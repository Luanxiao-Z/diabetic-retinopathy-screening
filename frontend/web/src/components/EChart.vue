<template>
  <div ref="el" class="echart" :style="{ height }"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

/**
 * ECharts 容器。height 支持尺寸档位（如 240px / 300px / 360px），
 * 未传入时使用默认 320px，避免各页图表高度不一致。
 */
const props = withDefaults(defineProps<{ option: EChartsOption; height?: string }>(), {
  height: '320px'
})

const el = ref<HTMLElement>()
const chart = shallowRef<echarts.ECharts>()

function render() {
  if (!el.value) return
  if (!chart.value) {
    chart.value = echarts.init(el.value)
  }
  chart.value.setOption(props.option, true)
}

function resize() {
  chart.value?.resize()
}

onMounted(() => {
  render()
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
  window.removeEventListener('resize', resize)
  chart.value?.dispose()
})
</script>

<style scoped>
.echart {
  width: 100%;
}
</style>
