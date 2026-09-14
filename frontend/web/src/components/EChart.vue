<template>
  <div ref="el" class="echart"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

const props = defineProps<{ option: EChartsOption }>()
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

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart.value?.dispose()
})
</script>

<style scoped>
.echart {
  width: 100%;
  height: 320px;
}
</style>
